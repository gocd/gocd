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


describe "/pipelines/show_for_trigger.html.erb" do
  before(:each) do
    @yesterday = org.joda.time.DateTime.new.minusDays(1).toDate()
    @today = java.util.Date.new()
    job_history = JobHistory.new()
    job_history.addJob("unit", JobState::Completed, JobResult::Passed, @yesterday)
    stage_history = StageInstanceModels.new()
    stage_history.addStage("stage-1", job_history)
    @pim = PipelineHistoryMother.singlePipeline("pipline-name", stage_history)

    @material_revisions = create_svn_material_revisions("Folder", "SvnName", @modification = create_modification(@yesterday, "1234", "username", "I changed something"))
    @hg_revisions = ModificationsMother.createHgMaterialRevisions()
    @material_revisions.addAll(@hg_revisions)

    @pim.setMaterialRevisionsOnBuildCause(@material_revisions)

    @latest_material_revisions = create_svn_material_revisions("Folder", "SvnName", create_modification(@today, "1235", "anonymous", "something changed"))
    @latest_material_revisions.add_all(@hg_revisions)

    @pim.setLatestRevisions(@latest_material_revisions)

    svn_material = @material_revisions.getMaterialRevision(0).getMaterial()
    hg_material = @material_revisions.getMaterialRevision(1).getMaterial()
    @material_configs = MaterialConfigs.new([svn_material.config(), hg_material.config()])
    @pim.setMaterialConfigs(@material_configs)

    @not_run_with_history_material = MaterialsMother.hgMaterial("not-run-with-history")

    assign(:pipeline, @pim)
    assign(:variables, @variables = EnvironmentVariablesConfig.new())

  end

  def create_modification(date, rev, username, comment)
    modification = Modification.new(username, comment, "foo@bar.com", date, rev)
    modification
  end

  def create_svn_material_revisions(folder, name, modification)
    material_revisions = ModificationsMother.createMaterialRevisions(MaterialsMother.svnMaterial("url", folder, "user", "pass", true, "*.doc"), modification)
    material_revisions.materials().get(0).setName(CaseInsensitiveString.new(name))
    material_revisions
  end

  it "should display revision number, time and material name/url" do
    config_of_latest_hg_rev = @pim.getMaterials().get(1)
    latest_hg_rev = @hg_revisions.getMaterialRevision(0)
    config_of_latest_hg_rev.setName(CaseInsensitiveString.new("named_hg_material"))

    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => true}}

    Capybara.string(response.body).find("div.change_materials").tap do |div|
      expect(div).to have_selector(".revision_number[title='1234']", :text => "1234")
      expect(div).to have_selector(".date[title='#{@yesterday.iso8601}']", :text => "1 day ago")
      expect(div).to have_selector(".material_name", :text => "SvnName")
      expect(div).to have_selector(".user", :text => "username")
      expect(div).to have_selector(".comment", :text => "I changed something")
      expect(div).to have_selector(".folder", :text => "Folder")
      expect(div).to have_selector("input[name='material_fingerprint[#{latest_hg_rev.getMaterial().getPipelineUniqueFingerprint()}]']")
    end

    expect(response.body).to have_selector("div.change_materials .folder.not_set", :text => "not-set")
    expect(response.body).to have_selector("button[type='submit'][value='Trigger Pipeline']")

    Capybara.string(response.body).all("div.change_materials .material_summary").tap do |divs|
      expect(divs[1]).to have_selector(".revision_number[title='#{latest_hg_rev.getLatestRevisionString()}']", :text => latest_hg_rev.getLatestShortRevision())
      expect(divs[1]).to have_selector(".material_name", :text => "named_hg_material")
    end
  end

  it "should html escape user name and comment" do
    latest_hg_rev = @hg_revisions.getMaterialRevision(0)
    latest_hg_rev.getMaterial().setName(CaseInsensitiveString.new("named_hg_material"))
    @modification.setComment("<script>alert('Check-in comment')</script>")
    @modification.setUserName("<script>alert('Check-in user')</script>")

    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => true}}

    Capybara.string(response.body).find("div.change_materials").tap do |div|
      expect(div).to have_selector(".user", :text => "<script>alert('Check-in user')</script>")
      expect(div).to have_selector(".comment", :text => "<script>alert('Check-in comment')</script>")
    end
  end

  it "should show truncated material name with full name in title" do
    @pim.getMaterials().get(1).setName(CaseInsensitiveString.new("foo_bar_baz_quuz_ban_pavan"))

    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => true}}

    expect(response.body).to have_selector(".materials .material_name[title=foo_bar_baz_quuz_ban_pavan]", :text => "foo_bar_ba..._ban_pavan")
  end

  it "should disable trigger_with_options button when preparing to schedule" do
    @pim.setCanRun(false)
    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => true}}

    expect(response.body).to have_selector("button[type='submit'][disabled='disabled']")
  end

  it "should include hidden input field of original revisions" do
    svn_material = @material_revisions.getMaterialRevision(0).getMaterial()
    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => true}}

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

  it "should include the labels" do
    svn_material = @material_revisions.getMaterialRevision(0).getMaterial()
    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => true}}

    Capybara.string(response.body).all(".material_detail dt").tap do |dts|
      dts[0].text.should == "Subversion"
      dts[1].text.should == "Dest:"
      dts[2].text.should == "Date:"
      dts[3].text.should == "User:"
      dts[4].text.should == "Comment:"
      dts[5].text.should == "Last run with:"
      dts[6].text.should == "Revision to trigger with:"
    end
  end

  it "should include modifications for new pipeline if using old material" do
    @material_configs = MaterialConfigs.new([MaterialConfigsMother.hgMaterialConfig("not-run"), @not_run_with_history_material.config()])
    @pim.setMaterialConfigs(@material_configs)
    revisions = ModificationsMother.empty()
    revisions.addRevision(MaterialRevision.new(@not_run_with_history_material, [create_modification(@today, "123", "user", "changes")]))
    @pim.setLatestRevisions(revisions)
    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => true}}

    Capybara.string(response.body).all(".material_detail").tap do |divs|
      expect(divs[1]).to have_selector(".revision_number[title='N/A']", :text => "N/A")
      expect(divs[1]).to have_selector(".date[title='N/A']", :text => "N/A")
      expect(divs[1]).to have_selector(".material_name", :text => "not-run-with-history")
      expect(divs[1]).to have_selector(".user", :text => "N/A")
      expect(divs[1]).to have_selector(".comment", :text => "N/A")
      expect(divs[1]).to have_selector(".folder", :text => "not-set")
      expect(divs[1]).to have_selector("input#material-number-1-original[type='hidden'][value='123']", visible: false)
      expect(divs[1]).to have_selector("input#material-number-1-autocomplete[type='text']")
    end

    Capybara.string(response.body).all(".material_summary").tap do |divs|
      expect(divs[1]).to have_selector(".material_name", :text => "not-run-with-history")
      expect(divs[1]).to have_selector(".revision_number[title='Latest Available Revision'].updated", :text => "latest")
    end
  end

  it "should use the pegged revision that is passed in for a given material" do
    hg_material = MaterialConfigsMother.hgMaterialConfig("not-run")
    @material_configs = MaterialConfigs.new([hg_material, @not_run_with_history_material.config()])
    @pim.setMaterialConfigs(@material_configs)
    revisions = ModificationsMother.empty()
    revisions.addRevision(MaterialRevision.new(@not_run_with_history_material, [create_modification(@today, "123", "user", "changes")]))
    @pim.setLatestRevisions(revisions)

    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => true, :pegged_revisions => {@not_run_with_history_material.getPipelineUniqueFingerprint() => "abc"}}}

    Capybara.string(response.body).all(".material_detail").tap do |divs|
      expect(divs[1]).to have_selector(".revision_number[title='N/A']", :text => "N/A")
      expect(divs[1]).to have_selector(".date[title='N/A']", :text => "N/A")
      expect(divs[1]).to have_selector(".material_name", :text => "not-run-with-history")
      expect(divs[1]).to have_selector(".user", :text => "N/A")
      expect(divs[1]).to have_selector(".comment", :text => "N/A")
      expect(divs[1]).to have_selector(".folder", :text => "not-set")
      expect(divs[1]).to have_selector("input#material-number-1-original[type=hidden][value='abc']", visible: false)
      expect(divs[1]).not_to have_selector("input#material-number-1-autocomplete")
      expect(divs[1]).to have_selector("#material-number-1-pegged[title='abc']", :text => "abc")
    end

    Capybara.string(response.body).all(".material_summary").tap do |divs|
      expect(divs[1]).to have_selector(".material_name", :text => "not-run-with-history")
      expect(divs[1]).to have_selector(".revision_number.updated", :text => "abc")
    end
  end

  it "should include new materials that have never run" do
    @material_configs = MaterialConfigs.new([MaterialConfigsMother.hgMaterialConfig("not-run"), @not_run_with_history_material.config()])
    @pim.setMaterialConfigs(@material_configs)

    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => true}}

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
      expect(divs[1]).to have_selector(".material_name", :text => "not-run-with-history")
      expect(divs[1]).to have_selector(".user", :text => "N/A")
      expect(divs[1]).to have_selector(".comment", :text => "N/A")
      expect(divs[1]).to have_selector(".folder", :text => "not-set")
      expect(divs[1]).to have_selector("#material-number-1-latest[title='Latest Available Revision']", :text => "latest")
    end
  end

  it "should render variables" do
    @variables.add("foo", "foo_value")
    @variables.add("bar", "bar_value")
    @variables.add("blah", "<script>&")

    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => true}}

    Capybara.string(response.body).find(".variables", visible: false).tap do |div|
      expect(div).to have_selector("span", :text => "Override Environment and Pipeline level variables:", visible: false)
      @variables.each_with_index do |variable, index|
        expect(div).to have_selector(".variable input[id='variable-#{index}'][type='text'][name='variables[#{variable.getName()}]'][value='#{variable.getValue()}']", visible: false)
        expect(div).to have_selector(".variable label", :text => "#{variable.getName()}", visible: false)
        expect(div).to have_selector(".variable .message.hidden", :text => "Overwritten. Default: #{variable.getValue()}", visible: false)
      end
    end
  end

  it "should not render tab for secure variables when there are none" do
    @variables.add("foo", "foo_value")
    @variables.add("bar", "bar_value")

    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => true}}

    expect(response.body).not_to have_selector(".sub_tabs_container #secure_environment_variables_tab")
  end

  it "should render tab and content for secure variables" do
    @variables.add("foo", "foo_value")
    @variables.add("bar", "bar_value")
    @variables.add(secure_env_variable("secure_foo", "secure_foo_value"))
    @variables.add(secure_env_variable("secure_bar", "secure_bar_value"))

    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => true}}

    Capybara.string(response.body).find(".sub_tabs_container li#secure_environment_variables_tab").tap do |div|
      expect(div).to have_selector("a.tab_button_body_match_text", :text => "secure-environment-variables")
      expect(div).to have_selector("a", :text => "Secure Variables")
    end

    Capybara.string(response.body).find("div.secure_variables", visible: false).tap do |div|
      expect(div).to have_selector("label", :text => "secure_foo", visible: false)
      expect(div).to have_selector("input[type='password'][name='secure_variables[secure_foo]'][value='******']", visible: false)
      expect(div).to have_selector("label", :text => "secure_bar", visible: false)
      expect(div).to have_selector("input[type='password'][name='secure_variables[secure_bar]'][value='******']", visible: false)
    end
  end

  it "should have material tab header" do
    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => true}}

    expect(response.body).to have_selector(".change_materials .sub_tabs_container ul li.current_tab a", :text => "Materials")

    Capybara.string(response.body).all(".change_materials .sub_tabs_container ul li").tap do |lis|
      expect(lis).not_to have_selector("a", :text => "Environment Variables")
    end
  end

  it "should have material tab content" do
    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => true}}

    expect(response.body).to have_selector(".sub_tab_container .sub_tab_container_content #tab-content-of-materials")
    expect(response.body).not_to have_selector(".sub_tab_container .sub_tab_container_content #tab-content-of-environment-variables.variables")
  end

  it "should have environment variables tab header when variables are defined" do
    @variables.add("foo", "foo_value")

    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => true}}

    expect(response.body).to have_selector(".change_materials .sub_tabs_container ul li a", :text => "Environment Variables")
  end

  it "should have environment variables tab contents when variables are defined" do
    @variables.add("foo", "foo_value")

    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => true}}

    expect(response.body).to have_selector(".sub_tab_container .sub_tab_container_content #tab-content-of-environment-variables.variables", visible: false)
  end

  it "should have an override link next to each secure variable name" do
    @variables.add(secure_env_variable("secure_foo", "secure_foo_value"))

    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => true}}

    Capybara.string(response.body).find("div#tab-content-of-secure-environment-variables", visible: false).tap do |div|
      expect(div).to have_selector("label[for='secure_variables_secure_foo']", :text => "secure_foo", visible: false)
      expect(div).to have_selector("a.override", :text => "Override", visible: false)
      expect(div).to have_selector("input[name='secure_variables[secure_foo]'][value='******'][disabled='disabled']", visible: false)
    end
  end

  it "should render process material revision comment for display" do
    view.should_receive(:render_simple_comment).with("comment").and_return("simplified")

    assign(:pipeline, pipeline_instance = double("pipeline instance"))
    pipeline_instance.should_receive(:getName).at_least(:once).and_return("pipeline_name")
    pipeline_instance.should_receive(:getMaterials).at_least(:once).and_return([material = double("material")])
    pipeline_instance.should_receive(:findCurrentMaterialRevisionForUI).at_least(:once).and_return(revision = double("revision"))
    pipeline_instance.should_receive(:getLatestMaterialRevision).at_least(:once).and_return(revision)
    pipeline_instance.should_receive(:canRun).at_least(:once).and_return(true)

    revision.should_receive(:getLatestRevisionString).at_least(:once).and_return("1234")
    revision.should_receive(:getLatestShortRevision).at_least(:once).and_return("1234")
    revision.should_receive(:getDateOfLatestModification).at_least(:once).and_return(java.util.Date.new())
    revision.should_receive(:getLatestUser).at_least(:once).and_return("user")
    revision.should_receive(:getLatestComment).at_least(:once).and_return("comment")
    revision.should_receive(:hasModifications).at_least(:once).and_return(false)

    material.should_receive(:getPipelineUniqueFingerprint).at_least(:once).and_return("fingerprint")
    material.should_receive(:getDisplayName).at_least(:once).and_return("package material")
    material.should_receive(:getTruncatedDisplayName).at_least(:once).and_return("p1")
    material.should_receive(:getTypeForDisplay).at_least(:once).and_return("PACKAGE")
    material.should_receive(:getFolder).at_least(:once).and_return("f2")

    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => true}}

    Capybara.string(response.body).find("div.change_materials").tap do |div|
      expect(div).to have_selector(".revision_number[title='1234']", :text => "1234")
      expect(div).to have_selector(".comment", :text => "simplified")
    end
  end

  def secure_env_variable(key, cipher_text)
    secure_variable = EnvironmentVariableConfig.new(key, "")
    ReflectionUtil.setField(secure_variable, "encryptedValue", EncryptedVariableValueConfig.new(cipher_text))
    ReflectionUtil.setField(secure_variable, "isSecure", true)
    secure_variable
  end
end
