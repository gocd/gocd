require 'rubygems'

class Gem::Platform
  @local = new(ENV['BUNDLER_SPEC_PLATFORM']) if ENV['BUNDLER_SPEC_PLATFORM']
end

if ENV['BUNDLER_SPEC_VERSION']
  module Bundler
    VERSION = ENV['BUNDLER_SPEC_VERSION'].dup
  end
end

class Object
  if ENV['BUNDLER_SPEC_RUBY_ENGINE']
    remove_const :RUBY_ENGINE if defined?(RUBY_ENGINE)
    RUBY_ENGINE = ENV['BUNDLER_SPEC_RUBY_ENGINE']

    if RUBY_ENGINE == "jruby"
      JRUBY_VERSION = ENV["BUNDLER_SPEC_RUBY_ENGINE_VERSION"]
    end
  end
end
