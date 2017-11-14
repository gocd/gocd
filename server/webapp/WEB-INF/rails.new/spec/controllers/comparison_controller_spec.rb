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

describe ComparisonController do
  include StageModelMother

  before :each do
    allow(controller).to receive(:pipeline_history_service).and_return(@phs = double('PipelineHistoryService'))
    allow(controller).to receive(:mingle_config_service).and_return(@mingle_service = double('MingleConfigService'))
  end

  describe "comparison_controller" do
    before :each do
      allow(controller).to receive(:current_user).and_return(@loser = Username.new(CaseInsensitiveString.new("loser")))
    end

    describe "show" do
      before :each do
        allow(@mingle_service).to receive(:mingleConfigForPipelineNamed)
        @result = HttpOperationResult.new
        allow(HttpOperationResult).to receive(:new).and_return(@result)
      end

      it "should load from and to pipeline instances and cruise_config" do
        to_pipeline = PipelineInstanceModel.createPipeline("some_pipeline", 17, "some-label", BuildCause.createWithEmptyModifications(), stage_history_for("dev", "prod"))
        expect(@phs).to receive(:findPipelineInstance).with("some_pipeline", 17, @loser, @result).and_return(to_pipeline)

        from_pipeline = PipelineInstanceModel.createPipeline("some_pipeline", 10, "some-label", BuildCause.createWithEmptyModifications(), stage_history_for("dev", "prod"))
        expect(@phs).to receive(:findPipelineInstance).with("some_pipeline", 10, @loser, @result).and_return(from_pipeline)

        pipeline_instances = PipelineInstanceModels.createPipelineInstanceModels()
        (1..10).each do |i|
          pipeline_instances << PipelineInstanceModel.createPipeline("some_pipeline", i, "some-label", BuildCause.createWithEmptyModifications(), stage_history_for("dev", "prod"))
        end

        get :show, params: { :pipeline_name => "some_pipeline", :from_counter => "10", :to_counter => "17" }

        expect(assigns[:from_pipeline]).to eq(from_pipeline)
        expect(assigns[:to_pipeline]).to eq(to_pipeline)
        expect(assigns[:cruise_config]).to eq(controller.go_config_service.getCurrentConfig())
      end
    end

    describe "show edge cases" do
      it "should bump up pipeline counter to 1 when counter is 0" do
        get :show, params: { :pipeline_name => "some_pipeline", :from_counter => "0", :to_counter => "42" }
        expect(response).to redirect_to(compare_pipelines_path(:pipeline_name => "some_pipeline", :from_counter => "1", :to_counter => "42"))

        get :show, params: { :pipeline_name => "some_pipeline", :from_counter => "42", :to_counter => "0" }
        expect(response).to redirect_to(compare_pipelines_path(:pipeline_name => "some_pipeline", :from_counter => "42", :to_counter => "1"))
      end
    end

    describe "list" do
      it "should get list of pipeline instances" do
        result = HttpLocalizedOperationResult.new
        allow(HttpLocalizedOperationResult).to receive(:new).and_return(result)
        expect(@phs).to receive(:findMatchingPipelineInstances).with("some_pipeline", "query", 10, @loser, result).and_return(:from_pipeline)
        get :list, params: { :pipeline_name => "some_pipeline", :other_pipeline_counter => 10, :q => "query", :format => "json" }
        expect(assigns[:pipeline_instances]).to eq(:from_pipeline)
      end
    end

    describe "timeline" do
      it "should get pipeline history" do
        expect(@phs).to receive(:findPipelineInstancesByPageNumber).with("pipeline_up", 5, 10, "loser").and_return(:some_pipeline_instances)
        get :timeline, params: { :pipeline_name => "pipeline_up", :page => "5" }
        expect(assigns[:pipeline_instances]).to eq(:some_pipeline_instances)
      end
    end
  end
end
