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

require File.join(File.dirname(__FILE__), "..", "..", "spec_helper")
require File.join(File.dirname(__FILE__), "..", "auto_refresh_examples")


describe 'stages/_jobs.html.erb' do
  include JobMother, GoUtil, ReflectiveUtil

  before :each do
    stage = StageMother.completedStageInstanceWithTwoPlans("bar_stage")
    stage.setIdentifier(StageIdentifier.new("foo_pipeline", 10, "label-10", "bar_stage" "2"))
    stage.calculateResult()

    @stage = StageSummaryModel.new(stage, nil, nil, nil)

    @jobs = jobs_model

    in_params(:pipeline_name => "cruise", :pipeline_counter => "1", :stage_name => "dev", :stage_counter => "2", :tab => "jobs")
  end

  it "should wrap the job name if its too long" do
    render :partial => "stages/jobs", :locals => {:scope => {:jobs => jobs_with_long_and_short_name, :stage => @stage, :has_operate_permissions => true}}
    response.should have_tag(".jobs_summary") do
      with_tag ".job" do |jobs|
        first, second = jobs.map! do |job_tag|
          job_tag.to_s
        end
        first.should have_tag("wbr")
        second.should_not have_tag("wbr")
      end
    end
  end

  describe "rerun form" do
    it "should display form only if stage is complete" do
      stage = StageMother.scheduledStage("foo_pipeline", 10, "bar_stage", 2, "quux")
      stage.calculateResult()
      render :partial => "stages/jobs", :locals => {:scope => {:jobs => @jobs, :stage => StageSummaryModel.new(stage, nil, nil, nil)}}
      response.body.should_not have_tag("form")
    end

    it "should display form and checkbox next to job names only if stage is complete" do
      render :partial => "stages/jobs", :locals => {:scope => {:jobs => @jobs, :stage => @stage, :has_operate_permissions => true}}
      response.body.should have_tag("form[action='/pipelines/cruise/1/dev/2/rerun-jobs?tab=jobs']") do |form|
        form.should have_tag("button[type='submit']", "RERUN")
        form.should have_tag("input[type='checkbox'][name='jobs[]'][value='first']")
        form.should have_tag("input[type='checkbox'][name='jobs[]'][value='second']")
        form.should have_tag("input[type='checkbox'][name='jobs[]'][value='third']")
        form.should have_tag("input[type='checkbox'][name='jobs[]'][value='fourth']")
        form.should have_tag("input[type='checkbox'][name='jobs[]'][value='fifth']")
      end
    end

    it "should not display form and checkbox next to job names when stage is active" do
      @stage.getState().should_receive(:completed).any_number_of_times.and_return(false)
      render :partial => "stages/jobs", :locals => {:scope => {:jobs => @jobs, :stage => @stage, :has_operate_permissions => true}}
      response.body.should_not have_tag("form[action='/pipelines/cruise/1/dev/2/rerun-jobs?tab=jobs']")
      response.body.should_not have_tag("button[type='submit']", "RERUN")
      response.body.should_not have_tag("input[type='checkbox'][name='jobs[]'][value='first']")
      response.body.should_not have_tag("input[type='checkbox'][name='jobs[]'][value='second']")
      response.body.should_not have_tag("input[type='checkbox'][name='jobs[]'][value='third']")
      response.body.should_not have_tag("input[type='checkbox'][name='jobs[]'][value='fourth']")
      response.body.should_not have_tag("input[type='checkbox'][name='jobs[]'][value='fifth']")
    end

    it "should not display rerun button if user is not authorized" do
      render :partial => 'stages/jobs', :locals => {:scope => {:jobs => @jobs, :stage => @stage, :has_operate_permissions => false}}
      response.body.should have_tag("div#job_actions") do
        without_tag("button[type='submit']", 'RERUN')
      end
      response.body.should have_tag("table.jobs_summary") do
        without_tag("input[type='checkbox'][name='jobs[]'][value='first']")
        without_tag("input[type='checkbox'][name='jobs[]'][value='second']")
        without_tag("input[type='checkbox'][name='jobs[]'][value='third']")
        without_tag("input[type='checkbox'][name='jobs[]'][value='fourth']")
        without_tag("input[type='checkbox'][name='jobs[]'][value='fifth']")
      end
    end

    it "should display rerun button if user is authorized" do
      render :partial => 'stages/jobs', :locals => {:scope => {:jobs => @jobs, :stage => @stage, :has_operate_permissions => true}}
      response.body.should have_tag("div#job_actions") do
        with_tag("button[type='submit']", 'RERUN')
      end
      response.body.should have_tag("table.jobs_summary") do
        with_tag("input[type='checkbox'][name='jobs[]'][value='first']")
        with_tag("input[type='checkbox'][name='jobs[]'][value='second']")
        with_tag("input[type='checkbox'][name='jobs[]'][value='third']")
        with_tag("input[type='checkbox'][name='jobs[]'][value='fourth']")
        with_tag("input[type='checkbox'][name='jobs[]'][value='fifth']")
      end

    end

  end

  it "should mark jobs table differently when has re-runs" do
    @stage.getStage().getJobInstances().get(0).setRerun(true)
    @stage.getStage().setRerunOfCounter(10)
    render :partial => "stages/jobs", :locals => {:scope => {:jobs => @jobs, :stage => @stage}}
    response.should have_tag("table.stage_with_rerun_jobs")
  end

  it "should mark job entry when is re-run" do
    get_val(@jobs[0], 'instance').setRerun(true)
    render :partial => "stages/jobs", :locals => {:scope => {:jobs => @jobs, :stage => @stage}}
    response.should have_tag("tr.job.is_rerun td.job_name a", "first")
    response.should_not have_tag("tr.job.not_rerun td a", "first")
    response.should have_tag("tr.job.not_rerun td.job_name a", "second")
    response.should_not have_tag("tr.job.is_rerun td a", "second")
  end

  it "should get job name, state, result" do
    render :partial => "stages/jobs", :locals => {:scope => {:jobs => @jobs, :stage => @stage}}

    response.should have_tag(".jobs_summary") do
      with_tag ".job" do |jobs|
        first, second, third, fourth, fifth = jobs.map! do |job_tag|
          job_tag.to_s
        end
        first.should have_tag(".job_name a[href='/tab/build/detail/blah-pipeline/1/blah-stage/2/first']", "first")
        first.should have_tag(".job_result", /Failed/)
        first.should have_tag(".job_state", "Completed")
        first.should have_tag(".elapsed_time", /^2 minutes/)
        first.should have_tag(".agent[title='location-1'] a[href='#{agent_detail_path(:uuid => 'agent1')}']", "host1(1.1.1.1)")

        second.should have_tag(".job_name a[href='/tab/build/detail/blah-pipeline/1/blah-stage/2/second']", "second")
        second.should have_tag(".job_result", "Passed")
        second.should have_tag(".job_state", "Completed")
        second.should have_tag(".elapsed_time", /^2 minutes/)
        second.should have_tag(".agent[title='location-2'] a[href='#{agent_detail_path(:uuid => 'agent2')}']", "host2(2.2.2.2)")

        third.should have_tag(".job_name a[href='/tab/build/detail/blah-pipeline/1/blah-stage/2/third']", "third")
        third.should have_tag(".job_result", "Active")
        third.should have_tag(".job_state", "Scheduled")
        third.should have_tag(".elapsed_time", "")
        third.should have_tag(".agent", /Not yet assigned/)

        fourth.should have_tag(".job_name a[href='/tab/build/detail/blah-pipeline/1/blah-stage/2/fourth']", "fourth")
        fourth.should have_tag(".job_result", /Active/)
        fourth.should have_tag(".job_state", "Building")
        fourth.should have_tag(".elapsed_time", /^2 minutes/)
        fourth.should have_tag(".agent a[href='#{agent_detail_path(:uuid => 'agent4')}']", "host4(4.4.4.4)")

        fifth.should have_tag(".job_name a[href='/tab/build/detail/blah-pipeline/1/blah-stage/2/fifth']", "fifth")
        fifth.should have_tag(".job_result", /Cancelled/)
        fifth.should have_tag(".job_state", "Completed")
        fifth.should have_tag(".elapsed_time", /^2 minutes/)
        fifth.should have_tag(".agent a[href='#{agent_detail_path(:uuid => 'agent5')}']", "host5(5.5.5.5)")
      end
    end
  end

  it "should show job transition times" do
    render :partial => "stages/jobs", :locals => {:scope => {:jobs => @jobs, :stage => @stage}}

    response.should have_tag(".jobs_summary") do
      with_tag ".job" do |jobs|
        first, second, third, fourth, fifth = jobs.map! do |job_tag| job_tag.to_s end
        time_first, time_second, time_third, time_fourth, time_fifth = (0..4).map do |index| @jobs[index].getTransitions().first.getStateChangeTime().getTime() end

        assert_job_transition_times(first, time_first, "scheduled", "assigned", "preparing", "building", "completing", "completed")
        assert_job_transition_times(second, time_second, "scheduled", "assigned", "preparing", "building", "completing", "completed")
        assert_job_transition_times(third, time_third, "scheduled")
        assert_job_transition_times(fifth, time_fifth, "scheduled", "assigned", "preparing", "building", "completing", "completed")

        # Fourth job has weird transition times setup for it. :(
        fourth.should have_tag(".job[data-scheduled=?]", time_fourth)
        fourth.should have_tag(".job[data-assigned=?]", time_fourth - 240000)
        fourth.should have_tag(".job[data-preparing=?]", time_fourth - 180000)
        fourth.should have_tag(".job[data-building=?]", time_fourth - 120000)
      end
    end
  end
end

def assert_job_transition_times job_tag, expected_first_transition_time, *transitions_expected
  transitions_expected.each_with_index do |transition, index|
    expected_time = expected_first_transition_time + (60000 * index)
    error_message_if_expectation_fails = "Expected data-#{transition}=\"#{expected_time}\" in: #{job_tag}"
    job_tag.should have_tag(".job[data-#{transition}=?]", expected_time), error_message_if_expectation_fails
  end
end