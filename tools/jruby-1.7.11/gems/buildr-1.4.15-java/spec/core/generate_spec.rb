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


describe Buildr::Generate do

  describe 'Generated buildfile' do
    it 'should be a legal buildfile' do
      File.open('buildfile', 'w') { |file| file.write Generate.from_directory(Dir.pwd).join("\n") }
      lambda { Buildr.application.run }.should_not raise_error
    end

    it 'should not contain NEXT_VERSION because it was removed in buildr 1.3.3' do
      buildfile = Generate.from_directory(Dir.pwd)
      buildfile.each { |line| line.should_not include('NEXT_VERSION')}
    end
  end
end
