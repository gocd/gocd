ENV["BUNDLE_GEMFILE"] ||= File.expand_path("../Gemfile", __dir__)

# When running in production (NOT just building FOR production), force bundler to ignore groups including the assets pipeline stuff
if ENV["RAILS_ENV"] == "production" and not ENV.has_key?("RAILS_GROUPS")
  require "bundler"
  Bundler.setup(:default)
else
  require "bundler/setup"
end
