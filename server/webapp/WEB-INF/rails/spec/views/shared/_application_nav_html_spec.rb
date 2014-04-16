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

require File.expand_path(File.dirname(__FILE__) + '/../../spec_helper')

describe "/shared/_application_nav.html.erb" do
  include GoUtil

  before do
    class << template
      include ApplicationHelper
    end
    stub_server_health_messages
    assigns[:user] = com.thoughtworks.go.server.domain.Username::ANONYMOUS
    template.stub!(:is_user_an_admin?).and_return(true)
  end

  describe :header do
    before :each do
      template.stub!(:url_for_path).and_return('url_for_path')
      template.stub!(:url_for).and_return('url_for')
      template.stub!(:can_view_admin_page?).and_return(true)
    end

    it 'should have the header links' do
      render :partial => "shared/application_nav.html.erb"

      assert_header_values = {'recent-activity' => 'PIPELINES', 'environments' => 'ENVIRONMENTS', 'agents' => 'AGENTS', 'admin' => 'ADMIN'}

      response.body.should have_tag("ul.tabs") do
        assert_header_values.each do |key, value|
          with_tag("li#cruise-header-tab-#{key}") do
            with_tag("a", value)
          end
        end
      end
    end
  end

  describe "user name and logout" do
    it "should display username and logout botton if a user is logged in" do
      assigns[:user] = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new("maulik suchak"))
      template.stub!(:can_view_admin_page?).and_return(false)

      render :partial => "shared/application_nav.html.erb", :path_parameters => {:controller => 'pipelines', :action => 'index'}, :locals => {:scope => {:admin_tab_url => "foo/admin"}}
      response.should have_tag(".current_user a[href='#']", "maulik suchak")
      response.should have_tag(".current_user a[href='/tab/mycruise/user']", "Preferences")
      response.should have_tag(".current_user a[href='/auth/logout']", "Sign out")
      response.should have_tag(".user .help a[href='/help/index.html']", "Help")
    end

    it "should not display username and logout botton if anonymous user is logged in" do
      assigns[:user] = com.thoughtworks.go.server.domain.Username::ANONYMOUS
      template.stub!(:can_view_admin_page?).and_return(false)

      render :partial => "shared/application_nav.html.erb", :path_parameters => {:controller => 'pipelines', :action => 'index'}, :locals => {:scope => {:admin_tab_url => "foo/admin"}}

      response.should_not have_tag(".current_user a[href='#']", "maulik suchak")
      response.should_not have_tag(".current_user a[href='/tab/mycruise/user']", "Preferences")
      response.should_not have_tag(".current_user a[href='/auth/logout']", "Sign out")
      response.should have_tag(".user .help a[href='/help/index.html']", "Help")
    end
  end
  describe "server health messages" do

    it "should render header with pipelines tab selected as current" do
      template.stub!(:can_view_admin_page?).and_return(false)

      render :partial => "shared/application_nav.html.erb", :path_parameters => {:controller => 'pipelines', :action => 'index'}, :locals => {:scope => {:admin_tab_url => "foo/admin"}}

      response.should have_tag("#cruise-header-tab-recent-activity.current")
    end
    
    #it "should mark admin tab as hilighted when current_tab override used" do
    #  pending("#7862 - to be fixed by Maulik later - Sharanya/Sachin")
    #  assigns[:current_tab_name] = "admin"
    #  assigns[:user] = com.thoughtworks.go.server.domain.Username::ANONYMOUS
    #  template.stub!(:can_view_admin_page?).and_return(true)
    #
    #  render :partial => "shared/application_nav.html.erb", :path_parameters => {:controller => 'pipelines', :action => 'index'}, :locals => {:scope => {:admin_tab_url => "foo/admin"}}
    #
    #  response.should have_tag("#cruise-header-tab-admin.current a[href=/admin/pipelines]")
    #end

    it "should not mark admin tab as hilighted when not overridden" do
      assigns[:current_tab_name] = nil
      assigns[:user] = com.thoughtworks.go.server.domain.Username::ANONYMOUS
      template.stub!(:can_view_admin_page?).and_return(true)

      render :partial => "shared/application_nav.html.erb", :path_parameters => {:controller => 'environments', :action => 'index'}, :locals => {:scope => {:admin_tab_url => "/foo/admin/pipelines"}}

      response.should_not have_tag("#cruise-header-tab-admin.current")
    end

    it "should render header with pipelines not selected as current when visiting environment page" do
      assigns[:user] = com.thoughtworks.go.server.domain.Username::ANONYMOUS
      template.stub!(:can_view_admin_page?).and_return(false)

      render :partial => "shared/application_nav.html.erb", :path_parameters => {:controller => 'environments', :action => 'index'}, :locals => {:scope => {:admin_tab_url => "foo/admin"}}

      response.should_not have_tag("#cruise-header-tab-recent-activity.current")
    end

    it "should hookup auto refresh of server health messages" do
      assigns[:user] = com.thoughtworks.go.server.domain.Username::ANONYMOUS
      template.stub!(:can_view_admin_page?).and_return(false)
      render :partial => "shared/application_nav.html.erb", :path_parameters => {:controller => 'agents', :action => 'index'}, :locals => {:scope => {:admin_tab_url => "foo/admin"}}
      response.should have_tag("script[type='text/javascript']", /Util.on_load\(function\(\) {new AjaxRefresher\('\/server\/messages.json', null\);}\);/)
    end

    it "should hookup auto refresh with update once when auto refresh is false" do
      assigns[:user] = com.thoughtworks.go.server.domain.Username::ANONYMOUS
      params[:autoRefresh] = 'false'
      template.stub!(:can_view_admin_page?).and_return(false)
      render :partial => "shared/application_nav.html.erb", :path_parameters => {:controller => 'agents', :action => 'index'}, :locals => {:scope => {:admin_tab_url => "foo/admin"}}
      response.should_not have_tag("script[type='text/javascript']")
    end
  end

  describe "licese expiry warning" do

    before :each do
      template.stub!(:can_view_admin_page?).and_return(false)
    end

    it "should not render the popup to show that the license is about to expire when the request attribute is not set" do
      render :partial => "shared/application_nav.html.erb", :path_parameters => {:controller => 'agents', :action => 'index'}, :locals => {:scope => {:admin_tab_url => "foo/admin"}}

      response.body.should_not have_tag("div.license_about_to_expire")
    end

    it "should render the popup to show that the license is about to expire" do
      session["LICENSE_EXPIRING_IN"] = 6
      render :partial => "shared/application_nav.html.erb", :path_parameters => {:controller => 'agents', :action => 'index'}, :locals => {:scope => {:admin_tab_url => "foo/admin"}}

      response.body.should have_tag("div.license_about_to_expire") do
        with_tag(".title", "Your Go license key will expire in 6 days.")
        with_tag(".suggestion", "Please contact your ThoughtWorks Studios account executive or email studios@thoughtworks.com to obtain a new license")
        with_tag("button", "Remind Me Later")
        with_tag("div[style='display:none']") do
          with_tag("form[method='post'][action='#{dismiss_license_expiry_warning_path}']") do
            with_tag("button", "DO NOT SHOW ME AGAIN")
          end
        end
      end
      response.body.should have_tag("script", /title: 'Go license expiry'/)
      session['LICENSE_EXPIRING_IN'].nil?.should be_true
    end
  end

  describe :admin_dropdown do
    before :each do
      template.stub!(:can_view_admin_page?).and_return(true)
      template.stub!(:tab_with_display_name).and_return('some_random_text')

      template.stub!(:pipeline_groups_path).and_return('pipeline_groups_path')
      template.stub!(:templates_path).and_return('templates_path')
      template.stub!(:config_view_path).and_return('config_view_path')
      template.stub!(:pipelines_snippet_path).and_return('pipelines_snippet_path')
      template.stub!(:edit_server_config_path).and_return('edit_server_config_path')
      template.stub!(:user_listing_path).and_return('user_listing_path')
      template.stub!(:oauth_clients_path).and_return('oauth_clients_path')
      template.stub!(:gadgets_oauth_clients_path).and_return('gadgets_oauth_clients_path')
      template.stub!(:backup_server_path).and_return('backup_server_path')
      template.stub!(:plugins_listing_path).and_return('plugins_listing_path')
      template.stub!(:package_repositories_new_path).and_return('package_repositories_new_path')
    end

    it 'should show dropdown items for admin link on header' do
      render :partial => "shared/application_nav.html.erb"

      assert_values = {"Pipelines" => "pipeline_groups_path", "Templates" => "templates_path", "Config XML" => "config_view_path", "Server Configuration" => "edit_server_config_path", "User Summary" => "user_listing_path",
                       "OAuth Clients" => "oauth_clients_path", "OAuth Enabled Gadget Providers" => "gadgets_oauth_clients_path", "Backup" => "backup_server_path", "Plugins" => "plugins_listing_path", "Package Repositories" => "package_repositories_new_path"}

      response.body.should have_tag("ul.tabs") do
        with_tag("li#cruise-header-tab-admin") do
          with_tag("a[data-toggle='dropdown']", "ADMIN")
          with_tag("ul.dropdown-menu[role='menu']") do
            assert_values.each do |key, value|
              with_tag("li[role='presentation']") do
                with_tag("a[href='#{value}']", key)
              end
            end
          end
        end
      end
    end

    it "should show only templates in admin dropdown if user is just template admin" do
      template.stub!(:is_user_a_template_admin?).and_return(true)
      template.stub!(:is_user_an_admin?).and_return(false)
      template.stub!(:is_user_a_group_admin?).and_return(false)

      render :partial => "shared/application_nav.html.erb"

      assert_values = {"Pipelines" => "pipeline_groups_path", "Config XML" => "config_view_path", "Server Configuration" => "edit_server_config_path", "User Summary" => "user_listing_path",
                       "OAuth Clients" => "oauth_clients_path", "OAuth Enabled Gadget Providers" => "gadgets_oauth_clients_path", "Backup" => "backup_server_path", "Plugins" => "plugins_listing_path", "Package Repositories" => "package_repositories_new_path"}

      response.body.should have_tag("ul.tabs") do
        with_tag("li#cruise-header-tab-admin") do
          with_tag("a[data-toggle='dropdown']", "ADMIN")
          with_tag("ul.dropdown-menu[role='menu']") do
            assert_values.each do |key, value|
              without_tag("a[href='#{value}']", key)
            end
            with_tag("li[role='presentation']") do
              with_tag("a[href='templates_path']", "Templates")
            end
          end
        end
      end
    end

    it "should show only tabs relevent to group admin in admin dropdown if user is just group admin" do
      template.stub!(:is_user_a_template_admin?).and_return(false)
      template.stub!(:is_user_an_admin?).and_return(false)
      template.stub!(:is_user_a_group_admin?).and_return(true)

      render :partial => "shared/application_nav.html.erb"

      assert_values_not_there = {"Templates" => "templates_path", "Server Configuration" => "edit_server_config_path", "User Summary" => "user_listing_path",
                       "OAuth Clients" => "oauth_clients_path", "OAuth Enabled Gadget Providers" => "gadgets_oauth_clients_path", "Backup" => "backup_server_path"}

      assert_values_there = {"Pipelines" => "pipeline_groups_path", "Config XML" => "pipelines_snippet_path", "Plugins" => "plugins_listing_path", "Package Repositories" => "package_repositories_new_path"}

      response.body.should have_tag("ul.tabs") do
        with_tag("li#cruise-header-tab-admin") do
          with_tag("a[data-toggle='dropdown']", "ADMIN")
          with_tag("ul.dropdown-menu[role='menu']") do
            assert_values_not_there.each do |key, value|
              without_tag("a[href='#{value}']", key)
            end
            assert_values_there.each do |key, value|
              with_tag("li[role='presentation']") do
                with_tag("a[href='#{value}']", key)
              end
            end
          end
        end
      end
    end

    it "should show tabs relevent to group admin in admin dropdown if user is both template and group admin" do
      template.stub!(:is_user_a_template_admin?).and_return(true)
      template.stub!(:is_user_an_admin?).and_return(false)
      template.stub!(:is_user_a_group_admin?).and_return(true)

      render :partial => "shared/application_nav.html.erb"

      assert_values_not_there = {"Server Configuration" => "edit_server_config_path", "User Summary" => "user_listing_path",
                       "OAuth Clients" => "oauth_clients_path", "OAuth Enabled Gadget Providers" => "gadgets_oauth_clients_path", "Backup" => "backup_server_path"}

      assert_values_there = {"Pipelines" => "pipeline_groups_path", "Templates" => "templates_path", "Config XML" => "pipelines_snippet_path", "Plugins" => "plugins_listing_path", "Package Repositories" => "package_repositories_new_path"}

      response.body.should have_tag("ul.tabs") do
        with_tag("li#cruise-header-tab-admin") do
          with_tag("a[data-toggle='dropdown']", "ADMIN")
          with_tag("ul.dropdown-menu[role='menu']") do
            assert_values_not_there.each do |key, value|
              without_tag("a[href='#{value}']", key)
            end
            assert_values_there.each do |key, value|
              with_tag("li[role='presentation']") do
                with_tag("a[href='#{value}']", key)
              end
            end
          end
        end
      end
    end

    it "should disable admin link on header in case of non-admins" do
      template.stub!(:can_view_admin_page?).and_return(false)

      render :partial => "shared/application_nav.html.erb"

      assert_values_not_there = {"Pipelines" => "pipeline_groups_path", "Templates" => "templates_path", "Config XML" => "config_view_path", "Server Configuration" => "edit_server_config_path", "User Summary" => "user_listing_path",
                                 "OAuth Clients" => "oauth_clients_path", "OAuth Enabled Gadget Providers" => "gadgets_oauth_clients_path", "Backup" => "backup_server_path",  "Plugins" => "plugins_listing_path", "Package Repositories" => "package_repositories_new_path",}

      response.body.should have_tag("ul.tabs") do
        with_tag("li#cruise-header-tab-admin") do
          with_tag("span", "ADMIN")
          without_tag("a[data-toggle='dropdown']", "ADMIN")
          without_tag("ul.dropdown-menu[role='menu']") do
            assert_values_not_there.each do |key, value|
              without_tag("a[href='#{value}']", key)
            end
          end
        end
      end
    end

    it "should add current css class to Admin menu when current tab is admin" do
      assigns[:current_tab_name] = "admin"
      render :partial => "shared/application_nav.html.erb", :path_parameters => {:controller => 'pipelines', :action => 'index'}, :locals => {:scope => {:admin_tab_url => "foo/admin"}}
      response.body.should have_tag("li[id='cruise-header-tab-admin'][class='current']")
    end
  end
end
