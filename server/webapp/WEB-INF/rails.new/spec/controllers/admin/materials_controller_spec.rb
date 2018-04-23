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

require 'rails_helper'

describe Admin::MaterialsController do
  include MockRegistryModule
  include ConfigSaveStubbing

  before :each do
    @go_config_service = stub_service(:go_config_service)
    @pipeline_pause_service = stub_service(:pipeline_pause_service)
    allow(controller).to receive(:populate_config_validity)
  end

  describe "index" do
    before :each do
      @pause_info = PipelinePauseInfo.paused("just for fun", "loser")
      @pipeline_name = "pipeline-name"
      expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with(@pipeline_name).and_return(@pause_info)
      allow(@go_config_service).to receive(:registry).and_return(MockRegistryModule::MockRegistry.new)
      @cruise_config = double("Cruise Config")
      expect(@cruise_config).to receive(:name).and_return(@pipeline_name)
      a = double('config wrapper')
      expect(a).to receive(:getConfig).and_return(@cruise_config)
      expect(a).to receive(:getCruiseConfig).and_return(@cruise_config)
      expect(a).to receive(:getProcessedConfig).and_return(@cruise_config)
      expect(@go_config_service).to receive(:loadForEdit).and_return(a)
    end

    it "should set current tab param" do
      get :index, params: { :stage_parent => "pipelines", :pipeline_name => @pipeline_name }

      expect(controller.params[:current_tab]).to eq('materials')
      assert_template layout: "pipelines/details"
    end
  end

  describe "delete" do

    before :each do
      @cruise_config = BasicCruiseConfig.new()
      @cruise_config_mother = GoConfigMother.new
      @material_config = GitMaterialConfig.new("http://git.thoughtworks.com")

      @pipeline = @cruise_config_mother.addPipeline(@cruise_config, "pipeline-name", "stage-name", MaterialConfigs.new([@material_config].to_java(MaterialConfig)), ["build-name"].to_java(java.lang.String))

      @pipeline_config_for_edit = ConfigForEdit.new(@pipeline, @cruise_config, @cruise_config)

      ReflectionUtil.setField(@cruise_config, "md5", "1234abcd")
      @user = Username.new(CaseInsensitiveString.new("loser"))
      allow(controller).to receive(:current_user).and_return(@user)

      @pause_info = PipelinePauseInfo.paused("just for fun", "loser")
      expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
      allow(@go_config_service).to receive(:registry).and_return(MockRegistryModule::MockRegistry.new)
    end

    it "should delete an existing material" do
      stub_localized_result
      stub_save_for_success

      @pipeline.addMaterialConfig(hg = HgMaterialConfig.new("url", nil))
      expect(@pipeline.materialConfigs().size).to eq(2)

      delete :destroy, params: { :stage_parent => "pipelines", :pipeline_name => "pipeline-name", :config_md5 => "1234abcd", :finger_print => @material_config.getPipelineUniqueFingerprint() }

      expect(@pipeline.materialConfigs().size).to eq(1)
      expect(@pipeline.materialConfigs().first).to eq(hg)
      expect(@cruise_config.getAllErrors().size).to eq(0)
    end

    it "should assign config_errors for display when delete fails due to validation errors" do
      stub_localized_result
      stub_save_for_validation_error do |result, config, node|
        config.errors().add("base", "someError")
        result.badRequest(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT_PIPELINE", ["pipeline-name"]))
      end

      expect(@pipeline.materialConfigs().size).to eq(1)

      delete :destroy, params: { :stage_parent => "pipelines", :pipeline_name => "pipeline-name", :config_md5 => "1234abcd", :finger_print => @material_config.getPipelineUniqueFingerprint() }

      expect(@cruise_config.getAllErrors().size).to eq(1)
      expect(response.status).to eq(400)
      assert_template layout: "pipelines/details"
    end
  end
end
