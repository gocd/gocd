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

describe "/agent_details/job_run_history.html.erb" do
  include AgentMother
  include GoUtil
  include AgentsHelper

  before :each do
    stub_context_path(view)
    stub_context_path(self)
    in_params(:uuid => "UUID_host1")
    assign(:agent, idle_agent(:hostname => 'Agent01', :location => '/var/lib/cruise-agent', :operating_system => "Linux", :uuid => "UUID_host1"))
    @a_d = org.joda.time.DateTime.new(1984, 12, 23, 0, 0, 0, 0)
    @a = @a_d.plusDays(1)

    @awesome_day_date = @a_d.toDate()
    @another_day_date = @a.toDate()

    job1 = JobInstanceMother.passed("first", @awesome_day_date, 5000)
    job1.setPipelineStillConfigured(true)
    job2 = JobInstanceMother.cancelled("second", @another_day_date, 10000)
    job2.setPipelineStillConfigured(true)
    job3 = JobInstanceMother.rescheduled("third", "uuid")

    assign(:job_instances,JobInstancesModel.new(JobInstances.new([job1, job2, job3].to_java(JobInstance)), Pagination.pageByNumber(6, 40, 2)))
    class << view
      include AgentsHelper
    end
  end

  describe :tabs do

    it "should have details and job history tabs" do

      render

      Capybara.string(response.body).find(".sub_tabs_container").tap do |ele|
        ele.find("ul.tabs").tap do |ul|
          ul.all("li").tap do |li|
            expect(li[0]).to have_selector("a[href='#'][onclick='location.href = \"/agents/UUID_host1\"']", :text=>"Details")
          end
          ul.find("li.current_tab").tap do |li|
            expect(li).to have_selector("a[href='#']", :text=>"Job Run History")
          end
        end
      end
    end
  end

  describe :job_run_history do

    it "should show table for job history" do
      render

      Capybara.string(response.body).find(".job_history_table").tap do |ele|
          ele.find("table.jobs thead tr.header").tap do |tr|
            expect(tr).to have_selector("th.pipeline a[href='/agents/UUID_host1/job_run_history?column=pipeline&order=ASC']", :text=>"Pipeline")
            expect(tr).to have_selector("th.stage a[href='/agents/UUID_host1/job_run_history?column=stage&order=ASC']", :text=>"Stage")
            expect(tr).to have_selector("th.job a[href='/agents/UUID_host1/job_run_history?column=job&order=ASC']", :text=>"Job")
            expect(tr).to have_selector("th.result a[href='/agents/UUID_host1/job_run_history?column=result&order=ASC']", :text=>"Result")
            expect(tr).to have_selector("th.completed a[href='/agents/UUID_host1/job_run_history?column=completed&order=ASC']", :text=>"Completed")
            expect(tr).to have_selector("th.duration", :text=>"Duration")
          end
      end
    end

    it "should show table for job history" do
      assign(:job_instances, JobInstancesModel.new(JobInstances.new(), Pagination.pageByNumber(6, 40, 2)))
      render

      expect(response.body).not_to have_selector(".job_history_table")
      expect(response.body).to have_selector("div.notification p.information", :text=>"This agent has no completed job runs.")
    end

    it "should show job history" do
      render

      Capybara.string(response.body).find(".job_history_table").tap do |ele|
        ele.all("table.jobs tbody tr").tap do |tr|
            expect(tr[0]).to have_selector("td.pipeline", :text=>"pipeline")
            expect(tr[0]).to have_selector("td.stage", :text=>"stage")
            expect(tr[0]).to have_selector("td.job a[href='#{build_locator_url("pipeline/1/stage/1/first")}']", :text=>"first")
            expect(tr[0]).to have_selector("td.result", :text=>"Passed")
            expect(tr[0]).to have_selector("td.completed", :text=>@a_d.plusSeconds(25).toDate().iso8601())
            expect(tr[0]).to have_selector("td.duration", :text=>"00:00:25")

            expect(tr[1]).to have_selector("td.pipeline", :text=>"pipeline")
            expect(tr[1]).to have_selector("td.stage", :text=>"stage")
            expect(tr[1]).to have_selector("td.job a[href='#{build_locator_url("pipeline/1/stage/1/second")}']", :text=>"second")
            expect(tr[1]).to have_selector("td.result", :text=>"Cancelled")
            expect(tr[1]).to have_selector("td.completed", :text=>@a.plusSeconds(10).toDate().iso8601())
            expect(tr[1]).to have_selector("td.duration", :text=>"00:00:10")

            expect(tr[2]).to have_selector("td.pipeline", :text=>"pipeline")
            expect(tr[2]).to have_selector("td.stage", :text=>"stage")
            expect(tr[2]).to have_selector("td.job span[title='Pipeline config for this job no longer exists.']", :text=>"third")
            expect(tr[2]).to have_selector("td.result", :text=>"Rescheduled")
            expect(tr[2]).to have_selector("td.completed", :text=>"-")
            expect(tr[2]).to have_selector("td.duration", :text=>"-")

        end
      end
    end

    it "should show pagination links" do
      render

      Capybara.string(response.body).find(".pagination").tap do |ele|
        expect(ele).to have_selector("span.current_page",:text=>"6")
        expect(ele).to have_selector("span",:text=>"...")
        expect(ele).to have_selector("a[href='#{job_run_history_on_agent_path(:uuid => "UUID_host1", :page => "5")}']",:text=>"5")
        expect(ele).to have_selector("a[href='#{job_run_history_on_agent_path(:uuid => "UUID_host1", :page => "7")}']",:text=>"7")
      end
    end
  end
end
