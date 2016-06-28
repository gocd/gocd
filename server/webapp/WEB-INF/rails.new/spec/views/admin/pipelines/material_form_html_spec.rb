##########################GO-LICENSE-START################################
# Copyright 2016 ThoughtWorks, Inc.
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
    assign(:all_pipelines, java.util.ArrayList.new)
    tvms = java.util.ArrayList.new
    tvms.add(com.thoughtworks.go.presentation.TaskViewModel.new(com.thoughtworks.go.config.AntTask.new,"template","erb"))
    assign(:task_view_models, tvms)
    assign(:config_context, create_config_context(MockRegistryModule::MockRegistry.new))

    @cruise_config = BasicCruiseConfig.new
    repository1 = PackageRepositoryMother.create("repo1", "repo1-name", "pluginid", "version", Configuration.new([ConfigurationPropertyMother.create("k1", false, "v1")].to_java(ConfigurationProperty)))
    repository2 = PackageRepositoryMother.create("repo2", "repo2-name", "pluginid", "version", Configuration.new([ConfigurationPropertyMother.create("k1", false, "v1")].to_java(ConfigurationProperty)))
    pkg1 = PackageDefinitionMother.create("pkg1", "package1-name", Configuration.new([ConfigurationPropertyMother.create("k2", false, "v2")].to_java(ConfigurationProperty)), repository1)
    @pkg2 = PackageDefinitionMother.create("pkg2", "package2-name", Configuration.new([ConfigurationPropertyMother.create("k2", false, "v2")].to_java(ConfigurationProperty)), repository1)
    repository1.setPackages(Packages.new([pkg1, @pkg2].to_java(PackageDefinition)))
    repos = PackageRepositories.new
    repos.add(repository1)
    repos.add(repository2)
    @cruise_config.setPackageRepositories(repos)
    assign(:cruise_config, @cruise_config)
    assign(:original_cruise_config, @cruise_config)
    view.stub(:is_user_a_group_admin?).and_return(false)
    set(@cruise_config, "md5", "abc")
    view.stub(:render_pluggable_form_template).and_return("template")
  end

  describe "Materials" do

    it "should have material section " do
      render

      Capybara.string(response.body).find('div.steps_panes.sub_tab_container_content div#tab-content-of-materials').tap do |tab|
        expect(tab).to have_selector("h2.section_title", :text => "Step 2: Materials")
        expect(tab).to have_selector("label", :text => "Material Type*")
        tab.find("select[name='pipeline_group[pipeline][materials][materialType]']") do |select|
          expect(select).to have_selector("option[value='SvnMaterial']", :text => "Subversion")
          expect(select).to have_selector("option[value='GitMaterial']", :text => "Git")
          expect(select).to have_selector("option[value='HgMaterial']", :text => "Mercurial")
          expect(select).to have_selector("option[value='P4Material']", :text => "Perforce")
          expect(select).to have_selector("option[value='DependencyMaterial']", :text => "Pipeline")
        end
        expect(tab).to have_selector("button.cancel_button", :text => "Cancel")
        expect(tab).to have_selector("button#next_to_materials", :text => "Next")
      end
    end

    describe "Svn material" do

      it "should render all svn material attributes" do
        in_params(:pipeline_name => "pipeline_name")

        render

        Capybara.string(response.body).find('div#tab-content-of-materials #material_forms .SvnMaterial').tap do |form|
          expect(form).to have_selector("label", :text => "URL*")
          expect(form).to have_selector("input[type='text'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::URL}]']")

          expect(form).to have_selector("label", :text => "Username")
          expect(form).to have_selector("input.svn_username[type='text'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::USERNAME}]']")

          expect(form).to have_selector("label", :text => "Password")
          expect(form).to have_selector("input.svn_password[type='password'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::PASSWORD}]']")

          expect(form).to have_selector("div.hidden input[type='checkbox'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::PASSWORD_CHANGED}]'][value='1'][checked='checked']")

          expect(form).to have_selector("label", :text => "Poll for new changes")
          expect(form).to have_selector("input[type='checkbox'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.ScmMaterialConfig::AUTO_UPDATE}]'][checked='checked']")
        end
      end

      it "should display check connection button" do
        render

        expect(response.body).to have_selector("button#check_connection_svn", :text => "CHECK CONNECTION")
        expect(response.body).to have_selector("#vcsconnection-message_svn", :text => "", visible: false)
      end

      it "should display new svn material view with errors" do
        error = config_error(com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::URL, "Url is wrong")
        error.add(com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::USERNAME, "Username is wrong")
        error.add(com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::PASSWORD, "Password is wrong")

        set(@material_config, "errors", error)

        render

        Capybara.string(response.body).find("form[method='post'][action='create_path']").tap do |form|
          Capybara.string(response.body).find("div.steps_panes.sub_tab_container_content div#tab-content-of-materials #material_forms .SvnMaterial").tap do |svn|
            expect(svn).to have_selector("div.field_with_errors input[type='text'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::URL}]'][value='svn://foo']")
            expect(svn).to have_selector("div.form_error", :text => "Url is wrong")

            expect(svn).to have_selector("div.field_with_errors input[type='text'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::USERNAME}]'][value='loser']")
            expect(svn).to have_selector("div.form_error", :text => "Username is wrong")

            expect(svn).to have_selector("div.field_with_errors input[type='password'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.svn.SvnMaterialConfig::PASSWORD}]'][value='secret']")
            expect(svn).to have_selector("div.form_error", :text => "Password is wrong")
          end
        end
      end
    end

    describe "Git Material" do
      it "should render all git material attributes" do
        in_params(:pipeline_name => "pipeline_name")

        render

        Capybara.string(response.body).find("div#tab-content-of-materials #material_forms .GitMaterial").tap do |form|
          expect(form).to have_selector("label", :text => "URL*")
          expect(form).to have_selector("input[type='text'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.git.GitMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.git.GitMaterialConfig::URL}]']")

          expect(form).to have_selector("label", :text => "Branch")
          expect(form).to have_selector("input[type='text'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.git.GitMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.git.GitMaterialConfig::BRANCH}]']")

          expect(form).to have_selector("label", :text => "Poll for new changes")
          expect(form).to have_selector("input[type='checkbox'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.git.GitMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.ScmMaterialConfig::AUTO_UPDATE}]'][checked='checked']")

          expect(form).to have_selector("label", :text => "Shallow clone (recommended for large repositories)")
          expect(form).to have_selector("input[type='checkbox'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.git.GitMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.git.GitMaterialConfig::SHALLOW_CLONE}]']")
        end
      end
    end

    describe "Hg Material" do
      it "should render all hg material attributes" do
        in_params(:pipeline_name => "pipeline_name")

        render

        Capybara.string(response.body).find("div#tab-content-of-materials #material_forms .HgMaterial").tap do |form|
          expect(form).to have_selector("label", :text => "URL*")
          expect(form).to have_selector("input[type='text'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig::URL}]']")

          expect(form).to have_selector("label", :text => "Poll for new changes")
          expect(form).to have_selector("input[type='checkbox'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.ScmMaterialConfig::AUTO_UPDATE}]'][checked='checked']")
        end
      end
    end

    describe "Tfs Material" do
      it "should render all tfs material attributes" do
        in_params(:pipeline_name => "pipeline_name")

        render

        Capybara.string(response.body).find("div#tab-content-of-materials #material_forms .TfsMaterial").tap do |form|
          expect(form).to have_selector("label", :text => "URL*")
          expect(form).to have_selector("input[type='text'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.ScmMaterialConfig::URL}]']")
          expect(form).to have_selector("input.tfs_username[type='text'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.ScmMaterialConfig::USERNAME}]']")
          expect(form).to have_selector("input.tfs_password[type='password'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.ScmMaterialConfig::PASSWORD}]']")
          expect(form).to have_selector("input[type='text'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig::PROJECT_PATH}]']")

          expect(form).to have_selector("label", :text => "Poll for new changes")
          expect(form).to have_selector("input[type='checkbox'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.ScmMaterialConfig::AUTO_UPDATE}]'][checked='checked']")

          expect(form).not_to have_selector("input[type='text'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig::TYPE}][workspaceOwner]']")
          expect(form).not_to have_selector("input[type='text'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig::TYPE}][workspace]']")
        end
      end
    end

    describe "P4 Material" do
      it "should render all P4 material attributes" do
        in_params(:pipeline_name => "pipeline_name")

        render

        Capybara.string(response.body).find("div#tab-content-of-materials #material_forms .P4Material").tap do |form|
          expect(form).to have_selector("label", :text => "Server and Port*")
          expect(form).to have_selector("input[type='text'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::SERVER_AND_PORT}]']")

          expect(form).to have_selector("label", :text => "Username")
          expect(form).to have_selector("input.p4_username[type='text'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::USERNAME}]']")

          expect(form).to have_selector("label", :text => "Password")
          expect(form).to have_selector("input.p4_password[type='password'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::PASSWORD}]']")

          expect(form).to have_selector("label", :text => "View*")
          expect(form).to have_selector("textarea[name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::VIEW}]']")

          expect(form).to have_selector("label[for='material_useTickets']", :text => "Use tickets")
          expect(form).to have_selector("input[id='material_useTickets'][type='checkbox'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::USE_TICKETS}]']")

          expect(form).to have_selector("label", :text => "Poll for new changes")
          expect(form).to have_selector("input[type='checkbox'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.perforce.P4MaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.ScmMaterialConfig::AUTO_UPDATE}]'][checked='checked']")
        end
      end
    end

    describe "Dependency Material" do
      it "should render all Dependency material attributes" do
        in_params(:pipeline_name => "pipeline_name")

        render

        Capybara.string(response.body).find("div#tab-content-of-materials .DependencyMaterial").tap do |form|
          expect(form).to have_selector("label", :text => "Pipeline [Stage]*")
          expect(form).to have_selector("input[type='text'][name='pipeline_group[pipeline][materials][#{com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig::TYPE}][#{com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig::PIPELINE_STAGE_NAME}]']")
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

        render

        Capybara.string(response.body).find("div#tab-content-of-materials #material_forms .PackageMaterial").tap do |form|
          expect(form).to have_selector("input[type='hidden'][name='material_type'][value='#{com.thoughtworks.go.config.materials.PackageMaterialConfig::TYPE}']")
          expect(form).to have_selector("input[type='radio'][name='material[create_or_associate_pkg_def]'][value='associate'][checked='checked']")
          expect(form).to have_selector("input[type='radio'][name='material[create_or_associate_pkg_def]'][value='create']")
          form.find("select.required[name='material[package_definition[repositoryId]]']") do |select|
            expect(select).not_to have_selector("option")
          end
          form.find("select.required[name='material[#{PackageMaterialConfig::PACKAGE_ID}]']") do |select|
            expect(select).not_to have_selector("option")
          end
        end
      end

      it "should pre-select repo and package for the material" do
        @material.setPackageDefinition(@pkg2)
        in_params(:pipeline_name => "pipeline-name", :finger_print => "foo")

        render :partial => "admin/pipelines/materials/package_material_form.html", :locals => {:scope => {:material => @material, :form => @form}}

        expect(response.body).to have_selector("input[type='hidden'][name='material_type'][value='#{@material.getType()}']")
        expect(response.body).to have_selector("input[type='radio'][name='material[create_or_associate_pkg_def]'][value='create']")
        expect(response.body).to have_selector("input[type='radio'][name='material[create_or_associate_pkg_def]'][value='associate'][checked='checked']")

        Capybara.string(response.body).find("select.required[name='material[package_definition[repositoryId]]']").tap do |select|
          expect(select).not_to have_selector("option")
        end
        Capybara.string(response.body).find("select.required[name='material[#{PackageMaterialConfig::PACKAGE_ID}]']").tap do |select|
          expect(select).not_to have_selector("option")
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
          @package_view_model_new = PackageViewModel.new(@metadata, @package)
          assign(:package_configuration, @package_view_model_new)
        end

        it "should render show_package_definition with check connection if the form was opened for associating a existing package definition" do
          in_params(:pipeline_name => "pipeline-name", :finger_print => "foo", :material => {:create_or_associate_pkg_def => "associate"})

          stub_template "admin/package_definitions/show_package_definition_for_new_pipeline_wizard" => "show package definition for new pipeline wizard"

          render :partial => "admin/pipelines/materials/package_material_form.html", :locals => {:scope => {:material => @material, :form => @form}}

          assert_template partial: "admin/package_definitions/show_package_definition_for_new_pipeline_wizard", :locals => {:scope => {:package_configuration => @package_view_model_new}}
        end

        it "should render new package definition with check connection if the form was opened to create a new package definition" do
          in_params(:pipeline_name => "pipeline-name", :finger_print => "foo", :material => {:create_or_associate_pkg_def => "create"})

          stub_template "admin/package_definitions/form_for_new_pipeline_wizard" => "form for new pipeline wizard"

          render :partial => "admin/pipelines/materials/package_material_form.html", :locals => {:scope => {:material => @material, :form => @form}}

          assert_template partial: "admin/package_definitions/form_for_new_pipeline_wizard", :locals => {:scope => {:package_configuration => @package_view_model_new}}
        end

        it "should render check connection when a form is opened for creating a new package definition" do
          in_params(:pipeline_name => "pipeline-name", :finger_print => "foo", :material => {:create_or_associate_pkg_def => "create"})

          render :partial => "admin/pipelines/materials/package_material_form.html", :locals => {:scope => {:material => @material, :form => @form}}

          Capybara.string(response.body).find('.new_form_content').tap do |form|
            form.find(".field.no-label-element") do |field|
              expect(field).to have_selector("button.submit#check_package span", :text => "CHECK PACKAGE")
              expect(field).to have_selector("span#package_check_message")
            end
          end
        end

        it "should render check connection when a form is opened for associating an existing package definition" do
          in_params(:pipeline_name => "pipeline-name", :finger_print => "foo", :material => {:create_or_associate_pkg_def => "associate"})

          render :partial => "admin/pipelines/materials/package_material_form.html", :locals => {:scope => {:material => @material, :form => @form}}

          Capybara.string(response.body).find('.new_form_content').tap do |form|
            form.find(".field.no-label-element") do |field|
              expect(field).to have_selector("button.submit#check_package span", :text => "CHECK PACKAGE")
              expect(field).to have_selector("span#package_check_message")
            end
          end
        end

        it "should not render package configuration elements if there is no package configured" do
          assign(:package_configuration, nil)
          in_params(:pipeline_name => "pipeline-name", :finger_print => "foo", :material => {:create_or_associate_pkg_def => "associate"})

          render :partial => "admin/pipelines/materials/package_material_form.html", :locals => {:scope => {:material => @material, :form => @form}}

          expect(response.body).not_to have_selector("#check_package span")
          expect(response.body).not_to have_selector("span#package_check_message")
        end

        it "should render warning if no repositories exist" do
          @cruise_config.setPackageRepositories(PackageRepositories.new)

          render :partial => "admin/pipelines/materials/package_material_form.html", :locals => {:scope => {:material => @material, :form => @form}}

          Capybara.string(response.body).find('.new_form_content').tap do |div|
              expect(div).to have_selector("p.warnings", :text => "No repositories found. Please add a package repository first.")
              div.find("p.warnings") do |warnings|
                expect(warnings).to have_selector("a[href='#{package_repositories_new_path}']", :text => "add a package repository")
              end
          end
        end
      end
    end
  end
end
