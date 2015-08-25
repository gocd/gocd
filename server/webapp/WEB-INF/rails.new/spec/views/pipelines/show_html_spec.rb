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

describe "/pipelines/show.html.erb" do
  before(:each)  do
    @yesterday = org.joda.time.DateTime.new.minusDays(1).toDate()
    job_history = JobHistory.new()
    job_history.addJob("unit", JobState::Completed, JobResult::Passed, @yesterday)
    stage_history = StageInstanceModels.new()
    stage_history.addStage("stage-1", job_history)
    @pim = PipelineHistoryMother.singlePipeline("pipline-name", stage_history)

    modification = Modification.new(@yesterday, "1234", "label-1234", nil)
    modification.setUserName("username")
    modification.setComment("I changed something")
    @material_revisions = ModificationsMother.createMaterialRevisions(MaterialsMother.svnMaterial("url", "Folder", "user", "pass", true, "*.doc"), modification)
    @material_revisions.materials().get(0).setName(CaseInsensitiveString.new("SvnName"))
    @hg_revisions = ModificationsMother.createHgMaterialRevisions()
    @material_revisions.addAll(@hg_revisions)
    @pim.setMaterialRevisionsOnBuildCause(@material_revisions)

    svn_material_config = @material_revisions.getMaterialRevision(0).getMaterial().config()
    hg_material_config = @material_revisions.getMaterialRevision(1).getMaterial().config()
    @material_configs = MaterialConfigs.new([svn_material_config, hg_material_config])
    @pim.setMaterialConfigs(@material_configs)

    @not_run_with_history_material = MaterialsMother.hgMaterial("not-run-with-history")

    assign(:pipeline, @pim)
    assign(:variables, @variables = EnvironmentVariablesConfig.new)
  end

  it "should display revision number, time and material name/url" do
    config_of_latest_hg_rev = @pim.getMaterials().get(1)
    latest_hg_rev = @hg_revisions.getMaterialRevision(0)
    config_of_latest_hg_rev.setName(CaseInsensitiveString.new("named_hg_material"))

    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => false}}

    Capybara.string(response.body).find("div.change_materials").tap do |div|
      expect(div).to have_selector(".revision_number[title='1234']", :text => "1234")
      expect(div).to have_selector(".date[title='#{@yesterday.iso8601}']", :text => "1 day ago")
      expect(div).to have_selector(".material_name", :text => "SvnName")
      expect(div).to have_selector(".user", :text => "username")
      expect(div).to have_selector(".comment", :text => "I changed something")
      expect(div).to have_selector(".folder", :text => "Folder")
      expect(div).to have_selector("input[name='material_fingerprint[#{latest_hg_rev.getMaterial().getPipelineUniqueFingerprint()}]']")
    end

    expect(response.body).to have_selector("div.change_materials .folder.not_set", "not-set")
    expect(response.body).to have_selector("button[type='submit'][value='Deploy Changes']")

    Capybara.string(response.body).all("div.change_materials .material_summary").tap do |divs|
      expect(divs[1]).to have_selector(".revision_number[title='#{latest_hg_rev.getLatestRevisionString()}']", :text => latest_hg_rev.getLatestShortRevision())
      expect(divs[1]).to have_selector(".material_name", :text => "named_hg_material")
    end
  end

  it "should show truncated material name with full name in title" do
    @pim.getMaterials().get(1).setName(CaseInsensitiveString.new("foo_bar_baz_quuz_ban_pavan"))

    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => false}}

    expect(response.body).to have_selector(".materials .material_name[title=foo_bar_baz_quuz_ban_pavan]", :text => "foo_bar_ba..._ban_pavan")
  end

  it "should disable deploy button when preparing to schedule" do
    @pim.setCanRun(false)

    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => false}}

    expect(response.body).to have_selector("button[type='submit'][disabled='disabled']")
  end

  it "should include hidden input field of original revisions" do
    svn_material = @material_revisions.getMaterialRevision(0).getMaterial()

    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => false}}

    Capybara.string(response.body).all(".material_detail").tap do |divs|
      expect(divs[0]).to have_selector(".revision_number[title='1234']", :text => "1234")
      expect(divs[0]).to have_selector(".date[title='#{@yesterday.iso8601}']", :text => "1 day ago")
      expect(divs[0]).to have_selector(".material_name", :text => "SvnName")
      expect(divs[0]).to have_selector(".user", :text => "username")
      expect(divs[0]).to have_selector(".comment", :text => "I changed something")
      expect(divs[0]).to have_selector(".folder", :text => "Folder")
      expect(divs[0]).to have_selector("input.autocomplete-input[name='material_fingerprint[#{svn_material.getPipelineUniqueFingerprint()}]']")
      expect(divs[0]).to have_selector("input.original-revision[name='original_fingerprint[#{svn_material.getPipelineUniqueFingerprint()}]']")
      expect(divs[0]).to have_selector("input.original-revision[value='1234']")
    end
  end

  it "should include new materials that have never run" do
    @material_configs = MaterialConfigs.new([MaterialConfigsMother.hgMaterialConfig("not-run"), @not_run_with_history_material.config()])
    @pim.setMaterialConfigs(@material_configs)

    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => false}}

    Capybara.string(response.body).all(".material_summary").tap do |divs|
      expect(divs[0]).to have_selector(".material_name", :text => "not-run")
      expect(divs[0]).to have_selector(".revision_number[title='Latest Available Revision'].updated", :text => "latest")
    end

    Capybara.string(response.body).all(".material_detail").tap do |divs|
      expect(divs[0]).to have_selector(".revision_number[title='N/A']", :text => "N/A")
      expect(divs[0]).to have_selector(".date[title='N/A']", :text => "N/A")
      expect(divs[0]).to have_selector(".material_name", :text => "not-run")
      expect(divs[0]).to have_selector(".user", :text => "N/A")
      expect(divs[0]).to have_selector(".comment", :text => "N/A")
      expect(divs[0]).to have_selector(".folder", :text => "not-set")
      expect(divs[0]).to have_selector("#material-number-0-latest[title='Latest Available Revision']", :text => "latest")
    end

    Capybara.string(response.body).all(".material_detail").tap do |divs|
      expect(divs[1]).to have_selector(".revision_number[title='N/A']", :text => "N/A")
      expect(divs[1]).to have_selector(".date[title='N/A']", :text => "N/A")
      expect(divs[1]).to have_selector(".material_name", :text => "not-run")
      expect(divs[1]).to have_selector(".user", :text => "N/A")
      expect(divs[1]).to have_selector(".comment", :text => "N/A")
      expect(divs[1]).to have_selector(".folder", :text => "not-set")
      expect(divs[1]).to have_selector("#material-number-1-latest[title='Latest Available Revision']", :text => "latest")
    end
  end

  it "should include the labels" do
    svn_material = @material_revisions.getMaterialRevision(0).getMaterial()

    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => false}}

    Capybara.string(response.body).all(".material_detail dt") do |dts|
      dts[0].text.should == "Subversion"
      dts[1].text.should == "Dest:"
      dts[2].text.should == "Date:"
      dts[3].text.should == "User:"
      dts[4].text.should == "Comment:"
      dts[5].text.should == "Currently Deployed:"
      dts[6].text.should == "Revision to Deploy:"
    end
  end
end
