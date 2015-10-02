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

describe "_form.html.erb" do
  include GoUtil, FormUI
  before(:each) do
    @material = PackageMaterialConfig.new
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
    set(@cruise_config, "md5", "abc")
  end

  it "should render all material attributes" do
    in_params(:pipeline_name => "pipeline-name")
    render :partial => "admin/materials/package/form.html", :locals => {:scope => {:material => @material, :url => "url", :submit_label => "save"}}

    expect(response.body).to have_selector("input[type='hidden'][name='current_tab'][value='materials']")
    expect(response.body).to have_selector("input[type='radio'][name='material[create_or_associate_pkg_def]'][value='associate'][checked='checked']")
    expect(response.body).to have_selector("input[type='radio'][name='material[create_or_associate_pkg_def]'][value='create']")
    expect(response.body).to have_selector(".popup_form input[type='hidden'][name='material_type'][value='#{@material.getType()}']")
    Capybara.string(response.body).find("select[name='material[package_definition[repositoryId]]']").tap do |select|
      expect(select).not_to have_selector("option")
    end
    Capybara.string(response.body).find("select[name='material[#{PackageMaterialConfig::PACKAGE_ID}]']").tap do |select|
      expect(select).not_to have_selector("option")
    end
    expect(response.body).to have_selector(".form_buttons button[type='submit'] span", :text => "SAVE")
  end

  it "should pre-select repo and package for the material" do
    @material.setPackageDefinition(@pkg2)
    in_params(:pipeline_name => "pipeline-name", :finger_print => "foo")

    render :partial => "admin/materials/package/form.html", :locals => {:scope => {:material => @material, :url => "url", :submit_label => "save"}}

    expect(response.body).to have_selector("input[type='hidden'][name='current_tab'][value='materials']")
    expect(response.body).to have_selector(".popup_form input[type='hidden'][name='material_type'][value='#{@material.getType()}']")
    expect(response.body).to have_selector("input[type='radio'][name='material[create_or_associate_pkg_def]'][value='create']")
    expect(response.body).to have_selector("input[type='radio'][name='material[create_or_associate_pkg_def]'][value='associate'][checked='checked']")
    Capybara.string(response.body).find("select[name='material[package_definition[repositoryId]]']").tap do |select|
      expect(select).not_to have_selector("option")
    end
    Capybara.string(response.body).find("select[name='material[#{PackageMaterialConfig::PACKAGE_ID}]']").tap do |select|
      expect(select).not_to have_selector("option")
    end
    expect(response.body).to have_selector(".form_buttons button[type='submit'] span", :text => "SAVE")
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

      stub_template "admin/package_definitions/show_package_definition" => "show package definition"

      render :partial => "admin/materials/package/form.html", :locals => {:scope => {:material => @material, :url => "url", :submit_label => "save"}}

      assert_template partial: "admin/package_definitions/show_package_definition", :locals => {:scope => {:package_configuration => @package_view_model_new}}
    end

    it "should render new package definition with check connection if the form was opened to create a new package definition" do
      in_params(:pipeline_name => "pipeline-name", :finger_print => "foo", :material => {:create_or_associate_pkg_def => "create"})

      stub_template "admin/package_definitions/form" => "package definitions form"

      render :partial => "admin/materials/package/form.html", :locals => {:scope => {:material => @material, :url => "url", :submit_label => "save"}}

      assert_template partial: "admin/package_definitions/form", :locals => {:scope => {:package_configuration => @package_view_model_new}}
    end

    it "should render check connection when a form is opened for creating a new package definition" do
      in_params(:pipeline_name => "pipeline-name", :finger_print => "foo", :material => {:create_or_associate_pkg_def => "create"})

      render :partial => "admin/materials/package/form.html", :locals => {:scope => {:material => @material, :url => "url", :submit_label => "save"}}

      Capybara.string(response.body).find(".new_form_content").tap do |new_form_content|
        new_form_content.find(".field.no-label-element") do |no_label_element|
          expect(no_label_element).to have_selector("button.submit#check_package span", :text => "CHECK PACKAGE")
          expect(no_label_element).to have_selector("span#package_check_message")
        end
      end
    end

    it "should render check connection when a form is opened for associating an existing package definition" do
      in_params(:pipeline_name => "pipeline-name", :finger_print => "foo", :material => {:create_or_associate_pkg_def => "associate"})

      render :partial => "admin/materials/package/form.html", :locals => {:scope => {:material => @material, :url => "url", :submit_label => "save"}}

      Capybara.string(response.body).find(".new_form_content").tap do |new_form_content|
        new_form_content.find(".field.no-label-element") do |no_label_element|
          expect(no_label_element).to have_selector("button.submit#check_package span", :text => "CHECK PACKAGE")
          expect(no_label_element).to have_selector("span#package_check_message")
        end
      end
    end

    it "should render warning if no repositories exist" do
      @cruise_config.setPackageRepositories(PackageRepositories.new)

      render :partial => "admin/materials/package/form.html", :locals => {:scope => {:material => @material, :url => "url", :submit_label => "save"}}

      Capybara.string(response.body).find(".new_form_content").tap do |new_form_content|
          expect(new_form_content).to have_selector("p.warnings", :text => "No repositories found. Please add a package repository first.")
          new_form_content.find("p.warnings") do |warnings|
            expect(warnings).to have_selector("a[href='#{package_repositories_new_path}']", :text => "add a package repository")
          end
      end
    end


    it "should not render package configuration elements if there is no package configured" do
      assign(:package_configuration, nil)
      in_params(:pipeline_name => "pipeline-name", :finger_print => "foo", :material => {:create_or_associate_pkg_def => "associate"})

      render :partial => "admin/materials/package/form.html", :locals => {:scope => {:material => @material, :url => "url", :submit_label => "save"}}

      expect(response.body).not_to have_selector("#check_package span")
      expect(response.body).not_to have_selector("span#package_check_message")
    end
  end
end
