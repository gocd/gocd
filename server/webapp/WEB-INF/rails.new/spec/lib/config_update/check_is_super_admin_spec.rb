##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##########################GO-LICENSE-END##################################

require 'spec_helper'

describe ConfigUpdate::CheckIsSuperAdmin do
  include ::ConfigUpdate::CheckIsSuperAdmin

  before do
    allow(self).to receive(:params).and_return(@params = {})
    @security_service = double("security_service") #Instance variable because the module expects this to be defined
    @user = "loser" #Instance variable because the module expects this to be defined
  end

  it "should return unsuccessful result if user is not an admin" do
    cruise_config = GoConfigMother.configWithPipelines(["pipeline"].to_java(java.lang.String))
    result = HttpLocalizedOperationResult.new
    @security_service.should_receive(:isUserAdmin).with("loser").and_return(false)

    checkPermission(cruise_config, result)

    result.isSuccessful().should be_false
    result.httpCode().should == 401
    result.message(Spring.bean("localizer")).should == "Unauthorized to edit configuration"
  end

  it "should return successful if user is an admin" do
    cruise_config = GoConfigMother.configWithPipelines(["pipeline"].to_java(java.lang.String))
    result = HttpLocalizedOperationResult.new
    @security_service.should_receive(:isUserAdmin).with("loser").and_return(true)

    checkPermission(cruise_config, result)

    result.isSuccessful().should be_true
    result.httpCode().should == 200
  end

end
