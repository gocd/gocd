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

require 'rubygems'
require 'spork'
require 'rexml/document'

ENV["RAILS_ENV"] ||= 'test'
require File.expand_path("../../config/environment", __FILE__)
require 'rspec/rails'
require 'rspec/autorun'
require 'capybara/rspec'

# Requires supporting ruby files with custom matchers and macros, etc,
# in spec/support/ and its subdirectories.
Dir[Rails.root.join("spec/support/**/*.rb")].each { |f| require f }

Dir["#{File.dirname(__FILE__)}/util/*.rb"].each { |f| load f }

RSpec.configure do |config|
  # If true, the base class of anonymous controllers will be inferred
  # automatically. This will be the default behavior in future versions of
  # rspec-rails.
  config.infer_base_class_for_anonymous_controllers = false

  # Run specs in random order to surface order dependencies. If you find an
  # order dependency and want to debug it, you can fix the order by providing
  # the seed, which is printed after each run.
  #     --seed 1234
  config.order = "random"

  # clear flash messages for every spec
  config.before(:each) do
    com.thoughtworks.go.server.web.FlashMessageService.useFlash(com.thoughtworks.go.server.web.FlashMessageService::Flash.new)
  end

  config.after(:each) do
    ServiceCacheStrategy.instance.clear_services
  end
end

ApplicationController.class_eval do
  def should_receive_render_with(*expected)
    self.should_receive(:render).with(*expected) do |*actual|
      actual.should == expected
      @performed_render = true
    end
  end

  def should_receive_redirect_to(expected_url)
    self.should_receive(:redirect_to).with(expected_url) do |actual_url|
      actual_url.should =~ expected_url
      @performed_redirect = true
    end
  end
end

ActionController::TestCase.class_eval do
  setup :base_urls

  def base_urls
    setup_base_urls
  end
end

def stub_localized_result
  result = com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult.new
  com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult.stub(:new).and_return(result)
  result
end

def uuid_pattern
  hex = "[a-f0-9]"
  "#{hex}{8}-#{hex}{4}-#{hex}{4}-#{hex}{4}-#{hex}{12}"
end

def with_caching(perform_caching)
  old_perform_caching = ActionController::Base.perform_caching
  begin
    ActionController::Base.perform_caching = perform_caching
    yield
  ensure
    ActionController::Base.perform_caching = old_perform_caching
  end
end


include SporkConfig
include JavaImports
include JavaSpecImports
include CacheStoreForTest
include FixtureTestHelpers
include ExtraSpecAssertions
include CacheTestHelpers
include MiscSpecExtensions