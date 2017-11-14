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

shared_examples_for :stages_controller do

    describe "increment_index" do

      before do
        expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
        allow(@go_config_service).to receive(:registry).and_return(MockRegistryModule::MockRegistry.new)
      end

      it "should increment stage order" do
        stub_save_for_success

        @pipeline.clear()
        @pipeline.add(StageConfigMother.oneBuildPlanWithResourcesAndMaterials('stage-to-move'))
        @pipeline.add(StageConfigMother.oneBuildPlanWithResourcesAndMaterials('fixed_stage'))

        post :increment_index, params: { :stage_parent => @stage_parent, :pipeline_name => "pipeline-name", :stage_name => "stage-to-move", :config_md5 => "1234abcd" }

        expect(@pipeline.get(0).name()).to eq(CaseInsensitiveString.new("fixed_stage"))
        expect(@pipeline.get(1).name()).to eq(CaseInsensitiveString.new("stage-to-move"))
        assert_save_arguments
        assert_update_command ::ConfigUpdate::SaveAsPipelineOrTemplateAdmin, ConfigUpdate::PipelineOrTemplateNode, ConfigUpdate::PipelineStageSubject
      end
    end

    describe "decrement_index" do

      before do
        expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
        allow(@go_config_service).to receive(:registry).and_return(MockRegistryModule::MockRegistry.new)
      end

      it "should decrement stage order" do
        stub_save_for_success
        @pipeline.clear()
        @pipeline.add(StageConfigMother.oneBuildPlanWithResourcesAndMaterials('fixed_stage'))
        @pipeline.add(StageConfigMother.oneBuildPlanWithResourcesAndMaterials('stage-to-move'))

        post :decrement_index, params: { :stage_parent => @stage_parent, :pipeline_name => "pipeline-name", :stage_name => "stage-to-move", :config_md5 => "1234abcd" }

        expect(@pipeline.get(0).name()).to eq(CaseInsensitiveString.new("stage-to-move"))
        expect(@pipeline.get(1).name()).to eq(CaseInsensitiveString.new("fixed_stage"))
        assert_save_arguments
        assert_update_command ::ConfigUpdate::SaveAsPipelineOrTemplateAdmin, ConfigUpdate::PipelineOrTemplateNode, ConfigUpdate::PipelineStageSubject
      end
    end
end
