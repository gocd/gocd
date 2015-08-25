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

describe "Log4jLogger" do
  before(:each) do
    @message = 'A debug message'
    @integer_message = 12345
    @test_appender = LogFixture.startListening
    @logger = Log4jLogger.new
  end

  after(:each) do
    @test_appender.stopListening
  end

  it "test_should_log_debugging_message_when_debugging" do
    @logger.level = Logger::DEBUG
    @logger.debug(@message)
    expect(@test_appender.getMessages.include?(@message)).to eq(true)
  end

  it "test_should_be_able_to_add_debugging_message_directly" do
    @logger.level = Logger::DEBUG
    @logger.add(Logger::DEBUG, @message)
    expect(@test_appender.getMessages.include?(@message)).to eq(true)
  end

  it "test_should_not_log_debug_messages_when_log_level_is_info" do
    @logger.level = Logger::INFO
    @logger.debug(@message)
    expect(@test_appender.getMessages.include?(@message)).to eq(false)
  end

  it "test_should_add_message_passed_as_block_when_using_add" do
    @logger.level = Logger::INFO
    @logger.info { @message }
    expect(@test_appender.getMessages.include?(@message)).to eq(true)
  end

  it "test_should_convert_message_to_string" do
    @logger.level = Logger::INFO
    @logger.info @integer_message
    expect(@test_appender.getMessages.include?(@integer_message.to_s)).to eq(true)
  end

  it "test_should_convert_message_to_string_when_passed_in_block" do
    @logger.level = Logger::INFO
    @logger.info { @integer_message }
    expect(@test_appender.getMessages.include?(@integer_message.to_s)).to eq(true)
  end

  it "test_should_not_evaluate_block_if_message_wont_be_logged" do
    @logger.level = Logger::INFO
    evaluated = false
    @logger.debug { evaluated = true }
    expect(evaluated).to eq(false)
  end

  it "test_should_not_mutate_message" do
    message_copy = @message.dup
    @logger.info @message
    expect(message_copy).to eq(@message)
  end

  it "test_should_know_if_its_loglevel_is_below_a_given_level" do
    Log4jLogger::Severity.constants.each do |level|
      @logger.level = Log4jLogger::Severity.const_get(level) - 1
      expect(@logger.send("#{level.downcase}?")).to eq(true)
    end
  end

  it "test_should_log_warn_level_message_for_unknown" do
    @logger.level = Logger::ERROR
    @logger.unknown(@message)
    actual = @test_appender.allLogs
    expect(actual).to include("WARN")
  end

end
