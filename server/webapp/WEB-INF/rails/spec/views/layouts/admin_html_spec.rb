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

require 'rails_helper'
require_relative 'layout_html_examples'

describe "/layouts/admin" do
  it_should_behave_like :layout

  before do
    @layout_name = 'layouts/admin'
    @admin_url = "/admin/pipelines"
    @user = Username::ANONYMOUS
    assign(:user, @user)
    assign(:error_count, 0)
    assign(:warning_count, 0)
    allow(view).to receive(:can_view_admin_page?).and_return(true)
    allow(view).to receive(:is_user_a_group_admin?).and_return(true)
    allow(view).to receive(:is_user_an_admin?).and_return(true)
    allow(view).to receive(:is_user_a_template_admin?).and_return(false)
    allow(view).to receive(:is_user_authorized_to_view_templates?).and_return(false)
  end

  it "should display warning message when config not valid" do
    assign(:config_valid, false)
    render :inline => '<div>content</div>', :layout => @layout_name
    expect(response.body).to have_selector(".flash p.warning", "Invalid config on disk. Displaying the last known valid config (Editing config through Go will overwrite the invalid copy. Edit it on disk to fix this problem).")
  end

  describe "user-summary" do
    before(:each) do
      assign(:tab_name, "user-listing")
    end

    it "should show content" do
      render :inline => '<div>content</div>', :layout => @layout_name
      expect(response.body).to have_selector('#tab-content-of-user-listing', 'content')
    end
  end

  describe "pipeline-groups" do
    before(:each) do
      assign(:tab_name, "pipeline-groups")
    end

    it "should show content" do
      render :inline => '<div>content</div>', :layout => @layout_name
      expect(response.body).to have_selector('#tab-content-of-pipeline-groups')
    end

    it "should show tab button" do
      render :inline => "<div>content</div>", :layout => @layout_name
      expect(response.body).to have_selector("#pipeline-groups-tab-button.current_tab a#tab-link-of-pipeline-groups[href='/admin/pipelines']")
    end
  end

  describe "templates" do
    before(:each) do
      assign(:tab_name, "templates")
    end

    it "should show content" do
      render :inline => '<div>content</div>', :layout => @layout_name

      expect(response.body).to have_selector('#tab-content-of-templates')
    end

    it "should show tab button for super admins" do
      render :inline => "<div>content</div>", :layout => @layout_name
      expect(response.body).to have_selector("#templates-tab-button.current_tab a#tab-link-of-templates[href='/admin/templates']")
    end

    it "should not be visible for group admins" do
      allow(view).to receive(:is_user_authorized_to_view_templates?).and_return(false)
      allow(view).to receive(:is_user_a_group_admin?).and_return(true)
      allow(view).to receive(:is_user_an_admin?).and_return(false)

      render :inline => "<div>content</div>", :layout => @layout_name
      expect(response.body).to_not have_selector("#templates-tab-button.current_tab a#tab-link-of-templates[href='/admin/templates']")
    end

    it "should be visible for template admins" do
      allow(view).to receive(:is_user_authorized_to_view_templates?).and_return(true)
      allow(view).to receive(:is_user_an_admin?).and_return(false)

      render :inline => "<div>content</div>", :layout => @layout_name

      expect(response.body).to have_selector("#templates-tab-button.current_tab a#tab-link-of-templates[href='/admin/templates']")
    end

  end

  describe "server-backup" do

    it "should show backup tab" do
      render :inline => "<div>content</div>", :layout => @layout_name
      Capybara.string(response.body).find(".sub_tabs_container ul") do |ul|
        ul.find("#backup-tab-button") do |button|
          expect(button).to have_selector("a#tab-link-of-backup[href='/admin/backup']")
        end
      end
    end

    it "should show the backup tab as current if on that tab" do
      assign(:tab_name, "backup")

      render :inline => "<div>content</div>", :layout => @layout_name

      Capybara.string(response.body).find(".sub_tabs_container ul") do |ul|
        expect(ul).to have_selector("#backup-tab-button.current_tab")
      end
    end

    it "should show the contents for the backup tab" do
      assign(:tab_name, "backup")

      render :inline => "<div>content</div>", :layout => @layout_name

      expect(response.body).to have_selector(".sub_tab_container_content #tab-content-of-backup")
    end
  end

  describe "xml" do
    before(:each) do
      assign(:tab_name, "pipelines-snippet")
    end

    it "should show xml tab for group admin" do
      allow(view).to receive(:is_user_a_group_admin?).and_return(true)
      allow(view).to receive(:is_user_an_admin?).and_return(false)

      render :inline => 'content', :layout => @layout_name

      Capybara.string(response.body).find("#tab-content-of-pipelines-snippet.current_tab") do |tab|
        expect(tab).to have_selector("a[id='tab-link-of-pipelines-snippet'][href='/admin/pipelines/snippet']")
      end
    end

    it "should not show xml tab for go administrators" do
      allow(view).to receive(:is_user_a_group_admin?).and_return(true)
      allow(view).to receive(:is_user_an_admin?).and_return(true)

      render :inline => 'content', :layout => @layout_name
      expect(response.body).to_not have_selector('li#tab-content-of-pipelines-snippet')
    end

    it "should not show source xml tab for group admin" do
      allow(view).to receive(:is_user_a_group_admin?).and_return(true)
      allow(view).to receive(:is_user_an_admin?).and_return(false)

      render :inline => 'content', :layout => @layout_name

      expect(response.body).to_not have_selector('#source-xml-tab-button')
    end
  end

  describe "package-repositories" do
    before(:each) do
      assign(:tab_name, "package-repositories")
    end

    it "should show package repositories tab for super admin" do
      allow(view).to receive(:is_user_an_admin?).and_return(true)
      allow(view).to receive(:is_user_a_group_admin?).and_return(false)


      render :inline => 'content', :layout => @layout_name

      Capybara.string(response.body).find("#package-repositories-tab-button.current_tab") do |tab|
        expect(tab).to have_selector("a[id='tab-link-of-package-repositories'][href='/admin/package_repositories/new']")
      end
    end

    it "should show package repositories tab for group admin" do
      allow(view).to receive(:is_user_a_group_admin?).and_return(true)
      allow(view).to receive(:is_user_an_admin?).and_return(false)

      render :inline => 'content', :layout => @layout_name
      package_repositories_new_path

      Capybara.string(response.body).find("#package-repositories-tab-button.current_tab") do |tab|
        expect(tab).to have_selector("a[id='tab-link-of-package-repositories'][href='/admin/package_repositories/list']")
      end
    end
  end
end
