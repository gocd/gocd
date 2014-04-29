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

describe 'scala' do

  it 'should automatically add the remote oss.sonatype.org repository' do
    # NOTE: the sandbox environment clears "repositories.remote" so we can't
    #       test for this spec right now.
    #
    # repositories.remote.should include('http://oss.sonatype.org/content/repositories/releases')
  end

  it "should provide the Scala version string" do
    Scala.version_str.should eql(Buildr::Scala::SCALA_VERSION_FOR_SPECS)
  end
end
