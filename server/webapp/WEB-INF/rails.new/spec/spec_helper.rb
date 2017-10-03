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

ENV["RAILS_ENV"] ||= 'test'
require File.expand_path("../../config/environment", __FILE__)
require 'rspec/rails'
require 'rspec/autorun'
require 'capybara/rspec'
require 'rspec-extra-formatters'

# Requires supporting ruby files with custom matchers and macros, etc,
# in spec/support/ and its subdirectories.
Dir[Rails.root.join("spec/support/**/*.rb")].each { |f| require f }

Dir["#{File.dirname(__FILE__)}/util/*.rb"].each { |f| load f }

# make sure that we capture the default headers before any tests mess it up.
$rack_default_headers = Rack::MockRequest::DEFAULT_ENV.dup

RSpec.configure do |config|
  # Use color not only in STDOUT but also in pagers and files
  config.tty   = true
  config.color = true

  # config.add_formatter :documentation
  config.add_formatter :documentation
  # config.add_formatter 'TapFormatter',   File.join(ENV['REPORTS_DIR'] || Rails.root.join('tmp/reports'), 'spec_full_report')
  config.add_formatter 'JUnitFormatter', File.join(ENV['REPORTS_DIR'] || Rails.root.join('tmp/reports'), 'spec_full_report.xml')

  # If true, the base class of anonymous controllers will be inferred
  # automatically. This will be the default behavior in future versions of
  # rspec-rails.
  config.infer_base_class_for_anonymous_controllers = false

  # config.backtrace_exclusion_patterns = []

  config.include ApiSpecHelper
  # Run specs in random order to surface order dependencies. If you find an
  # order dependency and want to debug it, you can fix the order by providing
  # the seed, which is printed after each run.
  #     --seed 1234
  config.order = "random"

  # clear flash messages for every spec
  config.before(:each) do
    com.thoughtworks.go.server.web.FlashMessageService.useFlash(com.thoughtworks.go.server.web.FlashMessageService::Flash.new)
    setup_base_urls
  end

  config.after(:each) do
    ServiceCacheStrategy.instance.clear_services
  end

  config.mock_with :rspec do |mocks|
    # In RSpec 3, `any_instance` implementation blocks will be yielded the receiving
    # instance as the first block argument to allow the implementation block to use
    # the state of the receiver.
    # In RSpec 2.99, to maintain compatibility with RSpec 3 you need to either set
    # this config option to `false` OR set this to `true` and update your
    # `any_instance` implementation blocks to account for the first block argument
    # being the receiving instance.
    mocks.yield_receiver_to_any_instance_implementation_blocks = true
  end

  # rspec-rails 3 will no longer automatically infer an example group's spec type
  # from the file location. You can explicitly opt-in to the feature using this
  # config option.
  # To explicitly tag specs without using automatic inference, set the `:type`
  # metadata manually:
  #
  #     describe ThingsController, :type => :controller do
  #       # Equivalent to being in spec/controllers
  #     end
  config.infer_spec_type_from_file_location!
  config.mock_with :rspec do |mocks|
    mocks.yield_receiver_to_any_instance_implementation_blocks = true
  end
end

def stub_localized_result
  result = com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult.new
  allow(com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult).to receive(:new).and_return(result)
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


include JavaImports
include JavaSpecImports
include CacheStoreForTest
include FixtureTestHelpers
include ExtraSpecAssertions
include CacheTestHelpers
include MiscSpecExtensions
include GoCDCustomMatchers
