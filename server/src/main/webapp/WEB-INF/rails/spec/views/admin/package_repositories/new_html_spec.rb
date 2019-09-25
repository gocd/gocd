#
# Copyright 2019 ThoughtWorks, Inc.
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
#

require 'rails_helper'

describe "admin/package_repositories/new.html.erb" do
  include GoUtil
  include FormUI
  include ReflectiveUtil
  include MockRegistryModule

  before(:each) do
    allow(view).to receive(:package_repositories_create_path).and_return("create_package_repositories_path")
    assign(:cruise_config, @cruise_config = BasicCruiseConfig.new)
    set(@cruise_config, "md5", "abc")
    assign(:package_repository, PackageRepository.new)
    assign(:package_repositories, PackageRepositories.new)
  end

  describe "new.html" do

    it "should have a page title and view title" do
      render

      expect(view.instance_variable_get("@view_title")).to eq("Administration")
    end

    it "should have package repository listing panel" do
      render

      Capybara.string(response.body).find('div#package-repositories').tap do |div|
        expect(div).to have_selector ".navigation"
        expect(div).to have_selector("a.add", :text => "Add New Repository")
      end
    end

    it "should have ajax_form_submit_errors div" do
      render

      Capybara.string(response.body).find("div#package-repositories").tap do |div|
        expect(div).to have_selector("#ajax_form_submit_errors.form_submit_errors")
      end
    end

    it "should render package repository form with installed plugin" do
      allow(view).to receive(:package_material_plugins).and_return([["[Select]", ""], "pluginid"])

      render

      expect(response.body).to have_selector("h2", :text => "Add Package RepositoryWhat is a Package Repository?")

      Capybara.string(response.body).find("div#package-repositories form[method='post']").tap do |form|
        expect(form).to have_selector "label[for='package_repository_name']"
        expect(form).to have_selector "input#package_repository_name[name='package_repository[name]']"

        expect(form).to have_selector "label[for='package_repository_pluginConfiguration_id']"
        form.find("select#package_repository_pluginConfiguration_id[name='package_repository[pluginConfiguration][id]']") do |select|
          expect(select).to have_selector("option[value='']", :text => "[Select]")
          expect(select).to have_selector("option[value='pluginid']", :text => "pluginid")
        end
        expect(form).not_to have_selector("div .information", :text => "No plugins found. To configure a package repository you must have plugin(s) installed.")

        expect(form).to have_selector("p.required", :text => "* indicates a required field")

        expect(form).to have_selector("button span", :text => "SAVE")
        expect(form).to have_selector("button span", :text => "RESET")
      end
    end

    it "should display message when no package repository plugin installed" do
      allow(view).to receive(:package_material_plugins).and_return([["[Select]", ""]])

      render

      Capybara.string(response.body).find("select#package_repository_pluginConfiguration_id[name='package_repository[pluginConfiguration][id]']").tap do |select|
        expect(select).to have_selector("option[value='']", :text => "[Select]")
      end

      expect(response.body).to have_selector("div .information", :text => "No plugins found. To configure a package repository you must have plugin(s) installed.")
    end
  end
end
