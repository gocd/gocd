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

require File.join(File.dirname(__FILE__), "..", "..", "..", "..", "spec_helper")

describe "_form.html.erb" do
  include GoUtil, FormUI
  before(:each) do
    @material = PackageMaterialConfig.new
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
    assign(:cruise_config, @cruise_config)
    assign(:original_cruise_config, @cruise_config)
    set(@cruise_config, "md5", "abc")
  end

  it "should render all material attributes" do
    in_params(:pipeline_name => "pipeline-name")
    render :partial => "admin/materials/package/form.html", :locals => {:scope => {:material => @material, :url => "url", :submit_label => "save"}}

    response.body.should have_tag("input[type='hidden'][name='current_tab'][value=?]", "materials")
    response.body.should have_tag("input[type='radio'][name='material[create_or_associate_pkg_def]'][value='associate'][checked=?]", "checked")
    response.body.should have_tag("input[type='radio'][name='material[create_or_associate_pkg_def]'][value='create']")
    response.body.should have_tag(".popup_form input[type='hidden'][name='material_type'][value='#{@material.getType()}']")
    response.body.should have_tag("select[name='material[package_definition[repositoryId]]']") do
      without_tag("option")
    end
    response.body.should have_tag("select[name='material[#{PackageMaterialConfig::PACKAGE_ID}]']") do
      without_tag("option")
    end
    response.body.should have_tag(".form_buttons button[type='submit'] span", "SAVE")
  end

  it "should pre-select repo and package for the material" do
    @material.setPackageDefinition(@pkg2)
    in_params(:pipeline_name => "pipeline-name", :finger_print => "foo")

    render :partial => "admin/materials/package/form.html", :locals => {:scope => {:material => @material, :url => "url", :submit_label => "save"}}

    response.body.should have_tag("input[type='hidden'][name='current_tab'][value=?]", "materials")
    response.body.should have_tag(".popup_form input[type='hidden'][name='material_type'][value='#{@material.getType()}']")
    response.body.should have_tag("input[type='radio'][name='material[create_or_associate_pkg_def]'][value='create']")
    response.body.should have_tag("input[type='radio'][name='material[create_or_associate_pkg_def]'][value='associate'][checked=?]", "checked")
    response.body.should have_tag("select[name='material[package_definition[repositoryId]]']") do
      without_tag("option")
    end
    response.body.should have_tag("select[name='material[#{PackageMaterialConfig::PACKAGE_ID}]']") do
      without_tag("option")
    end
    response.body.should have_tag(".form_buttons button[type='submit'] span", "SAVE")
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
      assign(:package_configuration, PackageViewModel.new(@metadata, @package))
    end

    it "should render show_package_definition with check connection if the form was opened for associating a existing package definition" do
      in_params(:pipeline_name => "pipeline-name", :finger_print => "foo", :material => {:create_or_associate_pkg_def => "associate"})

      @controller.template.should_receive(:render).with(:partial => 'admin/package_definitions/show_package_definition', :locals => {:scope => {:package_configuration => assigns[:package_configuration]}})
      render :partial => "admin/materials/package/form.html", :locals => {:scope => {:material => @material, :url => "url", :submit_label => "save"}}
    end

    it "should render new package definition with check connection if the form was opened to create a new package definition" do
      in_params(:pipeline_name => "pipeline-name", :finger_print => "foo", :material => {:create_or_associate_pkg_def => "create"})

      @controller.template.should_receive(:render).with(:partial => 'admin/package_definitions/form', :locals => {:scope => {:package_configuration => assigns[:package_configuration]}})
      render :partial => "admin/materials/package/form.html", :locals => {:scope => {:material => @material, :url => "url", :submit_label => "save"}}
    end

    it "should render check connection when a form is opened for creating a new package definition" do
      in_params(:pipeline_name => "pipeline-name", :finger_print => "foo", :material => {:create_or_associate_pkg_def => "create"})
      render :partial => "admin/materials/package/form.html", :locals => {:scope => {:material => @material, :url => "url", :submit_label => "save"}}

      response.body.should have_tag(".new_form_content") do
        with_tag(".field.no-label-element") do
          with_tag("button.submit#check_package span", "CHECK PACKAGE")
          with_tag("span#package_check_message")
        end
      end
    end

    it "should render check connection when a form is opened for associating an existing package definition" do
      in_params(:pipeline_name => "pipeline-name", :finger_print => "foo", :material => {:create_or_associate_pkg_def => "associate"})
      render :partial => "admin/materials/package/form.html", :locals => {:scope => {:material => @material, :url => "url", :submit_label => "save"}}

      response.body.should have_tag(".new_form_content") do
        with_tag(".field.no-label-element") do
          with_tag("button.submit#check_package span", "CHECK PACKAGE")
          with_tag("span#package_check_message")
        end
      end
    end

    it "should render warning if no repositories exist" do
      assigns[:original_cruise_config].setPackageRepositories(PackageRepositories.new)
      render :partial => "admin/materials/package/form.html", :locals => {:scope => {:material => @material, :url => "url", :submit_label => "save"}}

      response.body.should have_tag(".new_form_content") do
          with_tag("p.warnings", "No repositories found. Please add a package repository first.") do
            with_tag("a[href='#{package_repositories_new_path}']", "add a package repository")
          end
      end
    end


    it "should not render package configuration elements if there is no package configured" do
      assign(:package_configuration, nil)
      in_params(:pipeline_name => "pipeline-name", :finger_print => "foo", :material => {:create_or_associate_pkg_def => "associate"})

      render :partial => "admin/materials/package/form.html", :locals => {:scope => {:material => @material, :url => "url", :submit_label => "save"}}

      response.body.should_not have_tag("#check_package span")
      response.body.should_not have_tag("span#package_check_message")
    end
  end
end