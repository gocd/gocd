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


require File.join(File.dirname(__FILE__), '../spec_helpers')


unless RUBY_PLATFORM =~ /java/
  describe ENV, 'JAVA_HOME on OS X' do
    before do
      @old_home, ENV['JAVA_HOME'] = ENV['JAVA_HOME'], nil
      @old_env_java = Object.module_eval { remove_const :ENV_JAVA }
      Config::CONFIG.should_receive(:[]).with('host_os').and_return('darwin0.9')
    end

    it 'should point to default JVM' do
      load File.expand_path('../lib/buildr/java/rjb.rb')
      ENV['JAVA_HOME'].should == '/System/Library/Frameworks/JavaVM.framework/Home'
    end

    it 'should use value of environment variable if specified' do
      ENV['JAVA_HOME'] = '/System/Library/Frameworks/JavaVM.specified'
      load File.expand_path('../lib/buildr/java/rjb.rb')
      ENV['JAVA_HOME'].should == '/System/Library/Frameworks/JavaVM.specified'
    end

    after do
      ENV['JAVA_HOME'] = @old_home
      ENV_JAVA.replace @old_env_java
    end
  end
end


describe Java, '#tools_jar' do
  before do
    @old_home = ENV['JAVA_HOME']
  end
  
  describe 'when JAVA_HOME points to a JDK' do
    before do
      Java.instance_eval { @tools_jar = nil }
      write 'jdk/lib/tools.jar'
      ENV['JAVA_HOME'] = File.expand_path('jdk')
    end
  
    it 'should return the path to tools.jar' do
      Java.tools_jar.should point_to_path('jdk/lib/tools.jar')
    end
  end
  
  describe 'when JAVA_HOME points to a JRE inside a JDK' do
    before do
      Java.instance_eval { @tools_jar = nil }
      write 'jdk/lib/tools.jar'
      ENV['JAVA_HOME'] = File.expand_path('jdk/jre')
    end
    
    it 'should return the path to tools.jar' do
      Java.tools_jar.should point_to_path('jdk/lib/tools.jar')
    end
  end
  
  describe 'when there is no tools.jar' do
    before do
      Java.instance_eval { @tools_jar = nil }
      ENV['JAVA_HOME'] = File.expand_path('jdk')
    end
    
    it 'should return nil' do
      Java.tools_jar.should be_nil
    end
  end
  
  after do
    ENV['JAVA_HOME'] = @old_home
  end
end


describe Java::JavaWrapper do
  it 'should be removed in version 1.5 since it was deprecated in version 1.3' do
    Buildr::VERSION.should < '1.5'
    lambda { Java::JavaWrapper }.should_not raise_error
  end
end
