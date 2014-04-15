require 'aruba/cucumber'
require 'rspec/expectations'

Before do
  RUBY_PLATFORM =~ /java/ ? @aruba_timeout_seconds = 60 : @aruba_timeout_seconds = 5
end
