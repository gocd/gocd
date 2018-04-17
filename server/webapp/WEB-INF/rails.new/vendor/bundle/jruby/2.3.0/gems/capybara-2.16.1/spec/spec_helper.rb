# frozen_string_literal: true
require 'rspec/expectations'
require "capybara/spec/spec_helper"
require "pry"

RSpec.configure do |config|
  Capybara::SpecHelper.configure(config)
  config.filter_run_including focus_: true unless ENV['TRAVIS']
  config.run_all_when_everything_filtered = true
end

def rspec2?
  !defined?(::RSpec::Expectations::Version) || (Gem::Version.new(RSpec::Expectations::Version::STRING) < Gem::Version.new('3.0'))
end
