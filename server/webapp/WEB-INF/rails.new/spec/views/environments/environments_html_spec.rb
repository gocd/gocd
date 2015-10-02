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

describe 'environments/_environments.html.erb' do
  describe "with environments" do
    before do
      @yesterday = org.joda.time.DateTime.new.minusDays(1).toDate()
      job_history = JobHistory.new()
      job_history.addJob("unit", JobState::Completed, JobResult::Passed, @yesterday)
      stage_history = StageInstanceModels.new()
      stage_history.addStage("stage-1", job_history)
      @pim = PipelineHistoryMother.singlePipeline("pipline-name", stage_history)
      @pim2 = PipelineHistoryMother.singlePipeline("pipeline-name-2", stage_history)

      svn_revisions = ModificationsMother.createSvnMaterialRevisions(Modification.new(@yesterday, "1234", "label-10", nil))
      hg_revisions = ModificationsMother.createHgMaterialRevisions()
      @pim.setMaterialRevisionsOnBuildCause(svn_revisions)
      @pim.setMaterialConfigs(Materials.new([svn_revisions.getMaterialRevision(0).getMaterial()]).convertToConfigs())
      @pim2.setMaterialRevisionsOnBuildCause(hg_revisions)
      @pim2.setMaterialConfigs(Materials.new([hg_revisions.getMaterialRevision(0).getMaterial()]).convertToConfigs())
      @pipeline_model = PipelineModel.new(@pim.getName(), true, true, PipelinePauseInfo::notPaused())
      @pipeline_model.addPipelineInstance(@pim)
      @pipeline_model_2 = PipelineModel.new(@pim2.getName(), true, true, PipelinePauseInfo::notPaused())
      @pipeline_model_2.addPipelineInstance(@pim2)

      @uat = Environment.new("uat", [@pipeline_model])
      @prod = Environment.new("prod", [@pipeline_model_2])
      assign(:environments, [@uat, @prod])

      class << view
        include StagesHelper
      end
    end

    it "should have wrapping div for ajax refresh" do
      render :partial=>'environments/environments.html.erb', locals: {scope: {show_edit_environments: true}}

      expect(response).to have_selector("div#environment_uat_panel .environment h2", :text => "uat")
    end

    it "should yield for show hide script" do
      script_snippet = "Util.on_load(function() { AjaxRefreshers.main().afterRefreshOf('environment_pipeline_pipline-name_panel', function() { make_collapsable('environment_pipeline_pipline-name_panel'); });});";

      render :partial=>'environments/environments.html.erb', :locals => {:scope => {:show_edit_environments => true}}

      expect(response).to have_selector("script[type='text/javascript']", visible: false)
      Capybara.string(response.body).all("script[type='text/javascript']", visible: false).tap do |script|
        expect(script.length).to eq(2)
      end
      expect(response).to have_selector("script[type='text/javascript']", :text => script_snippet, visible: false)
    end

    it "should show a message to add pipelines for an environment without pipelines" do
      @empty = Environment.new("empty", [])
      assign(:environments, [@empty])

      render :partial=>'environments/environments.html.erb', :locals => {:scope => {:show_edit_environments => true}}

      expect(response).to have_selector("div#environment_empty_panel .environment h2", :text => "empty")
      expect(response).to have_selector("div#environment_empty_panel .pipelines span", :text => "There are no pipelines configured for this environment.")
    end

    it "should render environments using environment partial" do
      stub_template "_environment.html.erb" => "Content for: <%= scope[:environment].name %>"

      render :partial => 'environments/environments.html.erb', :locals => {:scope => {:show_edit_environments => true}}

      expect(response).to have_selector("div#environment_uat_panel", :text => "Content for: uat")
      expect(response).to have_selector("div#environment_prod_panel", :text => "Content for: prod")
    end
  end
end


