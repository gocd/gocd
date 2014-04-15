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


describe Buildr::Ant do
  
  it 'should pick Ant version from ant build settings' do
    Buildr::Ant.instance_eval { @dependencies = nil }
    write 'build.yaml', 'ant: 1.2.3'
    Buildr::Ant.dependencies.should include("org.apache.ant:ant:jar:1.2.3")
  end
  
  it 'should have REQUIRES up to version 1.5 since it was deprecated in version 1.3.3' do
    Buildr::VERSION.should < '1.5'
    lambda { Ant::REQUIRES }.should_not raise_error
  end

end
