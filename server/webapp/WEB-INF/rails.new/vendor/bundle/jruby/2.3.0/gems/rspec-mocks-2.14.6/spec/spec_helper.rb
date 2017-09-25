require 'yaml'
begin
  require 'psych'
rescue LoadError
end

RSpec::Matchers.define :include_method do |expected|
  match do |actual|
    actual.map { |m| m.to_s }.include?(expected.to_s)
  end
end

module VerifyAndResetHelpers
  def verify(object)
    RSpec::Mocks.proxy_for(object).verify
  end

  def reset(object)
    RSpec::Mocks.proxy_for(object).reset
  end
end

RSpec.configure do |config|
  config.mock_with :rspec
  config.color_enabled = true
  config.order = :random
  config.run_all_when_everything_filtered = true
  config.treat_symbols_as_metadata_keys_with_true_values = true
  config.filter_run_including :focus

  config.expect_with :rspec do |expectations|
    expectations.syntax = :expect
  end

  old_verbose = nil
  config.before(:each, :silence_warnings) do
    old_verbose = $VERBOSE
    $VERBOSE = nil
  end

  config.after(:each, :silence_warnings) do
    $VERBOSE = old_verbose
  end

  config.include VerifyAndResetHelpers
end

shared_context "with syntax" do |syntax|
  orig_syntax = nil

  before(:all) do
    orig_syntax = RSpec::Mocks.configuration.syntax
    RSpec::Mocks.configuration.syntax = syntax
  end

  after(:all) do
    RSpec::Mocks.configuration.syntax = orig_syntax
  end
end

