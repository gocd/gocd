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

describe "/server/_counts.json.erb" do

  before do
    assign(:current_server_health_states, ServerHealthStates.new([]))
  end

  it "should not render errors when error count is zero" do
    render :partial => "server/counts.html.erb"

    expect(response.body).to_not have_selector('.messages .error_count', "Errors: 0")
  end

  it "should render warnings and errors if there are any in the header" do
    first = ServerHealthState.error("first error", "first description", HealthStateType.invalidConfig())
    second = ServerHealthState.error("second error", "second description", HealthStateType.invalidConfig())
    third = ServerHealthState.warning("first warning", "third description", HealthStateType.artifactsDirChanged())

    assign(:current_server_health_states, ServerHealthStates.new([first, second, third]))

    render :partial => "server/counts.html.erb"

    expect(response.body).to have_selector('a .messages .error_count', "Errors: 2")
    expect(response.body).to have_selector('a .messages .warning_count', "Warnings: 1")
    expect(response.body).to have_selector('.messages', /Errors: 2 &\s+ Warnings: 1/)
    end

  it "should render only errors if there are only errors in the header" do
    first = ServerHealthState.error("first error", "first description", HealthStateType.invalidConfig())
    second = ServerHealthState.error("second error", "second description", HealthStateType.invalidConfig())

    assign(:current_server_health_states, ServerHealthStates.new([first, second]))

    render :partial => "server/counts.html.erb"

    expect(response.body).to have_selector('a .messages .error_count', "Errors: 2")
    expect(response.body).to_not have_selector('.messages .warning_count')
  end

  it "should render only warnings if there are only warnings in the header" do
    first = ServerHealthState.warning("first warning", "third description", HealthStateType.artifactsDirChanged())

    assign(:current_server_health_states, ServerHealthStates.new([first]))

    render :partial => "server/counts.html.erb"

    expect(response.body).to_not have_selector('.messages .error_count')
    expect(response.body).to have_selector('a .messages .warning_count', "Warnings: 1")
  end

  it "should not render errors and warnings when both are zero" do
    render :partial => "server/counts.html.erb"

    expect(response.body).to_not have_selector('.messages .warning_count')
    expect(response.body).to_not have_selector('.messages .error_count')
  end
end
