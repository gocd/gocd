##########################################################################
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
##########################################################################

require 'spec_helper'

describe "/navigation_elements/navigation" do
  include GoUtil

  before do
    class << view
      include ApplicationHelper
    end
    assign(:user, com.thoughtworks.go.server.domain.Username::ANONYMOUS)
    allow(view).to receive(:is_user_an_admin?).and_return(true)
  end

  partial_page = "navigation_elements/navigation"
  describe :header do
    before :each do
      allow(view).to receive(:url_for_path).and_return('url_for_path')
      allow(view).to receive(:url_for).and_return('url_for')
      allow(view).to receive(:can_view_admin_page?).and_return(true)
    end

    it 'should have the header links' do
      render :partial => partial_page

      assert_header_values = ['Pipelines', 'Environments', 'Agents', 'Admin']

      assert_header_values.each do |key|
        expect(Capybara.string(response.body)).to have_link("#{key}")
      end
    end
  end

  describe "user name and logout" do
    it "should display username and logout botton if a user is logged in" do
      assign(:user, com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new("go_user")))
      allow(view).to receive(:can_view_admin_page?).and_return(false)

      controller.request.path_parameters[:controller] = 'pipelines'
      controller.request.path_parameters[:action]     = 'index'
      render :partial => partial_page

      expect(response.body).to have_selector(".current-user a[href='#']", text: "go_user")
      expect(response.body).to have_selector(".current-user a[href='https://go.cd/help']", text: "Help")
      expect(response.body).to have_selector(".current-user a[href='/tab/mycruise/user']", text: "Preferences")
      expect(response.body).to have_selector(".current-user a[href='/auth/logout']", text: "Sign out")
    end

    it "should not display username and logout botton if anonymous user is logged in" do
      assign(:user, com.thoughtworks.go.server.domain.Username::ANONYMOUS)
      allow(view).to receive(:can_view_admin_page?).and_return(false)

      controller.request.path_parameters[:controller] = 'pipelines'
      controller.request.path_parameters[:action]     = 'index'
      render :partial => partial_page

      expect(response.body).to_not have_selector(".current-user a[href='#']", text: "go_user")
      expect(response.body).to_not have_selector(".current-user a[href='/tab/mycruise/user']", text: "Preferences")
      expect(response.body).to_not have_selector(".current-user a[href='/auth/logout']", text: "Sign out")
      expect(response.body).to have_selector(".current-user a[href='https://go.cd/help']", text: "Help")
    end
  end

  describe :admin_dropdown do
    before :each do
      allow(view).to receive(:can_view_admin_page?).and_return(true)
      @assert_values = {"Pipelines"     => pipeline_groups_path, "Templates" => templates_path, "Config XML" => config_view_path, "Server Configuration" => edit_server_config_path, "User Summary" => user_listing_path,
                        "OAuth Clients" => oauth_engine.clients_path, "OAuth Enabled Gadget Providers" => gadgets_oauth_clients_path, "Backup" => backup_server_path, "Plugins" => plugins_listing_path, "Package Repositories" => package_repositories_new_path}
    end

    it 'should show dropdown items for admin link on header' do
      render :partial => partial_page

      Capybara.string(response.body).find("ul.admin-dropdown").tap do |ul_tabs_li|
        @assert_values.each do |key, value|
          ul_tabs_li.find("li:contains('#{key}')").tap do |li|
            expect(li).to have_selector("a[href='#{value}']", text: key)
          end
        end
      end
    end

    it "should show only templates in admin dropdown if user is just template admin" do
      allow(view).to receive(:is_user_a_template_admin?).and_return(true)
      allow(view).to receive(:is_user_an_admin?).and_return(false)
      allow(view).to receive(:is_user_a_group_admin?).and_return(false)

      render :partial => partial_page

      Capybara.string(response.body).find("ul.admin-dropdown").tap do |ul_tabs_li|
        ul_tabs_li.all("li[role='presentation']").tap do |li|
          expect(li.size).to be(1)
          expect(li.first).to have_selector("a[href='#{templates_path}']", text: 'Templates')
        end
      end
    end

    it "should show only tabs relevant to group admin in admin dropdown if user is just group admin" do
      allow(view).to receive(:is_user_a_template_admin?).and_return(false)
      allow(view).to receive(:is_user_an_admin?).and_return(false)
      allow(view).to receive(:is_user_a_group_admin?).and_return(true)

      render :partial => partial_page

      assert_values_there = {"Pipelines" => pipeline_groups_path, "Config XML" => pipelines_snippet_path, "Plugins" => plugins_listing_path, "Package Repositories" => package_repositories_new_path}

      Capybara.string(response.body).find("ul.admin-dropdown").tap do |ul_tabs_li|
        expect(ul_tabs_li.all("li").size).to be(assert_values_there.length)

        assert_values_there.each do |key, value|
          ul_tabs_li.find("li:contains('#{key}')").tap do |li|
            expect(li).to have_selector("a[href='#{value}']", text: key)
          end
        end
      end
    end

    it "should show tabs relevant to group admin and template admin in admin dropdown if user is both template and group admin" do
      allow(view).to receive(:is_user_a_template_admin?).and_return(true)
      allow(view).to receive(:is_user_an_admin?).and_return(false)
      allow(view).to receive(:is_user_a_group_admin?).and_return(true)

      render :partial => partial_page

      assert_values_there = {"Pipelines" => pipeline_groups_path, "Templates" => templates_path, "Config XML" => pipelines_snippet_path, "Plugins" => plugins_listing_path, "Package Repositories" => package_repositories_new_path}

      Capybara.string(response.body).find("ul.admin-dropdown").tap do |ul_tabs_li|
        expect(ul_tabs_li.all("li").size).to be(assert_values_there.length)

        assert_values_there.each do |key, value|
          ul_tabs_li.find("li:contains('#{key}')").tap do |li|
            expect(li).to have_selector("a[href='#{value}']", text: key)
          end
        end
      end
    end

    it "should disable admin link on header in case of non-admins" do
      allow(view).to receive(:can_view_admin_page?).and_return(false)

      render :partial => partial_page

      expect(Capybara.string(response.body).find(".nav-left li span")).to have_text("Admin")
    end
  end
end
