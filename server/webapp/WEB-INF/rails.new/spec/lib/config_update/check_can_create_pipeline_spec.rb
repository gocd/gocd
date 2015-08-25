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

describe ConfigUpdate::CheckCanCreatePipeline do
  include ::ConfigUpdate::CheckCanCreatePipeline

  before do
    allow(self).to receive(:params).and_return(@params = {})
    @security_service = double("security_service") #Instance variable because the module expects this to be defined
    @user_helper = double("user_helper") #Instance variable because the module expects this to be defined
    @user = Username.new(CaseInsensitiveString.new('loser')) #Instance variable because the module expects this to be defined
  end

  it "should return 401 if user is not a group admin" do
    cruise_config = GoConfigMother.configWithPipelines(["his-pipeline", "my-pipeline", "her-pipeline"].to_java(java.lang.String))
    result = HttpLocalizedOperationResult.new
    @params[:pipeline_group] = {:group => PipelineConfigs::DEFAULT_GROUP}
    @security_service.should_receive(:isUserAdminOfGroup).with(CaseInsensitiveString.new("loser"), PipelineConfigs::DEFAULT_GROUP).and_return(false)

    checkPermission(cruise_config, result)

    result.isSuccessful().should be_false
    result.httpCode().should == 401
    result.message(Spring.bean("localizer")).should == "Unauthorized to create pipeline."
    end

  it "should return 401 if user is a normal user and group does not exist" do
    cruise_config = GoConfigMother.configWithPipelines(["his-pipeline", "my-pipeline", "her-pipeline"].to_java(java.lang.String))
    result = HttpLocalizedOperationResult.new
    @params[:pipeline_group] = {:group => "some_junk_group"}
    @security_service.should_receive(:isUserAdminOfGroup).with(CaseInsensitiveString.new("loser"), "some_junk_group").never
    @security_service.should_receive(:isUserAdmin).with(@user).and_return(false)

    checkPermission(cruise_config, result)

    result.isSuccessful().should be_false
    result.httpCode().should == 401
    result.message(Spring.bean("localizer")).should == "Unauthorized to create pipeline."
  end

  it "should return 401 if user is an admin but no group_name given(happens when using community edition)" do
    cruise_config = GoConfigMother.configWithPipelines(["his-pipeline", "my-pipeline", "her-pipeline"].to_java(java.lang.String))
    result = HttpLocalizedOperationResult.new
    @security_service.should_receive(:isUserAdminOfGroup).with(CaseInsensitiveString.new("loser"), PipelineConfigs::DEFAULT_GROUP).and_return(false)

    checkPermission(cruise_config, result)

    result.isSuccessful().should be_false
    result.httpCode().should == 401
    result.message(Spring.bean("localizer")).should == "Unauthorized to create pipeline."
  end

  it "should return 401 if user is not a group admin for 'defaultGroup' and group name is empty" do
    cruise_config = GoConfigMother.configWithPipelines(["his-pipeline", "my-pipeline", "her-pipeline"].to_java(java.lang.String))
    result = HttpLocalizedOperationResult.new
    @params[:pipeline_group] = {:group => ""}
    @security_service.should_receive(:isUserAdminOfGroup).with(CaseInsensitiveString.new("loser"), PipelineConfigs::DEFAULT_GROUP).and_return(false)

    checkPermission(cruise_config, result)

    result.isSuccessful().should be_false
    result.httpCode().should == 401
    result.message(Spring.bean("localizer")).should == "Unauthorized to create pipeline."
  end

  it "should return 401 if user tries to create 'defaultGroup' and is not an admin" do
    cruise_config = GoConfigMother.new.cruiseConfigWithPipelineUsingTwoMaterials()
    result = HttpLocalizedOperationResult.new
    @params[:pipeline_group] = {:group => ""}
    @security_service.should_receive(:isUserAdmin).with(@user).and_return(false)

    checkPermission(cruise_config, result)

    result.isSuccessful().should be_false
    result.httpCode().should == 401
    result.message(Spring.bean("localizer")).should == "Unauthorized to create pipeline."
  end

  it "should return successful result if user is a group admin of given pipeline" do
    cruise_config = GoConfigMother.configWithPipelines(["his-pipeline", "my-pipeline", "her-pipeline"].to_java(java.lang.String))
    result = HttpLocalizedOperationResult.new
    @params[:pipeline_group] = {:group => PipelineConfigs::DEFAULT_GROUP}
    @security_service.should_receive(:isUserAdminOfGroup).with(CaseInsensitiveString.new("loser"), PipelineConfigs::DEFAULT_GROUP).and_return(true)

    checkPermission(cruise_config, result)

    result.isSuccessful().should be_true
  end

  it "should return successful result if user is a super admin and group does not exist" do
    cruise_config = GoConfigMother.configWithPipelines(["his-pipeline", "my-pipeline", "her-pipeline"].to_java(java.lang.String))
    result = HttpLocalizedOperationResult.new
    @params[:pipeline_group] = {:group => "some_junk_group"}
    @security_service.should_receive(:isUserAdminOfGroup).with(CaseInsensitiveString.new("loser"), "some_junk_group").never
    @security_service.should_receive(:isUserAdmin).with(@user).and_return(true)

    checkPermission(cruise_config, result)

    result.isSuccessful().should be_true
  end
end
