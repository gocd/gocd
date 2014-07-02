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

describe "admin/package_repositories/new.html.erb" do
  include GoUtil, FormUI, ReflectiveUtil
  include Admin::ConfigContextHelper
  include MockRegistryModule

  before(:each) do
    template.stub(:package_repositories_create_path).and_return("create_package_repositories_path")
    assigns[:cruise_config] = @cruise_config = CruiseConfig.new
    set(@cruise_config, "md5", "abc")
    assigns[:package_repository] = PackageRepository.new
    assigns[:package_repositories] = PackageRepositories.new
  end

  describe "new.html" do

    it "should have a page title and view title" do
      render "/admin/package_repositories/new.html"
      assigns[:view_title].should == "Administration"
    end

    it "should have package repository listing panel" do
      render "/admin/package_repositories/new.html"
      response.body.should have_tag("div#package-repositories") do
        with_tag ".navigation"
        with_tag("a.add", "Add New Repository")
      end
    end
    
    it "should have ajax_form_submit_errors div" do
      render "/admin/package_repositories/new.html"
      response.body.should have_tag("div#package-repositories") do
        with_tag("#ajax_form_submit_errors.form_submit_errors")
      end
    end


    it "should have add package repository form" do
      template.stub(:package_material_plugins).and_return([["[Select]", ""], "pluginid"])
      render "/admin/package_repositories/new.html"
      response.body.should have_tag("h2","Add Package RepositoryWhat is a Package Repository?")
      response.body.should have_tag("div#package-repositories form[method='post']") do
        with_tag "label[for='package_repository_name']"
        with_tag "input#package_repository_name[name='package_repository[name]']"

        with_tag "label[for='package_repository_pluginConfiguration_id']"
        with_tag "select#package_repository_pluginConfiguration_id[name='package_repository[pluginConfiguration][id]']" do
          with_tag("option[value='']","[Select]")
          with_tag("option[value='pluginid']","pluginid")
        end
        without_tag("div .information", "No plugins found. To configure a package repository you must have plugin(s) installed.")

        with_tag("p.required","* indicates a required field")

        with_tag("button span","SAVE")
        with_tag("button span","RESET")
      end
    end

    it "should display message next to select box if no plugins found" do
      template.stub(:package_material_plugins).and_return([["[Select]", ""]])
      render "/admin/package_repositories/new.html"
      with_tag "select#package_repository_pluginConfiguration_id[name='package_repository[pluginConfiguration][id]']" do
        with_tag("option[value='']","[Select]")
      end
      with_tag("div .information", "No plugins found. To configure a package repository you must have plugin(s) installed.")
    end
  end
end
