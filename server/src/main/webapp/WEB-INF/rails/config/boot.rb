ENV["BUNDLE_GEMFILE"] ||= File.expand_path("../Gemfile", __dir__)

# When running in production (NOT just building FOR production), force bundler to ignore groups including the assets pipeline stuff
if ENV["RAILS_ENV"] == "production" and not ENV.has_key?("RAILS_GROUPS")
  require "bundler"
  Bundler.setup(:default)
else
  require "bundler/setup"
end

# workaround for https://github.com/ruby-concurrency/concurrent-ruby/issues/1077 since https://github.com/rails/rails/pull/54264 wont be backported to Rails 7.0. Fixed on 7.1.
require "logger"
