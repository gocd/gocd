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

describe "admin/pipelines/new.html.erb" do
  include GoUtil, FormUI, ReflectiveUtil
  include Admin::ConfigContextHelper
  include MockRegistryModule

  before(:each) do
    view.stub(:pipeline_create_path).and_return("create_path")

    @pipeline = PipelineConfigMother.createPipelineConfig("", "defaultStage", ["defaultJob"].to_java(java.lang.String))
    @material_config = SvnMaterialConfig.new("svn://foo", "loser", "secret", true, "dest")
    @material_config.setName(CaseInsensitiveString.new("Svn Material Name"))
    @pipeline.materialConfigs().clear()
    @pipeline.addMaterialConfig(@material_config)
    @pipeline_group = BasicPipelineConfigs.new
    @pipeline_group.add(@pipeline)

    assign(:pipeline, @pipeline)
    assign(:pipeline_group, @pipeline_group)
    assign(:template_list, Array.new)
    assign(:all_pipelines, ArrayList.new)
    tvms = java.util.ArrayList.new
    tvms.add(com.thoughtworks.go.presentation.TaskViewModel.new(com.thoughtworks.go.config.ExecTask.new, "admin/tasks/exec/new", "erb"))
    assign(:task_view_models, tvms)
    assign(:config_context, create_config_context(MockRegistryModule::MockRegistry.new))

    @cruise_config = BasicCruiseConfig.new
    assign(:cruise_config, @cruise_config)
    assign(:original_cruise_config, @cruise_config)
    set(@cruise_config, "md5", "abc")
    view.stub(:is_user_a_group_admin?).and_return(false)
    job_configs = JobConfigs.new([JobConfig.new(CaseInsensitiveString.new("defaultJob"))].to_java(JobConfig))
    stage_config = StageConfig.new(CaseInsensitiveString.new("defaultStage"), job_configs)
    view.stub(:default_stage_config).and_return(stage_config)
  end

  it "should have a page title and view title" do
    render

    expect(view.instance_variable_get("@view_title")).to eq("Add Pipeline")
    expect(view.instance_variable_get("@page_header")).to have_selector("h1#page-title", :text => "Add Pipeline")
  end

  it "should show wizard steps and the steps should be disabled" do
    render

    Capybara.string(response.body).find('div.steps_wrapper').tap do |div|
      div.find("ul.tabs") do |ul|
        ul.find("li#step1_link.disabled") do |li|
          expect(li).to have_selector("a[href='#']", :text => "Step 1:Basic Settings")
          expect(li).to have_selector("a.tab_button_body_match_text[href='#']", :text => "basic-settings")
        end
        ul.find("li#step2_link.disabled") do |li|
          expect(li).to have_selector("a[href='#']", :text => "Step 2:Materials")
          expect(li).to have_selector("a.tab_button_body_match_text[href='#']", :text => "materials")
        end
        ul.find("li#step3_link.disabled") do |li|
          expect(li).to have_selector("a[href='#']", :text => "Step 3:Stage/Job")
          expect(li).to have_selector("a.tab_button_body_match_text[href='#']", :text => "stage-and-job")
        end
      end
    end
  end

  describe "Basic Settings" do

    it "should have a text box for pipeline name and group name" do
      assign(:group_name, "")

      render

      Capybara.string(response.body).find("form[method='post'][action='create_path']").tap do |form|
        form.find("div.steps_panes.sub_tab_container_content div#tab-content-of-basic-settings div.form_content") do |div|
          expect(div).to have_selector("label[for='pipeline_group_pipeline_name']", :text => "Pipeline Name*")
          expect(div).to have_selector("input[name='pipeline_group[pipeline][#{com.thoughtworks.go.config.PipelineConfig::NAME}]']")
          expect(div).to have_selector("label[for='pipeline_group_group']", :text => "Pipeline Group Name")
          expect(div).to have_selector("input[name='pipeline_group[#{com.thoughtworks.go.config.PipelineConfigs::GROUP}]'][value='']")
        end
      end
    end

    it "should populate group name if adding to an existing group" do
      assign(:group_name, "foo.bar")

      render

      Capybara.string(response.body).find("form[method='post'][action='create_path']").tap do |form|
        form.find("div.steps_panes.sub_tab_container_content div#tab-content-of-basic-settings div.form_content") do |div|
          expect(div).to have_selector("label[for='pipeline_group_pipeline_name']", :text => "Pipeline Name*")
          expect(div).to have_selector("input[name='pipeline_group[pipeline][#{com.thoughtworks.go.config.PipelineConfig::NAME}]']")
          expect(div).to have_selector("label[for='pipeline_group_group']", :text => "Pipeline Group Name")
          expect(div).to have_selector("input[name='pipeline_group[#{com.thoughtworks.go.config.PipelineConfigs::GROUP}]'][value='foo.bar']")
        end
      end
    end

    it "should show dropdown for group name if user is a group admin" do
      assign(:groups_list, ["foo.bar", "some_other_group"])
      view.stub(:is_user_a_group_admin?).and_return(true)

      render

      Capybara.string(response.body).find("form[method='post'][action='create_path']").tap do |form|
        form.find("div.steps_panes.sub_tab_container_content div#tab-content-of-basic-settings div.form_content") do |div|
          expect(div).to have_selector("label[for='pipeline_group_group']", :text => "Pipeline Group Name")
          expect(div).not_to have_selector("input[name='pipeline_group[#{com.thoughtworks.go.config.PipelineConfigs::GROUP}]'][value='']")

          form.find("select[name='pipeline_group[#{com.thoughtworks.go.config.PipelineConfigs::GROUP}]']") do |select|
            expect(select).to have_selector("option[value='foo.bar']", :text => "foo.bar")
            expect(select).to have_selector("option[value='some_other_group']", :text => "some_other_group")
          end
        end
      end
    end

    it "should have section title" do
      render

      Capybara.string(response.body).find("form[method='post'][action='create_path']").tap do |form|
        form.find("div.steps_panes.sub_tab_container_content div#tab-content-of-basic-settings") do |div|
          expect(div).to have_selector("h2.section_title", :text => "Step 1: Basic Settings")
        end
      end
    end

    it "should have form buttons" do
      render

      Capybara.string(response.body).find('div.steps_panes.sub_tab_container_content div#tab-content-of-basic-settings').tap do |div|
        expect(div).to have_selector("button.cancel_button", :text => "Cancel")
        expect(div).to have_selector("button#next_to_settings", :text => "Next")
      end

      Capybara.string(response.body).find('div.steps_panes.sub_tab_container_content div#tab-content-of-materials').tap do |div|
        expect(div).to have_selector("button.cancel_button", :text => "Cancel")
        expect(div).to have_selector("button#next_to_materials", :text => "Next")
        expect(div).to have_selector("button#prev_to_materials", :text => "Previous")
      end

      Capybara.string(response.body).find('div.steps_panes.sub_tab_container_content div#tab-content-of-stage-and-job').tap do |div|
        expect(div).to have_selector("button.cancel_button", :text => "Cancel")
        expect(div).to have_selector("button.finish", :text => "FINISH")
        expect(div).to have_selector("button#prev_to_stage_and_job", :text => "Previous")
      end
    end

    it "should have config md5 hidden field" do
      render

      Capybara.string(response.body).find("form[method='post'][action='create_path']").tap do |form|
        expect(form).to have_selector("input[type='hidden'][name='config_md5'][value='abc']")
      end
    end
  end

  describe "Materials" do

    it "should have material section " do
      render

      Capybara.string(response.body).find('div.steps_panes.sub_tab_container_content div#tab-content-of-materials').tap do |div|
        expect(div).to have_selector("h2.section_title", :text => "Step 2: Materials")
        expect(div).to have_selector("label", :text => "Material Type*")
        div.find("select[name='pipeline_group[pipeline][materials][materialType]']") do |select|
          expect(select).to have_selector("option[value='SvnMaterial']", :text => "Subversion")
        end
        expect(div).to have_selector("button.cancel_button", :text => "Cancel")
        expect(div).to have_selector("button#next_to_materials", :text => "Next")
      end
    end

    describe "Svn materials" do

      it "should render all svn material attributes" do
        in_params(:pipeline_name => "pipeline_name")

        render

        Capybara.string(response.body).find('div#tab-content-of-materials div.form_content').tap do |div|
          expect(div).to have_selector("label", :text => "URL*")
          expect(div).to have_selector("input[type='text'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::URL}]']")

          expect(div).to have_selector("label", :text => "Username")
          expect(div).to have_selector("input[type='text'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::USERNAME}]']")

          expect(div).to have_selector("label", :text => "Password")
          expect(div).to have_selector("input[type='password'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::PASSWORD}]']")
        end
      end

      it "should display check connection button" do
        render

        expect(response.body).to have_selector("button#check_connection_svn", :text => "CHECK CONNECTION")
        expect(response.body).to have_selector("#vcsconnection-message_svn", :text => "")

        expect(response.body).to have_selector("button#check_connection_hg", :text => "CHECK CONNECTION")
        expect(response.body).to have_selector("#vcsconnection-message_hg", :text => "")

        expect(response.body).to have_selector("button#check_connection_git", :text => "CHECK CONNECTION")
        expect(response.body).to have_selector("#vcsconnection-message_git", :text => "")

        expect(response.body).to have_selector("button#check_connection_p4", :text => "CHECK CONNECTION")
        expect(response.body).to have_selector("#vcsconnection-message_p4", :text => "")
      end

      it "should display new svn material view with errors" do
        error = config_error(com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::URL, "Url is wrong")
        error.add(com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::USERNAME, "Username is wrong")
        error.add(com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::PASSWORD, "Password is wrong")

        set(@material_config, "errors", error)

        render

        Capybara.string(response.body).find("form[method='post'][action='create_path']").tap do |form|
          form.find("div.steps_panes.sub_tab_container_content div#tab-content-of-materials div.form_content") do |div|
            expect(div).to have_selector("div.field_with_errors input[type='text'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::URL}]'][value='svn://foo']")
            expect(div).to have_selector("div.form_error", :text => "Url is wrong")

            expect(div).to have_selector("div.field_with_errors input[type='text'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::USERNAME}]'][value='loser']")
            expect(div).to have_selector("div.form_error", :text => "Username is wrong")

            expect(div).to have_selector("div.field_with_errors input[type='password'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::PASSWORD}]'][value='secret']")
            expect(div).to have_selector("div.form_error", :text => "Password is wrong")
          end
        end
      end
    end

  end

  describe "Stage and Job" do

    it "should have the title Stage/Job" do
      render

      Capybara.string(response.body).find('div.steps_panes.sub_tab_container_content div#tab-content-of-stage-and-job').tap do |div|
        expect(div).to have_selector("h2.section_title", :text => "Step 3: Stage/Job")
      end
    end

    it "should not have next button on the stage step" do
      render

      Capybara.string(response.body).find('div.steps_panes.sub_tab_container_content div#tab-content-of-stage-and-job').tap do |div|
        expect(div).not_to have_selector("button.next")
      end
    end

    it "should have configuration type" do
      render

      Capybara.string(response.body).find('div.steps_panes.sub_tab_container_content div#tab-content-of-stage-and-job').tap do |div|
        expect(div).to have_selector("label", :text => "Configuration Type")
        expect(div).to have_selector("label[for='pipeline_configurationType_stages']", :text => "Define Stages")
        expect(div).to have_selector("input#pipeline_configurationType_stages[type='radio'][checked='checked']")
        expect(div).to have_selector("label[for='pipeline_configurationType_template']", :text => "Use Template")
        expect(div).to have_selector("input#pipeline_configurationType_template[type='radio']")
      end
    end

    it "should be possible to see a pipeline with template" do
      @pipeline.clear()
      @pipeline.setTemplateName(CaseInsensitiveString.new("template_foo"))

      render

      Capybara.string(response.body).find('div.steps_panes.sub_tab_container_content div#tab-content-of-stage-and-job').tap do |div|
        expect(div).to have_selector(".define_or_template label", :text => "Configuration Type")
        expect(div).to have_selector("label[for='pipeline_configurationType_stages']", :text => "Define Stages")
        expect(div).to have_selector("input#pipeline_configurationType_stages[type='radio']")
        expect(div).not_to have_selector("input#pipeline_configurationType_stages[type='radio'][checked='checked']")
        expect(div).to have_selector("label[for='pipeline_configurationType_template']", :text => "Use Template")
        expect(div).to have_selector("input#pipeline_configurationType_template[type='radio'][checked='checked']")
      end
    end

    it "should be able to see stage information" do
      render

      Capybara.string(response.body).find('div.steps_panes.sub_tab_container_content div#tab-content-of-stage-and-job').tap do |div|
        expect(div).to have_selector("input[name='pipeline_group[pipeline][stage][name]'][value='defaultStage']")
        expect(div).to have_selector("input[name='pipeline_group[pipeline][stage][approval][type]'][type='radio'][value='success']")
        expect(div).to have_selector("input[name='pipeline_group[pipeline][stage][approval][type]'][type='radio'][value='manual']")
      end
    end

    it "should be able to see basic job information" do
      render

      Capybara.string(response.body).find('div.steps_panes.sub_tab_container_content div#tab-content-of-stage-and-job').tap do |div|
        expect(div).to have_selector("input[name='pipeline_group[pipeline][stage][jobs][][name]'][value='defaultJob']")
        div.find("select[name='pipeline_group[pipeline][stage][jobs][][tasks][taskOptions]']") do |select|
          expect(select).to have_selector("option[value='exec']", :text => "More...")
          expect(select).to have_selector("option[value='rake']", :text => "Rake")
          expect(select).to have_selector("option[value='ant']", :text => "Ant")
          expect(select).to have_selector("option[value='nant']", :text => "NAnt")
        end
        expect(div).to have_selector("input[name='pipeline_group[pipeline][stage][jobs][][tasks][exec][command]']")
        expect(div).to have_selector("textarea[name='pipeline_group[pipeline][stage][jobs][][tasks][exec][argListString]']")
        expect(div).to have_selector("input[name='pipeline_group[pipeline][stage][jobs][][tasks][exec][workingDirectory]']")
      end
    end

    it "should display template dropdown" do
      assign(:template_list, TemplatesConfig.new([PipelineTemplateConfigMother.createTemplate("foo"), PipelineTemplateConfigMother.createTemplate("bar_template_name")].to_java(PipelineTemplateConfig)))

      render

      Capybara.string(response.body).find('div.steps_panes.sub_tab_container_content div#tab-content-of-stage-and-job').tap do |div|
        div.find("div#select_template_container") do |select_template_container|
          select_template_container.find("select[name='pipeline_group[pipeline][templateName]']") do |select|
            expect(select).to have_selector("option[value='foo']", :text => "foo")
            expect(select).to have_selector("option[value='bar_template_name']", :text => "bar_template_name")
          end
        end
      end
    end
  end

  describe "Client Side Validations" do
    it "Basic Settings Tab validation" do
      render

      Capybara.string(response.body).find("form[method='post'][action='create_path']").tap do |form|
        form.find("div.steps_panes.sub_tab_container_content div#tab-content-of-basic-settings div.form_content") do |div|
          expect(div).to have_selector("input[name='pipeline_group[pipeline][#{com.thoughtworks.go.config.PipelineConfig::NAME}]'][class='required pattern_match uniquePipelineName']")
          expect(div).to have_selector("input[name='pipeline_group[#{com.thoughtworks.go.config.PipelineConfigs::GROUP}]'][class='required pattern_match']")
        end
      end
    end

    it "Materials Tab validation" do
      render

      Capybara.string(response.body).find("form[method='post'][action='create_path']").tap do |form|
        form.find("div.steps_panes.sub_tab_container_content div#tab-content-of-materials div.form_content") do |div|
          expect(div).to have_selector("input[name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::URL}]'].required")
          expect(div).to have_selector("input[name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig::URL}]'].required")
          expect(div).to have_selector("input[name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.git.GitMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.git.GitMaterialConfig::URL}]'].required")
          expect(div).to have_selector("input[name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::SERVER_AND_PORT}]'].required")
          expect(div).to have_selector("textarea[name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::VIEW}]'].required")
          expect(div).to have_selector("input[name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig::PIPELINE_STAGE_NAME}]'].required")
          expect(div).to have_selector("input[name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig::PROJECT_PATH}]'].required")
        end
      end
    end

    it "Stage-Job Tab validation" do
      render

      Capybara.string(response.body).find("form[method='post'][action='create_path']").tap do |form|
        form.find("div.steps_panes.sub_tab_container_content div#tab-content-of-stage-and-job") do |div|
          expect(div).to have_selector("input[name='pipeline_group[pipeline][stage][name]'].required.pattern_match")
          expect(div).to have_selector("input[name='pipeline_group[pipeline][stage][jobs][][name]'].required.pattern_match")
          expect(div).to have_selector("input[name='pipeline_group[pipeline][stage][jobs][][tasks][exec][#{com.thoughtworks.go.config.ExecTask::COMMAND}]'].required")
        end
      end
    end
  end
end
