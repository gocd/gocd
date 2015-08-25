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

describe 'environments/index.json.erb' do
  include EnvironmentsHelper

  describe "render" do

    def assert_result
      json = JSON.parse(response.body)

      expect(json["unused_feature"]).to eq({"html" => "environment", "parent_id" => "unused_feature"})
      expect(json["environment_environment_panel"]).to eq({"html" => "pipeline_1", "parent_id" => "ajax_environments", "index" => 0, "type" => "group_of_pipelines"})
      expect(json["environment_environment_other_panel"]).to eq({"html" => "pipeline_other_env", "parent_id" => "ajax_environments", "index" => 1, "type" => "group_of_pipelines"})
      expect(json["model_1"]).to eq({"html" => "pipeline_1", "parent_id" => "environment_environment_panel", "index" => 0, "type" => "pipeline"})
      expect(json["model_2"]).to eq({"html" => "pipeline_2", "parent_id" => "environment_environment_panel", "index" => 1, "type" => "pipeline"})
      expect(json["other-env"]).to eq({"html" => "pipeline_other_env", "parent_id" => "environment_environment_other_panel", "index" => 0, "type" => "pipeline"})
    end

    it "should create json with 'environments' partial" do
      assign(:environments, [env1 = double('environment', :name => 'environment', :getPipelineModels => [pipeline_model_1 = Object.new, pipeline_model_2 = Object.new]),
                             env2 = double("environment_other", :name => 'environment_other', :getPipelineModels => [pipeline_other_env = Object.new])])

      allow(view).to receive(:render_json).with(:partial => "no_environments.html.erb", :locals => {:scope => {}}).and_return("\"environment\"")
      allow(view).to receive(:render_json).with(:partial => "environment.html.erb", :locals => {:scope => {:environment => env1, :omit_pipeline => true}}).and_return("\"pipeline_1\"")
      allow(view).to receive(:render_json).with(:partial => "environment.html.erb", :locals => {:scope => {:environment => env2, :omit_pipeline => true}}).and_return("\"pipeline_other_env\"")

      allow(view).to receive(:env_dom_id).exactly(3).with("environment").and_return('environment_environment_panel')
      allow(view).to receive(:env_dom_id).exactly(2).with("environment_other").and_return('environment_environment_other_panel')

      allow(view).to receive(:render_json).with(:partial => "environment_pipeline.html.erb", :locals => {:scope => {:pipeline_model => pipeline_model_1}}).and_return("\"pipeline_1\"")
      allow(view).to receive(:render_json).with(:partial => "environment_pipeline.html.erb", :locals => {:scope => {:pipeline_model => pipeline_model_2}}).and_return("\"pipeline_2\"")
      allow(view).to receive(:render_json).with(:partial => "environment_pipeline.html.erb", :locals => {:scope => {:pipeline_model => pipeline_other_env}}).and_return("\"pipeline_other_env\"")
      allow(view).to receive(:view_cache_key).and_return(view_cache_key = double('view_cache_key'))
      view_cache_key.stub(:forEnvironmentPipelineBox).and_return("key")

      allow(view).to receive(:env_pipeline_dom_id).with(pipeline_model_1).and_return('model_1')
      allow(view).to receive(:env_pipeline_dom_id).with(pipeline_model_2).and_return('model_2')
      allow(view).to receive(:env_pipeline_dom_id).with(pipeline_other_env).and_return('other-env')

      render

      assert_result
    end
  end

  describe "caching" do
    it "should cache pipeline partials of different pipelines separately" do
      environment1 = environment_for_caching(JobState::Building, JobResult::Unknown)
      environment2 = environment_for_caching(JobState::Completed, JobResult::Failed)
      key_proc = proc { |environment| [ViewCacheKey.new.forEnvironmentPipelineBox(environment.getPipelineModels()[0]), {:subkey => "environment_json_#{env_dom_id(environment.name())}"}] }
      check_fragment_caching(environment1, environment2, key_proc) do |environment|
        assign(:environments, [environment])
        render
      end
    end

    def environment_for_caching(latest_pipeline_job_state, latest_pipeline_job_result)
      model = PipelineModel.new("pipelineName", true, true, PipelinePauseInfo.notPaused())
      stages = StageInstanceModels.new
      stages.add(stage_instance("stageName", 13, latest_pipeline_job_state, latest_pipeline_job_result))
      stages.add(NullStageHistoryItem.new("stage2", true))
      pipeline_instance = PipelineInstanceModel.createPipeline("pipelineName", 10, "label-10", BuildCause.createExternal(), stages)
      pipeline_instance.setId(12)
      model.addPipelineInstance(pipeline_instance)
      Environment.new("uat", [model])
    end

    def stage_instance(name, id, state, job_result)
      jobs = JobHistory.new()
      jobs.addJob("dev", state, job_result, java.util.Date.new())
      stage_instance = StageInstanceModel.new(name, "2", jobs)
      stage_instance.setId(id)
      stage_instance
    end
  end
end


