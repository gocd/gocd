# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with this
# work for additional information regarding copyright ownership.  The ASF
# licenses this file to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations under
# the License.


require File.expand_path(File.join(File.dirname(__FILE__), '..', 'spec_helpers'))


describe Extension do

  before do
    @saved_modules   = Project.class_eval { @extension_modules }.dup
    @saved_callbacks = Project.class_eval { @global_callbacks }.dup
  end

  after do
    modules   = @saved_modules
    callbacks = @saved_callbacks
    Project.class_eval do
      @global_callbacks  = callbacks
      @extension_modules = modules
    end
  end

  it 'should call Extension.first_time during include' do
    TestExtension.should_receive(:first_time_called).once
    class Buildr::Project
      include TestExtension
    end
  end

  it 'should call before_define and after_define in order when project is defined' do
    begin
      TestExtension.callback do |extension|
        extension.should_receive(:before_define_called).once.ordered
        extension.should_receive(:after_define_called).once.ordered
      end
      class Buildr::Project
        include TestExtension
      end
      define('foo')
    ensure
      TestExtension.callback { |ignore| }
    end
  end

  it 'should call before_define and after_define for each project defined' do
    begin
      extensions = 0
      TestExtension.callback do |extension|
        extensions += 1
        extension.should_receive(:before_define_called).once.ordered
        extension.should_receive(:after_define_called).once.ordered
      end
      class Buildr::Project
        include TestExtension
      end
      define('foo')
      define('bar')
      extensions.should equal(2)
    ensure
      TestExtension.callback { |ignore| }
    end
  end

  it 'should call before_define callbacks in dependency order' do
    class Buildr::Project
      include ExtensionOneTwo
      include ExtensionThreeFour
    end
    define('foo')
    project('foo').before_order.should == [ :one, :two, :three, :four ]
    project('foo').after_order.should  == [ :four, :three, :two, :one ]
  end

  it 'should call before_define callbacks when extending project' do
    define('foo') do
      extend ExtensionOneTwo
      extend ExtensionThreeFour
    end
    project('foo').before_order.should == [ :two, :one, :four, :three ]
    project('foo').after_order.should  == [ :four, :three, :two, :one ]
  end

  it 'should raise error when including if callback dependencies cannot be satisfied' do
    class Buildr::Project
      include ExtensionOneTwo # missing ExtensionThreeFour
    end
    lambda { define('foo') }.should raise_error
  end

  it 'should raise error when extending if callback dependencies cannot be satisfied' do
    lambda {
      define('foo') do |project|
        extend ExtensionOneTwo # missing ExtensionThreeFour
      end
    }.should raise_error
  end

  it 'should ignore dependencies when extending project' do
    define('bar') do |project|
      extend ExtensionThreeFour # missing ExtensionOneTwo
    end
    project('bar').before_order.should == [:four, :three]
    project('bar').after_order.should == [:four, :three]
  end
end

module TestExtension
  include Extension

  def initialize(*args)
    super
    # callback is used to obtain extension instance created by buildr
    @@callback.call(self) if @@callback
  end

  def self.callback(&block)
    @@callback = block
  end

  first_time do
    self.first_time_called()
  end

  before_define do |project|
    project.before_define_called()
  end

  after_define do |project|
    project.after_define_called()
  end

  def self.first_time_called()
  end

end

module BeforeAfter
  def before_order
    @before_order ||= []
  end

  def after_order
    @after_order ||= []
  end
end

module ExtensionOneTwo
  include Extension, BeforeAfter

  before_define(:two => :one) do |project|
    project.before_order << :two
  end

  before_define(:one) do |project|
    project.before_order << :one
  end

  after_define(:one => :two) do |project|
    project.after_order << :one
  end

  after_define(:two => :three) do |project|
    project.after_order << :two
  end
end

module ExtensionThreeFour
  include Extension, BeforeAfter

  before_define(:three => :two)

  before_define(:four => :three) do |project|
    project.before_order << :four
  end

  before_define(:three) do |project|
    project.before_order << :three
  end

  after_define(:three => :four) do |project|
    project.after_order << :three
  end

  after_define(:four) do |project|
    project.after_order << :four
  end
end

