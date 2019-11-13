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


describe Admin::JobsController do
  include GoUtil

  describe "actions" do
    render_views


    before do
      allow(controller).to receive(:populate_config_validity)
      @cruise_config = BasicCruiseConfig.new()
      cruise_config_mother = GoConfigMother.new
      @pipeline = cruise_config_mother.addPipeline(@cruise_config, "pipeline-name", "stage-name", ["job-1", "job-2"].to_java(java.lang.String))
      @artifact1 = BuildArtifactConfig.new('src', 'dest')
      @artifact2 = BuildArtifactConfig.new('src2', 'dest2')
      @pipeline.get(0).getJobs().get(0).artifactTypeConfigs().add(@artifact1)
      @pipeline.get(0).getJobs().get(0).artifactTypeConfigs().add(@artifact2)

      expect(controller).to receive(:load_pipeline) do
        controller.instance_variable_set('@processed_cruise_config', @cruise_config)
        controller.instance_variable_set('@cruise_config', @cruise_config)
        controller.instance_variable_set('@pipeline', @pipeline)
      end

    end

    describe "edit settings" do
      it "should display 'Job Name' " do
        get :edit, params:{:stage_parent=> "pipelines", :current_tab => "settings", :pipeline_name => @pipeline.name().to_s, :stage_name => @pipeline.get(0).name().to_s, :job_name => @pipeline.get(0).getJobs().get(0).name().to_s}
        expect(response.status).to eq(200)
        expect(response.body).to have_selector("input[name='job[name]']")
      end
    end

    describe "edit artifacts" do
      include FormUI

      it "should display errors on artifact" do
        error = config_error(BuildArtifactConfig::SRC, "Source is wrong")
        error.add(BuildArtifactConfig::DEST, "Dest is wrong")
        set(@artifact1, "errors", error)

        get :edit, params:{:stage_parent=> "pipelines", :current_tab => :artifacts, :pipeline_name => @pipeline.name().to_s, :stage_name => @pipeline.get(0).name().to_s, :job_name => @pipeline.get(0).getJobs().get(0).name().to_s}

        expect(response.status).to eq(200)
        expect(response.body).to have_selector("h3", :text=>"Artifacts")
        expect(response.body).to have_selector("div[class='artifact-container'] div[class='artifact'] input[class='form_input artifact_source'][value='src']")
        expect(response.body).to have_selector("div[class='artifact-container'] div.form_error", :text => "Source is wrong")
        expect(response.body).to have_selector("div[class='artifact-container'] div[class='artifact'] div.field_with_errors input[class='form_input artifact_destination'][value='dest']")
        expect(response.body).to have_selector("div[class='artifact-container'] div.form_error", :text => "Dest is wrong")

        expect(response.body).to have_selector("div[class='artifact-container'] div[class='artifact'] input[class='form_input artifact_source'][value='src2']")
        expect(response.body).to have_selector("div[class='artifact-container'] div[class='artifact'] input[class='form_input artifact_destination'][value='dest2']")
      end
    end
  end
end
