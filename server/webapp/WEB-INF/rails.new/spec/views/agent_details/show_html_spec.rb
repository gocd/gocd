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


describe "/agent_details/show.html.erb" do
  include AgentMother

  before do
    view.stub(:is_user_an_admin?).and_return(true)
  end

  describe :tabs do

    it "should have details and job history tabs" do
      assign(:agent, idle_agent(:hostname => 'Agent01', :location => '/var/lib/cruise-agent', :operating_system => "Linux", :uuid => uuid = "UUID_host1"))
      allow(view).to receive(:job_run_history_on_agent_path).with(:uuid => uuid).and_return("job_history_on_agent_page")

      render

      Capybara.string(response.body).find(".sub_tabs_container").tap do |ele|
        ele.find("ul.tabs").tap do |ul|
          ul.find("li.current_tab").tap do |li|
            expect(li).to have_selector("a[href='#']",:text=> "Details")
          end
          ul.all("li").tap do |li|
            expect(li[1]).to have_selector("a[href='#'][onclick='location.href = \"job_history_on_agent_page\"']",:text=> "Job Run History")
          end
        end
      end
    end

    it "should not have job history tab when user is not an admin" do
      assign(:agent, idle_agent(:hostname => 'Agent01', :location => '/var/lib/cruise-agent', :operating_system => "Linux", :uuid => uuid = "UUID_host1"))
      allow(view).to receive(:is_user_an_admin?).and_return(false)

      render

      expect(response.body).not_to have_selector("a", :text=>"Job Run History")
    end
  end

  describe :details do

    it "should show agent details" do
      assign(:agent, idle_agent(:hostname => 'Agent01', :location => '/var/lib/cruise-agent', :uuid => uuid = "UUID_host1", :space => 10*1024*1024*1024, :operating_system => "Linux",
                                   :ip_address => '127.0.0.1', :resources => "vs.net,nant", :environments=> ["uat", "blah"]))

      render

      Capybara.string(response.body).find(".agent_details_pane").tap do |ele|
          ele.find(".free_space").tap do |f|
            expect(f).to have_selector("label", :text=>"Free Space:")
            expect(f).to have_selector("span", :text=>"10.0 GB")
          end
          ele.find(".sandbox").tap do |f|
            expect(f).to have_selector("label", :text=>"Sandbox:")
            expect(f).to have_selector("span", :text=>"/var/lib/cruise-agent")
          end
          ele.find(".ip").tap do |f|
            expect(f).to have_selector("label", :text=>"IP Address:")
            expect(f).to have_selector("span", :text=>"127.0.0.1")
          end
          ele.find(".os").tap do |f|
            expect(f).to have_selector("label", :text=>"OS:")
            expect(f).to have_selector("span", :text=>"Linux")
          end
          ele.find(".resources").tap do |f|
            expect(f).to have_selector("label", :text=>"Resources:")
            expect(f).to have_selector("span", :text=>"nant | vs.net")
          end
          ele.find(".environments").tap do |f|
            expect(f).to have_selector("label", :text=>"Environments:")
            expect(f).to have_selector("span", :text=> 'blah | uat')
          end
      end
    end

    it "should show message when there are no resources or environments for an agent" do
      assign(:agent, idle_agent())

      render
      Capybara.string(response.body).find(".agent_details_pane").tap do |ele|
        ele.find(".resources").tap do |f|
          expect(f).to have_selector("label",:text=>"Resources:")
          expect(f).to have_selector("span",:text=>"no resources specified")
        end
        ele.find(".environments").tap do |f|
          expect(f).to have_selector("label",:text=>"Environments:")
          expect(f).to have_selector("span",:text=>"no environments specified")
        end
      end
    end
  end
end
