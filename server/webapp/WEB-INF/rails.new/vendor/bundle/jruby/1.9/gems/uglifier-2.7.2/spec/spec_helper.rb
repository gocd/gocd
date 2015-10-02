# encoding: UTF-8
require 'uglifier'
require 'rspec'
require 'source_map'

# Requires supporting files with custom matchers and macros, etc,
# in ./support/ and its subdirectories.
Dir["#{File.dirname(__FILE__)}/support/**/*.rb"].each { |f| require f }

if ENV["ALASKA"]
  require 'alaska'
  require 'tempfile'
  ExecJS.runtime = Alaska::Runtime.new
end

RSpec.configure do |config|
  config.mock_with :rspec do |mock|
    mock.syntax = :expect
  end
  config.expect_with :rspec do |expect|
    expect.syntax = :expect
  end
end
