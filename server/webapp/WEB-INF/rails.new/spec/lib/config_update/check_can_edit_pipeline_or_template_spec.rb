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

describe ConfigUpdate::CheckCanEditPipelineOrTemplate do
  include ::ConfigUpdate::CheckCanEditPipelineOrTemplate

  before do
    allow(self).to receive(:params).and_return(@params = {})
    @security_service = double("security_service") #Instance variable because the module expects this to be defined
    @user_helper = double("user_helper") #Instance variable because the module expects this to be defined
    @user = CaseInsensitiveString.new("loser") #Instance variable because the module expects this to be defined
  end

  it "should return unsuccessful result if user is not a group admin of given pipeline" do
    cruise_config = GoConfigMother.configWithPipelines(["his-pipeline", "my-pipeline", "her-pipeline"].to_java(java.lang.String))
    result = HttpLocalizedOperationResult.new
    @params[:pipeline_name] = "my-pipeline"
    @params[:stage_parent] = "pipelines"
    @security_service.should_receive(:isUserAdminOfGroup).with(CaseInsensitiveString.new("loser"), PipelineConfigs::DEFAULT_GROUP).and_return(false)

    checkPermission(cruise_config, result)

    result.isSuccessful().should be_false
    result.httpCode().should == 401
    result.message(Spring.bean("localizer")).should == "Unauthorized to edit my-pipeline pipeline."
  end

  it "should return successful result if user is a group admin of given pipeline" do
    cruise_config = GoConfigMother.configWithPipelines(["his-pipeline", "my-pipeline", "her-pipeline"].to_java(java.lang.String))
    result = HttpLocalizedOperationResult.new
    @params[:pipeline_name] = "my-pipeline"
    @params[:stage_parent] = "pipelines"
    @security_service.should_receive(:isUserAdminOfGroup).with(CaseInsensitiveString.new("loser"), PipelineConfigs::DEFAULT_GROUP).and_return(true)

    checkPermission(cruise_config, result)

    result.isSuccessful().should be_true
    end

   it "should return successful result when user is trying to edit a template and user is template admin or super admin" do
    cruise_config = GoConfigMother.configWithPipelines(["his-pipeline", "my-pipeline", "her-pipeline"].to_java(java.lang.String))
    result = HttpLocalizedOperationResult.new
    @params[:pipeline_name] = "my-pipeline"
    @params[:stage_parent] = "templates"
    @security_service.should_receive(:isAuthorizedToEditTemplate).with("my-pipeline", Username.new(CaseInsensitiveString.new("anonymous"))).and_return(true)

    checkPermission(cruise_config, result)

    result.isSuccessful().should be_true
   end

  it "should return unsuccessful result editing a template and user is not super admin" do
    cruise_config = GoConfigMother.configWithPipelines(["his-pipeline", "my-pipeline", "her-pipeline"].to_java(java.lang.String))
    result = HttpLocalizedOperationResult.new
    @params[:pipeline_name] = "my-pipeline"
    @params[:stage_parent] = "templates"
    @security_service.should_receive(:isAuthorizedToEditTemplate).with("my-pipeline", Username.new(CaseInsensitiveString.new("anonymous"))).and_return(false)

    checkPermission(cruise_config, result)

    result.isSuccessful().should be_false
    result.httpCode().should == 401
    result.message(Spring.bean("localizer")).should == "Unauthorized to edit my-pipeline pipeline."
  end

  it "should give a generic unauthorized message when pipeline name and group name are not available" do
    cruise_config = GoConfigMother.configWithPipelines(["his-pipeline", "my-pipeline", "her-pipeline"].to_java(java.lang.String))
    result = HttpLocalizedOperationResult.new
    @params[:stage_parent] = "templates"
    @security_service.should_receive(:isAuthorizedToEditTemplate).with(nil, Username.new(CaseInsensitiveString.new("anonymous"))).and_return(false)

    checkPermission(cruise_config, result)

    result.isSuccessful().should be_false
    result.httpCode().should == 401
    result.message(Spring.bean("localizer")).should == "Unauthorized to edit."
  end

  it "should return unsuccessful result if user is not a group admin of given pipeline group" do
    cruise_config = GoConfigMother.configWithPipelines(["his-pipeline", "my-pipeline", "her-pipeline"].to_java(java.lang.String))
    result = HttpLocalizedOperationResult.new
    @params[:group_name] = "my-pipeline-group"
    @params[:stage_parent] = "templates"
    @security_service.should_receive(:isAuthorizedToEditTemplate).with(nil, Username.new(CaseInsensitiveString.new("anonymous"))).and_return(false)

    checkPermission(cruise_config, result)

    result.isSuccessful().should be_false
    result.httpCode().should == 401
    result.message(Spring.bean("localizer")).should == "Unauthorized to edit 'my-pipeline-group' group."
  end
end
