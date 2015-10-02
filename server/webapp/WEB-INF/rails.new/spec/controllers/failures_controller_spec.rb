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

describe FailuresController do
  before do
    @failure_service = double("failure_service")
    @user = Username.new(CaseInsensitiveString.new("foo"))
    controller.stub(:failure_service).and_return(@failure_service)
    controller.stub(:current_user).and_return(@user)
    @job_id = JobIdentifier.new(StageIdentifier.new("pipeline_foo", 12, "stage_bar", "34"), "build_dev")
    @failure_details = FailureDetails.new("message", "stack_trace")
  end

  it "should resolve the route to show action" do
    expect({:get => "/failures/foo_pipeline/10/bar_stage/5/baz_job/quux_suite/bang_test"}).to route_to(:controller => "failures", :action => "show", :pipeline_name => "foo_pipeline", :pipeline_counter => "10", :stage_name => "bar_stage", :stage_counter => "5", :job_name => "baz_job", :suite_name => "quux_suite", :test_name => "bang_test", :no_layout => true)
  end

  it "should load failure message and stack trace" do
    @failure_service.should_receive(:failureDetailsFor).with(@job_id, 'suite_name', 'test_name', @user, an_instance_of(HttpLocalizedOperationResult)).and_return(@failure_details)
    #suite_name and test_name are ParamEncode#enc'ed because they can potentially have special characters like dot(.) or slash(/) etc.

    get :show, :pipeline_name => "pipeline_foo", :pipeline_counter => "12", :stage_name => "stage_bar", :stage_counter => "34", :job_name => "build_dev", :suite_name => "c3VpdGVfbmFtZQ%3D%3D%0A", :test_name => "dGVzdF9uYW1l%0A", :no_layout => true

    assigns[:failure_details].should == @failure_details
    assert_template layout: false
  end

  it "should render error message when fails" do
    @failure_service.should_receive(:failureDetailsFor) do |_, _, _, _, result|
      result.connectionError(LocalizedMessage.string("ON"))
    end

    get :show, :pipeline_name => "pipeline_foo", :pipeline_counter => "12", :stage_name => "stage_bar", :stage_counter => "34", :job_name => "build_dev", :suite_name => "suite_name", :test_name => "test_name", :no_layout => true

    assigns[:failure_details].should be_nil
    response.status.should == 400
    response.body.should == "on\n"
  end
end
