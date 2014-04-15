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

describe "/server/messages.json.erb" do
  it "should render errors if there are any in the header" do
    first = ServerHealthState.error("first error", "first description", HealthStateType.invalidConfig())
    second = ServerHealthState.error("second error", "second description", HealthStateType.invalidConfig())
    third = ServerHealthState.warning("first warning", "third description", HealthStateType.artifactsDirChanged())
    assigns[:current_server_health_states] = ServerHealthStates.new([first, second, third])

    render "server/messages.json.erb"

    json = JSON.parse(response.body)
    json["cruise_message_counts"]["html"].should have_tag('.messages .error_count', "Errors: 2")
    json["cruise_message_counts"]["html"].should have_tag('.messages .warning_count', "Warnings: 1")

    body = json["cruise_message_body"]["html"]
    body.should have_tag('.error') do |errors|
      assert_message_and_desc errors[0], first.getMessageWithTimestamp(), 'first description'
      assert_message_and_desc errors[1], second.getMessageWithTimestamp(), 'second description'
    end
    body.should have_tag('.warning') do |warning|
      assert_message_and_desc warning, third.getMessageWithTimestamp(), 'third description'
    end
  end

  it "should return empty html if there are no errors or warnings" do
    assigns[:current_server_health_states] = ServerHealthStates.new()

    render "server/messages.json.erb"

    json = JSON.parse(response.body)
    json["cruise_message_counts"]["html"].should == ""
    json["cruise_message_counts"]["html"].should == ""

    json["cruise_message_body"]["html"].should == ""
  end

  def assert_message_and_desc tag, message, desc
    tag.should have_tag('.message', message)
    tag.should have_tag('.description', desc)
  end
end