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

describe "admin/pipelines/_material_form.html.erb" do
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

    assigns[:pipeline] = @pipeline
    assigns[:pipeline_group] = @pipeline_group
    assigns[:template_list] = Array.new
    assigns[:all_pipelines] = java.util.ArrayList.new
    tvms = java.util.ArrayList.new
    tvms.add(com.thoughtworks.go.presentation.TaskViewModel.new(com.thoughtworks.go.config.AntTask.new,"template","erb"))
    assigns[:task_view_models] = tvms
    assigns[:config_context]= create_config_context(MockRegistryModule::MockRegistry.new)

    @cruise_config = CruiseConfig.new
    repository1 = PackageRepositoryMother.create("repo1", "repo1-name", "pluginid", "version", Configuration.new([ConfigurationPropertyMother.create("k1", false, "v1")].to_java(ConfigurationProperty)))
    repository2 = PackageRepositoryMother.create("repo2", "repo2-name", "pluginid", "version", Configuration.new([ConfigurationPropertyMother.create("k1", false, "v1")].to_java(ConfigurationProperty)))
    pkg1 = PackageDefinitionMother.create("pkg1", "package1-name", Configuration.new([ConfigurationPropertyMother.create("k2", false, "v2")].to_java(ConfigurationProperty)), repository1)
    @pkg2 = PackageDefinitionMother.create("pkg2", "package2-name", Configuration.new([ConfigurationPropertyMother.create("k2", false, "v2")].to_java(ConfigurationProperty)), repository1)
    repository1.setPackages(Packages.new([pkg1, @pkg2].to_java(PackageDefinition)))
    repos = PackageRepositories.new
    repos.add(repository1)
    repos.add(repository2)
    @cruise_config.setPackageRepositories(repos)
    assigns[:cruise_config] = @cruise_config
    assigns[:original_cruise_config] = @cruise_config
    template.stub(:is_user_a_group_admin?).and_return(false)
    set(@cruise_config, "md5", "abc")
    template.stub(:render_pluggable_form_template).and_return("template")
  end

  describe "Materials" do

    it "should have material section " do
      render "admin/pipelines/new"

      response.body.should have_tag("div.steps_panes.sub_tab_container_content div#tab-content-of-materials") do
        with_tag("h2.section_title", "Step 2: Materials")
        with_tag("label","Material Type*")
        with_tag("select[name='pipeline_group[pipeline][materials][materialType]']") do
          with_tag("option[value='SvnMaterial']", "Subversion")
          with_tag("option[value='GitMaterial']", "Git")
          with_tag("option[value='HgMaterial']", "Mercurial")
          with_tag("option[value='P4Material']", "Perforce")
          with_tag("option[value='DependencyMaterial']", "Pipeline")
        end
        with_tag("button.cancel_button", "Cancel")
        with_tag("button#next_to_materials", "Next")
      end
    end

    describe "Svn material" do

      it "should render all svn material attributes" do
        in_params(:pipeline_name => "pipeline_name")

        render "admin/pipelines/new"

        response.body.should have_tag("div#tab-content-of-materials #material_forms .SvnMaterial") do
          with_tag("label", "URL*")
          with_tag("input[type='text'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::URL}]']")

          with_tag("label", "Username")
          with_tag("input.svn_username[type='text'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::USERNAME}]']")

          with_tag("label", "Password")
          with_tag("input.svn_password[type='password'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::PASSWORD}]']")

          with_tag("div.hidden input[type='checkbox'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::PASSWORD_CHANGED}]'][value='1'][checked='checked']")

          with_tag("label", "Poll for new changes")
          with_tag("input[type='checkbox'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.ScmMaterialConfig::AUTO_UPDATE}]'][checked='checked']")
        end
      end

      it "should display check connection button" do
        render "admin/pipelines/new"

        response.body.should have_tag("button#check_connection_svn", "CHECK CONNECTION")
        response.body.should have_tag("#vcsconnection-message_svn", "")
      end

      it "should display new svn material view with errors" do
        error = config_error(com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::URL, "Url is wrong")
        error.add(com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::USERNAME, "Username is wrong")
        error.add(com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::PASSWORD, "Password is wrong")

        set(@material_config, "errors", error)

        render "admin/pipelines/new.html"

        response.body.should have_tag("form[method='post'][action='create_path']") do
          with_tag("div.steps_panes.sub_tab_container_content div#tab-content-of-materials #material_forms .SvnMaterial") do
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

    describe "Git Material" do
      it "should render all git material attributes" do
        in_params(:pipeline_name => "pipeline_name")

        render "admin/pipelines/new"

        response.body.should have_tag("div#tab-content-of-materials #material_forms .GitMaterial") do
          with_tag("label", "URL*")
          with_tag("input[type='text'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.git.GitMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.git.GitMaterialConfig::URL}]']")

          with_tag("label", "Branch")
          with_tag("input[type='text'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.git.GitMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.git.GitMaterialConfig::BRANCH}]']")

          with_tag("label", "Poll for new changes")
          with_tag("input[type='checkbox'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.git.GitMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.ScmMaterialConfig::AUTO_UPDATE}]'][checked='checked']")
        end
      end
    end

    describe "Hg Material" do
      it "should render all hg material attributes" do
        in_params(:pipeline_name => "pipeline_name")

        render "admin/pipelines/new"

        response.body.should have_tag("div#tab-content-of-materials #material_forms .HgMaterial") do
          with_tag("label", "URL*")
          with_tag("input[type='text'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig::URL}]']")

          with_tag("label", "Poll for new changes")
          with_tag("input[type='checkbox'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.ScmMaterialConfig::AUTO_UPDATE}]'][checked='checked']")
        end
      end
    end

    describe "Tfs Material" do
      it "should render all tfs material attributes" do
        in_params(:pipeline_name => "pipeline_name")

        render "admin/pipelines/new"

        response.body.should have_tag("div#tab-content-of-materials #material_forms .TfsMaterial") do
          with_tag("label", "URL*")
          with_tag("input[type='text'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.ScmMaterialConfig::URL}]']")
          with_tag("input.tfs_username[type='text'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.ScmMaterialConfig::USERNAME}]']")
          with_tag("input.tfs_password[type='password'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.ScmMaterialConfig::PASSWORD}]']")
          with_tag("input[type='text'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig::PROJECT_PATH}]']")

          with_tag("label", "Poll for new changes")
          with_tag("input[type='checkbox'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.ScmMaterialConfig::AUTO_UPDATE}]'][checked='checked']")

          without_tag("input[type='text'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig::TYPE}][workspaceOwner]']")
          without_tag("input[type='text'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig::TYPE}][workspace]']")
        end
      end
    end

    describe "P4 Material" do
      it "should render all P4 material attributes" do
        in_params(:pipeline_name => "pipeline_name")

        render "admin/pipelines/new"

        response.body.should have_tag("div#tab-content-of-materials #material_forms .P4Material") do
          with_tag("label", "Server and Port*")
          with_tag("input[type='text'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::SERVER_AND_PORT}]']")

          with_tag("label", "Username")
          with_tag("input.p4_username[type='text'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::USERNAME}]']")

          with_tag("label", "Password")
          with_tag("input.p4_password[type='password'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::PASSWORD}]']")

          with_tag("label", "View*")
          with_tag("textarea[name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::VIEW}]']")

          with_tag("label[for='material_useTickets']", "Use tickets")
          with_tag("input[id='material_useTickets'][type='checkbox'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::USE_TICKETS}]']")

          with_tag("label", "Poll for new changes")
          with_tag("input[type='checkbox'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.ScmMaterialConfig::AUTO_UPDATE}]'][checked='checked']")
        end
      end
    end

    describe "Dependency Material" do
      it "should render all Dependency material attributes" do
        in_params(:pipeline_name => "pipeline_name")

        render "admin/pipelines/new"

        response.body.should have_tag("div#tab-content-of-materials .DependencyMaterial") do
          with_tag("label", "Pipeline [Stage]*")
          with_tag("input[type='text'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig::PIPELINE_STAGE_NAME}]']")
        end
      end
    end

    describe "Package Material" do

      before(:each) do
        @material = PackageMaterialConfig.new
        @material_configs = MaterialConfigs.new([@material].to_java(com.thoughtworks.go.domain.materials.MaterialConfig))
        fields_for(:pipeline_group, @pipeline_group) do |gf|
          gf.fields_for(:pipeline, @pipeline) do |pf|
            pf.fields_for(:materials, @material_configs) do |mf|
              mf.fields_for(com.thoughtworks.go.config.materials.PackageMaterialConfig::TYPE, @material) do |f|
                @form = f
              end
            end
          end
        end
      end

      it "should render all material attributes" do
        in_params(:pipeline_name => "pipeline-name")

        render "admin/pipelines/new"

        response.body.should have_tag("div#tab-content-of-materials #material_forms .PackageMaterial") do
          with_tag("input[type='hidden'][name='material_type'][value='#{com.thoughtworks.go.config.materials.PackageMaterialConfig::TYPE}']")
          with_tag("input[type='radio'][name='material[create_or_associate_pkg_def]'][value='associate'][checked=?]", "checked")
          with_tag("input[type='radio'][name='material[create_or_associate_pkg_def]'][value='create']")
          with_tag("select.required[name='material[package_definition[repositoryId]]']") do
            without_tag("option")
          end
          with_tag("select.required[name='material[#{PackageMaterialConfig::PACKAGE_ID}]']") do
            without_tag("option")
          end
        end
      end

      it "should pre-select repo and package for the material" do
        @material.setPackageDefinition(@pkg2)
        in_params(:pipeline_name => "pipeline-name", :finger_print => "foo")

        render :partial => "admin/pipelines/materials/package_material_form.html", :locals => {:scope => {:material => @material, :form => @form}}

        response.body.should have_tag("input[type='hidden'][name='material_type'][value='#{@material.getType()}']")
        response.body.should have_tag("input[type='radio'][name='material[create_or_associate_pkg_def]'][value='create']")
        response.body.should have_tag("input[type='radio'][name='material[create_or_associate_pkg_def]'][value='associate'][checked=?]", "checked")
        response.body.should have_tag("select.required[name='material[package_definition[repositoryId]]']") do
          without_tag("option")
        end
        response.body.should have_tag("select.required[name='material[#{PackageMaterialConfig::PACKAGE_ID}]']") do
          without_tag("option")
        end
      end

      describe "render package definition" do
        before(:each) do
          @material.setPackageDefinition(@pkg2)
          @metadata = PackageConfigurations.new
          @metadata.addConfiguration(PackageConfiguration.new("key1").with(PackageConfiguration::SECURE, false).with(PackageConfiguration::DISPLAY_NAME, "Key 1"))
          @metadata.addConfiguration(PackageConfiguration.new("key2").with(PackageConfiguration::SECURE, false).with(PackageConfiguration::DISPLAY_NAME, "Key 2"))
          @metadata.addConfiguration(PackageConfiguration.new("key3_secure").with(PackageConfiguration::SECURE, true).with(PackageConfiguration::DISPLAY_NAME, "Key 3 Secure"))

          p1 = ConfigurationProperty.new(ConfigurationKey.new("key1"), ConfigurationValue.new("value1"), nil, nil)
          p2 = ConfigurationProperty.new(ConfigurationKey.new("key2"), ConfigurationValue.new("value2"), nil, nil)
          p3_secure = ConfigurationProperty.new(ConfigurationKey.new("key3_secure"), nil, EncryptedConfigurationValue.new("secure"), nil)
          @package = PackageDefinition.new("go", "package-name", Configuration.new([p1, p2, p3_secure].to_java(ConfigurationProperty)))
          assigns[:package_configuration] = PackageViewModel.new @metadata, @package
        end

        it "should render show_package_definition with check connection if the form was opened for associating a existing package definition" do
          in_params(:pipeline_name => "pipeline-name", :finger_print => "foo", :material => {:create_or_associate_pkg_def => "associate"})

          @controller.template.should_receive(:render).with(:partial => 'admin/package_definitions/show_package_definition_for_new_pipeline_wizard', :locals => {:scope => {:package_configuration => assigns[:package_configuration]}})
          render :partial => "admin/pipelines/materials/package_material_form.html", :locals => {:scope => {:material => @material, :form => @form}}
        end

        it "should render new package definition with check connection if the form was opened to create a new package definition" do
          in_params(:pipeline_name => "pipeline-name", :finger_print => "foo", :material => {:create_or_associate_pkg_def => "create"})

          @controller.template.should_receive(:render).with(:partial => 'admin/package_definitions/form_for_new_pipeline_wizard', :locals => {:scope => {:package_configuration => assigns[:package_configuration]}})
          render :partial => "admin/pipelines/materials/package_material_form.html", :locals => {:scope => {:material => @material, :form => @form}}
        end

        it "should render check connection when a form is opened for creating a new package definition" do
          in_params(:pipeline_name => "pipeline-name", :finger_print => "foo", :material => {:create_or_associate_pkg_def => "create"})
          render :partial => "admin/pipelines/materials/package_material_form.html", :locals => {:scope => {:material => @material, :form => @form}}

          response.body.should have_tag(".new_form_content") do
            with_tag(".field.no-label-element") do
              with_tag("button.submit#check_package span", "CHECK PACKAGE")
              with_tag("span#package_check_message")
            end
          end
        end

        it "should render check connection when a form is opened for associating an existing package definition" do
          in_params(:pipeline_name => "pipeline-name", :finger_print => "foo", :material => {:create_or_associate_pkg_def => "associate"})
          render :partial => "admin/pipelines/materials/package_material_form.html", :locals => {:scope => {:material => @material, :form => @form}}

          response.body.should have_tag(".new_form_content") do
            with_tag(".field.no-label-element") do
              with_tag("button.submit#check_package span", "CHECK PACKAGE")
              with_tag("span#package_check_message")
            end
          end
        end

        it "should not render package configuration elements if there is no package configured" do
          assigns[:package_configuration] = nil
          in_params(:pipeline_name => "pipeline-name", :finger_print => "foo", :material => {:create_or_associate_pkg_def => "associate"})

          render :partial => "admin/pipelines/materials/package_material_form.html", :locals => {:scope => {:material => @material, :form => @form}}

          response.body.should_not have_tag("#check_package span")
          response.body.should_not have_tag("span#package_check_message")
        end

        it "should render warning if no repositories exist" do
          assigns[:original_cruise_config].setPackageRepositories(PackageRepositories.new)
          render :partial => "admin/pipelines/materials/package_material_form.html", :locals => {:scope => {:material => @material, :form => @form}}

          response.body.should have_tag(".new_form_content") do
            with_tag("p.warnings", "No repositories found. Please add a package repository first.") do
              with_tag("a[href='#{package_repositories_new_path}']", "add a package repository")
            end
          end
        end

      end
    end
  end
end