ENV["BUNDLE_GEMFILE"] ||= File.expand_path("../Gemfile", __dir__)

# When running in production (NOT just building FOR production), force bundler to ignore groups including the assets pipeline stuff
if ENV["RAILS_ENV"] == "production" and not ENV.has_key?("RAILS_GROUPS")
  require "bundler"
  Bundler.setup(:default)
else
  require "bundler/setup"
end

# workaround JRuby 9.4.13.0 issue with the require used within win32/registry.rb, used by resolv.rb via net/http and Capybara
# See https://github.com/jruby/jruby/issues/8866
if ENV["RAILS_ENV"] == "test" and ::RbConfig::CONFIG['host_os'] =~ /mswin/
  module Kernel
    alias_method :original_require, :require

    def require(path)
      original_require(path == 'Win32API' ? 'win32api' : path)
    end
  end
end

