require 'rspec/core'

RSpec::configure do |c|
  c.backtrace_exclusion_patterns << /vendor\//
  c.backtrace_exclusion_patterns << /lib\/rspec\/rails/
end

require 'rails/version'
require 'rspec/rails/extensions'
require 'rspec/rails/view_rendering'
require 'rspec/rails/adapters'
require 'rspec/rails/matchers'
require 'rspec/rails/fixture_support'

# Load rspec-activemodel-mocks if available; otherwise fall back
# to the deprecated mock methods
require 'rspec/rails/mocks'
require 'rspec/rails/module_inclusion'
require 'rspec/rails/example'
require 'rspec/rails/infer_type_configuration'
require 'rspec/rails/vendor/capybara'
require 'rspec/rails/vendor/webrat'
