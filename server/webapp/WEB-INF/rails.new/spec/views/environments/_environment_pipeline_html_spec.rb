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

def render_environment(pipeline_model)
  render :partial => "environments/environment_pipeline", :locals=>{:scope => {:pipeline_model => pipeline_model}}
end

def create_pipeline_model()
  PipelineModel.new("pipeline", true, true, PipelinePauseInfo::notPaused())
end

describe "/environments/_environment_pipeline.html.erb" do
  include PipelineModelMother

  before do
    class << view
      include StagesHelper
    end
    allow(view).to receive(:is_user_an_admin?).and_return(false)
  end

  it "should show 'no historical data' when there is no history for the pipeline" do
    pipeline_model =  create_pipeline_model()
    pipeline_model.addPipelineInstance(PipelineInstanceModel.createEmptyPipelineInstanceModel("pipeline", BuildCause.createWithEmptyModifications(),  StageInstanceModels.new))

    render_environment(pipeline_model)
    expect(response.body).to have_selector(".status .message", :text => "No historical data")
  end

  it "should display pipeline label and schedule time" do
    now = org.joda.time.DateTime.new
    first_job = PipelineHistoryMother.job(JobState::Completed, JobResult::Cancelled, now.toDate())
    stage = PipelineHistoryMother.stagePerJob("stage", [first_job])
    pipeline_model =  create_pipeline_model()
    pipeline_model.addPipelineInstance(PipelineHistoryMother.singlePipeline("pipeline", stage))

    render_environment(pipeline_model)

    Capybara.string(response.body).find(".status .label", :text => /Label:\s+1/).tap do |label|
      expect(label).to have_selector("a", :text => "1")
    end
    expect(response.body).to have_selector(".status .schedule_time[title='#{now.toDate()}']")
  end

  it "should display status for last active stage" do
    now = org.joda.time.DateTime.new
    tomorrow = now.plusDays(1)
    first_job = PipelineHistoryMother.job(JobState::Completed, JobResult::Cancelled, tomorrow.toDate())
    second_job = PipelineHistoryMother.job(JobState::Completed, JobResult::Passed, now.toDate())
    stage = PipelineHistoryMother.stagePerJob("stage", [first_job, second_job])
    pipeline_model =  create_pipeline_model()
    pipeline_model.addPipelineInstance(PipelineHistoryMother.singlePipeline("pipeline", stage))

    render_environment(pipeline_model)

    expect(response.body).to have_selector(".latest_stage", :text => "Cancelled: stage-0")
  end

  it "should display last run stage with 9.7em width when 2 stages" do
    now = org.joda.time.DateTime.new
    tomorrow = now.plusDays(1)
    first_job = PipelineHistoryMother.job(JobState::Completed, JobResult::Cancelled, tomorrow.toDate())
    second_job = PipelineHistoryMother.job(JobState::Completed, JobResult::Passed, now.toDate())
    stage = PipelineHistoryMother.stagePerJob("stage", [first_job, second_job])
    pipeline_model =  create_pipeline_model()
    pipeline_model.addPipelineInstance(PipelineHistoryMother.singlePipeline("pipeline", stage))

    render_environment(pipeline_model)

    Capybara.string(response.body).all(".stages a .last_run_stage .stage_bar").tap do |stages|
      expect(stages[0]["style"]).to match(/width: 9.75em/)
    end
  end

  it "should display not run stage with 9.9em width when 2 stages" do
    now = org.joda.time.DateTime.new
    tomorrow = now.plusDays(1)
    last_run__job = PipelineHistoryMother.job(JobState::Completed, JobResult::Cancelled, tomorrow.toDate())
    first_run_job = PipelineHistoryMother.job(JobState::Completed, JobResult::Passed, now.toDate())
    stages = PipelineHistoryMother.stagePerJob("stage", [last_run__job, first_run_job])
    pipeline_model =  create_pipeline_model()
    pipeline_model.addPipelineInstance(PipelineHistoryMother.singlePipeline("pipeline", stages))

    render_environment(pipeline_model)

    Capybara.string(response.body).all(".stages a .stage_bar") do |stages|
      second_stage = stages[1]
      expect(second_stage["class"]).to match("Passed")
      second_stage.attributes["class"].should_not include("last_run_stage")
      second_stage.attributes["style"].should include("width: 9.9167em")
    end
  end

  it "should display stage status and title" do
    now = org.joda.time.DateTime.new
    tomorrow = now.plusDays(1)
    cancelled_job = PipelineHistoryMother.job(JobState::Completed, JobResult::Cancelled, tomorrow.toDate())
    passed_job = PipelineHistoryMother.job(JobState::Completed, JobResult::Passed, now.toDate())
    stages = PipelineHistoryMother.stagePerJob("stage", [cancelled_job, passed_job])
    pipeline_model =  create_pipeline_model()
    pipeline_model.addPipelineInstance(PipelineHistoryMother.singlePipeline("pipeline", stages))

    render_environment(pipeline_model)

    Capybara.string(response.body).all(".stages a .stage_bar").tap do |stages|
      expect(stages[0]["class"]).to match("Cancelled")
      expect(stages[0]["title"]).to match("stage-0 \\(Cancelled\\)")
      expect(stages[1]["class"]).to match("Passed")
      expect(stages[1]["title"]).to match("stage-1 \\(Passed\\)")
    end
  end

  it "should display a single stage with 19.7em width when 1 stage" do
    now = org.joda.time.DateTime.new
    first_run_job = PipelineHistoryMother.job(JobState::Completed, JobResult::Passed, now.toDate())
    stages = PipelineHistoryMother.stagePerJob("stage", [first_run_job])
    pipeline_model =  create_pipeline_model()
    pipeline_model.addPipelineInstance(PipelineHistoryMother.singlePipeline("pipeline", stages))

    render_environment(pipeline_model)

    Capybara.string(response.body).all(".stages a .last_run_stage .stage_bar").tap do |stages|
      expect(stages[0]["style"]).to match("width: 19.75em")
    end
  end

  it "should show deploy button and new revisions available message only when there are new modifications" do
    now = org.joda.time.DateTime.new
    tomorrow = now.plusDays(1)
    first_job = PipelineHistoryMother.job(JobState::Completed, JobResult::Cancelled, tomorrow.toDate())
    second_job = PipelineHistoryMother.job(JobState::Completed, JobResult::Passed, now.toDate())
    stage = PipelineHistoryMother.stagePerJob("stage", [first_job, second_job])
    pim = PipelineHistoryMother.singlePipeline("pipeline-name", stage)
    pim.setLatestRevisions(MaterialRevisions::EMPTY)
    pim.setMaterialConfigs(MaterialConfigs.new([MaterialConfigsMother.hgMaterialConfig()]))

    pipeline_model =  create_pipeline_model()
    pipeline_model.addPipelineInstance(pim)

    render_environment(pipeline_model)

    Capybara.string(response.body).find(".deploy").tap do |deploy|
      deploy.find("form[action='/api/pipelines/pipeline-name/schedule']").tap do |form|
        expect(form).to have_selector("button[type='submit'][value='Deploy Latest']")
      end
    end
    expect(response).to have_selector(".has_new_materials")
  end

  it "should show deploy button when no revisions have run and there are no new revisions found (autoUpdate=false)" do
    stage = PipelineHistoryMother.stagePerJob("stage", [PipelineHistoryMother.job(JobState::Completed, JobResult::Cancelled, org.joda.time.DateTime.new.toDate())])

    pim = PipelineInstanceModel.createPipeline("pipeline-name", -1, "1", BuildCause.createNeverRun(), stage)
    pim.setCounter(1)
    pim.setLatestRevisions(MaterialRevisions::EMPTY)
    pipeline_model =  create_pipeline_model()
    pipeline_model.addPipelineInstance(pim)

    render_environment(pipeline_model)

    Capybara.string(response.body).find(".deploy").tap do |deploy|
      deploy.find("form[action='/api/pipelines/pipeline-name/schedule']").tap do |form|
        expect(form).to have_selector("button[type='submit'][value='Deploy Latest']")
      end
    end
    expect(response).to_not have_selector(".warn_message", :text => "new revisions")
  end

  it "should NOT show deploy button when no new modifications" do
    now = org.joda.time.DateTime.new
    tomorrow = now.plusDays(1)
    first_job = PipelineHistoryMother.job(JobState::Completed, JobResult::Cancelled, tomorrow.toDate())
    second_job = PipelineHistoryMother.job(JobState::Completed, JobResult::Passed, now.toDate())
    stage = PipelineHistoryMother.stagePerJob("stage", [first_job, second_job])
    pim = PipelineHistoryMother.singlePipeline("pipeline-name", stage)

    material_revisions = ModificationsMother.createHgMaterialRevisions()
    pim.setLatestRevisions(material_revisions)
    pim.setMaterialRevisionsOnBuildCause(material_revisions)

    hg_material = material_revisions.getMaterialRevision(0).getMaterial()
    pim.setMaterialConfigs(MaterialConfigs.new([hg_material.config()]))

    pipeline_model =  create_pipeline_model()
    pipeline_model.addPipelineInstance(pim)

    render_environment(pipeline_model)

    expect(response.body).to_not have_selector(".warn_message")
    expect(response.body).to_not have_selector("input#deploy-pipeline-name")
  end

  it "should show new revisions message only when there are new modifications" do
    now = org.joda.time.DateTime.new
    tomorrow = now.plusDays(1)
    first_job = PipelineHistoryMother.job(JobState::Completed, JobResult::Cancelled, tomorrow.toDate())
    second_job = PipelineHistoryMother.job(JobState::Completed, JobResult::Passed, now.toDate())
    stage = PipelineHistoryMother.stagePerJob("stage", [first_job, second_job])
    pim = PipelineHistoryMother.singlePipeline("pipeline-name", stage)
    pim.setLatestRevisions(pim.getBuildCause().getMaterialRevisions())

    hg_material = pim.getCurrentRevisions().getMaterialRevision(0).getMaterial()
    pim.setMaterialConfigs(MaterialConfigs.new([hg_material.config()]))

    pipeline_model =  create_pipeline_model()
    pipeline_model.addPipelineInstance(pim)

    render_environment(pipeline_model)

    expect(response.body).to_not have_selector(".warn_message")
  end

  it "should disable deploy button when preparing to schedule" do
    pipeline_model =  create_pipeline_model()
    pipeline_model.addPipelineInstance(PipelineInstanceModel.createPreparingToSchedule("pipeline-name", StageInstanceModels.new().addFutureStage("stage-1", false)))

    render_environment(pipeline_model)

    expect(response.body).to have_selector("button#deploy-pipeline-name[disabled='disabled']")
  end

  it "should always show compare link" do
    now = org.joda.time.DateTime.new
    first_run_job = PipelineHistoryMother.job(JobState::Completed, JobResult::Passed, now.toDate())
    stages = PipelineHistoryMother.stagePerJob("stage", [first_run_job])
    pipeline_model =  create_pipeline_model()
    pipeline_model.addPipelineInstance(PipelineHistoryMother.singlePipeline("pipeline", stages))

    render_environment(pipeline_model)

    expect(response.body).to have_selector("span.compare_pipeline")
  end

  it "should render pipeline partial with admin status passed along" do
    pipeline_model_1 =  create_pipeline_model()
    pipeline_model_1.addPipelineInstance(PipelineInstanceModel.createPreparingToSchedule("pipeline-name", StageInstanceModels.new().addFutureStage("stage-1", false)))

    stub_template "shared/_pipeline.html.erb" => "PIPELINE_PARTIAL"

    render_environment(pipeline_model_1)

    assert_template partial: "shared/_pipeline.html" , locals: {scope: {pipeline_model: pipeline_model_1, show_compare: true}}
    expect(rendered).to match("PIPELINE_PARTIAL")
  end

  describe :deployed_revisions do

    def environments_partial
      {:partial => "environments/environment_pipeline", :locals=>{:scope => {:pipeline_model => @pipeline_model}}}
    end

    before(:each)  do
      @yesterday = org.joda.time.DateTime.new.minusDays(1).toDate()
      job_history = JobHistory.new()
      job_history.addJob("unit", JobState::Completed, JobResult::Passed, @yesterday)
      stage_history = StageInstanceModels.new()
      stage_history.addStage("stage-1", job_history)
      @pim = PipelineHistoryMother.singlePipeline("pipeline-name", stage_history)

      @material_revisions = ModificationsMother.createSvnMaterialRevisions(Modification.new(@yesterday, "1234", "label-10", nil))
      @hg_revisions = ModificationsMother.createHgMaterialRevisions()
      @material_revisions.addAll(@hg_revisions)
      @pim.setMaterialRevisionsOnBuildCause(@material_revisions)

      svn_material = @material_revisions.getMaterialRevision(0).getMaterial()
      hg_material = @material_revisions.getMaterialRevision(1).getMaterial()
      @materials = MaterialConfigs.new([svn_material.config(), hg_material.config()])
      @pim.setMaterialConfigs(@materials)

      @pim.setLatestRevisions(@material_revisions)

      @pipeline_model =  create_pipeline_model()
      @pipeline_model.addPipelineInstance(@pim)
    end

    it "should display revision number, time and material name/url" do
      latest_hg_rev = @hg_revisions.getMaterialRevision(0)
      @pim.getMaterials().get(1).setName(CaseInsensitiveString.new("named_hg_material"))

      render environments_partial

      Capybara.string(response.body).find(".deployed_revisions").tap do |deployed_revisions|
        expect(deployed_revisions).to have_selector(".revision_number[title='1234']", :text => "1234")
        expect(deployed_revisions).to have_selector(".date[title='#{@yesterday.iso8601}']", :text => "1 day ago")
        expect(deployed_revisions).to have_selector(".material_name", :text => "url") #url chosen my material mother

        expect(deployed_revisions).to have_selector(".revision_number[title='#{latest_hg_rev.getLatestRevisionString()}']", :text => "#{latest_hg_rev.getLatestShortRevision()}")
        expect(deployed_revisions).to have_selector(".material_name", :text => "named_hg_material")
      end
    end

    it "should include new materials that have never run and no revisions" do
      @pim.setMaterialConfigs(MaterialConfigs.new([MaterialConfigsMother.hgMaterialConfig("not-run")]))

      render environments_partial

      Capybara.string(response.body).find(".deployed_revisions").tap do |deployed_revisions|
        expect(deployed_revisions).to have_selector(".material_name", :text => "not-run")
        expect(deployed_revisions).to have_selector(".revision_number[title='N/A']", :text => "N/A")
        expect(deployed_revisions).to have_selector(".date[title='N/A']", :text => "N/A")
        expect(deployed_revisions).to_not have_selector("img[alt='new revisions available']")
      end
    end

    it "should include new materials that have never run and have revisions" do
      material = MaterialsMother.hgMaterial("not-run")
      @pim.setMaterialConfigs(MaterialConfigs.new([material.config()]))

      revisions = MaterialRevisions.new([].to_java(MaterialRevision))
      revisions.addRevision(material, ModificationsMother.multipleModificationsInHg())
      @pim.setLatestRevisions(revisions)

      render environments_partial
      Capybara.string(response.body).find(".deployed_revisions.has_new_materials").tap do |deployed_revisions_has_new_materials|
        expect(deployed_revisions_has_new_materials).to have_selector(".material_name", :text => "not-run")
        expect(deployed_revisions_has_new_materials).to have_selector(".revision_number[title='N/A']", :text => "N/A")
        expect(deployed_revisions_has_new_materials).to have_selector(".date[title='N/A']", :text => "N/A")
      end
    end

    it "should show materials when no history" do
      @pim.setMaterialRevisionsOnBuildCause(MaterialRevisions::EMPTY)

      render environments_partial

      Capybara.string(response.body).find(".deployed_revisions.has_new_materials").tap do |deployed_revisions_has_new_materials|

        expect(deployed_revisions_has_new_materials).to have_selector(".material_name", :text => "url")
        expect(deployed_revisions_has_new_materials).to have_selector(".revision_number[title='N/A']", :text => "N/A")
        expect(deployed_revisions_has_new_materials).to have_selector(".date[title='N/A']", :text => "N/A")

        expect(deployed_revisions_has_new_materials).to have_selector(".material_name", :text => "hg-url")
        expect(deployed_revisions_has_new_materials).to have_selector(".revision_number[title='N/A']", :text => "N/A")
        expect(deployed_revisions_has_new_materials).to have_selector(".date[title='N/A']", :text => "N/A")
      end
    end

    it "should show truncated material name with full name in title" do
      @pim.getMaterials().get(1).setName(CaseInsensitiveString.new("foo_bar_baz_quuz_ban_pavan"))

      render environments_partial

      expect(response.body).to have_selector(".materials .material_name[title=foo_bar_baz_quuz_ban_pavan]", :text => "foo_bar_ba..._ban_pavan")
    end

    it "should show change button for materials" do
      render :partial => "environments/environment_pipeline", :locals=>{:scope => {:pipeline_model => @pipeline_model}}

      expect(response.body).to have_selector("form button.change_revision[type=submit][value='Deploy Specific Revision']")
    end

    it "should spit javascript in content body if last" do
      render :partial => "environments/environment_pipeline", :locals=>{:scope => {:pipeline_model => @pipeline_model}}

      expect(response.body).to have_selector("script[type='text/javascript']", :text => "Util.on_load(function() { AjaxRefreshers.main().afterRefreshOf('environment_pipeline_pipeline-name_panel', function() { make_collapsable('environment_pipeline_pipeline-name_panel'); });});", :visible => false)
    end

    it "should show new revisions available image at aggregation level" do
      today = org.joda.time.DateTime.new.toDate()
      @pim.setLatestRevisions(ModificationsMother.createSvnMaterialRevisions(Modification.new(today, "12345", "label-10", nil)))

      render environments_partial

      expect(response.body).to have_selector(".deployed_revisions.has_new_materials")
    end


    it "should show new revisions available image for changed material" do
      today = org.joda.time.DateTime.new.toDate()
      @pim.setLatestRevisions(ModificationsMother.createSvnMaterialRevisions(Modification.new(today, "1234", "label-10", nil)))

      render environments_partial

      Capybara.string(response.body).all(".deployed_revisions table.materials tr").tap do |materials|
        expect(materials[0]).to have_selector("th.material_name", :text => "Material")
        expect(materials[0]).to have_selector("th.revision_number", :text => "Revision")
        expect(materials[0]).to have_selector("th.date", :text => "Check-in/trigger")

        expect(materials[1]).to have_selector(".material_name[title='url']")
        expect(materials[1]).to_not have_selector("img")

        expect(materials[2]).to have_selector(".material_name[title='hg-url']")
        expect(materials[2]).to have_selector("img.has_new_material_revisions[src='/assets/icon-12-alert.png']")
      end
    end

    it "should not show new revisions available image for material that has not changed" do
      today = org.joda.time.DateTime.new.toDate()
      @pim.setLatestRevisions(ModificationsMother.createSvnMaterialRevisions(Modification.new(today, "1234", "label-10", nil)))

      render environments_partial

      expect(response.body).to have_selector(".deployed_revisions .materials img.has_new_material_revisions[src='/assets/icon-12-alert.png']")
    end

    it "should not show new revisions available image at aggregation level when none" do
      today = org.joda.time.DateTime.new.toDate()
      @pim.setLatestRevisions(@material_revisions)

      render environments_partial

      expect(response.body).to_not have_selector(".deployed_revisions img")
    end

    it "should display number of materials in a pipeline" do
      render environments_partial

      expect(response.body).to have_selector(".deployed_revisions a.materials_count", :text => "2 Material(s):")
    end

    it "should not display when buildcause has no materials" do
      @pipeline_model =  create_pipeline_model()
      @pipeline_model.addPipelineInstance(PipelineInstanceModel.createPreparingToSchedule("pipeline-name", StageInstanceModels.new().addFutureStage("stage-1", false)))

      render environments_partial

      expect(response.body).to_not have_selector(".deployed_revisions")
    end

    it "should should wire change button to pipelines controller" do
      render environments_partial

      expect(response.body).to have_selector("form[action='/pipelines/show']")
    end
  end
end
