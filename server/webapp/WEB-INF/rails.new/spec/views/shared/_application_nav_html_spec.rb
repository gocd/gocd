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

describe "/shared/_application_nav.html.erb" do
  include GoUtil

  before do
    class << view
      include ApplicationHelper
    end
    stub_server_health_messages
    assign(:user, com.thoughtworks.go.server.domain.Username::ANONYMOUS)
    allow(view).to receive(:is_user_an_admin?).and_return(true)
  end

  describe :header do
    before :each do
      allow(view).to receive(:url_for_path).and_return('url_for_path')
      allow(view).to receive(:url_for).and_return('url_for')
      allow(view).to receive(:can_view_admin_page?).and_return(true)
    end

    it 'should have the header links' do
      render :partial => "shared/application_nav.html.erb"

      assert_header_values = {'pipelines' => 'PIPELINES', 'environments' => 'ENVIRONMENTS', 'agents' => 'AGENTS', 'admin' => 'ADMIN'}

      Capybara.string(response.body).find('ul.tabs').tap do |ul_tabs|
        assert_header_values.each do |key, value|
          ul_tabs.find("li#cruise-header-tab-#{key}") do |ul_tabs_li|
            expect(ul_tabs_li).to have_selector("a", value)
          end
        end
      end
    end
  end

  describe "user name and logout" do
    it "should display username and logout botton if a user is logged in" do
      assign(:user, com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new("maulik suchak")))
      allow(view).to receive(:can_view_admin_page?).and_return(false)

      controller.request.path_parameters[:controller] = 'pipelines'
      controller.request.path_parameters[:action] = 'index'
      render :partial => "shared/application_nav.html.erb", :locals => {:scope => {:admin_tab_url => "foo/admin"}}
      expect(response.body).to have_selector(".current_user a[href='#']", "maulik suchak")
      expect(response.body).to have_selector(".current_user a[href='/tab/mycruise/user']", "Preferences")
      expect(response.body).to have_selector(".current_user a[href='/auth/logout']", "Sign out")
      expect(response.body).to have_selector(".user .help a[href='http://www.go.cd/documentation/user/current']", "Help")
    end

    it "should not display username and logout botton if anonymous user is logged in" do
      assign(:user, com.thoughtworks.go.server.domain.Username::ANONYMOUS)
      allow(view).to receive(:can_view_admin_page?).and_return(false)

      controller.request.path_parameters[:controller] = 'pipelines'
      controller.request.path_parameters[:action] = 'index'
      render :partial => "shared/application_nav.html.erb", :locals => {:scope => {:admin_tab_url => "foo/admin"}}

      expect(response.body).to_not have_selector(".current_user a[href='#']", "maulik suchak")
      expect(response.body).to_not have_selector(".current_user a[href='/tab/mycruise/user']", "Preferences")
      expect(response.body).to_not have_selector(".current_user a[href='/auth/logout']", "Sign out")
      expect(response.body).to have_selector(".user .help a[href='http://www.go.cd/documentation/user/current']", "Help")
    end
  end

  describe "server health messages" do

    it "should render header with pipelines tab selected as current" do
      allow(view).to receive(:can_view_admin_page?).and_return(false)

      controller.request.path_parameters[:controller] = 'pipelines'
      controller.request.path_parameters[:action] = 'index'
      assign(:current_tab_name, 'pipelines')
      render :partial => "shared/application_nav.html.erb", :locals => {:scope => {:admin_tab_url => "foo/admin"}}

      expect(response.body).to have_selector("#cruise-header-tab-pipelines.current")
    end

    it "should mark admin tab as hilighted when current_tab override used" do
      assign(:current_tab_name, "admin")
      assign(:user, com.thoughtworks.go.server.domain.Username::ANONYMOUS)
      allow(view).to receive(:can_view_admin_page?).and_return(true)
      controller.request.path_parameters[:controller] = 'pipelines'
      controller.request.path_parameters[:action] = 'index'
      render :partial => "shared/application_nav.html.erb", :locals => {:scope => {:admin_tab_url => "foo/admin"}}

      expect(response.body).to have_selector("#cruise-header-tab-admin.current a[href='/admin/pipelines']")
    end

    it "should not mark admin tab as hilighted when not overridden" do
      assign(:current_tab_name, nil)
      assign(:user, com.thoughtworks.go.server.domain.Username::ANONYMOUS)
      allow(view).to receive(:can_view_admin_page?).and_return(true)

      controller.request.path_parameters[:controller] = 'environments'
      controller.request.path_parameters[:action] = 'index'
      render :partial => "shared/application_nav.html.erb", :locals => {:scope => {:admin_tab_url => "/foo/admin/pipelines"}}

      expect(response.body).to_not have_selector("#cruise-header-tab-admin.current")
    end

    it "should render header with pipelines not selected as current when visiting environment page" do
      assign(:user, com.thoughtworks.go.server.domain.Username::ANONYMOUS)
      allow(view).to receive(:can_view_admin_page?).and_return(false)

      controller.request.path_parameters[:controller] = 'environments'
      controller.request.path_parameters[:action] = 'index'
      render :partial => "shared/application_nav.html.erb", :locals => {:scope => {:admin_tab_url => "foo/admin"}}

      expect(response.body).to_not have_selector("#cruise-header-tab-recent-activity.current")
    end

    it "should hookup auto refresh of server health messages" do
      assign(:user, com.thoughtworks.go.server.domain.Username::ANONYMOUS)
      allow(view).to receive(:can_view_admin_page?).and_return(false)
      controller.request.path_parameters[:controller] = 'agents'
      controller.request.path_parameters[:action] = 'index'
      render :partial => "shared/application_nav.html.erb", :locals => {:scope => {:admin_tab_url => "foo/admin"}}
      expect(response.body).to have_selector("script", visible: false, text: /Util.on_load\(function\(\) {new AjaxRefresher\('\/server\/messages.json', null\);}\);/)
    end

    it "should hookup auto refresh with update once when auto refresh is false" do
      assign(:user, com.thoughtworks.go.server.domain.Username::ANONYMOUS)
      params[:autoRefresh] = 'false'
      allow(view).to receive(:can_view_admin_page?).and_return(false)
      controller.request.path_parameters[:controller] = 'agents'
      controller.request.path_parameters[:action] = 'index'
      render :partial => "shared/application_nav.html.erb", :locals => {:scope => {:admin_tab_url => "foo/admin"}}
      expect(response.body).to_not have_selector("script[type='text/javascript']")
    end
  end

  describe :admin_dropdown do
    before :each do
      allow(view).to receive(:can_view_admin_page?).and_return(true)
      allow(view).to receive(:tab_with_display_name).and_return('some_random_text')

      allow(view).to receive(:pipeline_groups_path).and_return('pipeline_groups_path')
      allow(view).to receive(:templates_path).and_return('templates_path')
      allow(view).to receive(:config_view_path).and_return('config_view_path')
      allow(view).to receive(:pipelines_snippet_path).and_return('pipelines_snippet_path')
      allow(view).to receive(:edit_server_config_path).and_return('edit_server_config_path')
      allow(view).to receive(:user_listing_path).and_return('user_listing_path')
      allow(view).to receive(:oauth_clients_path).and_return('oauth_clients_path')
      allow(view).to receive(:gadgets_oauth_clients_path).and_return('gadgets_oauth_clients_path')
      allow(view).to receive(:backup_server_path).and_return('backup_server_path')
      allow(view).to receive(:plugins_listing_path).and_return('plugins_listing_path')
      allow(view).to receive(:package_repositories_new_path).and_return('package_repositories_new_path')

    end

    it 'should show dropdown items for admin link on header' do
      render :partial => "shared/application_nav.html.erb"

      assert_values = {"Pipelines" => "pipeline_groups_path", "Templates" => "templates_path", "Config XML" => "config_view_path", "Server Configuration" => "edit_server_config_path", "User Summary" => "user_listing_path",
                       "OAuth Clients" => "oauth_clients_path", "OAuth Enabled Gadget Providers" => "gadgets_oauth_clients_path", "Backup" => "backup_server_path", "Plugins" => "plugins_listing_path", "Package Repositories" => "package_repositories_new_path"}

      Capybara.string(response.body).find('ul.tabs').tap do |ul_tabs|
        ul_tabs.find("li#cruise-header-tab-admin") do |ul_tabs_li|
          expect(ul_tabs_li).to have_selector("a[data-toggle='dropdown']", "ADMIN")

          ul_tabs_li.find("ul.dropdown-menu[role='menu']") do |ul_dropdown|
            assert_values.each do |key, value|
              ul_dropdown.find("li[role='presentation']") do |ul_dropdown_li|
                expect(ul_dropdown_li).to have_selector("a[href='#{value}']", key)
              end
            end
          end
        end
      end
    end

    it "should show only templates in admin dropdown if user is just template admin" do
      allow(view).to receive(:is_user_a_view_admin?).and_return(true)
      allow(view).to receive(:is_user_an_admin?).and_return(false)
      allow(view).to receive(:is_user_a_group_admin?).and_return(false)

      render :partial => "shared/application_nav.html.erb"

      assert_values = {"Pipelines" => "pipeline_groups_path", "Config XML" => "config_view_path", "Server Configuration" => "edit_server_config_path", "User Summary" => "user_listing_path",
                       "OAuth Clients" => "oauth_clients_path", "OAuth Enabled Gadget Providers" => "gadgets_oauth_clients_path", "Backup" => "backup_server_path", "Plugins" => "plugins_listing_path", "Package Repositories" => "package_repositories_new_path"}

      Capybara.string(response.body).find('ul.tabs').tap do |ul_tabs|
        ul_tabs.find("li#cruise-header-tab-admin") do |ul_tabs_li|
          expect(ul_tabs_li).to have_selector("a[data-toggle='dropdown']", "ADMIN")

          ul_tabs_li.find("ul.dropdown-menu[role='menu']") do |ul_dropdown|
            assert_values.each do |key, value|
              expect(ul_dropdown).to_not have_selector("a[href='#{value}']", key)
            end
            ul_dropdown.find("li[role='presentation']") do |ul_dropdown_li|
              expect(ul_dropdown_li).to have_selector("a[href='templates_path']", "Templates")
            end
          end
        end
      end
    end

    it "should show only tabs relevent to group admin in admin dropdown if user is just group admin" do
      allow(view).to receive(:is_user_a_template_admin?).and_return(false)
      allow(view).to receive(:is_user_an_admin?).and_return(false)
      allow(view).to receive(:is_user_a_group_admin?).and_return(true)

      render :partial => "shared/application_nav.html.erb"

      assert_values_not_there = {"Templates" => "templates_path", "Server Configuration" => "edit_server_config_path", "User Summary" => "user_listing_path",
                       "OAuth Clients" => "oauth_clients_path", "OAuth Enabled Gadget Providers" => "gadgets_oauth_clients_path", "Backup" => "backup_server_path"}

      assert_values_there = {"Pipelines" => "pipeline_groups_path", "Config XML" => "pipelines_snippet_path", "Plugins" => "plugins_listing_path", "Package Repositories" => "package_repositories_new_path"}

      Capybara.string(response.body).find('ul.tabs').tap do |ul_tabs|
        ul_tabs.find("li#cruise-header-tab-admin") do |ul_tabs_li|
          expect(ul_tabs_li).to have_selector("a[data-toggle='dropdown']", "ADMIN")

          ul_tabs_li.find("ul.dropdown-menu[role='menu']") do |ul_dropdown|
            assert_values_not_there.each do |key, value|
              expect(ul_dropdown).to_not have_selector("a[href='#{value}']", key)
            end
            assert_values_there.each do |key, value|
              ul_dropdown.find("li[role='presentation']") do |ul_dropdown_li|
                expect(ul_dropdown_li).to have_selector("a[href='#{value}']", key)
              end
            end
          end
        end
      end
    end

    it "should show tabs relevent to group admin in admin dropdown if user is both template and group admin" do
      allow(view).to receive(:is_user_a_template_admin?).and_return(true)
      allow(view).to receive(:is_user_an_admin?).and_return(false)
      allow(view).to receive(:is_user_a_group_admin?).and_return(true)

      render :partial => "shared/application_nav.html.erb"

      assert_values_not_there = {"Server Configuration" => "edit_server_config_path", "User Summary" => "user_listing_path",
                       "OAuth Clients" => "oauth_clients_path", "OAuth Enabled Gadget Providers" => "gadgets_oauth_clients_path", "Backup" => "backup_server_path"}

      assert_values_there = {"Pipelines" => "pipeline_groups_path", "Templates" => "templates_path", "Config XML" => "pipelines_snippet_path", "Plugins" => "plugins_listing_path", "Package Repositories" => "package_repositories_new_path"}

      Capybara.string(response.body).find('ul.tabs').tap do |ul_tabs|
        ul_tabs.find("li#cruise-header-tab-admin") do |ul_tabs_li|
          expect(ul_tabs_li).to have_selector("a[data-toggle='dropdown']", "ADMIN")

          ul_tabs_li.find("ul.dropdown-menu[role='menu']") do |ul_dropdown|
            assert_values_not_there.each do |key, value|
              have_selector("a[href='#{value}']", key)
            end
            assert_values_there.each do |key, value|
              ul_dropdown.find("li[role='presentation']") do |ul_dropdown_li|
                expect(ul_dropdown_li).to have_selector("a[href='#{value}']", key)
              end
            end
          end
        end
      end
    end

    it "should disable admin link on header in case of non-admins" do
      allow(view).to receive(:can_view_admin_page?).and_return(false)

      render :partial => "shared/application_nav.html.erb"

      assert_values_not_there = {"Pipelines" => "pipeline_groups_path", "Templates" => "templates_path", "Config XML" => "config_view_path", "Server Configuration" => "edit_server_config_path", "User Summary" => "user_listing_path",
                                 "OAuth Clients" => "oauth_clients_path", "OAuth Enabled Gadget Providers" => "gadgets_oauth_clients_path", "Backup" => "backup_server_path",  "Plugins" => "plugins_listing_path", "Package Repositories" => "package_repositories_new_path",}

      Capybara.string(response.body).find('ul.tabs').tap do |ul_tabs|
        ul_tabs.find("li#cruise-header-tab-admin") do |ul_tabs_li|
          expect(ul_tabs_li).to have_selector("span", "ADMIN")
          expect(ul_tabs_li).to_not have_selector("a[data-toggle='dropdown']", "ADMIN")

          expect(ul_tabs_li).to_not have_selector("ul.dropdown-menu[role='menu']")

          assert_values_not_there.each do |key, value|
            expect(ul_tabs_li).to_not have_selector("a[href='#{value}']", key)
          end
        end
      end
    end

    it "should add current css class to Admin menu when current tab is admin" do
      assign(:current_tab_name, "admin")
      controller.request.path_parameters[:controller] = 'pipelines'
      controller.request.path_parameters[:action] = 'index'
      render :partial => "shared/application_nav.html.erb", :locals => {:scope => {:admin_tab_url => "foo/admin"}}
      expect(response.body).to have_selector("li[id='cruise-header-tab-admin'][class='current']")
    end
  end
end
