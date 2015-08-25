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

describe "admin/server/index.html.erb" do

  before(:each) do
    assign(:server_configuration_form, ServerConfigurationForm.new({:artifactsDir => "some_dir", :purgeArtifacts => "Size", :purgeStart => 10.5, :purgeUpto => 20.3, :siteUrl => "abc", :secureSiteUrl => "def", :commandRepositoryLocation => "default"}))
    view.stub(:cruise_config_md5).and_return('foo_bar_baz')
    view.stub(:l).and_return(localizer = Class.new do
      def method_missing method, *args
        com.thoughtworks.go.i18n.LocalizedMessage.string(args[0], args[1..-1].to_java(java.lang.Object)).localize(Spring.bean("localizer"))
      end
    end.new)
  end

  describe "artifacts management" do
    it "should show artifacts section" do
      render

      Capybara.string(response.body).find('div#pipeline_management').tap do |div|
        expect(div).to have_selector("h2.legend", :text => "Pipeline Management")

        div.find(".fieldset").tap do |fieldset|
          fieldset.all(".form_item").tap do |form_items|
            expect(form_items[0]).to have_selector("label", :text => "Artifacts Directory Location")
            expect(form_items[0]).to have_selector("input[type='text'][value='some_dir']")
          end
          fieldset.all(".form_item").tap do |form_items|
            expect(form_items[1]).to have_selector("label", :text => "Auto delete old artifacts:")
            form_items[1].all(".checkbox_row") do |checkbox_rows|
              expect(checkbox_rows[0]).to have_selector("input[type='radio'][name='server_configuration_form[purgeArtifacts]'][value='Never'][id='never_delete_artifacts']")
              expect(checkbox_rows[0]).to have_selector("label[for='never_delete_artifacts']", :text => "Never")
            end
            form_items[1].all(".checkbox_row") do |checkbox_rows|
              expect(checkbox_rows[1]).to have_selector("input[type='radio'][id='select_artifact_purge_size'][name='server_configuration_form[purgeArtifacts]'][value='Size'][checked='checked']")
              expect(checkbox_rows[1]).to have_selector("span.checkbox_label", :text => /^When available disk space is less than/)
              expect(checkbox_rows[1]).to have_selector("input[name='server_configuration_form[purgeStart]'][value='10.5']")
              expect(checkbox_rows[1]).to have_selector("input[name='server_configuration_form[purgeUpto]'][value='20.3']")
            end
          end
        end
      end
    end
  end

  describe "pipeline management" do
    it "should show hung job timeout options" do
      render

      Capybara.string(response.body).find('div#pipeline_management').tap do |div|
        expect(div).to have_selector("h2.legend", "Pipeline Management")

        div.find("#hung_job_timeout").tap do |hung_job|
          expect(hung_job).to have_selector("label", :text => "Default Job Timeout")
          expect(hung_job).to have_selector("input[type='radio'][name='server_configuration_form[timeoutType]'][value='overrideTimeout']")
          expect(hung_job).to have_selector("label", :text => "Cancel after")
          expect(hung_job).to have_selector("input[type='text'][name='server_configuration_form[jobTimeout]']")
          expect(hung_job).to have_selector("label", :text => "minute(s) of inactivity")
          expect(hung_job).to have_selector("label", :text => "Never timeout")
          expect(hung_job).to have_selector("input[type='radio'][name='server_configuration_form[timeoutType]'][value='neverTimeout']")
        end
      end
    end
  end

  describe "command repository management" do
    it "should show lookup command repository location field" do
      assign(:command_repository_base_dir_location, "/home/cruise/db/command_repository")

      render

      Capybara.string(response.body).find('div#command_repository_management').tap do |div|
        div.find("#lookup-command-repo-location").tap do |command_repo|
          expect(command_repo).to have_selector("label", :text => "Location")
          expect(command_repo).to have_selector("input[type='text'][name='server_configuration_form[commandRepositoryLocation]'][value='default']")
          expect(command_repo).to have_selector("label", :text => "/home/cruise/db/command_repository")
        end
      end
    end

    it "should show command repository cache reload button" do
      assign(:command_repository_base_dir_location, "/home/cruise/db/command_repository")

      render

      Capybara.string(response.body).find('div#command_repository_management').tap do |div|
        div.find("#lookup-command-reload").tap do |command_reload|
          expect(command_reload).to have_selector("button#reloadCommandRepoCache", :text => "Reload cache")
          expect(command_reload).to have_selector("span#command_repo_reload_result", :text => "")
        end
      end
    end
  end

  describe "server management" do
    it "should show server management section" do
      render

      Capybara.string(response.body).find('div#server_management').tap do |div|
        expect(div).to have_selector("h2.legend", "Server Management")

        div.find(".fieldset").tap do |fieldset|
          fieldset.all(".form_item").tap do |form_items|
            expect(form_items[0]).to have_selector("label", :text => "Site URL")
            expect(form_items[0]).to have_selector("input[type='text'][value='abc']")
            expect(form_items[1]).to have_selector("label", :text => "Secure Site URL")
            expect(form_items[1]).to have_selector("input[type='text'][value='def']")
          end
        end
      end
    end
  end

  describe "email notification management" do
    it "should show all the email fields" do
      render

      Capybara.string(response.body).find('div#mail_host_config').tap do |div|
        expect(div).to have_selector("h2.legend", "Email Notification")

        div.find(".fieldset").tap do |fieldset|
          fieldset.all(".form_item").tap do |form_items|
            expect(form_items[1]).to have_selector("label", :text => "Hostname*")
            expect(form_items[1]).to have_selector("input[type='text'][name='server_configuration_form[hostName]']")

            expect(form_items[2]).to have_selector("label", :text => "Port*")
            expect(form_items[2]).to have_selector("input[type='text'][name='server_configuration_form[port]']")

            expect(form_items[3]).to have_selector("label", :text => "Username")
            expect(form_items[3]).to have_selector("input[type='text'][name='server_configuration_form[username]']")

            expect(form_items[4]).to have_selector("label", :text => "Password")
            expect(form_items[4]).to have_selector("input[type='password'][name='server_configuration_form[password]']")
            expect(form_items[4]).to have_selector("label", :text => "Change Password")

            expect(form_items[5]).to have_selector("label", :text => "Use SMTPS")
            expect(form_items[5]).to have_selector("input[type='checkbox'][name='server_configuration_form[tls]']")
            expect(form_items[5]).to have_selector("div[class='contextual_help has_go_tip_right']")

            help_text = form_items[5].find("div[class='contextual_help has_go_tip_right']")["title"]
            expect(help_text).to start_with("This changes the protocol used to send the mail. It switches between SMTP and SMTPS")
            expect(help_text).to include("<a class='' href='http://www.go.cd/documentation/user/current/configuration/admin_mailhost_info.html#starttls'")

            expect(form_items[6]).to have_selector("label", :text => "From*")
            expect(form_items[6]).to have_selector("input[type='text'][name='server_configuration_form[from]']")

            expect(form_items[7]).to have_selector("label", :text => "Admin mail*")
            expect(form_items[7]).to have_selector("input[type='text'][name='server_configuration_form[adminMail]']")
          end
        end
      end
    end
  end

  describe "user management" do
    it "should have a text area for search bases" do
      server_config_form = ServerConfigurationForm.new({:ldap_search_base => "foo\\nbar\\nbaz,goo"})
      assign(:server_configuration_form, server_config_form)

      render

      Capybara.string(response.body).find('#user_management').tap do |div|
        expect(div).to have_selector("label[for='server_configuration_form_ldap_search_base']", :text => "Search Base*")
        expect(div).not_to have_selector("input[name='server_configuration_form[ldap_search_base]']")
        expect(div).to have_selector("textarea[name='server_configuration_form[ldap_search_base]'][class='large']", :text => "foo\\nbar\\nbaz,goo")
        expect(div).to have_selector(".contextual_help")
      end
    end
  end
end
