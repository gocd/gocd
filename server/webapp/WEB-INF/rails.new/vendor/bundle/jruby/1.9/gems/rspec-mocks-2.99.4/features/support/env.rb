require 'aruba/cucumber'
require 'rspec/expectations'

Before do
  RUBY_PLATFORM =~ /java/ ? @aruba_timeout_seconds = 60 : @aruba_timeout_seconds = 5
end

Aruba.configure do |config|
  config.before_cmd do |cmd|
    set_env('JRUBY_OPTS', "-X-C #{ENV['JRUBY_OPTS']}") # disable JIT since these processes are so short lived
  end
end if RUBY_PLATFORM == 'java'

