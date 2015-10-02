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

describe "/server/messages_body.html.erb" do

  before do
    assign(:current_server_health_states, ServerHealthStates.new([]))
  end

  it "should render errors and warnings on a page" do
    first = ServerHealthState.error("first error", "first description", HealthStateType.invalidConfig())
    second = ServerHealthState.error("second error", "second description", HealthStateType.invalidConfig())
    third = ServerHealthState.warning("first warning", "third description", HealthStateType.artifactsDirChanged())
    assign(:current_server_health_states, ServerHealthStates.new([first, second, third]))

    render :partial => "server/messages_body.html.erb"

    body = response.body
    Capybara.string(body).all('.error').tap do |errors|
      assert_message_and_desc errors[0], first.getMessageWithTimestamp(), 'first description'
      assert_message_and_desc errors[1], second.getMessageWithTimestamp(), 'second description'
    end
    Capybara.string(body).all('.warning').tap do |warnings|
      assert_message_and_desc warnings[0], third.getMessageWithTimestamp(), 'third description'
    end
  end

  it "should not render errors when there are none" do
    third = ServerHealthState.warning("first warning", "third description", HealthStateType.artifactsDirChanged())
    assign(:current_server_health_states, ServerHealthStates.new([third]))

    render :partial => "server/messages_body.html.erb"

    body = response.body
    expect(body).to_not have_selector('.error')
    Capybara.string(body).all('.warning').tap do |warnings|
      assert_message_and_desc warnings[0], third.getMessageWithTimestamp(), 'third description'
    end
  end

  it "should not render errors or warnings when there are none" do
    assign(:current_server_health_states, ServerHealthStates.new([]))

    render :partial => "server/messages_body.html.erb"

    body = response.body
    expect(body).to_not have_selector('.error')
    expect(body).to_not have_selector('.warning')
  end

  it "should not render warnings when there are no warnings" do
    first = ServerHealthState.error("first error", "first description", HealthStateType.invalidConfig())
    second = ServerHealthState.error("second error", "second description", HealthStateType.invalidConfig())
    assign(:current_server_health_states, ServerHealthStates.new([first, second]))

    render :partial => "server/messages_body.html.erb"

    body = response.body
    Capybara.string(body).all('.error').tap do |errors|
      assert_message_and_desc errors[0], first.getMessageWithTimestamp(), 'first description'
      assert_message_and_desc errors[1], second.getMessageWithTimestamp(), 'second description'
    end
    expect(body).to_not have_selector('.warning')
  end

  def assert_message_and_desc tag, message, desc
    expect(tag).to have_selector('.message', message)
    expect(tag).to have_selector('.description', desc)
  end
end
