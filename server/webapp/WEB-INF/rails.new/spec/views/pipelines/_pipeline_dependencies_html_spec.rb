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



describe "/pipelines/_pipeline_dependencies.html.erb" do
  before(:each)  do

    @yesterday = org.joda.time.DateTime.new.minusDays(1).toDate()
    job_history = JobHistory.withJob("unit", JobState::Completed, JobResult::Passed, @yesterday)
    stage1 = StageInstanceModel.new("stage", "21", StageResult::Passed, StageIdentifier.new("pipeline-name", 23, "stage", "21"))
    stage2 = StageInstanceModel.new("stage-1", "2", StageResult::Passed, StageIdentifier.new("pipeline-name", 23, "stage-1", "2"))
    stage3 = StageInstanceModel.new("stage-2", "2", StageResult::Cancelled, StageIdentifier.new("pipeline-name", 23, "stage-2", "2"))

    stage1.setBuildHistory(job_history);
    stage2.setBuildHistory(job_history);

    stages = StageInstanceModels.new;
    stages.add( stage1)
    stages.add( stage2)
    @pim = PipelineHistoryMother.singlePipeline("pipeline-name", stages)
    modification = Modification.new(@yesterday, "1234", "label-1", nil)

    @svn_revisions = ModificationsMother.createSvnMaterialRevisions(modification)
    @svn_revisions.getMaterialRevision(0).markAsChanged()
    @svn_revisions.materials().get(0).setName(CaseInsensitiveString.new("SvnName"))


    @dependency_revisions = ModificationsMother.changedDependencyMaterialRevision("up_pipeline", 10, "label-10", "up_stage", 5, Time.now)

    @svn_revisions.addRevision(@dependency_revisions)
    @pim.setMaterialRevisionsOnBuildCause(@svn_revisions)

    @down1 = PipelineHistoryMother.pipelineHistoryItemWithOneStage("down1", "stage", java.util.Date.new())
    @down1.setMaterialConfigs(MaterialConfigs.new([MaterialConfigsMother.dependencyMaterialConfig("pipeline-name", "stage")]))
    @down2 = PipelineHistoryMother.singlePipeline("down2", stages)
    @down2.setMaterialConfigs(MaterialConfigs.new([MaterialConfigsMother.dependencyMaterialConfig("pipeline-name", "stage-1")]))
    @down3 = PipelineHistoryMother.pipelineHistoryItemWithOneStage("down3", "stage-2", java.util.Date.new())
    @down3.setMaterialConfigs(MaterialConfigs.new([MaterialConfigsMother.dependencyMaterialConfig("pipeline-name", "stage-2")]))

    assign :graph, PipelineDependencyGraphOld.new(@pim, PipelineInstanceModels.createPipelineInstanceModels([@down1, @down2, @down3]))
  end

  def render_partial()
    render :partial=> "pipelines/pipeline_dependencies", :locals => {:scope => {}}
  end

  it "should show the current pipeline" do
    render_partial

    Capybara.string(response.body).find(".current .pipeline").tap do |f|
      expect(f).to have_selector(".name a[href='/tab/pipeline/history/pipeline-name']", :text => "pipeline-name")
      expect(f).to have_selector(".content a", :text => "1")
      expect(f).to have_selector(".stages")
    end
  end

  it "should show pipeline links if a pipeline is upstream" do
    dependency_revisions = MaterialRevisions.new([ModificationsMother.dependencyMaterialRevision("up", 2, "label", "dev-stage", 1, @yesterday)].to_java('com.thoughtworks.go.domain.MaterialRevision'))
    @pim.setMaterialRevisionsOnBuildCause(dependency_revisions)

    render_partial

    Capybara.string(response.body).find(".upstream .pipeline .content").tap do |content|
      expect(content).to have_selector(".material.dependency[title='up - up/2']", :text => "Pipeline")
      expect(content).to have_selector("div a[href='/tab/pipeline/history/up']", :text => "up")
      expect(content).to have_selector("a[href='/pipelines/up/2/dev-stage/1/pipeline']", :text => "label")
      expect(content).to have_selector("div:nth-child(3)", :text => "up/2/dev-stage/1")
    end
  end

  it "should show material details if a material is upstream" do
    render_partial
    Capybara.string(response.body).all(".upstream .content div").tap do |divs|
      expect(divs[0]["title"]).to eq "SvnName - 1234"
      expect(divs[0].text).to eq "Subversion"
      expect(divs[1].text).to eq "SvnName - 1234"
    end
  end



  it "should show the downstream pipelines" do
    render_partial
    expect(response).to have_selector(".downstream .pipeline .name a[href='/tab/pipeline/history/down1']", :text => "down1")
    expect(response).to have_selector(".downstream .pipeline .name a[href='/tab/pipeline/history/down2']", :text => "down2")
  end

  it "should show trigger for downstream pipelines and populate the pegged revision" do
    render_partial

    Capybara.string(response.body).find("#downstream_down1").tap do |pipeline|
      expect(pipeline).to have_selector("button#deploy-with-options-down1")
      expect(pipeline).to have_selector("input[type='hidden'][name='pegged_revisions[b5a42fa56a06e4ca35b8e66369065e485b8ceffabf5063438362c67d2bbeaf90]'][value='pipeline-name/23/stage/21']")
    end

    Capybara.string(response.body).find("#downstream_down2").tap do |pipeline|
      expect(pipeline).to have_selector("button#deploy-with-options-down2")
      expect(pipeline).to have_selector("input[type='hidden'][name='pegged_revisions[736d704feab607fa0872cf6ce570fa196dfeb2c3f4d698dd8f0760b3e6aa6b5a]'][value='pipeline-name/23/stage-1/2']")
    end

    Capybara.string(response.body).find("#downstream_down3").tap do |pipeline|
      expect(pipeline).to have_selector("button#deploy-with-options-down3[disabled]")
    end
  end

  it "should disable the trigger if cannot run pipeline" do
    @down1.setCanRun(false)
    @down2.setCanRun(true)

    render_partial

    Capybara.string(response.body).find("#downstream_down1").tap do |pipeline|
      expect(pipeline).to have_selector("button#deploy-with-options-down1[disabled='disabled']")
      expect(pipeline).to have_selector("input[type='hidden'][name='pegged_revisions[b5a42fa56a06e4ca35b8e66369065e485b8ceffabf5063438362c67d2bbeaf90]'][value='pipeline-name/23/stage/21']")
    end

    Capybara.string(response.body).find("#downstream_down2").tap do |pipeline|
      expect(pipeline).to have_selector("button#deploy-with-options-down2")
      expect(pipeline).to have_selector("input[type='hidden'][name='pegged_revisions[736d704feab607fa0872cf6ce570fa196dfeb2c3f4d698dd8f0760b3e6aa6b5a]'][value='pipeline-name/23/stage-1/2']")
    end
  end

  it "should disable the trigger if downstream dependends on an upstream pipeline's stage which has not been run yet" do
    @down1.setCanRun(true)
    @down2.setCanRun(true)

    @pim.stageHistory().remove(@pim.stage('stage-1'))

    render_partial

    Capybara.string(response.body).find("#downstream_down1").tap do |pipeline|
      expect(pipeline).to have_selector("button#deploy-with-options-down1")
      expect(pipeline).to have_selector("input[type='hidden'][name='pegged_revisions[b5a42fa56a06e4ca35b8e66369065e485b8ceffabf5063438362c67d2bbeaf90]'][value='pipeline-name/23/stage/21']")
    end

    Capybara.string(response.body).find("#downstream_down2").tap do |pipeline|
      expect(pipeline).to have_selector("button#deploy-with-options-down2[disabled='disabled']")
      expect(pipeline).to_not have_selector("input[type='hidden'][name='pegged_revisions[736d704feab607fa0872cf6ce570fa196dfeb2c3f4d698dd8f0760b3e6aa6b5a]'][value='pipeline-name/23/stage-1/2']")
    end
  end

  it "should display stage bar for downsteam" do
    render_partial
    Capybara.string(response.body).find("#downstream_down1").tap do |pipeline|
      expect(pipeline).to have_selector("a[href='/pipelines/down1/1/stage/1/pipeline']")
      expect(pipeline).to have_selector(".stages .stage_bar.Unknown[title='stage (Unknown)']")
    end

    Capybara.string(response.body).find("#downstream_down2").tap do |pipeline|
      expect(pipeline).to have_selector("a[href='/pipelines/down2/1/stage/21/pipeline']")
      expect(pipeline).to have_selector(".stages a[href='/pipelines/down2/1/stage/21'] .stage_bar.Passed[title='stage (Passed)']")
      expect(pipeline).to have_selector(".stages a[href='/pipelines/down2/1/stage-1/2'] .stage_bar.Passed[title='stage-1 (Passed)']")
    end
  end

  it "should not display label for downstream pipeline if it has not been triggered yet" do
    render_partial
    Capybara.string(response.body).find("#downstream_down1").tap do |pipeline|
      expect(pipeline).to have_selector("a[href='/pipelines/down1/1/stage/1/pipeline']")
      expect(pipeline).to have_selector(".stages .stage_bar.Unknown[title='stage (Unknown)']")
      expect(pipeline).to_not have_selector(".pipeline_instance a", :text => "unknown")
    end
  end
end
