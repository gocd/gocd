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
load File.join(File.dirname(__FILE__), 'material_controller_examples.rb')

describe Admin::Materials::DependencyController do
  include MockRegistryModule
  before do
    controller.stub(:populate_health_messages)
    controller.stub(:go_config_service).and_return(@go_config_service = Object.new)
    @go_config_service.stub(:registry).and_return(MockRegistryModule::MockRegistry.new)

    import com.thoughtworks.go.helper.NoOpMetricsProbeService unless defined? NoOpMetricsProbeService
    @metrics_probe_service = NoOpMetricsProbeService.new
  end

  describe "new,edit,create and destroy actions" do
    before do
      @material = DependencyMaterialConfig.new(CaseInsensitiveString.new("up-pipeline"), CaseInsensitiveString.new("up-stage"))
      @short_material_type = 'dependency'
    end

    it_should_behave_like :material_controller

    def add_pipelines(*pipelines)
      pipelines.each do |pipeline|
        @cruise_config_mother.addPipeline(@cruise_config, pipeline, "stage-name", ["build-name"].to_java(java.lang.String))
      end
    end

    def setup_other_form_objects
      target_pipeline = @cruise_config_mother.addPipeline(@cruise_config, "pipeline1", "stage-1", ["build-name"].to_java(java.lang.String))
      pipeline = @cruise_config_mother.addPipeline(@cruise_config, "pipeline2", "stage-1", ["build-name"].to_java(java.lang.String))

      @cruise_config_mother.addStageToPipeline(@cruise_config, "pipeline1", "stage-2", ["build-name"].to_java(java.lang.String))
      @cruise_config_mother.addStageToPipeline(@cruise_config, "pipeline1", "stage-3", ["build-name"].to_java(java.lang.String))

    end

    def setup_for_new_material
    end

    def new_material
      DependencyMaterialConfig.new(CaseInsensitiveString.new(""), CaseInsensitiveString.new(""))
    end

    def assert_successful_create
      dependency_material_config = DependencyMaterialConfig.new(CaseInsensitiveString.new("new-some-kinda-material"), CaseInsensitiveString.new("new-up-pipeline"), CaseInsensitiveString.new("new-up-stage"))
      @pipeline.materialConfigs().get(1).should == dependency_material_config
    end

    def update_payload
      {:materialName =>  "new-some-kinda-material", :pipelineStageName => "new-up-pipeline [new-up-stage]"}
    end

    def assert_successful_update
      @pipeline.materialConfigs().get(0).getPipelineName().should == CaseInsensitiveString.new("new-up-pipeline")
      @pipeline.materialConfigs().get(0).getStageName().should == CaseInsensitiveString.new("new-up-stage")
    end
  end

  describe "load pipeline [stage] json" do

    def add_pipelines(*pipelines)
      pipelines.each do |pipeline|
        @cruise_config_mother.addPipeline(@cruise_config, pipeline, "stage-name", ["build-name"].to_java(java.lang.String))
      end
    end

    before(:each) do
      controller.stub(:populate_config_validity)
      @go_config_service = double('Go Config Service')
      controller.stub(:go_config_service).and_return(@go_config_service)
      @pipeline_pause_service = double('Pipeline Pause Service')
      controller.stub(:pipeline_pause_service).and_return(@pipeline_pause_service)

      @cruise_config_mother = GoConfigMother.new

      import com.thoughtworks.go.helper.ConfigFileFixture unless defined? ConfigFileFixture
      @cruise_config = GoConfigMother.defaultCruiseConfig()
      @cruise_config_mother.addPipeline(@cruise_config, "pipeline3", "stage-3", ["job-3"].to_java(java.lang.String))

      ReflectionUtil.setField(@cruise_config, "md5", "1234abcd")
      @user = Username.new(CaseInsensitiveString.new("loser"))
      controller.stub(:current_user).and_return(@user)
      @result = HttpLocalizedOperationResult.new
      HttpLocalizedOperationResult.stub(:new).and_return(@result)
      pause_info = PipelinePauseInfo.paused("just for fun", "loser")
      @pipeline_pause_service.should_receive(:pipelinePauseInfo).with("pipeline-name").and_return(pause_info)
      @go_config_service.stub(:registry).and_return(MockRegistryModule::MockRegistry.new)
    end

    it "should return pipeline [stage] json in alphabetical order" do
      @cruise_config_mother.addPipeline(@cruise_config, "pipeline2", "stage-2", ["job-2"].to_java(java.lang.String))
      @cruise_config_mother.addPipeline(@cruise_config, "a", "b", ["job-1"].to_java(java.lang.String))
      @cruise_config_mother.addPipeline(@cruise_config, "pipeline1", "stage-1", ["job-1"].to_java(java.lang.String))
      @cruise_config_mother.addPipeline(@cruise_config, "Ab", "stage-1", ["job-1"].to_java(java.lang.String))

      pipeline = @cruise_config_mother.addPipeline(@cruise_config, "pipeline-name", "stage-name", ["build-name"].to_java(java.lang.String))
      @pipeline_config_for_edit = ConfigForEdit.new(pipeline, @cruise_config, MagicalGoConfigXmlLoader.new(nil, nil,@metrics_probe_service).preprocessAndValidate(@cruise_config))
      @go_config_service.should_receive(:loadForEdit).with("pipeline-name", @user, @result).and_return(@pipeline_config_for_edit)

      get :new, :pipeline_name => "pipeline-name"

      assigns[:pipeline_stages_json].should == "[{\"pipeline\":\"a\",\"stage\":\"b\"},{\"pipeline\":\"Ab\",\"stage\":\"stage-1\"},{\"pipeline\":\"pipeline1\",\"stage\":\"stage-1\"},{\"pipeline\":\"pipeline2\",\"stage\":\"stage-2\"},{\"pipeline\":\"pipeline3\",\"stage\":\"stage-3\"}]"
    end

    it "should not return pipeline [stage] json for pipelines which the user has no view permissions for" do
      @cruise_config_mother.addPipeline(@cruise_config, "pipeline2", "stage-2", ["job-2"].to_java(java.lang.String))
      @cruise_config_mother.addPipeline(@cruise_config, "a", "b", ["job-1"].to_java(java.lang.String))
      @security_service = double('Security Service')
      controller.stub(:security_service).and_return(@security_service)
      @security_service.should_receive(:hasViewOrOperatePermissionForPipeline).with(@user, "pipeline2").and_return(false)
      @security_service.should_receive(:hasViewOrOperatePermissionForPipeline).with(@user, "a").and_return(true)
      @security_service.should_receive(:hasViewOrOperatePermissionForPipeline).with(@user, "pipeline3").and_return(true)
      pipeline = @cruise_config_mother.addPipeline(@cruise_config, "pipeline-name", "stage-name", ["build-name"].to_java(java.lang.String))
      @pipeline_config_for_edit = ConfigForEdit.new(pipeline, @cruise_config, MagicalGoConfigXmlLoader.new(nil, nil, @metrics_probe_service).preprocessAndValidate(@cruise_config))
      @go_config_service.should_receive(:loadForEdit).with("pipeline-name", @user, @result).and_return(@pipeline_config_for_edit)

      get :new, :pipeline_name => "pipeline-name"

      assigns[:pipeline_stages_json].should == "[{\"pipeline\":\"a\",\"stage\":\"b\"},{\"pipeline\":\"pipeline3\",\"stage\":\"stage-3\"}]"
    end

    it "should return pipeline [stage] json for pipeline with template configured" do
      @cruise_config_mother.addPipelineWithTemplate(@cruise_config, "pipeline2", "template-1", "stage-2", ["job-2"].to_java(java.lang.String))

      pipeline = @cruise_config_mother.addPipeline(@cruise_config, "pipeline-name", "stage-name", ["build-name"].to_java(java.lang.String))
      @pipeline_config_for_edit = ConfigForEdit.new(pipeline, @cruise_config, MagicalGoConfigXmlLoader.new(nil, nil, @metrics_probe_service).preprocessAndValidate(@cruise_config))
      @go_config_service.should_receive(:loadForEdit).with("pipeline-name", @user, @result).and_return(@pipeline_config_for_edit)

      get :new, :pipeline_name => "pipeline-name"

      expect(response.response_code).to eq(200)
      assigns[:pipeline_stages_json].should == "[{\"pipeline\":\"pipeline2\",\"stage\":\"stage-2\"},{\"pipeline\":\"pipeline3\",\"stage\":\"stage-3\"}]"
    end

    it "should return pipeline [stage] json for pipeline with template configured" do
      @cruise_config_mother.addPipelineWithTemplate(@cruise_config, "pipeline2", "template-1", "stage-2", ["job-2"].to_java(java.lang.String))

      pipeline = @cruise_config_mother.addPipeline(@cruise_config, "pipeline-name", "stage-name", ["build-name"].to_java(java.lang.String))
      @pipeline_config_for_edit = ConfigForEdit.new(pipeline, @cruise_config, MagicalGoConfigXmlLoader.new(nil, nil, @metrics_probe_service).preprocessAndValidate(@cruise_config))
      @go_config_service.should_receive(:loadForEdit).with("pipeline-name", @user, @result).and_return(@pipeline_config_for_edit)

      valid_fingerprint = pipeline.materialConfigs().first.getPipelineUniqueFingerprint()
      get :edit, :pipeline_name => "pipeline-name", :finger_print => valid_fingerprint

      expect(response.response_code).to eq(200)
      assigns[:pipeline_stages_json].should == "[{\"pipeline\":\"pipeline2\",\"stage\":\"stage-2\"},{\"pipeline\":\"pipeline3\",\"stage\":\"stage-3\"}]"
    end

    it "should not try and render twice on failure to find material, while trying to load for edit" do
      pipeline = @cruise_config_mother.addPipeline(@cruise_config, "pipeline-name", "stage-name", ["build-name"].to_java(java.lang.String))
      pipeline_config_for_edit = ConfigForEdit.new(pipeline, @cruise_config, MagicalGoConfigXmlLoader.new(nil, nil, @metrics_probe_service).preprocessAndValidate(@cruise_config))

      @go_config_service.should_receive(:loadForEdit).and_return(pipeline_config_for_edit)

      get :edit, :pipeline_name => "pipeline-name", :finger_print => "invalid-fingerprint"

      expect(response.response_code).to eq(404)
    end
  end
end
