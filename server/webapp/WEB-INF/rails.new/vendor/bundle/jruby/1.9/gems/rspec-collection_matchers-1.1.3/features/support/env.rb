unless defined?(RSpec::Matchers.all)
  module RSpec
    module Matchers
      # aruba assumes this is defined
      def all
      end

      # aruba doesn't alias this itself on 2.99
      def an_output_string_including(partial)
        match partial
      end
    end
  end
end

require 'aruba/cucumber'

Before do
  if RUBY_PLATFORM =~ /java/
    @aruba_timeout_seconds = 30
  else
    @aruba_timeout_seconds = 10
  end
end

Aruba.configure do |config|
  config.before_cmd do |cmd|
    set_env('JRUBY_OPTS', "-X-C #{ENV['JRUBY_OPTS']}") # disable JIT since these processes are so short lived
  end
end if RUBY_PLATFORM == 'java'

Aruba.configure do |config|
  config.before_cmd do |cmd|
    set_env('RBXOPT', "-Xint=true #{ENV['RBXOPT']}") # disable JIT since these processes are so short lived
  end
end if defined?(Rubinius)
