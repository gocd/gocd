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

describe "/agent_details/job_run_history" do
  include AgentMother
  include GoUtil
  include AgentsHelper

  before :each do
    stub_context_path(template)
    stub_context_path(self)
    in_params(:uuid => "UUID_host1")
    assigns[:agent] = idle_agent(:hostname => 'Agent01', :location => '/var/lib/cruise-agent', :operating_system => "Linux", :uuid => "UUID_host1")
    @a_d = org.joda.time.DateTime.new(1984, 12, 23, 0, 0, 0, 0)
    @a = @a_d.plusDays(1)

    @awesome_day_date = @a_d.toDate()
    @another_day_date = @a.toDate()

    job1 = JobInstanceMother.passed("first", @awesome_day_date, 5000)
    job1.setPipelineStillConfigured(true)
    job2 = JobInstanceMother.cancelled("second", @another_day_date, 10000)
    job2.setPipelineStillConfigured(true)
    job3 = JobInstanceMother.rescheduled("third", "uuid")

    assigns[:job_instances] = JobInstancesModel.new(JobInstances.new([job1, job2, job3].to_java(JobInstance)), Pagination.pageByNumber(6, 40, 2))
    class << template
      include AgentsHelper
    end
  end

  describe :tabs do

    it "should have details and job history tabs" do

      render "/agent_details/job_run_history"

      response.body.should have_tag(".sub_tabs_container") do
        with_tag("ul.tabs") do
          with_tag("li") do
            with_tag("a[href='#'][onclick='location.href = \"/agents/UUID_host1\"']", "Details")
          end
          with_tag("li.current_tab") do
            with_tag("a[href='#']", "Job Run History")
          end
        end
      end
    end
  end

  describe :job_run_history do

    it "should show table for job history" do
      render "/agent_details/job_run_history"

      response.body.should have_tag(".job_history_table") do
        with_tag("table.jobs thead") do
          with_tag("tr.header") do
            with_tag("th.pipeline a[href=/agents/UUID_host1/job_run_history?column=pipeline&amp;order=ASC]", "Pipeline")
            with_tag("th.stage a[href=/agents/UUID_host1/job_run_history?column=stage&amp;order=ASC]", "Stage")
            with_tag("th.job a[href=/agents/UUID_host1/job_run_history?column=job&amp;order=ASC]", "Job")
            with_tag("th.result a[href=/agents/UUID_host1/job_run_history?column=result&amp;order=ASC]", "Result")
            with_tag("th.completed a[href=/agents/UUID_host1/job_run_history?column=completed&amp;order=ASC]", "Completed")
            with_tag("th.duration", "Duration")
          end
        end
      end
    end

    it "should show table for job history" do
      assigns[:job_instances] = JobInstancesModel.new(JobInstances.new(), Pagination.pageByNumber(6, 40, 2))
      render "/agent_details/job_run_history"

      response.body.should_not have_tag(".job_history_table")
      response.body.should have_tag("div.notification p.information", "This agent has no completed job runs.")
    end

    it "should show job history" do
      render "/agent_details/job_run_history"

      response.body.should have_tag(".job_history_table") do
        with_tag("table.jobs tbody") do
          with_tag("tr") do
            with_tag("td.pipeline", "pipeline")
            with_tag("td.stage", "stage")
            with_tag("td.job a[href='#{build_locator_url("pipeline/1/stage/1/first")}']", "first")
            with_tag("td.result", "Passed")
            with_tag("td.completed", @a_d.plusSeconds(25).toDate().iso8601())
            with_tag("td.duration", "00:00:25")
          end
          with_tag("tr") do
            with_tag("td.pipeline", "pipeline")
            with_tag("td.stage", "stage")
            with_tag("td.job a[href='#{build_locator_url("pipeline/1/stage/1/second")}']", "second")
            with_tag("td.result", "Cancelled")
            with_tag("td.completed", @a.plusSeconds(10).toDate().iso8601())
            with_tag("td.duration", "00:00:10")
          end
          with_tag("tr") do
            with_tag("td.pipeline", "pipeline")
            with_tag("td.stage", "stage")
            with_tag("td.job span[title='Pipeline config for this job no longer exists.']", "third")
            with_tag("td.result", "Rescheduled")
            with_tag("td.completed", @a.plusSeconds(10).toDate().iso8601())
            with_tag("td.duration", "-")
          end
        end
      end
    end

    it "should show pagination links" do
      render '/agent_details/job_run_history.html'

      response.should have_tag(".pagination") do
        with_tag("span.current_page", "6")
        with_tag("span", "...")
        with_tag("a[href=#{job_run_history_on_agent_path(:uuid => "UUID_host1", :page => "5")}]", "5")
        with_tag("a[href=#{job_run_history_on_agent_path(:uuid => "UUID_host1", :page => "7")}]", "7")
      end
    end
  end
end