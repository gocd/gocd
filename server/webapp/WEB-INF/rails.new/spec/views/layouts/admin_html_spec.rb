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
load File.join(File.dirname(__FILE__), 'layout_html_examples.rb')

describe "/layouts/admin" do
  include EngineUrlHelper

  before do
    stub_server_health_messages
  end
  before do
    @layout_name = 'layouts/admin'
    @admin_url = "/admin/pipelines"
    @user = Object.new
    assign(:user, @user)
    assign(:error_count, 0)
    assign(:warning_count, 0)
    @user.stub(:anonymous?).and_return(true)
    allow(view).to receive(:can_view_admin_page?).and_return(true)
    allow(view).to receive(:is_user_a_group_admin?).and_return(true)
    allow(view).to receive(:is_user_an_admin?).and_return(true)
    allow(view).to receive(:is_user_a_template_admin?).and_return(false)
    class << view
      def url_for_with_stub *args
        args.empty? ? "/go/" : url_for_without_stub(*args)
      end

      alias_method_chain :url_for, :stub
    end
    oauth_engine = double('oauth_engine')
    stub_oauth2_provider_engine oauth_engine
    allow(view).to receive(:oauth_engine).and_return(oauth_engine)

    main_app = double('main_app')
    stub_routes_for_main_app main_app
    allow(view).to receive(:main_app).and_return(main_app)
  end

  it_should_behave_like :layout

  it "should display only pipeline configurations and pipeline groups tab when the user is a group admin" do
    allow(view).to receive(:is_user_an_admin?).and_return(false)
    allow(view).to receive(:is_user_a_group_admin?).and_return(true)

    render :inline => '<div>content</div>', :layout => @layout_name
    Capybara.string(response.body).find('.sub_tab_container').tap do |tab|
      expect(tab).to have_selector("#pipeline-groups-tab-button")
      expect(tab).to_not have_selector("#source-xml-tab-button")
      expect(tab).to_not have_selector("#server-configuration-tab-button")
      expect(tab).to_not have_selector("#user-summary-tab-button")
      expect(tab).to_not have_selector("#oauth-clients-tab-button")
      expect(tab).to_not have_selector("#gadget-providers-tab-button")
    end
  end

  it "should display warning message when config not valid" do
    assign(:config_valid, false)
    render :inline => '<div>content</div>', :layout => @layout_name
    expect(response.body).to have_selector(".flash p.warning", "Invalid config on disk. Displaying the last known valid config (Editing config through Go will overwrite the invalid copy. Edit it on disk to fix this problem).")
  end

  describe "plugins" do
    before(:each) do
      assign(:tab_name, "plugins-listing")
    end

    it "should show plugins listing page if plugins are enabled" do
      allow(view).to receive(:is_plugins_enabled?).and_return(true)
      allow(view).to receive(:plugins_listing_path).and_return("some_path_to_plugins")
      render :inline => "<div>content</div>", :layout => @layout_name
      Capybara.string(response.body).find(".sub_tabs_container") do |tab|
        tab.find("li#plugins-listing-tab-button") do |li|
          expect(li).to have_selector("a#tab-link-of-plugins-listing[href='some_path_to_plugins']", "Plugins")
        end
      end
    end

    it "should not show plugins listing page if plugins are disabled" do
      allow(view).to receive(:is_plugins_enabled?).and_return(false)
      allow(view).to receive(:plugins_listing_path).and_return("some_path_to_plugins")
      render :inline => "<div>content</div>", :layout => @layout_name
      Capybara.string(response.body).find(".sub_tabs_container") do |tab|
        expect(tab).to_not have_selector("li#plugins-listing-tab-button")
      end
    end

    it "should show plugins listing page if plugins are enabled and user is admin" do
      allow(view).to receive(:is_plugins_enabled?).and_return(true)

      allow(view).to receive(:is_user_an_admin?).and_return(true)

      allow(view).to receive(:plugins_listing_path).and_return("some_path_to_plugins")
      render :inline => "<div>content</div>", :layout => @layout_name
      Capybara.string(response.body).find(".sub_tabs_container") do |tab|
        tab.find("li#plugins-listing-tab-button") do |li|
          expect(li).to have_selector("a#tab-link-of-plugins-listing[href='some_path_to_plugins']", "Plugins")
        end
      end
    end

    it "should show plugins listing page if plugins are enabled and user is group admin" do
      allow(view).to receive(:is_plugins_enabled?).and_return(true)

      allow(view).to receive(:is_user_an_admin?).and_return(false)
      allow(view).to receive(:is_user_a_group_admin?).and_return(true)

      allow(view).to receive(:plugins_listing_path).and_return("some_path_to_plugins")
      render :inline => "<div>content</div>", :layout => @layout_name
      Capybara.string(response.body).find(".sub_tabs_container") do |tab|
        tab.find("li#plugins-listing-tab-button") do
          expect(li).to have_selector("a#tab-link-of-plugins-listing[href='some_path_to_plugins']", "Plugins")
        end
      end
    end

    it "should not show plugins listing page if plugins are enabled and user is neither an admin nor a group admin" do
      allow(view).to receive(:is_plugins_enabled?).and_return(true)

      allow(view).to receive(:is_user_an_admin?).and_return(false)
      allow(view).to receive(:is_user_a_group_admin?).and_return(false)

      allow(view).to receive(:plugins_listing_path).and_return("some_path_to_plugins")
      render :inline => "<div>content</div>", :layout => @layout_name
      Capybara.string(response.body).find(".sub_tabs_container") do |tab|
        expect(tab).to_not have_selector("li#plugins-listing-tab-button")
      end
    end
  end

  describe "user-summary" do
    before(:each) do
      assign(:tab_name, "user-listing")
    end

    it "should show content" do
      render :inline => '<div>content</div>', :layout => @layout_name
      expect(response.body).to have_selector('#tab-content-of-user-listing', 'content')
    end

    it "should show tab button" do
      render :inline => "<div>content</div>", :layout => @layout_name
      expect(response.body).to have_selector("#user-summary-tab-button.current_tab a#tab-link-of-user-listing[href='/path/for/user/listing']", 'User Summary')
    end

    it "should not show oauth_clients tab button when on user-summary" do
      render :inline => "<div>content</div>", :layout => @layout_name
      expect(response.body).to_not have_selector("#oauth-clients-tab-button.current_tab")
      expect(response.body).to have_selector("#oauth-clients-tab-button a#tab-link-of-oauth-clients[href='/path/for/oauth/clients']", 'OAuth Clients')
    end
  end

  describe "pipeline-groups" do
    before(:each) do
      assign(:tab_name, "pipeline-groups")
    end

    it "should show content" do
      render :inline => '<div>content</div>', :layout => @layout_name
      expect(response.body).to have_selector('#tab-content-of-pipeline-groups', 'content')
    end

    it "should show tab button" do
      render :inline => "<div>content</div>", :layout => @layout_name
      expect(response.body).to have_selector("#pipeline-groups-tab-button.current_tab a#tab-link-of-pipeline-groups[href='/path/to/pipeline/groups']", 'Pipelines')
    end
  end

  describe "oauth-clients" do
    before(:each) do
      assign(:tab_name, "oauth-clients")
    end

    it "should show content" do
      render :inline => '<div>content</div>', :layout => @layout_name
      expect(response.body).to have_selector('#tab-content-of-oauth-clients', 'content')
    end

    it "should show tab button" do
      render :inline => "<div>content</div>", :layout => @layout_name
      expect(response.body).to have_selector("#oauth-clients-tab-button.current_tab a#tab-link-of-oauth-clients[href='/path/for/oauth/clients']", 'OAuth Clients')
    end

    it "should not highlight user_listing tab button when on oauth clients tab" do
      render :inline => "<div>content</div>", :layout => @layout_name
      expect(response.body).to_not have_selector("#user-summary-tab-button.current_tab")
      expect(response.body).to have_selector("#user-summary-tab-button a#tab-link-of-user-listing[href='/path/for/user/listing']", 'User Summary')
    end
  end

  describe "templates" do
    before(:each) do
      assign(:tab_name, "templates")
    end

    it "should show content" do
      render :inline => '<div>content</div>', :layout => @layout_name

      expect(response.body).to have_selector('#tab-content-of-templates', 'content')
    end

    it "should show tab button for super admins" do
      render :inline => "<div>content</div>", :layout => @layout_name
      expect(response.body).to have_selector("#templates-tab-button.current_tab a#tab-link-of-templates[href='/path/to/templates']", 'Templates')
    end

    it "should not be visible for group admins" do
      allow(view).to receive(:is_user_a_group_admin?).and_return(true)
      allow(view).to receive(:is_user_an_admin?).and_return(false)

      render :inline => "<div>content</div>", :layout => @layout_name
      expect(response.body).to_not have_selector("#templates-tab-button.current_tab a#tab-link-of-templates[href='/path/to/templates']", 'Templates')
    end

    it "should be visible for template admins" do
      allow(view).to receive(:is_user_a_template_admin?).and_return(true)
      allow(view).to receive(:is_user_an_admin?).and_return(false)

      render :inline => "<div>content</div>", :layout => @layout_name

      expect(response.body).to have_selector("#templates-tab-button.current_tab a#tab-link-of-templates[href='/path/to/templates']", 'Templates')
    end

  end

  describe "server-backup" do

    it "should show backup tab" do
      render :inline => "<div>content</div>", :layout => @layout_name
      Capybara.string(response.body).find(".sub_tabs_container ul") do |ul|
        ul.find("#backup-tab-button") do |button|
          expect(button).to have_selector("a#tab-link-of-backup[href='admin/backup']", "Backup")
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
        expect(tab).to have_selector("a[id='tab-link-of-pipelines-snippet'][href='admin/pipelines/snippet']", "Config XML")
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
        expect(tab).to have_selector("a[id='tab-link-of-package-repositories'][href='/admin/package_repositories/new']", "Package Repositories")
      end
    end

    it "should show package repositories tab for group admin" do
      allow(view).to receive(:is_user_a_group_admin?).and_return(true)
      allow(view).to receive(:is_user_an_admin?).and_return(false)

      render :inline => 'content', :layout => @layout_name

      Capybara.string(response.body).find("#package-repositories-tab-button.current_tab") do |tab|
        expect(tab).to have_selector("a[id='tab-link-of-package-repositories'][href='/admin/package_repositories/list']", "Package Repositories")
      end
    end
  end
end
