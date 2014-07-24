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

    assigns[:pipeline] = @pim
    assigns[:variables] = @variables = EnvironmentVariablesConfig.new
  end

  it "should have the same contents as the jsunit fixture" do
    @variables.add("foo","foo_value")
    @variables.add("bar","bar_value")
    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => false}}
    assert_fixture_equal("pipeline_deploy_test.html", response.body)
  end

  it "should display revision number, time and material name/url" do
    config_of_latest_hg_rev = @pim.getMaterials().get(1)
    latest_hg_rev = @hg_revisions.getMaterialRevision(0)
    config_of_latest_hg_rev.setName(CaseInsensitiveString.new("named_hg_material"))

    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => false}}

    response.body.should have_tag("div.change_materials") do
      with_tag(".revision_number[title=1234]", "1234")
      with_tag(".date[title='#{@yesterday.iso8601}']", "1 day ago")
      with_tag(".material_name", "SvnName")
      with_tag(".user", "username")
      with_tag(".comment", "I changed something")
      with_tag(".folder", "Folder")
      with_tag("input[name='material_fingerprint[#{latest_hg_rev.getMaterial().getPipelineUniqueFingerprint()}]']")
    end

    response.body.should have_tag("div.change_materials .folder.not_set", "not-set")
    response.body.should have_tag("button[type='submit'][value='Deploy Changes']")
    response.body.should have_tag("div.change_materials .material_summary") do
      with_tag(".revision_number[title=#{latest_hg_rev.getLatestRevisionString()}]", latest_hg_rev.getLatestShortRevision())
      with_tag(".material_name", "named_hg_material")
    end
  end

  it "should show truncated material name with full name in title" do
    @pim.getMaterials().get(1).setName(CaseInsensitiveString.new("foo_bar_baz_quuz_ban_pavan"))

    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => false}}
    response.should have_tag(".materials .material_name[title=foo_bar_baz_quuz_ban_pavan]", "foo_bar_ba..._ban_pavan")
  end

  it "should disable deploy button when preparing to schedule" do
    @pim.setCanRun(false)
    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => false}}
    response.body.should have_tag("button[type='submit'][disabled='disabled']")
  end

  it "should include hidden input field of original revisions" do
    svn_material = @material_revisions.getMaterialRevision(0).getMaterial()
    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => false}}

    response.body.should have_tag(".material_detail") do
      with_tag(".revision_number[title=1234]", "1234")
      with_tag(".date[title='#{@yesterday.iso8601}']", "1 day ago")
      with_tag(".material_name", "SvnName")
      with_tag(".user", "username")
      with_tag(".comment", "I changed something")
      with_tag(".folder", "Folder")
      with_tag("input.autocomplete-input[name=?]", "material_fingerprint[#{svn_material.getPipelineUniqueFingerprint()}]")
      with_tag("input.original-revision[name=?]", "original_fingerprint[#{svn_material.getPipelineUniqueFingerprint()}]")
      with_tag("input.original-revision[value=?]", '1234')
    end
  end

  it "should include new materials that have never run" do
    @material_configs = MaterialConfigs.new([MaterialConfigsMother.hgMaterialConfig("not-run"), @not_run_with_history_material.config()])
    @pim.setMaterialConfigs(@material_configs)

    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => false}}

    response.body.should have_tag(".material_summary") do
      with_tag(".material_name", "not-run")
      with_tag(".revision_number[title='Latest Available Revision'].updated", "latest")
    end

    response.should have_tag(".material_detail") do
      with_tag(".revision_number[title='N/A']", "N/A")
      with_tag(".date[title='N/A']", "N/A")
      with_tag(".material_name", "not-run")
      with_tag(".user", "N/A")
      with_tag(".comment", "N/A")
      with_tag(".folder", "not-set")
      with_tag("#material-number-0-latest[title='Latest Available Revision']", "latest")
    end

    response.body.should have_tag(".material_detail") do
      with_tag(".revision_number[title='N/A']", "N/A")
      with_tag(".date[title='N/A']", "N/A")
      with_tag(".material_name", "not-run")
      with_tag(".user", "N/A")
      with_tag(".comment", "N/A")
      with_tag(".folder", "not-set")
      with_tag("#material-number-0-latest[title='Latest Available Revision']", "latest")
    end
  end

  it "should include the labels" do
    svn_material = @material_revisions.getMaterialRevision(0).getMaterial()
    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => false}}

    response.body.should have_tag(".material_detail dt") do |dt|
      text_for(dt[0]).should == "Subversion"
      text_for(dt[1]).should == "Dest:"
      text_for(dt[2]).should == "Date:"
      text_for(dt[3]).should == "User:"
      text_for(dt[4]).should == "Comment:"
      text_for(dt[5]).should == "Currently Deployed:"
      text_for(dt[6]).should == "Revision to Deploy:"
    end
  end

  def text_for(dt)
    dt.children[0].to_s
  end
end
