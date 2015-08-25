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

describe "pipelines/index.html.erb" do
  include PipelineModelMother

  before(:each) do
    @pipeline_group_model = PipelineGroupModel.new("group-1")
    @pipeline_group_model.add(pipeline_model("pipeline-1", "label-1"))
    pipeline_2 = pipeline_model("pipeline-2", "label-2")
    previous_stage = StageInstanceModel.new("name", "2", StageResult::Failed, StageIdentifier.new("pipeline-2", 3, "label-007", "cruise", "2"))
    @active_stage = pipeline_2.getLatestPipelineInstance().getStageHistory().get(0)
    @active_stage.setBuildHistory(JobHistory.withJob("unit", JobState::Building, JobResult::Unknown, Time.now))
    @active_stage.setPreviousStage(previous_stage)
    @pipeline_group_model.add(pipeline_2)
    @pipeline_group_model.add(pipeline_model("pipeline-3", "label-3"))
    @pipeline_group_model.add(pipeline_model("pipeline-4", "label-4"))
    @pipeline_group_model_other = PipelineGroupModel.new("group-2")
    @pipeline_group_model_other.add(pipeline_model("pipeline-2-1", "label-3", true))
    @pipeline_group_model_empty = PipelineGroupModel.new("group-3-empty")
    assign(:pipeline_selections, PipelineSelections.new())
    assign(:pipeline_groups, [@pipeline_group_model, @pipeline_group_model_other, @pipeline_group_model_empty])
    assign(:pipeline_configs, BasicPipelineConfigs.new)
    class << view
      include StagesHelper
    end
    view.stub(:on_pipeline_dashboard?).and_return(true)
  end

  it "should render multiple groups" do
    render

    expect(response).to have_selector(".pipeline_group .entity_title", :text => "group-1")
    expect(response).to have_selector(".pipeline_group .entity_title", :text => "group-1")
    expect(response).to have_selector(".pipeline_group .entity_title", :text => "group-2")
    expect(response).to_not have_selector(".pipeline_group .entity_title", :text => "group-3-empty")
  end

  it "should render pipeline" do
    render

    Capybara.string(response.body).all(".pipeline_group").tap do |all_groups|
      expect(all_groups[0]).to have_selector(".entity_title", :text => "group-1")
      all_groups[0].all(".pipelines .pipeline").tap do |all_pipelines_in_group_1|
        first_pipeline_in_group_1 = all_pipelines_in_group_1[0]

        expect(first_pipeline_in_group_1).to have_selector(".title a", :text => "pipeline-1")
        expect(first_pipeline_in_group_1).to have_selector(".status.details .label a[href='/pipelines/value_stream_map/pipeline-1/5']", :text => "label-1")
          first_pipeline_in_group_1.find(".stages").tap do |stages|
          expect(stages).to have_selector(".latest_stage", :text => "Passed: cruise")
          stages.find("a.stage[href='/pipelines/pipeline-1/5/cruise/10']").tap do |stage_link|
            stage_link.find(".last_run_stage").tap do |last_run_stage|
              expect(last_run_stage).to have_selector(".stage_bar.Passed[title='cruise (Passed)']")
              expect(last_run_stage).to have_selector(".stage_bar.Passed[data-stage='cruise']")
            end
          end
        end

        second_pipeline_in_group_1 = all_pipelines_in_group_1[1]

        expect(second_pipeline_in_group_1).to have_selector(".title a", :text => "pipeline-2")
        expect(second_pipeline_in_group_1).to have_selector(".status.details .label a[href='/pipelines/value_stream_map/pipeline-2/5']", :text => "label-2")
        second_pipeline_in_group_1.find(".stages").tap do |stages|
          expect(stages).to have_selector(".latest_stage", :text => "Building: cruise")
          stages.find("a.stage[href='/pipelines/pipeline-2/5/cruise/10']").tap do |stage_link|
            stage_link.find(".last_run_stage").tap do |last_run_stage|
              expect(last_run_stage).to have_selector(".stage_bar.Building[title='cruise (Building)']")
              expect(last_run_stage).to have_selector(".stage_bar.Building[data-stage='cruise']")
            end
          end
        end
      end
    end
  end

  it "should render pipeline with actions icons for admin user" do
    pipeline_model = @pipeline_group_model.getPipelineModel("pipeline-1")
    pipeline_model.updateAdministrability(true)
    pipeline_model.getLatestPipelineInstance().setCurrentlyLocked(true)

    render

    Capybara.string(response.body).all(".pipeline_group").tap do |all_groups|
      expect(all_groups[0]).to have_selector(".entity_title", :text => "group-1")
      all_groups[0].all(".pipelines .pipeline").tap do |all_pipelines_in_group_1|
        first_pipeline_in_group_1 = all_pipelines_in_group_1[0]

        expect(first_pipeline_in_group_1).to have_selector("div.pipeline_header div.pipeline_name_link .title a", :text => "pipeline-1")
        expect(first_pipeline_in_group_1).to have_selector("div.pipeline_header div.pipeline_actions a")
        expect(first_pipeline_in_group_1).to have_selector("div.pipeline_header div.pipeline_actions span.locked_instance")
      end
    end
  end

  it "should render previous status of active stage" do
    render

    Capybara.string(response.body).all(".pipeline_group").tap do |all_groups|
      expect(all_groups[0]).to have_selector(".entity_title", :text => "group-1")
      all_groups[0].all(".pipelines .pipeline .pipeline_instance").tap do |all_pipelines_in_group_1|
        second_pipeline_in_group_1 = all_pipelines_in_group_1[1]

        expect(second_pipeline_in_group_1).to have_selector(".status.details .label a", :text => "label-2")
        second_pipeline_in_group_1.find(".previously").tap do |previously|
          expect(previously).to have_selector(".label", :text => "Previously:")
          expect(previously).to have_selector("a.result[href='/pipelines/pipeline-2/3/cruise/2'][title='label-007']", :text => "Failed")
        end
      end
    end
  end

  it "should skip previous status when active stage does not have previous" do
    @active_stage.setPreviousStage(nil)

    render

    expect(response).to_not have_selector(".previously")
  end

  it "should add css clear class on every third pipeline" do
    assigns[:pipeline_groups] = [@pipeline_group_model]

    render

    Capybara.string(response.body).find("#pipeline_groups_container #pipeline_group_group-1_panel .pipeline_group").tap do |group|
      expect(group).to have_selector(".entity_title", "group-1")
      expect(group.all(".divider").size).to eq(4)

      group.all(".pipelines .pipeline").tap do |all_pipelines|
        expect(all_pipelines[0]).to have_selector(".title a", :text => "pipeline-1")
        expect(all_pipelines[1]).to have_selector(".title a", :text => "pipeline-2")
        expect(all_pipelines[2]).to have_selector(".title a", :text => "pipeline-3")
        expect(all_pipelines[3]).to have_selector(".title a", :text => "pipeline-4")
      end

    end
  end

  it "should support no historical data" do
    render

    Capybara.string(response.body).all(".pipeline_group").tap do |all_groups|
      expect(all_groups[1]).to have_selector(".entity_title", :text => "group-2")

      expect(all_groups[1]).to have_selector(".entity_title", :text => "group-2")
      all_groups[1].all(".pipelines .pipeline").tap do |all_pipelines_in_group_2|
        first_pipeline_in_group_2 = all_pipelines_in_group_2[0]

        expect(first_pipeline_in_group_2).to have_selector(".title a", :text => "pipeline-2-1")
        expect(first_pipeline_in_group_2.all(".stages")).to be_empty
        expect(first_pipeline_in_group_2).to have_selector(".status .message", :text => "No historical data")
      end
    end
  end

  it "should display trigger button" do
    render

    expect(response).to have_selector("button[value='Trigger'][id='deploy-pipeline-1']")
    expect(response).to have_selector("button[value='Trigger'][id='deploy-pipeline-2']")
    expect(response).to have_selector("button[value='Trigger'][id='deploy-pipeline-3']")
    expect(response).to have_selector("button[value='Trigger'][id='deploy-pipeline-4']")
  end

  it "should render changes popup content holder" do
    modification = Modification.new(@date=java.util.Date.new, "1234", "label-1", nil)
    modification.setUserName("username")
    modification.setComment("I changed something")
    modification.setModifiedFiles([ModifiedFile.new("nimmappa/foo.txt", "", ModifiedAction::added)])
    svn_revisions = ModificationsMother.createMaterialRevisions(MaterialsMother.svnMaterial("url", "Folder", "user", "pass", true, "*.doc"), modification)
    svn_revisions.getMaterialRevision(0).markAsChanged()
    svn_revisions.materials().get(0).setName(CaseInsensitiveString.new("SvnName"))
    pipeline_model = pipeline_model("blah_pipeline", "blah_label", false, false, "working with agent", false)
    pipeline_model.getLatestPipelineInstance().setMaterialRevisionsOnBuildCause(svn_revisions)
    pipeline_group_model = PipelineGroupModel.new("group-1")
    pipeline_group_model.add(pipeline_model)
    assign(:pipeline_groups, [pipeline_group_model])

    allow(view).to receive(:go_config_service).and_return(config_service = double('go_config_service'))
    config_service.stub(:getCommentRendererFor).with("blah_pipeline").and_return(TrackingTool.new("http://pavan/${ID}", "#(\\d+)"))

    render

    expect(response).to have_selector("#dashboard_build_cause_content")
  end

  it "should display pipelines selector" do
    assign(:pipeline_configs, [PipelineConfigMother.createGroup("group-2",
                                                                   [PipelineConfigMother.pipelineConfig("pipeline-21"),
                                                                    PipelineConfigMother.pipelineConfig("pipeline-22"),
                                                                   ].to_java(PipelineConfig)),
                                  PipelineConfigMother.createGroup("group-1",
                                                                   [PipelineConfigMother.pipelineConfig("pipeline-11"),
                                                                    PipelineConfigMother.pipelineConfig("pipeline-12"),
                                                                   ].to_java(PipelineConfig))])
    assign(:pipeline_selections, PipelineSelections.new(["pipeline-22"]))

    render :partial => 'pipelines/pipelines_selector'

    expect(response).to have_selector("button#show_pipelines_selector")
    Capybara.string(response.body).find("form[action='/pipelines/select_pipelines'] #pipelines_selector_pipelines").tap do |pipeline_selector|
      expect(pipeline_selector).to have_selector("#select_group_group-1[checked]")
      expect(pipeline_selector).to_not have_selector("#select_group_group-2[checked]")
      expect(pipeline_selector).to have_selector("#select_group_group-2")
      expect(pipeline_selector).to have_selector("#select_pipeline_pipeline-11[checked]")
      expect(pipeline_selector).to have_selector("#select_pipeline_pipeline-12[checked]")
      expect(pipeline_selector).to have_selector("#select_pipeline_pipeline-21[checked]")
      expect(pipeline_selector).to_not have_selector("#select_pipeline_pipeline-22[checked]")
      expect(pipeline_selector).to have_selector("#select_pipeline_pipeline-22")
    end
    expect(response.body).to match(/PipelineFilter.initialize\(\)/)
  end

  it "should render pipelines in the group" do
    pipeline_group = double("PipelineGroupModel")
    pipeline_group.stub(:getPipelineModels).and_return(Arrays.asList([PipelineModel.new("SomeModel", true, true, PipelinePauseInfo.not_paused)].to_java))
    pipeline_group.stub(:getName).and_return("PipelineGroupName1")

    stub_template "_pipeline_group.html.erb" => "\"pipeline_group\""
    assign(:pipeline_groups, [pipeline_group])

    allow(view).to receive(:go_config_service).and_return(config_service = double('go_config_service'))
    config_service.stub(:getCommentRendererFor).with("blah_pipeline").and_return(TrackingTool.new("http://pavan/${ID}", "#(\\d+)"))

    render

    assert_template partial: "pipeline_group.html", locals: {scope: {pipeline_group: pipeline_group}}
  end
end
