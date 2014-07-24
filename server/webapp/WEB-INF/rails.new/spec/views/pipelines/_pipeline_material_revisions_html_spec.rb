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

    assigns[:pipeline] = @pim
    assigns[:variables] = @variables = EnvironmentVariablesConfig.new()

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
    response.body.should have_tag("button[type='submit'][value='Trigger Pipeline']")
    response.body.should have_tag("div.change_materials .material_summary") do
      with_tag(".revision_number[title=#{latest_hg_rev.getLatestRevisionString()}]", latest_hg_rev.getLatestShortRevision())
      with_tag(".material_name", "named_hg_material")
    end
  end

  it "should html escape user name and comment" do
    latest_hg_rev = @hg_revisions.getMaterialRevision(0)
    latest_hg_rev.getMaterial().setName(CaseInsensitiveString.new("named_hg_material"))
    @modification.setComment("<script>alert('Check-in comment')</script>")
    @modification.setUserName("<script>alert('Check-in user')</script>")

    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => true}}

    response.body.should have_tag("div.change_materials") do
      with_tag(".user", "&lt;script&gt;alert('Check-in user')&lt;/script&gt;")
      with_tag(".comment", "&lt;script&gt;alert('Check-in comment')&lt;/script&gt;")
    end
  end

  it "should show truncated material name with full name in title" do
    @pim.getMaterials().get(1).setName(CaseInsensitiveString.new("foo_bar_baz_quuz_ban_pavan"))

    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => true}}
    response.should have_tag(".materials .material_name[title=foo_bar_baz_quuz_ban_pavan]", "foo_bar_ba..._ban_pavan")
  end

  it "should disable trigger_with_options button when preparing to schedule" do
    @pim.setCanRun(false)
    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => true}}
    response.body.should have_tag("button[type='submit'][disabled='disabled']")
  end

  it "should include hidden input field of original revisions" do
    svn_material = @material_revisions.getMaterialRevision(0).getMaterial()
    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => true}}

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

  it "should include the labels" do
    svn_material = @material_revisions.getMaterialRevision(0).getMaterial()
    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => true}}

    response.body.should have_tag(".material_detail dt") do |dt|
      text_for(dt[0]).should == "Subversion"
      text_for(dt[1]).should == "Dest:"
      text_for(dt[2]).should == "Date:"
      text_for(dt[3]).should == "User:"
      text_for(dt[4]).should == "Comment:"
      text_for(dt[5]).should == "Last run with:"
      text_for(dt[6]).should == "Revision to trigger with:"
    end
  end

  def text_for(dt)
    dt.children[0].to_s
  end

  it "should include modifications for new pipeline if using old material" do
    @material_configs = MaterialConfigs.new([MaterialConfigsMother.hgMaterialConfig("not-run"), @not_run_with_history_material.config()])
    @pim.setMaterialConfigs(@material_configs)
    revisions = ModificationsMother.empty()
    revisions.addRevision(MaterialRevision.new(@not_run_with_history_material, [create_modification(@today, "123", "user", "changes")]))
    @pim.setLatestRevisions(revisions)
    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => true}}

    response.body.should have_tag(".material_detail") do
      with_tag(".revision_number[title='N/A']", "N/A")
      with_tag(".date[title='N/A']", "N/A")
      with_tag(".material_name", "not-run-with-history")
      with_tag(".user", "N/A")
      with_tag(".comment", "N/A")
      with_tag(".folder", "not-set")
      with_tag("input#material-number-1-original[type=hidden][value='123']")
      with_tag("input#material-number-1-autocomplete[type='text']")
    end

    response.body.should have_tag(".material_summary") do
      with_tag(".material_name", "not-run-with-history")
      with_tag(".revision_number[title='Latest Available Revision'].updated", "latest")
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

    response.body.should have_tag(".material_detail") do
      with_tag(".revision_number[title='N/A']", "N/A")
      with_tag(".date[title='N/A']", "N/A")
      with_tag(".material_name", "not-run-with-history")
      with_tag(".user", "N/A")
      with_tag(".comment", "N/A")
      with_tag(".folder", "not-set")
      with_tag("input#material-number-1-original[type=hidden][value='abc']")
      without_tag("input#material-number-1-autocomplete")
      with_tag("#material-number-1-pegged[title='abc']", "abc")
    end

    response.body.should have_tag(".material_summary") do
      with_tag(".material_name", "not-run-with-history")
      with_tag(".revision_number.updated", "abc")
    end
  end

  it "should include new materials that have never run" do
    @material_configs = MaterialConfigs.new([MaterialConfigsMother.hgMaterialConfig("not-run"), @not_run_with_history_material.config()])
    @pim.setMaterialConfigs(@material_configs)

    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => true}}

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
      with_tag(".material_name", "not-run-with-history")
      with_tag(".user", "N/A")
      with_tag(".comment", "N/A")
      with_tag(".folder", "not-set")
      with_tag("#material-number-1-latest[title='Latest Available Revision']", "latest")
    end
  end

  it "should render variables" do
    @variables.add("foo", "foo_value")
    @variables.add("bar", "bar_value")
    @variables.add("blah", "<script>&")
    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => true}}

    response.should have_tag(".variables") do
      with_tag "span", "Override Environment and Pipeline level variables:"
      @variables.each_with_index do |variable, index|
        with_tag ".variable input[id='variable-#{index}'][type='text'][name='variables[#{variable.getName()}]'][value='#{variable.getValue()}']"
        with_tag ".variable label", "#{variable.getName()}"
        with_tag ".variable .message.hidden", "Overwritten. Default: #{h(variable.getValue())}"
      end
    end
  end

  it "should not render tab for secure variables when there are none" do
    @variables.add("foo", "foo_value")
    @variables.add("bar", "bar_value")

    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => true}}

    response.should_not have_tag(".sub_tabs_container #secure_environment_variables_tab")
  end

  it "should render tab and content for secure variables" do
    @variables.add("foo", "foo_value")
    @variables.add("bar", "bar_value")
    @variables.add(secure_env_variable("secure_foo", "secure_foo_value"))
    @variables.add(secure_env_variable("secure_bar", "secure_bar_value"))

    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => true}}

    response.should have_tag(".sub_tabs_container li#secure_environment_variables_tab") do
      with_tag("a.tab_button_body_match_text", "secure-environment-variables")
      with_tag("a", "Secure Variables")
    end
    response.should have_tag("div.secure_variables") do
      with_tag("label", "secure_foo")
      with_tag("input[type='password'][name='secure_variables[secure_foo]'][value='******']")
      with_tag("label", "secure_bar")
      with_tag("input[type='password'][name='secure_variables[secure_bar]'][value='******']")
    end
  end

  it "should have material tab header" do
    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => true}}

    response.should have_tag(".change_materials .sub_tabs_container ul li.current_tab a", "Materials")
    response.should_not have_tag(".change_materials .sub_tabs_container ul li a", "Environment Variables")
  end

  it "should have material tab content" do
    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => true}}

    response.should have_tag(".sub_tab_container .sub_tab_container_content #tab-content-of-materials")
    response.should_not have_tag(".sub_tab_container .sub_tab_container_content #tab-content-of-environment-variables.variables")
  end

  it "should have environment variables tab header when variables are defined" do
    @variables.add("foo", "foo_value")
    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => true}}

    response.should have_tag(".change_materials .sub_tabs_container ul li a", "Environment Variables")
  end

  it "should have environment variables tab contents when variables are defined" do
    @variables.add("foo", "foo_value")
    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => true}}

    response.should have_tag(".sub_tab_container .sub_tab_container_content #tab-content-of-environment-variables.variables")
  end

  it "should have an override link next to each secure variable name" do
    @variables.add(secure_env_variable("secure_foo", "secure_foo_value"))

    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => true}}

    response.should have_tag("div#tab-content-of-secure-environment-variables") do
      with_tag("label[for='secure_variables_secure_foo']", "secure_foo")
      with_tag("a.override", "Override")
      with_tag("input[name='secure_variables[secure_foo]'][value='******'][disabled='disabled']")
    end
  end

  it "should render process material revision comment for display" do
    template.should_receive(:render_simple_comment).with("comment").and_return("simplified")

    assigns[:pipeline] = pipeline_instance = mock("pipeline instance")
    pipeline_instance.should_receive(:getName).any_number_of_times.and_return("pipeline_name")
    pipeline_instance.should_receive(:getMaterials).any_number_of_times.and_return([material = mock("material")])
    pipeline_instance.should_receive(:findCurrentMaterialRevisionForUI).any_number_of_times.and_return(revision = mock("revision"))
    pipeline_instance.should_receive(:getLatestMaterialRevision).any_number_of_times.and_return(revision)
    pipeline_instance.should_receive(:canRun).any_number_of_times.and_return(true)

    revision.should_receive(:getLatestRevisionString).any_number_of_times.and_return("1234")
    revision.should_receive(:getLatestShortRevision).any_number_of_times.and_return("1234")
    revision.should_receive(:getDateOfLatestModification).any_number_of_times.and_return(java.util.Date.new())
    revision.should_receive(:getLatestUser).any_number_of_times.and_return("user")
    revision.should_receive(:getLatestComment).any_number_of_times.and_return("comment")
    revision.should_receive(:hasModifications).any_number_of_times.and_return(false)

    material.should_receive(:getPipelineUniqueFingerprint).any_number_of_times.and_return("fingerprint")
    material.should_receive(:getDisplayName).any_number_of_times.and_return("package material")
    material.should_receive(:getTruncatedDisplayName).any_number_of_times.and_return("p1")
    material.should_receive(:getTypeForDisplay).any_number_of_times.and_return("PACKAGE")
    material.should_receive(:getFolder).any_number_of_times.and_return("f2")

    render :partial => "pipelines/pipeline_material_revisions.html", :locals => {:scope => {:show_on_pipelines => true}}

    response.body.should have_tag("div.change_materials") do
      with_tag(".revision_number[title=1234]", "1234")
      with_tag(".comment", "simplified")
    end

  end

  def secure_env_variable(key, cipher_text)
    secure_variable = EnvironmentVariableConfig.new(key, "")
    ReflectionUtil.setField(secure_variable, "encryptedValue", EncryptedVariableValueConfig.new(cipher_text))
    ReflectionUtil.setField(secure_variable, "isSecure", true)
    secure_variable
  end

end
