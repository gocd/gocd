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

require File.expand_path(File.dirname(__FILE__) + '/../../spec_helper')

describe "pipelines/index.html" do
  include PipelineModelMother

  before(:each) do
    @pipeline_group_model = PipelineGroupModel.new("group-1")
    @pipeline_group_model.add(pipeline_model("pipeline-1", "label-1"))
    pipeline_2 = pipeline_model("pipeline-2", "label-2")
    previous_stage = StageInstanceModel.new(name, "2", StageResult::Failed, StageIdentifier.new("pipeline-2", 3, "label-007", "cruise", "2"))
    @active_stage = pipeline_2.getLatestPipelineInstance().getStageHistory().get(0)
    @active_stage.setBuildHistory(JobHistory.withJob("unit", JobState::Building, JobResult::Unknown, Time.now))
    @active_stage.setPreviousStage(previous_stage)
    @pipeline_group_model.add(pipeline_2)
    @pipeline_group_model.add(pipeline_model("pipeline-3", "label-3"))
    @pipeline_group_model.add(pipeline_model("pipeline-4", "label-4"))
    @pipeline_group_model_other = PipelineGroupModel.new("group-2")
    @pipeline_group_model_other.add(pipeline_model("pipeline-2-1", "label-3", true))
    @pipeline_group_model_empty = PipelineGroupModel.new("group-3-empty")
    assigns[:pipeline_selections] = PipelineSelections.new()
    assigns[:pipeline_groups] = [@pipeline_group_model, @pipeline_group_model_other, @pipeline_group_model_empty]
    assigns[:pipeline_configs] = PipelineConfigs.new
    class << template
      include StagesHelper
    end
    template.stub!(:on_pipeline_dashboard?).and_return(true)
  end

  it "should render multiple groups" do
    render 'pipelines/index.html.erb'

    response.should have_tag(".pipeline_group .entity_title", "group-1")
    response.should have_tag(".pipeline_group .entity_title", "group-2")
    response.should_not have_tag(".pipeline_group .entity_title", "group-3-empty")
  end

  it "should render pipeline" do
    render "pipelines/index.html.erb"
    response.should have_tag(".pipeline_group") do
      with_tag(".entity_title", "group-1")
      with_tag(".pipelines .pipeline") do
        with_tag(".title a", "pipeline-1")
        with_tag(".status.details .label a[href=?]", "/pipelines/value_stream_map/pipeline-1/5", "label-1")
        with_tag(".stages") do
          with_tag(".latest_stage", "Passed: cruise")
          with_tag("a.stage[href=?]", "/pipelines/pipeline-1/5/cruise/10") do
            with_tag(".last_run_stage") do
              with_tag(".stage_bar.Passed[title=?]", "cruise (Passed)")
              with_tag(".stage_bar.Passed[data-stage=?]", "cruise")
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
    render "pipelines/index.html.erb"
    response.should have_tag(".pipeline_group") do
      with_tag(".entity_title", "group-1")
      with_tag(".pipelines .pipeline") do
        with_tag("div.pipeline_header div.pipeline_name_link .title a", "pipeline-1")
        with_tag("div.pipeline_header div.pipeline_actions a")
        with_tag("div.pipeline_header div.pipeline_actions span.locked_instance")
        with_tag(".status.details .label a[href=?]", "/pipelines/value_stream_map/pipeline-1/5", "label-1")
        with_tag(".stages") do
          with_tag(".latest_stage", "Passed: cruise")
          with_tag("a.stage[href=?]", "/pipelines/pipeline-1/5/cruise/10") do
            with_tag(".last_run_stage") do
              with_tag(".stage_bar.Passed[title=?]", "cruise (Passed)")
              with_tag(".stage_bar.Passed[data-stage=?]", "cruise")
            end
          end
        end
      end
    end
  end

  it "should render previous status of active stage" do
    render "pipelines/index.html.erb"
    response.should have_tag(".pipeline_group") do
      with_tag(".entity_title", "group-1")

      with_tag(".pipelines .pipeline .pipeline_instance") do
        with_tag(".status.details .label a", "label-2")
        with_tag(".previously") do
          with_tag(".label", "Previously:")
          with_tag("a.result[href=/pipelines/pipeline-2/3/cruise/2][title=label-007]", "Failed")
        end
      end
    end
  end

  it "should skip previous status when active stage does not have previous" do
    @active_stage.setPreviousStage(nil)
    render "pipelines/index.html.erb"
    response.should_not have_tag(".previously")
  end

  it "should add css clear class on every third pipeline" do
    assigns[:pipeline_groups] = [@pipeline_group_model]

    render "pipelines/index.html.erb"
    response.should have_tag("#pipeline_groups_container #pipeline_group_group-1_panel .pipeline_group") do
      with_tag(".entity_title", "group-1")

      with_tag(".pipelines") do |line|
        line.should have_tag(".divider") { |d| d.size.should ==  4}
        line.should have_tag(".pipeline .title a", "pipeline-1")
        line.should have_tag(".pipeline .title a", "pipeline-2")
        line.should have_tag(".pipeline .title a", "pipeline-3")
        line.should have_tag(".pipeline .title a", "pipeline-4")
      end

    end
  end

  it "should support no historical data" do
    render "pipelines/index.html.erb"
    response.should have_tag(".pipeline_group") do
      with_tag(".entity_title", "group-2")
      with_tag(".pipelines .pipeline") do
        with_tag(".title a", "pipeline-2-1")
        with_tag(".status .message", "No historical data")
      end
    end
  end

  it "should display trigger button" do
    render "pipelines/index.html.erb"

    response.should have_tag "button[value='Trigger'][id='deploy-pipeline-1']"
    response.should have_tag "button[value='Trigger'][id='deploy-pipeline-2']"
    response.should have_tag "button[value='Trigger'][id='deploy-pipeline-3']"
    response.should have_tag "button[value='Trigger'][id='deploy-pipeline-4']"
  end

  it "should have the same contents as the jsunit fixture" do
    render 'pipelines/index.html.erb'
    assert_fixture_equal("pipeline_dashboard_test.html", response.body)
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
    assigns[:pipeline_groups] = [pipeline_group_model]

    template.stub!(:go_config_service).and_return(config_service = mock('go_config_service'))
    config_service.stub(:getCommentRendererFor).with("blah_pipeline").and_return(TrackingTool.new("http://pavan/${ID}", "#(\\d+)"))

    render "pipelines/index.html.erb"
    response.body.should have_tag("#dashboard_build_cause_content")
  end

  it "should display pipelines selector" do
    assigns[:pipeline_configs] = [PipelineConfigMother.createGroup("group-2",
                                                                   [PipelineConfigMother.pipelineConfig("pipeline-21"),
                                                                    PipelineConfigMother.pipelineConfig("pipeline-22"),
                                                                   ].to_java(PipelineConfig)),
                                  PipelineConfigMother.createGroup("group-1",
                                                                   [PipelineConfigMother.pipelineConfig("pipeline-11"),
                                                                    PipelineConfigMother.pipelineConfig("pipeline-12"),
                                                                   ].to_java(PipelineConfig))]
    assigns[:pipeline_selections] = PipelineSelections.new(["pipeline-22"])
    render :partial => 'pipelines/pipelines_selector'

    response.should have_tag "button#show_pipelines_selector"
    response.should have_tag "form[action='/pipelines/select_pipelines'] #pipelines_selector_pipelines" do
      with_tag "#select_group_group-1[checked]"
      without_tag "#select_group_group-2[checked]"
      with_tag "#select_group_group-2"
      with_tag "#select_pipeline_pipeline-11[checked]"
      with_tag "#select_pipeline_pipeline-12[checked]"
      with_tag "#select_pipeline_pipeline-21[checked]"
      without_tag "#select_pipeline_pipeline-22[checked]"
      with_tag "#select_pipeline_pipeline-22"
    end
    response.body.should =~ /PipelineFilter.initialize\(\)/
  end

  it "should render pipelines in the group" do
    pipeline_group = mock("PipelineGroupModel")
    pipeline_group.stub(:getPipelineModels).and_return(Arrays.asList([PipelineModel.new("SomeModel", true, true, PipelinePauseInfo.not_paused)].to_java))
    pipeline_group.stub(:getName).and_return("PipelineGroupName1")

    template.should_receive(:render).with(:partial => "pipeline_group.html", :locals => {:scope => {:pipeline_group => pipeline_group}}).and_return("\"pipeline_group\"")
    assigns[:pipeline_groups] = [pipeline_group]

    template.stub!(:go_config_service).and_return(config_service = mock('go_config_service'))
    config_service.stub(:getCommentRendererFor).with("blah_pipeline").and_return(TrackingTool.new("http://pavan/${ID}", "#(\\d+)"))

    render "pipelines/index.html.erb"
  end

end
