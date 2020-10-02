#
# Copyright 2020 ThoughtWorks, Inc.
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

require 'rails_helper'

describe "Log4jLogger" do
  before(:each) do
    @message = SecureRandom.hex
    @integer_message = SecureRandom.random_number(100000000)
    logger_name = SecureRandom.hex
    @test_appender = LogFixture.logFixtureForLogger(logger_name)
    @logger = Log4jLogger::Logger.new(logger_name)
  end

  after(:each) do
    @test_appender.close
  end

  it "test_should_log_debugging_message_when_debugging" do
    @logger.java_logger.level = Java::ch.qos.logback.classic.Level::DEBUG
    @logger.debug(@message)
    expect(@test_appender.getLog).to include(@message)
  end

  it "test_should_be_able_to_add_debugging_message_directly" do
    @logger.java_logger.level = Java::ch.qos.logback.classic.Level::DEBUG
    @logger.add(Logger::DEBUG, @message)
    expect(@test_appender.getLog).to include(@message)
  end

  it "test_should_not_log_debug_messages_when_log_level_is_info" do
    @logger.java_logger.level = Java::ch.qos.logback.classic.Level::INFO
    @logger.debug(@message)
    expect(@test_appender.getLog).not_to include(@message)
  end

  it "test_should_add_message_passed_as_block_when_using_add" do
    @logger.java_logger.level = Java::ch.qos.logback.classic.Level::INFO
    @logger.info { @message }
    expect(@test_appender.getLog).to include(@message)
  end

  it "test_should_convert_message_to_string" do
    @logger.java_logger.level = Java::ch.qos.logback.classic.Level::INFO
    @logger.info @integer_message
    expect(@test_appender.getLog).to include(@integer_message.to_s)
  end

  it "test_should_convert_message_to_string_when_passed_in_block" do
    @logger.java_logger.level = Java::ch.qos.logback.classic.Level::INFO
    @logger.info { @integer_message }
    expect(@test_appender.getLog).to include(@integer_message.to_s)
  end

  it "test_should_not_evaluate_block_if_message_wont_be_logged" do
    @logger.java_logger.level = Java::ch.qos.logback.classic.Level::INFO
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
    @logger.java_logger.level = Java::ch.qos.logback.classic.Level::WARN

    expect(@logger.error?).to eq(true)
    expect(@logger.warn?).to eq(true)
    expect(@logger.debug?).to eq(false)
    expect(@logger.info?).to eq(false)
  end

  it "test_should_log_warn_level_message_for_unknown" do
    @logger.java_logger.level = Java::ch.qos.logback.classic.Level::WARN
    @logger.unknown(@message)
    expect(@test_appender.getLog).to include("WARN")
  end
end
