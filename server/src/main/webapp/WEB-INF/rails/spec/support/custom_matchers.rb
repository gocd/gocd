#
# Copyright 2024 Thoughtworks, Inc.
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
#

RSpec::Matchers.define :receive_render_with do |args|
  match do |controller|
    expect(controller).to receive(:render) do |actual|
      expect(actual).to eq(args)
    end
  end
end

class ReachedControllerError < StandardError
end

RSpec::Matchers.define :allow_action do |verb, expected_action, **args|
  match do |controller|
    @reached_controller = false
    allow(controller).to receive(expected_action).and_raise(ReachedControllerError)
    begin
      send(verb, expected_action, **args)
    rescue => ReachedControllerError
      # ignore
      @reached_controller = true
    rescue => e
      @reached_controller = false
      @exception          = e
    end

    @reached_controller && !@exception
  end

  failure_message do |controller|
    messages = []
    if !@reached_controller
      messages << "expected `#{controller}` to reach action #{verb.to_s.upcase} :#{expected_action.to_sym}, but did not."
    end

    if @exception
      messages << "An exception was raised #{@exception.message}."
    end

    messages.join("\n")
  end
end

RSpec::Matchers.define :disallow_action do |verb, expected_action, **args|
  chain :with do |expected_status, expected_message|
    @status_matcher  = RSpec::Matchers::BuiltIn::Eq.new(expected_status)
    @message_matcher = RSpec::Matchers::BuiltIn::Eq.new(expected_message)
  end

  match do |controller|
    @reached_controller = false
    allow(controller).to receive(expected_action).and_raise(ReachedControllerError)
    begin
      send(verb, expected_action, **args)
    rescue => ReachedControllerError
      # ignore
      @reached_controller = true
    rescue => e
      @reached_controller = false
      @exception          = e
    end

    failed = @reached_controller || @exception

    if @status_matcher && !@status_matcher.matches?(response.status)
      failed = true
      @failed_with_bad_status = true
    end

    if @message_matcher && !@message_matcher.matches?(JSON.parse(response.body)['message'])
      failed = true
      @failed_with_bad_message = true
    end

    !failed
  end

  failure_message do |controller|
    messages = []
    if @reached_controller
      messages << "expected `#{controller}` to not reach action #{verb.to_s.upcase} :#{expected_action.to_sym}."
    end

    if @exception
      messages << "An exception was raised #{@exception.message}."
    end

    if @failed_with_bad_status
      messages << @status_matcher.failure_message_for_should
    end

    if @failed_with_bad_message
      messages << @message_matcher.failure_message_for_should
    end

    messages.join("\n")
  end
end
