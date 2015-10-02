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

describe "stages/_elapsed_time.html.erb" do
  include StageModelMother

  it "should display elapsed time for jobs with no history or not started yet" do
    render :partial => "stages/elapsed_time", :locals=>{:scope => {:job => stage_with_5_jobs.inProgressJobs().get(0), :show_elapsed  => true}}
    expect(response).to have_selector ".elapsed_time", :text => /^[\s\S]*Elapsed:[\s\S]*2 minutes[\s\S]*$/
  end

  it "should display elapsed time for jobs if they go longer than before" do
    in_progress = stage_with_5_jobs.inProgressJobs()
    in_progress[0].stub(:getPercentComplete).and_return(100)
    render :partial => "stages/elapsed_time", :locals=>{:scope => {:job => in_progress.get(0), :show_elapsed  => true}}
    expect(response).to have_selector ".elapsed_time", :text => /^[\s\S]*Elapsed:[\s\S]*2 minutes[\s\S]*/
    end

  it "should not display elapsed: if time is empty" do
    in_progress = stage_with_5_jobs.inProgressJobs()
    in_progress[0].stub(:getPercentComplete).and_return(100)
    in_progress[0].stub(:getElapsedTime).and_return(Duration.new(0))
    render :partial => "stages/elapsed_time", :locals=>{:scope => {:job => in_progress.get(0), :show_elapsed  => true}}
    expect(response).to have_selector ".elapsed_time", :text => ""
  end

  it "should display progress bar when percent complete is between 0 and 100" do
    in_progress = stage_with_5_jobs.inProgressJobs()
    in_progress[0].stub(:getPercentComplete).and_return(75)
    render :partial => "stages/elapsed_time", :locals=>{:scope => {:job => in_progress.get(0)}}
    expect(response).to have_selector(".progress_bar_container div.progress_bar[style='width: 75%;']")
  end

  it "should display elapsed time for jobs if they are completed irrespective of percent complete" do
    non_passed_jobs = stage_with_5_jobs.nonPassedJobs()
    non_passed_jobs[0].stub(:getPercentComplete).and_return(98)
    render :partial => "stages/elapsed_time", :locals=>{:scope => {:job => non_passed_jobs.get(0), :show_elapsed  => true}}
    expect(response).to_not have_selector(".progress_bar_container div.progress_bar[style='width: 98%;']")
    expect(response).to have_selector ".elapsed_time", :text => /^[\s\S]*Elapsed:[\s\S]*2 minutes[\s\S]*/
  end
end
