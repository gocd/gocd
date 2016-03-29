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

describe "/server/messages.json.erb" do
  it "should render errors if there are any in the header" do
    first = ServerHealthState.error("first error", "first description", HealthStateType.invalidConfig())
    second = ServerHealthState.warningWithHtml("second error", 'second description <a>link</a>', HealthStateType.invalidConfig())
    third = ServerHealthState.warning("first warning", "third description", HealthStateType.artifactsDirChanged())
    assign(:current_server_health_states, ServerHealthStates.new([first, second, third]))

    render

    json = JSON.parse(response.body)
    counts = json["cruise_message_counts"]["html"]
    body = json["cruise_message_body"]["html"]

    expect(counts).to have_selector('.messages .error_count', text: "Errors: 1")
    expect(counts).to have_selector('.messages .warning_count', text: "Warnings: 2")

    Capybara.string(body).all('.error').tap do |errors|
      assert_message_and_desc errors[0], first.getMessageWithTimestamp(), 'first description'
    end

    Capybara.string(body).all('.warning').tap do |warnings|
      assert_message_and_desc warnings[0], second.getMessageWithTimestamp(), 'second description link'
      assert_message_and_desc warnings[1], third.getMessageWithTimestamp(), 'third description'
      expect(warnings[0]).to have_selector('.description a', text: 'link')

    end
  end

  it "should return empty html if there are no errors or warnings" do
    assign(:current_server_health_states, ServerHealthStates.new())

    render

    json = JSON.parse(response.body)
    counts = json["cruise_message_counts"]["html"]
    body = json["cruise_message_body"]["html"]

    expect(counts).to eq("")

    expect(body).to eq("")
  end

  def assert_message_and_desc tag, message, desc
    expect(tag).to have_selector('.message', text: message)
    expect(tag).to have_selector('.description', text: desc)
  end
end
