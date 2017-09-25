require 'jasmine_rails/save_fixture'

# configure Rspec environment to load JasmineRails helpers
# usage in spec_helper.rb:
#   require 'jasmine_rails/rspec'
RSpec.configure do |config|
  config.include JasmineRails::SaveFixture
end
