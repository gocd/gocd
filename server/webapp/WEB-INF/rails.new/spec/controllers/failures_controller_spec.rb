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

require 'rails_helper'

describe FailuresController do
  before do
    @failure_service = double("failure_service")
    @user = Username.new(CaseInsensitiveString.new("foo"))
    allow(controller).to receive(:failure_service).and_return(@failure_service)
    allow(controller).to receive(:current_user).and_return(@user)
    @job_id = JobIdentifier.new(StageIdentifier.new("pipeline_foo", 12, "stage_bar", "34"), "build_dev")
    @failure_details = FailureDetails.new("message", "stack_trace")
  end

  it "should load failure message and stack trace" do
    expect(@failure_service).to receive(:failureDetailsFor).with(@job_id, 'suite_name', 'test_name', @user, an_instance_of(HttpLocalizedOperationResult)).and_return(@failure_details)
    #suite_name and test_name are ParamEncode#enc'ed because they can potentially have special characters like dot(.) or slash(/) etc.

    get :show, params: { :pipeline_name => "pipeline_foo", :pipeline_counter => "12", :stage_name => "stage_bar", :stage_counter => "34", :job_name => "build_dev", :suite_name => "c3VpdGVfbmFtZQ%3D%3D%0A", :test_name => "dGVzdF9uYW1l%0A", :no_layout => true }

    expect(assigns[:failure_details]).to eq(@failure_details)
    assert_template layout: false
  end

  it "should render error message when fails" do
    expect(@failure_service).to receive(:failureDetailsFor) do |_, _, _, _, result|
      result.connectionError(LocalizedMessage.string("ON"))
    end

    get :show, params: { :pipeline_name => "pipeline_foo", :pipeline_counter => "12", :stage_name => "stage_bar", :stage_counter => "34", :job_name => "build_dev", :suite_name => "suite_name", :test_name => "test_name", :no_layout => true }

    expect(assigns[:failure_details]).to be_nil
    expect(response.status).to eq(400)
    expect(response.body).to eq("on\n")
  end
end
