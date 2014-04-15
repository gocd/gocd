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

def render_environment(pipeline_model)
  render :partial => "environments/environment_pipeline", :locals=>{:scope => {:pipeline_model => pipeline_model}}
end

def create_pipeline_model()
  PipelineModel.new("pipeline", true, true, PipelinePauseInfo::notPaused())
end

describe "/environments/_environment_pipeline.html.erb" do
  include PipelineModelMother

  before do
    class << template
      include StagesHelper
    end
    template.stub!(:is_user_an_admin?).and_return(false)
  end

  it "should show 'no historical data' when there is no history for the pipeline" do
    pipeline_model =  create_pipeline_model()
    pipeline_model.addPipelineInstance(PipelineInstanceModel.createEmptyPipelineInstanceModel("pipeline", BuildCause.createWithEmptyModifications(),  StageInstanceModels.new))

    render_environment(pipeline_model)
    response.body.should have_tag(".status .message", "No historical data")
  end

  it "should display pipeline label and schedule time" do
    now = org.joda.time.DateTime.new
    first_job = PipelineHistoryMother.job(JobState::Completed, JobResult::Cancelled, now.toDate())
    stage = PipelineHistoryMother.stagePerJob("stage", [first_job])
    pipeline_model =  create_pipeline_model()
    pipeline_model.addPipelineInstance(PipelineHistoryMother.singlePipeline("pipeline", stage))

    render_environment(pipeline_model)
    response.body.should have_tag(".status .label", /Label:\s+1/) do
      with_tag "a","1"
    end
    response.body.should have_tag(".status .schedule_time[title='#{now.toDate()}']")
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
    response.body.should have_tag(".latest_stage", "Cancelled: stage-0")
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
    response.body.should have_tag(".stages a .last_run_stage .stage_bar") do |stage|
      first_stage = stage[0]

      first_stage.attributes["style"].should include("width: 9.75em")
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
    response.body.should have_tag(".stages a .stage_bar") do |stage|
      second_stage = stage[1]
      second_stage.attributes["class"].should include("Passed")
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
    response.body.should have_tag(".stages a .stage_bar") do |stage|
      stage[0].attributes["class"].should include("Cancelled")
      stage[0].attributes["title"].should include("stage-0 (Cancelled)")
      stage[1].attributes["class"].should include("Passed")
      stage[1].attributes["title"].should include("stage-1 (Passed)")
    end
  end

  it "should display a single stage with 19.7em width when 1 stage" do
    now = org.joda.time.DateTime.new
    first_run_job = PipelineHistoryMother.job(JobState::Completed, JobResult::Passed, now.toDate())
    stages = PipelineHistoryMother.stagePerJob("stage", [first_run_job])
    pipeline_model =  create_pipeline_model()
    pipeline_model.addPipelineInstance(PipelineHistoryMother.singlePipeline("pipeline", stages))

    render_environment(pipeline_model)
    response.body.should have_tag(".stages a .last_run_stage .stage_bar") do |stage|
      first_stage = stage[0]


      first_stage.attributes["style"].should include("width: 19.75em")
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
    response.body.should have_tag(".deploy") do |deploy|
      deploy.should have_tag("form[action='/api/pipelines/pipeline-name/schedule']") do |form|
        form.should have_tag("button[type='submit'][value='Deploy Latest']")
      end
    end
    response.should have_tag(".has_new_materials")
  end

  it "should show deploy button when no revisions have run and there are no new revisions found (autoUpdate=false)" do
    stage = PipelineHistoryMother.stagePerJob("stage", [PipelineHistoryMother.job(JobState::Completed, JobResult::Cancelled, org.joda.time.DateTime.new.toDate())])

    pim = PipelineInstanceModel.createPipeline("pipeline-name", -1, "1", BuildCause.createNeverRun(), stage);
    pim.setCounter(1)
    pim.setLatestRevisions(MaterialRevisions::EMPTY)
    pipeline_model =  create_pipeline_model()
    pipeline_model.addPipelineInstance(pim)

    render_environment(pipeline_model)
    response.body.should have_tag(".deploy") do |deploy|
      deploy.should have_tag("form[action='/api/pipelines/pipeline-name/schedule']") do |form|
        form.should have_tag("button[type='submit'][value='Deploy Latest']")
      end
    end
    response.should_not have_tag(".warn_message", "new revisions")
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

    response.body.should_not have_tag(".warn_message")
    response.body.should_not have_tag("input#deploy-pipeline-name")
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

    response.body.should_not have_tag(".warn_message")
  end

  it "should disable deploy button when preparing to schedule" do
    pipeline_model =  create_pipeline_model()
    pipeline_model.addPipelineInstance(PipelineInstanceModel.createPreparingToSchedule("pipeline-name", StageInstanceModels.new().addFutureStage("stage-1", false)))

    render_environment(pipeline_model)

    response.body.should have_tag("button#deploy-pipeline-name[disabled='disabled']")
  end

  it "should always show compare link" do
    now = org.joda.time.DateTime.new
    first_run_job = PipelineHistoryMother.job(JobState::Completed, JobResult::Passed, now.toDate())
    stages = PipelineHistoryMother.stagePerJob("stage", [first_run_job])
    pipeline_model =  create_pipeline_model()
    pipeline_model.addPipelineInstance(PipelineHistoryMother.singlePipeline("pipeline", stages))

    render_environment(pipeline_model)

    response.body.should have_tag("span.compare_pipeline")
  end

  it "should render pipeline partial with admin status passed along" do
    pipeline_model =  create_pipeline_model()
    pipeline_model.addPipelineInstance(PipelineInstanceModel.createPreparingToSchedule("pipeline-name", StageInstanceModels.new().addFutureStage("stage-1", false)))
    template.should_receive(:render).with(:partial => "shared/pipeline.html", :locals => {:scope => {:pipeline_model => pipeline_model, :show_compare => true}}).and_return("pipeline")

    render_environment(pipeline_model)
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

      response.body.should have_tag(".deployed_revisions") do
        with_tag(".revision_number[title=1234]", "1234")
        with_tag(".date[title='#{@yesterday.iso8601}']", "1 day ago")
        with_tag(".material_name", "url") #url chosen my material mother
      end

      response.body.should have_tag(".deployed_revisions") do
        with_tag(".revision_number[title=#{latest_hg_rev.getLatestRevisionString()}]", latest_hg_rev.getLatestShortRevision())
        with_tag(".material_name", "named_hg_material")
      end
    end

    it "should include new materials that have never run and no revisions" do
      @pim.setMaterialConfigs(MaterialConfigs.new([MaterialConfigsMother.hgMaterialConfig("not-run")]))

      render environments_partial

      response.body.should have_tag(".deployed_revisions") do
        with_tag(".material_name", "not-run")
        with_tag(".revision_number[title='N/A']", "N/A")
        with_tag(".date[title='N/A']", "N/A")
        without_tag("img[alt='new revisions available']")
      end
    end

    it "should include new materials that have never run and have revisions" do
      material = MaterialsMother.hgMaterial("not-run")
      @pim.setMaterialConfigs(MaterialConfigs.new([material.config()]))

      revisions = MaterialRevisions.new([].to_java(MaterialRevision))
      revisions.addRevision(material, ModificationsMother.multipleModificationsInHg())
      @pim.setLatestRevisions(revisions)

      render environments_partial

      response.body.should have_tag(".deployed_revisions.has_new_materials") do
        with_tag(".material_name", "not-run")
        with_tag(".revision_number[title='N/A']", "N/A")
        with_tag(".date[title='N/A']", "N/A")
      end
    end

    it "should show materials when no history" do
      @pim.setMaterialRevisionsOnBuildCause(MaterialRevisions::EMPTY)

      render environments_partial

      response.body.should have_tag(".deployed_revisions.has_new_materials") do |m|
        with_tag(".material_name", "url")
        with_tag(".revision_number[title='N/A']", "N/A")
        with_tag(".date[title='N/A']", "N/A")
      end

      response.body.should have_tag(".deployed_revisions.has_new_materials") do |m|
        with_tag(".material_name", "hg-url")
        with_tag(".revision_number[title='N/A']", "N/A")
        with_tag(".date[title='N/A']", "N/A")
      end
    end

    it "should show truncated material name with full name in title" do
      @pim.getMaterials().get(1).setName(CaseInsensitiveString.new("foo_bar_baz_quuz_ban_pavan"))
      render environments_partial
      response.should have_tag(".materials .material_name[title=foo_bar_baz_quuz_ban_pavan]", "foo_bar_ba..._ban_pavan")
    end

    it "should show change button for materials" do
      render :partial => "environments/environment_pipeline", :locals=>{:scope => {:pipeline_model => @pipeline_model}}
      response.should have_tag("form button.change_revision[type=submit][value='Deploy Specific Revision']")
    end

    it "should spit javascript in content body if last" do
      render :partial => "environments/environment_pipeline", :locals=>{:scope => {:pipeline_model => @pipeline_model}}
      response.should have_tag("script[type='text/javascript']", "Util.on_load(function() { AjaxRefreshers.main().afterRefreshOf('environment_pipeline_pipeline-name_panel', function() { make_collapsable('environment_pipeline_pipeline-name_panel'); });});")
    end

    it "should show new revisions available image at aggregation level" do
      today = org.joda.time.DateTime.new.toDate()
      @pim.setLatestRevisions(ModificationsMother.createSvnMaterialRevisions(Modification.new(today, "12345", "label-10", nil)))

      render environments_partial
      response.body.should have_tag(".deployed_revisions.has_new_materials")
    end


    it "should show new revisions available image for changed material" do
      today = org.joda.time.DateTime.new.toDate()
      @pim.setLatestRevisions(ModificationsMother.createSvnMaterialRevisions(Modification.new(today, "1234", "label-10", nil)))
      render environments_partial
      response.should have_tag(".deployed_revisions table.materials tr") do |materials|
        materials[0].to_s.should have_tag("th.material_name","Material")
        materials[0].to_s.should have_tag("th.revision_number","Revision")
        materials[0].to_s.should have_tag("th.date","Check-in/trigger")

        materials[1].to_s.should have_tag(".material_name[title='url']")
        materials[1].to_s.should_not have_tag("img")

        materials[2].to_s.should have_tag(".material_name[title='hg-url']")
        materials[2].to_s.should have_tag("img.has_new_material_revisions[src='/images/icon-12-alert.png?N/A']")
      end
    end

    it "should not show new revisions available image for material that has not changed" do
      today = org.joda.time.DateTime.new.toDate()
      @pim.setLatestRevisions(ModificationsMother.createSvnMaterialRevisions(Modification.new(today, "1234", "label-10", nil)))
      render environments_partial
      response.should have_tag(".deployed_revisions .materials img.has_new_material_revisions[src='/images/icon-12-alert.png?N/A']")
    end

    it "should not show new revisions available image at aggregation level when none" do
      today = org.joda.time.DateTime.new.toDate()
      @pim.setLatestRevisions(@material_revisions)
      render environments_partial
      response.body.should_not have_tag(".deployed_revisions img")
    end

    it "should display number of materials in a pipeline" do
      render environments_partial
      response.body.should have_tag(".deployed_revisions a.materials_count", "2 Material(s):")
    end

    it "should not display when buildcause has no materials" do
      @pipeline_model =  create_pipeline_model()
      @pipeline_model.addPipelineInstance(PipelineInstanceModel.createPreparingToSchedule("pipeline-name", StageInstanceModels.new().addFutureStage("stage-1", false)))
      render environments_partial
      response.body.should_not have_tag(".deployed_revisions")
    end

    it "should should wire change button to pipelines controller" do
      render environments_partial
      response.should have_tag("form[action='/pipelines/show']")
    end

  end
end