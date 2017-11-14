require 'xpath'
require 'pry'

RSpec.configure do |config|
  config.expect_with(:rspec) { |c| c.syntax = :should }
end
