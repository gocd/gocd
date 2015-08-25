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

describe "stages/_jobs_breakdown.html.erb" do
  include StageModelMother

  it "should display job links" do
    render :partial => "stages/jobs_breakdown", :locals=>{:scope => {:message => "PASSED_JOBS", :jobs => stage_with_5_jobs.passedJobs(), :parent_id=>"jobs_passed"}}
    expect(response).to have_selector ".job a[href='/tab/build/detail/pipeline/1/stage/1/third']", :text => "third"
  end
end
