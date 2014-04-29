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

describe Buildr do
  describe "#replace_extension" do
    it "should replace filename extensions" do
      replace = lambda { |filename, ext| Util.replace_extension(filename, ext) }

      replace["foo.zip", "txt"].should eql("foo.txt")
      replace["foo.", "txt"].should eql("foo.txt")
      replace["foo", "txt"].should eql("foo.txt")

      replace["bar/foo.zip", "txt"].should eql("bar/foo.txt")
      replace["bar/foo.", "txt"].should eql("bar/foo.txt")
      replace["bar/foo", "txt"].should eql("bar/foo.txt")
    end
  end
end

describe Hash do
  describe "#only" do
    it "should find value for one key" do
      {:a => 1, :b => 2, :c => 3}.only(:a).should == {:a => 1}
    end

    it "should find values for multiple keys" do
      {:a => 1, :b => 2, :c => 3}.only(:b, :c).should == {:b => 2, :c => 3}
    end
  end
end

describe OpenObject do
  before do
    @obj = OpenObject.new({:a => 1, :b => 2, :c => 3})
  end

  it "should be kind of Hash" do
    Hash.should === @obj
  end

  it "should accept block that supplies default value" do
    obj = OpenObject.new { |hash, key| hash[key] = "New #{key}" }
    obj[:foo].should == "New foo"
    obj.keys.should == [:foo]
  end

  it "should combine initial values from hash argument and from block" do
    obj = OpenObject.new(:a => 6, :b => 2) { |h, k| h[k] = k.to_s * 2 }
    obj[:a].should == 6
    obj[:c].should == 'cc'
  end

  it "should allow reading a value by calling its name method" do
    @obj.b.should == 2
  end

  it "should allow setting a value by calling its name= method" do
    lambda { @obj.f = 32 }.should change { @obj.f }.to(32)
  end

  it "should allow changing a value by calling its name= method" do
    lambda { @obj.c = 17 }.should change { @obj.c }.to(17)
  end

  it "should implement only method like a hash" do
    @obj.only(:a).should == { :a => 1 }
  end
end

describe File do
  # Quite a few of the other specs depend on File#utime working correctly.
  # These specs validate that utime is working as expected.
  describe "#utime" do
    it "should update mtime of directories" do
      mkpath 'tmp'
      begin
        creation_time = File.mtime('tmp')

        sleep 1
        File.utime(nil, nil, 'tmp')

        File.mtime('tmp').should > creation_time
      ensure
        Dir.rmdir 'tmp'
      end
    end

    it "should update mtime of files" do
      FileUtils.touch('tmp')
      begin
        creation_time = File.mtime('tmp')

        sleep 1
        File.utime(nil, nil, 'tmp')

        File.mtime('tmp').should > creation_time
      ensure
        File.delete 'tmp'
      end
    end

    it "should be able to set mtime in the past" do
      FileUtils.touch('tmp')
      begin
        time = Time.at((Time.now - 10).to_i)
        File.utime(time, time, 'tmp')

        File.mtime('tmp').should == time
      ensure
        File.delete 'tmp'
      end
    end

    it "should be able to set mtime in the future" do
      FileUtils.touch('tmp')
      begin
        time = Time.at((Time.now + 10).to_i)
        File.utime(time, time, 'tmp')

        File.mtime('tmp').should == time
      ensure
        File.delete 'tmp'
      end
    end
  end
end
