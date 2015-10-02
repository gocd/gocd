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

describe Admin::MaterialsController do
  include MockRegistryModule
  before do
    controller.stub(:populate_health_messages)
  end

  include ConfigSaveStubbing

  describe "routes" do
    it "should resolve index" do
      {:get => "/admin/pipelines/pipeline.name/materials"}.should route_to(:controller => "admin/materials", :action => "index", :stage_parent => "pipelines", :pipeline_name => "pipeline.name")
    end

    it "should generate index" do
      admin_material_index_path(:pipeline_name => "foo.bar").should == "/admin/pipelines/foo.bar/materials"
    end
  end

  describe :index do
    before :each do
      @go_config_service = stub_service(:go_config_service)
      @pipeline_pause_service = stub_service(:pipeline_pause_service)
      @pause_info = PipelinePauseInfo.paused("just for fun", "loser")
      @pipeline_name = "pipeline-name"
      @pipeline_pause_service.should_receive(:pipelinePauseInfo).with(@pipeline_name).and_return(@pause_info)
      @go_config_service.stub(:registry).and_return(MockRegistryModule::MockRegistry.new)
      @go_config_service.should_receive(:checkConfigFileValid).and_return(GoConfigValidity.valid)
      @cruise_config = double("Cruise Config")
      @cruise_config.should_receive(:name).and_return(@pipeline_name)
      a = double('config wrapper')
      a.should_receive(:getConfig).and_return(@cruise_config)
      a.should_receive(:getCruiseConfig).and_return(@cruise_config)
      a.should_receive(:getProcessedConfig).and_return(@cruise_config)
      @go_config_service.should_receive(:loadForEdit).and_return(a)
    end

    it "should set current tab param" do
      get :index, {:stage_parent => "pipelines", :pipeline_name => @pipeline_name}

      controller.params[:current_tab].should == 'materials'
      assert_template layout: "pipelines/details"
    end
  end

  describe "delete" do

    before :each do
      controller.stub(:populate_config_validity)
      @cruise_config = BasicCruiseConfig.new()
      @cruise_config_mother = GoConfigMother.new
      @material_config = GitMaterialConfig.new("http://git.thoughtworks.com")

      @pipeline = @cruise_config_mother.addPipeline(@cruise_config, "pipeline-name", "stage-name", MaterialConfigs.new([@material_config].to_java(MaterialConfig)), ["build-name"].to_java(java.lang.String))

      @pipeline_config_for_edit = ConfigForEdit.new(@pipeline, @cruise_config, @cruise_config)

      ReflectionUtil.setField(@cruise_config, "md5", "1234abcd")
      @user = Username.new(CaseInsensitiveString.new("loser"))
      controller.stub(:current_user).and_return(@user)
      @result = stub_localized_result

      @go_config_service = stub_service(:go_config_service)
      @pipeline_pause_service = stub_service(:pipeline_pause_service)
      @pause_info = PipelinePauseInfo.paused("just for fun", "loser")
      @pipeline_pause_service.should_receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
      @go_config_service.stub(:registry).and_return(MockRegistryModule::MockRegistry.new)
    end

    it "should delete an existing material" do
      stub_save_for_success

      @pipeline.addMaterialConfig(hg = HgMaterialConfig.new("url", nil))
      @pipeline.materialConfigs().size.should == 2

      delete :destroy, :stage_parent => "pipelines", :pipeline_name => "pipeline-name", :config_md5 => "1234abcd", :finger_print => @material_config.getPipelineUniqueFingerprint()

      @pipeline.materialConfigs().size.should == 1
      @pipeline.materialConfigs().first.should == hg
      @cruise_config.getAllErrors().size.should == 0
    end

    it "should assign config_errors for display when delete fails due to validation errors" do
      stub_save_for_validation_error do |result, config, node|
        config.errors().add("base", "someError")
        result.badRequest(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT_PIPELINE", ["pipeline-name"]))
      end

      @pipeline.materialConfigs().size.should == 1

      delete :destroy, :stage_parent => "pipelines", :pipeline_name => "pipeline-name", :config_md5 => "1234abcd", :finger_print => @material_config.getPipelineUniqueFingerprint()

      @cruise_config.getAllErrors().size.should == 1
      response.status.should == 400
      assert_template layout: "pipelines/details"
    end
  end
end
