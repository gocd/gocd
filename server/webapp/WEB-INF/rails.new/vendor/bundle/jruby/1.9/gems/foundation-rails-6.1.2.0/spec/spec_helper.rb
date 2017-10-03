require 'capybara/rspec'
require 'bundler/setup'

Bundler.require(:default, :test)

Dir['./spec/support/**/*.rb'].each { |file| require file }

RSpec.configure do |config|
  config.include FoundationRailsTestHelpers

  config.before(:all) do
    create_dummy_app
    install_foundation
  end

  config.after(:all) do
    remove_dummy_app
  end
end
