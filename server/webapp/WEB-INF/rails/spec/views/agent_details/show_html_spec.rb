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

describe "/agent_details/show" do
  include AgentMother

  before do
    template.stub!(:is_user_an_admin?).and_return(true)
  end

  describe :tabs do

    it "should have details and job history tabs" do
      assigns[:agent] = idle_agent(:hostname => 'Agent01', :location => '/var/lib/cruise-agent', :operating_system => "Linux", :uuid => uuid = "UUID_host1")
      template.stub(:job_run_history_on_agent_path).with(:uuid => uuid).and_return("job_history_on_agent_page")

      render "/agent_details/show"

      response.body.should have_tag(".sub_tabs_container") do
        with_tag("ul.tabs") do
          with_tag("li.current_tab") do
            with_tag("a[href='#']", "Details")
          end
          with_tag("li") do
            with_tag("a[href='#'][onclick='location.href = \"job_history_on_agent_page\"']", "Job Run History")
          end
        end
      end
    end

    it "should not have job history tab when user is not an admin" do
      assigns[:agent] = idle_agent(:hostname => 'Agent01', :location => '/var/lib/cruise-agent', :operating_system => "Linux", :uuid => uuid = "UUID_host1")

      template.stub!(:is_user_an_admin?).and_return(false)

      render "/agent_details/show"

      response.body.should_not have_tag("a", "Job Run History")
    end
  end

  describe :details do

    it "should show agent details" do
      assigns[:agent] = idle_agent(:hostname => 'Agent01', :location => '/var/lib/cruise-agent', :uuid => uuid = "UUID_host1", :space => 10*1024*1024*1024, :operating_system => "Linux",
                                   :ip_address => '127.0.0.1', :resources => "vs.net,nant", :environments=> ["uat", "blah"])

      render "/agent_details/show"

      response.body.should have_tag(".agent_details_pane") do
        with_tag(".free_space") do
          with_tag("label", "Free Space:")
          with_tag("span", "10.0 GB")
        end
        with_tag(".sandbox") do
          with_tag("label", "Sandbox:")
          with_tag("span", "/var/lib/cruise-agent")
        end
        with_tag(".ip") do
          with_tag("label", "IP Address:")
          with_tag("span", "127.0.0.1")
        end
        with_tag(".os") do
          with_tag("label", "OS:")
          with_tag("span", "Linux")
        end
        with_tag(".resources") do
          with_tag("label", "Resources:")
          with_tag("span", "nant | vs.net")
        end
        with_tag(".environments") do
          with_tag("label", "Environments:")
          with_tag("span", "uat | blah")
        end
      end
    end

    it "should show message when there are no resources or environments for an agent" do
      assigns[:agent] = idle_agent()

      render "/agent_details/show"

      response.body.should have_tag(".agent_details_pane") do
        with_tag(".resources") do
          with_tag("label", "Resources:")
          with_tag("span", "no resources specified")
        end
        with_tag(".environments") do
          with_tag("label", "Environments:")
          with_tag("span", "no environments specified")
        end
      end
    end
  end
end