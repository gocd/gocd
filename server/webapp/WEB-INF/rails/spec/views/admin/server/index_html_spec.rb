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

describe "config/server/index.html.erb" do

  before(:each) do
    assigns[:server_configuration_form] = ServerConfigurationForm.new({:artifactsDir => "some_dir", :purgeArtifacts => "Size", :purgeStart => 10.5, :purgeUpto => 20.3, :siteUrl => "abc", :secureSiteUrl => "def", :commandRepositoryLocation => "default"})
    template.stub(:cruise_config_md5).and_return('foo_bar_baz')
    template.stub(:l).and_return(localizer = Class.new do
      def method_missing method, *args
        com.thoughtworks.go.i18n.LocalizedMessage.string(args[0], args[1..-1].to_java(java.lang.Object)).localize(Spring.bean("localizer"))
      end
    end.new)
  end

  describe "artifacts management" do
    it "should show artifacts section" do
      render 'admin/server/index.html.erb'

      response.body.should have_tag("div#pipeline_management") do
        with_tag("h2.legend", "Pipeline Management")
        with_tag(".fieldset") do
          with_tag(".form_item") do
            with_tag("label", "Artifacts Directory Location")
            with_tag("input[type='text'][value='some_dir']")
          end
          with_tag(".form_item") do
            with_tag("label", "Auto delete old artifacts:")
            with_tag(".checkbox_row") do
              with_tag("input[type='radio'][name='server_configuration_form[purgeArtifacts]'][value='Never'][id='never_delete_artifacts']")
              with_tag("label[for='never_delete_artifacts']", "Never")
            end
            with_tag(".checkbox_row") do
              with_tag("input[type='radio'][id='select_artifact_purge_size'][name='server_configuration_form[purgeArtifacts]'][value='Size'][checked='checked']")
              with_tag("span.checkbox_label", /^When available disk space is less than/)
              with_tag("input[name='server_configuration_form[purgeStart]'][value='10.5']")
              with_tag("input[name='server_configuration_form[purgeUpto]'][value='20.3']")
            end
          end
        end
      end
    end
  end

  describe "pipeline management" do
    it "should show hung job timeout options" do
      render 'admin/server/index.html.erb'
      response.body.should have_tag("div#pipeline_management") do
        with_tag("h2.legend", "Pipeline Management")

        with_tag("#hung_job_timeout") do
          with_tag("label", "Default Job Timeout")
          with_tag("input[type='radio'][name='server_configuration_form[timeoutType]'][value='overrideTimeout']")
          with_tag("label", "Cancel after")
          with_tag("input[type='text'][name='server_configuration_form[jobTimeout]']")
          with_tag("label", "minute(s) of inactivity")
          with_tag("label", "Never timeout")
          with_tag("input[type='radio'][name='server_configuration_form[timeoutType]'][value='neverTimeout']")
        end
      end
    end
  end

  describe "command repository management" do
    it "should show lookup command repository location field" do
      assigns[:command_repository_base_dir_location] = "/home/cruise/db/command_repository"
      render 'admin/server/index.html.erb'
      response.body.should have_tag("div#command_repository_management") do
          with_tag("#lookup-command-repo-location") do
            with_tag("label", "Location")
            with_tag("input[type='text'][name='server_configuration_form[commandRepositoryLocation]'][value='default']")
            with_tag("label", "/home/cruise/db/command_repository")
          end
      end
    end

    it "should show command repository cache reload button" do
      assigns[:command_repository_base_dir_location] = "/home/cruise/db/command_repository"
      render 'admin/server/index.html.erb'
      response.body.should have_tag("div#command_repository_management") do
          with_tag("#lookup-command-reload") do
            with_tag("button#reloadCommandRepoCache", "Reload cache")
            with_tag("span#command_repo_reload_result", "")
          end
      end
    end
  end

  describe "server management" do
    it "should show server management section" do
      render 'admin/server/index.html.erb'
      response.body.should have_tag("div#server_management") do
        with_tag("h2.legend", "Server Management")
        with_tag(".fieldset") do
          with_tag(".form_item") do
            with_tag("label", "Site URL")
            with_tag("input[type='text'][value='abc']")
            with_tag("label", "Secure Site URL")
            with_tag("input[type='text'][value='def']")
          end
        end
      end
    end
  end
  
  describe "user management" do
    it "should have a text area for search bases" do
      server_config_form = ServerConfigurationForm.new({:ldap_search_base => "foo\\nbar\\nbaz,goo"})
      assigns[:server_configuration_form] = server_config_form
      render "admin/server/index.html"

      response.body.should have_tag("#user_management") do
        with_tag("label[for='server_configuration_form_ldap_search_base']", "Search Base*")
        without_tag("input[name='server_configuration_form[ldap_search_base]']")
        with_tag("textarea[name='server_configuration_form[ldap_search_base]'][class='large']", "foo\\nbar\\nbaz,goo")
        with_tag(".contextual_help")
      end
    end
  end
end
