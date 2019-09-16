#
# Copyright 2019 ThoughtWorks, Inc.
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
#

require 'rails_helper'

describe ConfigUpdate::CheckCanCreatePipeline, :type => :helper do
  include ::ConfigUpdate::CheckCanCreatePipeline
  def params
    @params
  end

  before do
    @params = {}
    @security_service = double("security_service") #Instance variable because the module expects this to be defined
    @user_helper = double("user_helper") #Instance variable because the module expects this to be defined
    @user = Username.new(CaseInsensitiveString.new('loser')) #Instance variable because the module expects this to be defined
  end

  it "should return 403 if user is not a group admin" do
    cruise_config = GoConfigMother.configWithPipelines(["his-pipeline", "my-pipeline", "her-pipeline"].to_java(java.lang.String))
    result = HttpLocalizedOperationResult.new
    def params
      {:pipeline_group => {:group => PipelineConfigs::DEFAULT_GROUP}}
    end
    expect(@security_service).to receive(:isUserAdminOfGroup).with(CaseInsensitiveString.new("loser"), PipelineConfigs::DEFAULT_GROUP).and_return(false)

    checkPermission(cruise_config, result)

    expect(result.isSuccessful()).to be_falsey
    expect(result.httpCode()).to eq(403)
    expect(result.message()).to eq("Unauthorized to create pipeline.")
    end

  it "should return 403 if user is a normal user and group does not exist" do
    cruise_config = GoConfigMother.configWithPipelines(["his-pipeline", "my-pipeline", "her-pipeline"].to_java(java.lang.String))
    result = HttpLocalizedOperationResult.new
    @params = {:pipeline_group => {:group => "some_junk_group"}}
    expect(@security_service).to receive(:isUserAdminOfGroup).with(CaseInsensitiveString.new("loser"), "some_junk_group").never
    expect(@security_service).to receive(:isUserAdmin).with(@user).and_return(false)

    checkPermission(cruise_config, result)

    expect(result.isSuccessful()).to be_falsey
    expect(result.httpCode()).to eq(403)
    expect(result.message()).to eq("Unauthorized to create pipeline.")
  end

  it "should return 403 if user is an admin but no group_name given(happens when using community edition)" do
    cruise_config = GoConfigMother.configWithPipelines(["his-pipeline", "my-pipeline", "her-pipeline"].to_java(java.lang.String))
    result = HttpLocalizedOperationResult.new
    expect(@security_service).to receive(:isUserAdminOfGroup).with(CaseInsensitiveString.new("loser"), PipelineConfigs::DEFAULT_GROUP).and_return(false)

    checkPermission(cruise_config, result)

    expect(result.isSuccessful()).to be_falsey
    expect(result.httpCode()).to eq(403)
    expect(result.message()).to eq("Unauthorized to create pipeline.")
  end

  it "should return 403 if user is not a group admin for 'defaultGroup' and group name is empty" do
    cruise_config = GoConfigMother.configWithPipelines(["his-pipeline", "my-pipeline", "her-pipeline"].to_java(java.lang.String))
    result = HttpLocalizedOperationResult.new
    @params = {:pipeline_group => {:group => ""}}
    expect(@security_service).to receive(:isUserAdminOfGroup).with(CaseInsensitiveString.new("loser"), PipelineConfigs::DEFAULT_GROUP).and_return(false)

    checkPermission(cruise_config, result)

    expect(result.isSuccessful()).to be_falsey
    expect(result.httpCode()).to eq(403)
    expect(result.message()).to eq("Unauthorized to create pipeline.")
  end

  it "should return 403 if user tries to create 'defaultGroup' and is not an admin" do
    cruise_config = GoConfigMother.new.cruiseConfigWithPipelineUsingTwoMaterials()
    result = HttpLocalizedOperationResult.new
    @params = {:pipeline_group => {:group => ""}}
    expect(@security_service).to receive(:isUserAdmin).with(@user).and_return(false)

    checkPermission(cruise_config, result)

    expect(result.isSuccessful()).to be_falsey
    expect(result.httpCode()).to eq(403)
    expect(result.message()).to eq("Unauthorized to create pipeline.")
  end

  it "should return successful result if user is a group admin of given pipeline" do
    cruise_config = GoConfigMother.configWithPipelines(["his-pipeline", "my-pipeline", "her-pipeline"].to_java(java.lang.String))
    result = HttpLocalizedOperationResult.new
    @params = {:pipeline_group => {:group => ""}}
    expect(@security_service).to receive(:isUserAdminOfGroup).with(CaseInsensitiveString.new("loser"), PipelineConfigs::DEFAULT_GROUP).and_return(true)

    checkPermission(cruise_config, result)

    expect(result.isSuccessful()).to be_truthy
  end

  it "should return successful result if user is a super admin and group does not exist" do
    cruise_config = GoConfigMother.configWithPipelines(["his-pipeline", "my-pipeline", "her-pipeline"].to_java(java.lang.String))
    result = HttpLocalizedOperationResult.new
    @params = {:pipeline_group => {:group => "some_junk_group"}}
    expect(@security_service).to receive(:isUserAdminOfGroup).with(CaseInsensitiveString.new("loser"), "some_junk_group").never
    expect(@security_service).to receive(:isUserAdmin).with(@user).and_return(true)

    checkPermission(cruise_config, result)

    expect(result.isSuccessful()).to be_truthy
  end
end
