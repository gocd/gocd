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


require File.expand_path(File.join(File.dirname(__FILE__), 'spec_helpers'))

describe Buildr::VersionRequirement, '.create' do
  def create(str)
    Buildr::VersionRequirement.create(str)
  end

  it 'should complain on invalid input' do
    lambda { create }.should raise_error(Exception)
    lambda { create('%') }.should raise_error(Exception, /invalid character/)
    lambda { create('1#{0}') }.should raise_error(Exception, /invalid character/)
    lambda { create('1.0rc`exit`') }.should raise_error(Exception, /invalid character/)
    lambda { create(1.0) }.should raise_error(Exception)
    lambda { create('1.0') }.should_not raise_error(Exception)
    lambda { create('1.0rc3') }.should_not raise_error(Exception)
  end

  it 'should allow versions using hyphen' do
    lambda { create('1.0-rc3') }.should_not raise_error(Exception)
  end

  it 'should create a single version requirement' do
    create('1.0').should_not be_composed
  end

  it 'should create a composed version requirement' do
    create('1.0 | 2.1').should be_composed
  end
end

=begin
# TODO: Fix this.
# 1.  Can't use should_satisfy, this breaks under RSpec 1.2
# 2.  These should_satisfy calls are not proper specs since the subject is
#     the satistifed_by? method. satisfied_by should satisfy???
describe Buildr::VersionRequirement, '#satisfied_by?' do
  def should_satisfy(str, valids = [], invalids = [])
    req = Buildr::VersionRequirement.create(str)
    valids.each { |v| req.should be_satisfied_by(v) }
    invalids.each { |v| req.should_not be_satisfied_by(v) }
  end

  it 'should accept Gem version operators' do
    should_satisfy '1.0', %w(1 1.0), %w(1.1 0.1)
    should_satisfy '=1.0', %w(1 1.0), %w(1.1 0.1)
    should_satisfy '= 1.0', %w(1 1.0), %w(1.1 0.1)
    should_satisfy '!= 1.0', %w(0.9 1.1 2), %w(1 1.0 1.0.0)

    should_satisfy '>1.0', %w(1.0.1), %w(1 1.0 0.1)
    should_satisfy '>=1.0', %w(1.0.1 1 1.0), %w(0.9)

    should_satisfy '<1.0', %w(0.9 0.9.9), %w(1 1.0 1.1 2)
    should_satisfy '<=1.0', %w(0.9 0.9.9 1 1.0), %w(1.1 2)

    should_satisfy '~> 1.2.3', %w(1.2.3 1.2.3.4 1.2.4), %w(1.2.1 0.9 1.4 2)
  end

  it 'should accept logic not operator' do
    should_satisfy 'not 0.5', %w(0 1), %w(0.5)
    should_satisfy '!  0.5', %w(0 1), %w(0.5)
    should_satisfy '!= 0.5', %w(0 1), %w(0.5)
    should_satisfy '!<= 0.5', %w(0.5.1 2), %w(0.5)
  end

  it 'should accept logic or operator' do
    should_satisfy '0.5 or 2.0', %w(0.5 2.0), %w(1.0 0.5.1 2.0.9)
    should_satisfy '0.5 | 2.0', %w(0.5 2.0), %w(1.0 0.5.1 2.0.9)
  end

  it 'should accept logic and operator' do
    should_satisfy '>1.5 and <2.0', %w(1.6 1.9), %w(1.5 2 2.0)
    should_satisfy '>1.5 & <2.0', %w(1.6 1.9), %w(1.5 2 2.0)
  end

  it 'should assume logic and if missing operator between expressions' do
    should_satisfy '>1.5 <2.0', %w(1.6 1.9), %w(1.5 2 2.0)
  end

  it 'should allow combining logic operators' do
    should_satisfy '>1.0 | <2.0 | =3.0', %w(1.5 3.0 1 2 4)
    should_satisfy '>1.0 & <2.0 | =3.0', %w(1.3 3.0), %w(1 2)
    should_satisfy '=1.0 | <2.0 & =0.5', %w(0.5 1.0), %w(1.1 0.1 2)
    should_satisfy '~>1.1 | ~>1.3 | ~>1.5 | 2.0', %w(2 1.5.6 1.1.2 1.1.3), %w(1.0.9 0.5 2.2.1)
    should_satisfy 'not(2) | 1', %w(1 3), %w(2)
  end

  it 'should allow using parens to group logic expressions' do
    should_satisfy '(1.0)', %w(1 1.0), %w(0.9 1.1)
    should_satisfy '!( !(1.0) )', %w(1 1.0), %w(0.9 1.1)
    should_satisfy '1 | !(2 | 3)', %w(1), %w(2 3)
    should_satisfy '!(2 | 3) | 1', %w(1), %w(2 3)
  end
end
=end

describe Buildr::VersionRequirement, '#default' do
  it 'should return nil if missing default requirement' do
    Buildr::VersionRequirement.create('>1').default.should be_nil
    Buildr::VersionRequirement.create('<1').default.should be_nil
    Buildr::VersionRequirement.create('!1').default.should be_nil
    Buildr::VersionRequirement.create('!<=1').default.should be_nil
  end

  it 'should return the last version with a = requirement' do
    Buildr::VersionRequirement.create('1').default.should == '1'
    Buildr::VersionRequirement.create('=1').default.should == '1'
    Buildr::VersionRequirement.create('<=1').default.should == '1'
    Buildr::VersionRequirement.create('>=1').default.should == '1'
    Buildr::VersionRequirement.create('1 | 2 | 3').default.should == '3'
    Buildr::VersionRequirement.create('1 2 | 3').default.should == '3'
    Buildr::VersionRequirement.create('1 & 2 | 3').default.should == '3'
  end
end

describe Buildr::VersionRequirement, '#version?' do
  it 'should identify valid versions' do
    Buildr::VersionRequirement.version?('1').should be_true
    Buildr::VersionRequirement.version?('1a').should be_true
    Buildr::VersionRequirement.version?('1.0').should be_true
    Buildr::VersionRequirement.version?('11.0').should be_true
    Buildr::VersionRequirement.version?(' 11.0 ').should be_true
    Buildr::VersionRequirement.version?('11.0-alpha').should be_true
    Buildr::VersionRequirement.version?('r09').should be_true # BUILDR-615: com.google.guava:guava:jar:r09

    Buildr::VersionRequirement.version?('a').should be_false
    Buildr::VersionRequirement.version?('a1').should be_false
    Buildr::VersionRequirement.version?('r').should be_false
  end
end
