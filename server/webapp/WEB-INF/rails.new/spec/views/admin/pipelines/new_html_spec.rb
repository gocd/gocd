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

require File.join(File.dirname(__FILE__), "/../../../spec_helper")

describe "admin/pipelines/new.html.erb" do
  include GoUtil, FormUI, ReflectiveUtil
  include Admin::ConfigContextHelper
  include MockRegistryModule

  before(:each) do
    template.stub(:pipeline_create_path).and_return("create_path")

    @pipeline = PipelineConfigMother.createPipelineConfig("", "defaultStage", ["defaultJob"].to_java(java.lang.String))
    @material_config = SvnMaterialConfig.new("svn://foo", "loser", "secret", true, "dest")
    @material_config.setName(CaseInsensitiveString.new("Svn Material Name"))
    @pipeline.materialConfigs().clear()
    @pipeline.addMaterialConfig(@material_config)
    @pipeline_group = PipelineConfigs.new
    @pipeline_group.add(@pipeline)

    assign(:pipeline, @pipeline)
    assign(:pipeline_group, @pipeline_group)
    assign(:template_list, Array.new)
    assign(:all_pipelines, ArrayList.new)
    tvms = java.util.ArrayList.new
    tvms.add(com.thoughtworks.go.presentation.TaskViewModel.new(com.thoughtworks.go.config.ExecTask.new, "admin/tasks/exec/new", "erb"))
    assign(:task_view_models, tvms)
    assign(:config_context, create_config_context(MockRegistryModule::MockRegistry.new))

    @cruise_config = CruiseConfig.new
    assign(:cruise_config, @cruise_config)
    assign(:original_cruise_config, @cruise_config)
    set(@cruise_config, "md5", "abc")
    template.stub(:is_user_a_group_admin?).and_return(false)
    job_configs = JobConfigs.new([JobConfig.new(CaseInsensitiveString.new("defaultJob"))].to_java(JobConfig))
    stage_config = StageConfig.new(CaseInsensitiveString.new("defaultStage"), job_configs)
    template.stub(:default_stage_config).and_return(stage_config)
  end

  it "should have a page title and view title" do
    render "/admin/pipelines/new.html"

    assigns[:view_title].should == "Add Pipeline"
    assigns[:page_header].should have_tag("h1#page-title", "Add Pipeline")
  end

  it "should show wizard steps and the steps should be disabled" do
    render "/admin/pipelines/new.html"

    response.body.should have_tag("div.steps_wrapper") do
      with_tag("ul.tabs") do
        with_tag("li#step1_link.disabled") do
          with_tag("a[href='#']", "Step 1:Basic Settings")
          with_tag("a.tab_button_body_match_text[href='#']", "basic-settings")
        end
        with_tag("li#step2_link.disabled") do
          with_tag("a[href='#']", "Step 2:Materials")
          with_tag("a.tab_button_body_match_text[href='#']", "materials")
        end
        with_tag("li#step3_link.disabled") do
          with_tag("a[href='#']", "Step 3:Stage/Job")
          with_tag("a.tab_button_body_match_text[href='#']", "stage-and-job")
        end
      end
    end
  end

  describe "Basic Settings" do

    it "should have a text box for pipeline name and group name" do
      assign(:group_name, "")
      render "/admin/pipelines/new.html"

      response.body.should have_tag("form[method='post'][action='create_path']") do
        with_tag("div.steps_panes.sub_tab_container_content div#tab-content-of-basic-settings div.form_content") do
          with_tag("label[for='pipeline_group_pipeline_name']", "Pipeline Name*")
          with_tag("input[name='pipeline_group[pipeline][#{com.thoughtworks.go.config.PipelineConfig::NAME}]']")
          with_tag("label[for='pipeline_group_group']", "Pipeline Group Name")
          with_tag("input[name='pipeline_group[#{com.thoughtworks.go.config.PipelineConfigs::GROUP}]'][value='']")
        end
      end
    end

    it "should populate group name if adding to an existing group" do
      assign(:group_name, "foo.bar")
      render "/admin/pipelines/new.html"

      response.body.should have_tag("form[method='post'][action='create_path']") do
        with_tag("div.steps_panes.sub_tab_container_content div#tab-content-of-basic-settings div.form_content") do
          with_tag("label[for='pipeline_group_pipeline_name']", "Pipeline Name*")
          with_tag("input[name='pipeline_group[pipeline][#{com.thoughtworks.go.config.PipelineConfig::NAME}]']")
          with_tag("label[for='pipeline_group_group']", "Pipeline Group Name")
          with_tag("input[name='pipeline_group[#{com.thoughtworks.go.config.PipelineConfigs::GROUP}]'][value=?]", "foo.bar")
        end
      end
    end

    it "should show dropdown for group name if user is a group admin" do
      assign(:groups_list, ["foo.bar", "some_other_group"])
      template.stub(:is_user_a_group_admin?).and_return(true)
      
      render "admin/pipelines/new.html"

      response.body.should have_tag("form[method='post'][action='create_path']") do
        with_tag("div.steps_panes.sub_tab_container_content div#tab-content-of-basic-settings div.form_content") do
          with_tag("label[for='pipeline_group_group']", "Pipeline Group Name")
          without_tag("input[name='pipeline_group[#{com.thoughtworks.go.config.PipelineConfigs::GROUP}]'][value='']")
          with_tag("select[name='pipeline_group[#{com.thoughtworks.go.config.PipelineConfigs::GROUP}]']") do
            with_tag("option[value='foo.bar']", "foo.bar")
            with_tag("option[value='some_other_group']", "some_other_group")
          end
        end
      end
    end

    it "should have section title" do
      render "/admin/pipelines/new.html"

      response.body.should have_tag("form[method='post'][action='create_path']") do
        with_tag("div.steps_panes.sub_tab_container_content div#tab-content-of-basic-settings") do
          with_tag("h2.section_title", "Step 1: Basic Settings")
        end
      end
    end

    it "should have form buttons" do
      render "/admin/pipelines/new.html"

      response.body.should have_tag("div.steps_panes.sub_tab_container_content div#tab-content-of-basic-settings") do
        with_tag("button.cancel_button", "Cancel")
        with_tag("button#next_to_settings", "Next")
      end
      response.body.should have_tag("div.steps_panes.sub_tab_container_content div#tab-content-of-materials") do
        with_tag("button.cancel_button", "Cancel")
        with_tag("button#next_to_materials", "Next")
        with_tag("button#prev_to_materials", "Previous")
      end
      response.body.should have_tag("div.steps_panes.sub_tab_container_content div#tab-content-of-stage-and-job") do
        with_tag("button.cancel_button", "Cancel")
        with_tag("button.finish", "FINISH")
        with_tag("button#prev_to_stage_and_job", "Previous")
      end
    end

    it "should have config md5 hidden field" do
      render "/admin/pipelines/new.html"

      response.body.should have_tag("form[method='post'][action='create_path']") do
        with_tag("input[type='hidden'][name='config_md5'][value='abc']")
      end
    end
  end

  describe "Materials" do

    it "should have material section " do
      render "admin/pipelines/new"

      response.body.should have_tag("div.steps_panes.sub_tab_container_content div#tab-content-of-materials") do
        with_tag("h2.section_title", "Step 2: Materials")
        with_tag("label","Material Type*")
        with_tag("select[name='pipeline_group[pipeline][materials][materialType]']") do
          with_tag("option[value='SvnMaterial']", "Subversion")
        end
        with_tag("button.cancel_button", "Cancel")
        with_tag("button#next_to_materials", "Next")
      end
    end

    describe "Svn materials" do

      it "should render all svn material attributes" do
        in_params(:pipeline_name => "pipeline_name")

        render "admin/pipelines/new"

        response.body.should have_tag("div#tab-content-of-materials div.form_content") do
          with_tag("label", "URL*")
          with_tag("input[type='text'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::URL}]']")

          with_tag("label", "Username")
          with_tag("input[type='text'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::USERNAME}]']")

          with_tag("label", "Password")
          with_tag("input[type='password'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::PASSWORD}]']")
        end
      end

      it "should display check connection button" do
        render "admin/pipelines/new"

        response.body.should have_tag("button#check_connection_svn", "CHECK CONNECTION")
        response.body.should have_tag("#vcsconnection-message_svn", "")

        response.body.should have_tag("button#check_connection_hg", "CHECK CONNECTION")
        response.body.should have_tag("#vcsconnection-message_hg", "")

        response.body.should have_tag("button#check_connection_git", "CHECK CONNECTION")
        response.body.should have_tag("#vcsconnection-message_git", "")

        response.body.should have_tag("button#check_connection_p4", "CHECK CONNECTION")
        response.body.should have_tag("#vcsconnection-message_p4", "")
      end

      it "should display new svn material view with errors" do
        error = config_error(com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::URL, "Url is wrong")
        error.add(com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::USERNAME, "Username is wrong")
        error.add(com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::PASSWORD, "Password is wrong")

        set(@material_config, "errors", error)

        render "admin/pipelines/new.html"

        response.body.should have_tag("form[method='post'][action='create_path']") do
          with_tag("div.steps_panes.sub_tab_container_content div#tab-content-of-materials div.form_content") do
            with_tag("div.fieldWithErrors input[type='text'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::URL}]'][value='svn://foo']")
            with_tag("div.form_error", "Url is wrong")

            with_tag("div.fieldWithErrors input[type='text'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::USERNAME}]'][value='loser']")
            with_tag("div.form_error", "Username is wrong")

            with_tag("div.fieldWithErrors input[type='password'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::PASSWORD}]'][value='secret']")
            with_tag("div.form_error", "Password is wrong")
          end
        end
      end
    end

  end

  describe "Stage and Job" do

    it "should have the title Stage/Job" do
      render "admin/pipelines/new"
      response.body.should have_tag("div.steps_panes.sub_tab_container_content div#tab-content-of-stage-and-job") do
        with_tag("h2.section_title", "Step 3: Stage/Job")
      end
    end

    it "should not have next button on the stage step" do
      render "admin/pipelines/new"
      response.body.should have_tag("div.steps_panes.sub_tab_container_content div#tab-content-of-stage-and-job") do
        without_tag("button.next")
      end
    end

    it "should have configuration type" do
      render "admin/pipelines/new"
      response.body.should have_tag("div.steps_panes.sub_tab_container_content div#tab-content-of-stage-and-job") do
        with_tag("label", "Configuration Type")
        with_tag("label[for='pipeline_configurationType_stages']", "Define Stages")
        with_tag("input#pipeline_configurationType_stages[type='radio'][checked='checked']")
        with_tag("label[for='pipeline_configurationType_template']", "Use Template")
        with_tag("input#pipeline_configurationType_template[type='radio']")
      end
    end

    it "should be possible to see a pipeline with template" do
      @pipeline.clear()
      @pipeline.setTemplateName(CaseInsensitiveString.new("template_foo"))

      render "admin/pipelines/new"
      response.body.should have_tag("div.steps_panes.sub_tab_container_content div#tab-content-of-stage-and-job") do
        with_tag(".define_or_template label", "Configuration Type")
        with_tag("label[for='pipeline_configurationType_stages']", "Define Stages")
        with_tag("input#pipeline_configurationType_stages[type='radio']")
        without_tag("input#pipeline_configurationType_stages[type='radio'][checked='checked']")
        with_tag("label[for='pipeline_configurationType_template']", "Use Template")
        with_tag("input#pipeline_configurationType_template[type='radio'][checked='checked']")
      end
    end

    it "should be able to see stage information" do
      render "admin/pipelines/new"

      response.body.should have_tag("div.steps_panes.sub_tab_container_content div#tab-content-of-stage-and-job") do
        with_tag("input[name='pipeline_group[pipeline][stage][name]'][value=?]", "defaultStage")
        with_tag("input[name='pipeline_group[pipeline][stage][approval][type]'][type='radio'][value='success']")
        with_tag("input[name='pipeline_group[pipeline][stage][approval][type]'][type='radio'][value='manual']")
      end
    end

    it "should be able to see basic job information" do
      render "admin/pipelines/new"

      response.body.should have_tag("div.steps_panes.sub_tab_container_content div#tab-content-of-stage-and-job") do
        with_tag("input[name='pipeline_group[pipeline][stage][jobs][][name]'][value=?]", "defaultJob")
        with_tag("select[name='pipeline_group[pipeline][stage][jobs][][tasks][taskOptions]']") do
          with_tag("option[value='exec']", "More...")
          with_tag("option[value='rake']", "Rake")
          with_tag("option[value='ant']", "Ant")
          with_tag("option[value='nant']", "NAnt")
        end
        with_tag("input[name='pipeline_group[pipeline][stage][jobs][][tasks][exec][command]']")
        with_tag("textarea[name='pipeline_group[pipeline][stage][jobs][][tasks][exec][argListString]']")
        with_tag("input[name='pipeline_group[pipeline][stage][jobs][][tasks][exec][workingDirectory]']")
      end
    end

    it "should display template dropdown" do
      assign(:template_list, TemplatesConfig.new([PipelineTemplateConfigMother.createTemplate("foo"), PipelineTemplateConfigMother.createTemplate("bar_template_name")].to_java(PipelineTemplateConfig)))

      render "admin/pipelines/new"

      response.body.should have_tag("div.steps_panes.sub_tab_container_content div#tab-content-of-stage-and-job") do
        with_tag("div#select_template_container") do
          with_tag("select[name='pipeline_group[pipeline][templateName]']") do
            with_tag("option[value='foo']", "foo")
            with_tag("option[value='bar_template_name']", "bar_template_name")
          end
        end
      end
    end
  end

  describe "Client Side Validations" do
    it "Basic Settings Tab validation" do
      render "/admin/pipelines/new.html"

      response.body.should have_tag("form[method='post'][action='create_path']") do
        with_tag("div.steps_panes.sub_tab_container_content div#tab-content-of-basic-settings div.form_content") do
          with_tag("input[name='pipeline_group[pipeline][#{com.thoughtworks.go.config.PipelineConfig::NAME}]'][class='required pattern_match uniquePipelineName']")
          with_tag("input[name='pipeline_group[#{com.thoughtworks.go.config.PipelineConfigs::GROUP}]'][class='required pattern_match']")
        end
      end
    end

    it "Materials Tab validation" do
      render "/admin/pipelines/new.html"

      response.body.should have_tag("form[method='post'][action='create_path']") do
        with_tag("div.steps_panes.sub_tab_container_content div#tab-content-of-materials div.form_content") do
          with_tag("input[name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::URL}]'].required")
          with_tag("input[name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig::URL}]'].required")
          with_tag("input[name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.git.GitMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.git.GitMaterialConfig::URL}]'].required")
          with_tag("input[name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::SERVER_AND_PORT}]'].required")
          with_tag("textarea[name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::VIEW}]'].required")
          with_tag("input[name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig::PIPELINE_STAGE_NAME}]'].required")
          with_tag("input[name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig::PROJECT_PATH}]'].required")
        end
      end
    end

    it "Stage-Job Tab validation" do
      render "/admin/pipelines/new.html"

      response.body.should have_tag("form[method='post'][action='create_path']") do
        with_tag("div.steps_panes.sub_tab_container_content div#tab-content-of-stage-and-job") do
          with_tag("input[name='pipeline_group[pipeline][stage][name]'].required.pattern_match")
          with_tag("input[name='pipeline_group[pipeline][stage][jobs][][name]'].required.pattern_match")
          with_tag("input[name='pipeline_group[pipeline][stage][jobs][][tasks][exec][#{com.thoughtworks.go.config.ExecTask::COMMAND}]'].required")
        end
      end
    end
  end
end