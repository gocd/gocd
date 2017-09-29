require 'active_model'

# to avoid deprecation warning:
# [deprecated] I18n.enforce_available_locales will default to true in the future. If you really want to skip validation of your locale you can set I18n.enforce_available_locales = false to avoid this message.
I18n.enforce_available_locales = false

require 'rspec/collection_matchers'

Dir['./spec/support/**/*'].each {|f| require f}

RSpec.configure do |config|
  config.treat_symbols_as_metadata_keys_with_true_values = true if RSpec::Expectations::Version::STRING < "3.0"
  config.run_all_when_everything_filtered = true
  config.filter_run :focus

  config.order = 'random'

  config.expect_with :rspec do |rspec|
    rspec.syntax = :expect
  end

  config.mock_with :rspec do |rspec|
    rspec.syntax = :expect
  end
end

shared_context "with #should enabled", :uses_should do
  orig_syntax = nil

  before(:all) do
    orig_syntax = RSpec::Matchers.configuration.syntax
    RSpec::Matchers.configuration.syntax = [:expect, :should]
  end

  after(:all) do
    RSpec::Matchers.configuration.syntax = orig_syntax
  end
end

shared_context "with #should exclusively enabled", :uses_only_should do
  orig_syntax = nil

  before(:all) do
    orig_syntax = RSpec::Matchers.configuration.syntax
    RSpec::Matchers.configuration.syntax = :should
  end

  after(:all) do
    RSpec::Matchers.configuration.syntax = orig_syntax
  end
end

shared_context "with #expect exclusively enabled", :uses_only_expect do
  orig_syntax = nil

  before(:all) do
    orig_syntax = RSpec::Matchers.configuration.syntax
    RSpec::Matchers.configuration.syntax = :expect
  end

  after(:all) do
    RSpec::Matchers.configuration.syntax = orig_syntax
  end
end
