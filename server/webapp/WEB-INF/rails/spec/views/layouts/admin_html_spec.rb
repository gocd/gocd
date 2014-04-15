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
load File.join(File.dirname(__FILE__), 'layout_html_examples.rb')

describe "/layouts/admin" do
  before do
    stub_server_health_messages
  end
  before do
    @layout_name = 'admin'
    @admin_url = "/admin/pipelines"
    assigns[:user] = @user = Object.new
    assigns[:error_count] = 0
    assigns[:warning_count] = 0
    @user.stub(:anonymous?).and_return(true)
    template.stub!(:can_view_admin_page?).and_return(true)
    template.stub!(:is_user_a_group_admin?).and_return(true)
    template.stub!(:is_user_an_admin?).and_return(true)
    class << template
      def url_for_with_stub *args
        args.empty? ? "/go/" : url_for_without_stub(*args)
      end

      alias_method_chain :url_for, :stub
    end
    template.stub(:oauth_clients_path).and_return("/path/for/oauth/clients")
    template.stub(:user_listing_path).and_return("/path/for/user/listing")
    template.stub(:backup_server_path).and_return("admin/backup")
    template.stub(:pipelines_snippet_path).and_return("admin/pipelines/snippet")
    template.stub!(:is_user_a_template_admin?).and_return(false)
  end

  it_should_behave_like "layout"

  it "should display only pipeline configurations and pipeline groups tab when the user is a group admin" do
    template.stub!(:is_user_an_admin?).and_return(false)
    template.stub!(:is_user_a_group_admin?).and_return(true)

    render :inline => '<div>content</div>', :layout => @layout_name
    response.should have_tag('.sub_tab_container') do
      with_tag("#pipeline-groups-tab-button")
      without_tag("#source-xml-tab-button")
      without_tag("#server-configuration-tab-button")
      without_tag("#user-summary-tab-button")
      without_tag("#oauth-clients-tab-button")
      without_tag("#gadget-providers-tab-button")
    end
  end

  it "should display warning message when config not valid" do
    assigns[:config_valid] = false
    render :inline => '<div>content</div>', :layout => @layout_name
    response.body.should have_tag(".flash p.warning", "Invalid config on disk. Displaying the last known valid config (Editing config through Go will overwrite the invalid copy. Edit it on disk to fix this problem).")
  end

  describe "plugins" do
    before(:each) do
      assigns[:tab_name] = "plugins-listing"
    end

    it "should show plugins listing page if plugins are enabled" do
      template.stub!(:is_plugins_enabled?).and_return(true)
      template.stub!(:plugins_listing_path).and_return("some_path_to_plugins")
      render :inline => "<div>content</div>", :layout => @layout_name
      response.body.should have_tag(".sub_tabs_container") do
        with_tag("li#plugins-listing-tab-button") do
          with_tag("a#tab-link-of-plugins-listing[href='some_path_to_plugins']", "Plugins")
        end
      end
    end

    it "should not show plugins listing page if plugins are disabled" do
      template.stub!(:is_plugins_enabled?).and_return(false)
      template.stub!(:plugins_listing_path).and_return("some_path_to_plugins")
      render :inline => "<div>content</div>", :layout => @layout_name
      response.body.should have_tag(".sub_tabs_container") do
        without_tag("li#plugins-listing-tab-button")
      end
    end

    it "should show plugins listing page if plugins are enabled and user is admin" do
      template.stub!(:is_plugins_enabled?).and_return(true)

      template.stub(:is_user_an_admin?).and_return(true)

      template.stub!(:plugins_listing_path).and_return("some_path_to_plugins")
      render :inline => "<div>content</div>", :layout => @layout_name
      response.body.should have_tag(".sub_tabs_container") do
        with_tag("li#plugins-listing-tab-button") do
          with_tag("a#tab-link-of-plugins-listing[href='some_path_to_plugins']", "Plugins")
        end
      end
    end

    it "should show plugins listing page if plugins are enabled and user is group admin" do
      template.stub!(:is_plugins_enabled?).and_return(true)

      template.stub(:is_user_an_admin?).and_return(false)
      template.stub(:is_user_a_group_admin?).and_return(true)

      template.stub!(:plugins_listing_path).and_return("some_path_to_plugins")
      render :inline => "<div>content</div>", :layout => @layout_name
      response.body.should have_tag(".sub_tabs_container") do
        with_tag("li#plugins-listing-tab-button") do
          with_tag("a#tab-link-of-plugins-listing[href='some_path_to_plugins']", "Plugins")
        end
      end
    end

    it "should not show plugins listing page if plugins are enabled and user is neither an admin nor a group admin" do
      template.stub!(:is_plugins_enabled?).and_return(true)

      template.stub(:is_user_an_admin?).and_return(false)
      template.stub(:is_user_a_group_admin?).and_return(false)

      template.stub!(:plugins_listing_path).and_return("some_path_to_plugins")
      render :inline => "<div>content</div>", :layout => @layout_name
      response.body.should have_tag(".sub_tabs_container") do
        without_tag("li#plugins-listing-tab-button")
      end
    end
  end

  describe "user-summary" do
    before(:each) do
      assigns[:tab_name] = "user-listing"
    end

    it "should show content" do
      render :inline => '<div>content</div>', :layout => @layout_name
      response.should have_tag('#tab-content-of-user-listing', 'content')
    end

    it "should show tab button" do
      render :inline => "<div>content</div>", :layout => @layout_name
      response.should have_tag("#user-summary-tab-button.current_tab a#tab-link-of-user-listing[href=/path/for/user/listing]", 'User Summary')
    end

    it "should not show oauth_clients tab button when on user-summary" do
      render :inline => "<div>content</div>", :layout => @layout_name
      response.should_not have_tag("#oauth-clients-tab-button.current_tab")
      response.should have_tag("#oauth-clients-tab-button a#tab-link-of-oauth-clients[href=/path/for/oauth/clients]", 'OAuth Clients')
    end
  end

  describe "pipeline-groups" do
    before(:each) do
      assigns[:tab_name] = "pipeline-groups"
    end

    it "should show content" do
      render :inline => '<div>content</div>', :layout => @layout_name
      response.should have_tag('#tab-content-of-pipeline-groups', 'content')
    end

    it "should show tab button" do
      render :inline => "<div>content</div>", :layout => @layout_name
      response.should have_tag("#pipeline-groups-tab-button.current_tab a#tab-link-of-pipeline-groups[href=/admin/pipelines]", 'Pipelines')
    end
  end

  describe "oauth-clients" do
    before(:each) do
      assigns[:tab_name] = "oauth-clients"
    end

    it "should show content" do
      render :inline => '<div>content</div>', :layout => @layout_name
      response.should have_tag('#tab-content-of-oauth-clients', 'content')
    end

    it "should show tab button" do
      render :inline => "<div>content</div>", :layout => @layout_name
      response.should have_tag("#oauth-clients-tab-button.current_tab a#tab-link-of-oauth-clients[href=/path/for/oauth/clients]", 'OAuth Clients')
    end

    it "should not highlight user_listing tab button when on oauth clients tab" do
      render :inline => "<div>content</div>", :layout => @layout_name
      response.should_not have_tag("#user-summary-tab-button.current_tab")
      response.should have_tag("#user-summary-tab-button a#tab-link-of-user-listing[href=/path/for/user/listing]", 'User Summary')
    end
  end

  describe "templates" do
    before(:each) do
      assigns[:tab_name] = "templates"
    end

    it "should show content" do
      render :inline => '<div>content</div>', :layout => @layout_name

      response.body.should have_tag('#tab-content-of-templates', 'content')
    end

    it "should show tab button for super admins" do
      render :inline => "<div>content</div>", :layout => @layout_name
      response.should have_tag("#templates-tab-button.current_tab a#tab-link-of-templates[href=/admin/templates]", 'Templates')
    end

    it "should not be visible for group admins" do
      template.stub!(:is_user_a_group_admin?).and_return(true)
      template.stub!(:is_user_an_admin?).and_return(false)

      render :inline => "<div>content</div>", :layout => @layout_name
      response.should_not have_tag("#templates-tab-button.current_tab a#tab-link-of-templates[href=/admin/templates]", 'Templates')
    end

    it "should be visible for template admins" do
      template.stub!(:is_user_a_template_admin?).and_return(true)
      template.stub!(:is_user_an_admin?).and_return(false)

      render :inline => "<div>content</div>", :layout => @layout_name
      response.should have_tag("#templates-tab-button.current_tab a#tab-link-of-templates[href=/admin/templates]", 'Templates')
    end

  end

  describe "server-backup" do

    it "should show backup tab" do
      render :inline => "<div>content</div>", :layout => @layout_name
      response.should have_tag(".sub_tabs_container ul") do
        with_tag("#backup-tab-button") do
          with_tag("a#tab-link-of-backup[href='admin/backup']", "Backup")
        end
      end
    end

    it "should show the backup tab as current if on that tab" do
      assigns[:tab_name] = "backup"

      render :inline => "<div>content</div>", :layout => @layout_name

      response.should have_tag(".sub_tabs_container ul") do
        with_tag("#backup-tab-button.current_tab")
      end
    end

    it "should show the contents for the backup tab" do
      assigns[:tab_name] = "backup"

      render :inline => "<div>content</div>", :layout => @layout_name

      response.should have_tag(".sub_tab_container_content #tab-content-of-backup")
    end
  end

  describe "xml" do
    before(:each) do
      assigns[:tab_name] = "pipelines-snippet"
    end

    it "should show xml tab for group admin" do
      template.stub!(:is_user_a_group_admin?).and_return(true)
      template.stub!(:is_user_an_admin?).and_return(false)

      render :inline => 'content', :layout => @layout_name

      response.body.should have_tag('#tab-content-of-pipelines-snippet.current_tab') do
        with_tag("a[id='tab-link-of-pipelines-snippet'][href='admin/pipelines/snippet']", "Config XML")
      end
    end

    it "should not show xml tab for go administrators" do
      template.stub!(:is_user_a_group_admin?).and_return(true)
      template.stub!(:is_user_an_admin?).and_return(true)

      render :inline => 'content', :layout => @layout_name
      response.body.should_not have_tag('li#tab-content-of-pipelines-snippet')
    end

    it "should not show source xml tab for group admin" do
      template.stub!(:is_user_a_group_admin?).and_return(true)
      template.stub!(:is_user_an_admin?).and_return(false)

      render :inline => 'content', :layout => @layout_name

      response.body.should_not have_tag('#source-xml-tab-button')
    end
  end

  describe "package-repositories" do
    before(:each) do
      assigns[:tab_name] = "package-repositories"
    end

    it "should show package repositories tab for super admin" do
      template.stub!(:is_user_an_admin?).and_return(true)
      template.stub!(:is_user_a_group_admin?).and_return(false)

      render :inline => 'content', :layout => @layout_name

      response.body.should have_tag('#package-repositories-tab-button.current_tab') do
        with_tag("a[id='tab-link-of-package-repositories'][href='/admin/package_repositories/new']", "Package Repositories")
      end
    end

    it "should show package repositories tab for group admin" do
      template.stub!(:is_user_a_group_admin?).and_return(true)
      template.stub!(:is_user_an_admin?).and_return(false)

      render :inline => 'content', :layout => @layout_name

      response.body.should have_tag('#package-repositories-tab-button.current_tab') do
        with_tag("a[id='tab-link-of-package-repositories'][href='/admin/package_repositories/list']", "Package Repositories")
      end
    end
  end
end
