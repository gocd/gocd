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
require File.join(File.dirname(__FILE__), "..", "auto_refresh_examples")


describe 'stages/_jobs.html.erb' do
  include JobMother, GoUtil, ReflectiveUtil

  before :each do
    stage = StageMother.completedStageInstanceWithTwoPlans("bar_stage")
    stage.setIdentifier(StageIdentifier.new("foo_pipeline", 10, "label-10", "bar_stage" "2"))
    stage.calculateResult()

    @stage = StageSummaryModel.new(stage, nil, nil, nil)

    @jobs = jobs_model

    in_params(:pipeline_name => "cruise", :pipeline_counter => "1", :stage_name => "dev", :stage_counter => "2", :tab => "jobs", :action => "jobs")
  end

  it "should wrap the job name if its too long" do
    render :partial => "stages/jobs", :locals => {:scope => {:jobs => jobs_with_long_and_short_name, :stage => @stage, :has_operate_permissions => true}}
    Capybara.string(response.body).all(".jobs_summary .job").tap do |a|
      expect(a[0]).to have_selector("wbr")
      expect(a[1]).to_not have_selector("wbr")
    end
  end

  describe "rerun form" do
    it "should display form only if stage is complete" do
      stage = StageMother.scheduledStage("foo_pipeline", 10, "bar_stage", 2, "quux")
      stage.calculateResult()
      render :partial => "stages/jobs", :locals => {:scope => {:jobs => @jobs, :stage => StageSummaryModel.new(stage, nil, nil, nil)}}
      expect(response.body).to_not have_selector("form")
    end

    it "should display form and checkbox next to job names only if stage is complete" do
      render :partial => "stages/jobs", :locals => {:scope => {:jobs => @jobs, :stage => @stage, :has_operate_permissions => true}}

      Capybara.string(response.body).find("form[action='/pipelines/cruise/1/dev/2/rerun-jobs?tab=jobs']").tap do |form|
        expect(form).to have_selector("button[type='submit']", "RERUN")
        expect(form).to have_selector("input[type='checkbox'][name='jobs[]'][value='first']")
        expect(form).to have_selector("input[type='checkbox'][name='jobs[]'][value='second']")
        expect(form).to have_selector("input[type='checkbox'][name='jobs[]'][value='third']")
        expect(form).to have_selector("input[type='checkbox'][name='jobs[]'][value='fourth']")
        expect(form).to have_selector("input[type='checkbox'][name='jobs[]'][value='fifth']")
      end
    end

    it "should not display form and checkbox next to job names when stage is active" do
      @stage.getState().stub(:completed).and_return(false)
      render :partial => "stages/jobs", :locals => {:scope => {:jobs => @jobs, :stage => @stage, :has_operate_permissions => true}}

      expect(response.body).to_not have_selector("form")
      expect(response.body).to_not have_selector("button[type='submit']", "RERUN")
      expect(response.body).to_not have_selector("input[type='checkbox'][name='jobs[]'][value='first']")
      expect(response.body).to_not have_selector("input[type='checkbox'][name='jobs[]'][value='second']")
      expect(response.body).to_not have_selector("input[type='checkbox'][name='jobs[]'][value='third']")
      expect(response.body).to_not have_selector("input[type='checkbox'][name='jobs[]'][value='fourth']")
      expect(response.body).to_not have_selector("input[type='checkbox'][name='jobs[]'][value='fifth']")
    end

    it "should not display rerun button if user is not authorized" do
      render :partial => 'stages/jobs', :locals => {:scope => {:jobs => @jobs, :stage => @stage, :has_operate_permissions => false}}
      Capybara.string(response.body).find("div#job_actions").tap do |div|
        expect(div).to_not have_selector("button[type='submit']", 'RERUN')
      end
      Capybara.string(response.body).find("table.jobs_summary").tap do |table|
        expect(table).to_not have_selector("input[type='checkbox'][name='jobs[]'][value='first']")
        expect(table).to_not have_selector("input[type='checkbox'][name='jobs[]'][value='second']")
        expect(table).to_not have_selector("input[type='checkbox'][name='jobs[]'][value='third']")
        expect(table).to_not have_selector("input[type='checkbox'][name='jobs[]'][value='fourth']")
        expect(table).to_not have_selector("input[type='checkbox'][name='jobs[]'][value='fifth']")
      end
    end

    it "should display rerun button if user is authorized" do
      render :partial => 'stages/jobs', :locals => {:scope => {:jobs => @jobs, :stage => @stage, :has_operate_permissions => true}}

      Capybara.string(response.body).find("div#job_actions").tap do |f|
        expect(f).to have_selector("button[type='submit']", :text => 'RERUN')
      end

      Capybara.string(response.body).find("table.jobs_summary").tap do |f|
        expect(f).to have_selector("input[type='checkbox'][name='jobs[]'][value='first']")
        expect(f).to have_selector("input[type='checkbox'][name='jobs[]'][value='second']")
        expect(f).to have_selector("input[type='checkbox'][name='jobs[]'][value='third']")
        expect(f).to have_selector("input[type='checkbox'][name='jobs[]'][value='fourth']")
        expect(f).to have_selector("input[type='checkbox'][name='jobs[]'][value='fifth']")
      end
    end

  end

  it "should mark jobs table differently when has re-runs" do
    @stage.getStage().getJobInstances().get(0).setRerun(true)
    @stage.getStage().setRerunOfCounter(10)
    render :partial => "stages/jobs", :locals => {:scope => {:jobs => @jobs, :stage => @stage}}
    expect(response).to have_selector("table.stage_with_rerun_jobs")
  end

  it "should mark job entry when is re-run" do
    get_val(@jobs[0], 'instance').setRerun(true)
    render :partial => "stages/jobs", :locals => {:scope => {:jobs => @jobs, :stage => @stage}}
    expect(response).to have_selector("tr.job.is_rerun td.job_name a", :text => "first")
    expect(response).to_not have_selector("tr.job.not_rerun td a", :text => "first")
    expect(response).to have_selector("tr.job.not_rerun td.job_name a", :text => "second")
    expect(response).to_not have_selector("tr.job.is_rerun td a", :text => "second")
  end

  it "should smart break agent IP" do
    jobs =  []
    job = JobInstanceMother.completed("first", JobResult::Failed)
    job.setIdentifier(JobIdentifier.new("blah-pipeline", 1, "blah-label", "blah-stage", "2", job.getName()))
    i = 1
    host_name = "host_#{SecureRandom.hex(32)}_#{i}"
    agent = job.isAssignedToAgent()?
          AgentInstanceMother.updateLocation(
              AgentInstanceMother.updateIpAddress(
                  AgentInstanceMother.updateHostname(
                      AgentInstanceMother.updateUuid(AgentInstanceMother.building(), "agent_#{i}"), host_name), "#{i}.#{i}.#{i}.#{i}"), "location-#{i}") : nil
    jobs << JobInstanceModel.new(job, JobDurationStrategy::ConstantJobDuration.new(100), agent)
    render :partial => "stages/jobs", :locals => {:scope => {:jobs => jobs, :stage => @stage}}
    Capybara.string(response.body).find("td.agent").tap do |agent|
      expect(agent).to have_selector("a", :text => host_name)
    end
  end

  it "should get job name, state, result" do
    render :partial => "stages/jobs", :locals => {:scope => {:jobs => @jobs, :stage => @stage}}

    Capybara.string(response.body).all(".jobs_summary .job").tap do |f|
      expect(f[0]).to have_selector(".job_name a[href='/tab/build/detail/blah-pipeline/1/blah-stage/2/first']", :text => "first")
      expect(f[0]).to have_selector(".job_result", :text => /Failed/)
      expect(f[0]).to have_selector(".job_state", :text => "Completed")
      expect(f[0]).to have_selector(".elapsed_time", :text => /^[\s\S]*2 minutes[\s\S]*/)
      expect(f[0]).to have_selector(".agent[title='location-1'] a[href='#{agent_detail_path(:uuid => 'agent1')}']", :text => "host1(1.1.1.1)")

      expect(f[1]).to have_selector(".job_name a[href='/tab/build/detail/blah-pipeline/1/blah-stage/2/second']", :text => "second")
      expect(f[1]).to have_selector(".job_result", :text => "Passed")
      expect(f[1]).to have_selector(".job_state", :text => "Completed")
      expect(f[1]).to have_selector(".elapsed_time", :text => /^[\s\S]*2 minutes[\s\S]*/)
      expect(f[1]).to have_selector(".agent[title='location-2'] a[href='#{agent_detail_path(:uuid => 'agent2')}']", :text => "host2(2.2.2.2)")

      expect(f[2]).to have_selector(".job_name a[href='/tab/build/detail/blah-pipeline/1/blah-stage/2/third']", :text => "third")
      expect(f[2]).to have_selector(".job_result", :text => "Active")
      expect(f[2]).to have_selector(".job_state", :text => "Scheduled")
      expect(f[2]).to have_selector(".elapsed_time", :text => "")
      expect(f[2]).to have_selector(".agent", :text => /Not yet assigned/)

      expect(f[3]).to have_selector(".job_name a[href='/tab/build/detail/blah-pipeline/1/blah-stage/2/fourth']", :text => "fourth")
      expect(f[3]).to have_selector(".job_result", :text => /Active/)
      expect(f[3]).to have_selector(".job_state", :text => "Building")
      expect(f[3]).to have_selector(".elapsed_time", :text => /^[\s\S]*2 minutes[\s\S]*/)
      expect(f[3]).to have_selector(".agent a[href='#{agent_detail_path(:uuid => 'agent4')}']", :text => "host4(4.4.4.4)")

      expect(f[4]).to have_selector(".job_name a[href='/tab/build/detail/blah-pipeline/1/blah-stage/2/fifth']", :text => "fifth")
      expect(f[4]).to have_selector(".job_result", :text => /Cancelled/)
      expect(f[4]).to have_selector(".job_state", :text => "Completed")
      expect(f[4]).to have_selector(".elapsed_time", :text => /^[\s\S]*2 minutes[\s\S]*/)
      expect(f[4]).to have_selector(".agent a[href='#{agent_detail_path(:uuid => 'agent5')}']", :text => "host5(5.5.5.5)")
    end
  end

  it "should show job transition times" do
    render :partial => "stages/jobs", :locals => {:scope => {:jobs => @jobs, :stage => @stage}}

    time_first, time_second, time_third, time_fourth, time_fifth = (0..4).map do |index| @jobs[index].getTransitions().first.getStateChangeTime().getTime() end

    Capybara.string(response.body).all(".jobs_summary .job").tap do |f|
      assert_job_transition_times(f[0], time_first, "scheduled", "assigned", "preparing", "building", "completing", "completed")
      assert_job_transition_times(f[1], time_second, "scheduled", "assigned", "preparing", "building", "completing", "completed")
      assert_job_transition_times(f[2], time_third, "scheduled")
      assert_job_transition_times(f[4], time_fifth, "scheduled", "assigned", "preparing", "building", "completing", "completed")

      # Fourth job has weird transition times setup for it. :(
      expect(f[3]["data-scheduled"]).to eq time_fourth.to_s
      expect(f[3]["data-assigned"]).to eq (time_fourth - 240000).to_s
      expect(f[3]["data-preparing"]).to eq (time_fourth - 180000).to_s
      expect(f[3]["data-building"]).to eq (time_fourth - 120000).to_s
    end
  end
end

def assert_job_transition_times job_tag, expected_first_transition_time, *transitions_expected
  transitions_expected.each_with_index do |transition, index|
    expected_time = expected_first_transition_time + (60000 * index)
    error_message_if_expectation_fails = "Expected data-#{transition}=\"#{expected_time}\" in: #{job_tag.inspect}"
    expect(job_tag["data-#{transition}"]).to eq(expected_time.to_s), error_message_if_expectation_fails
  end
end
