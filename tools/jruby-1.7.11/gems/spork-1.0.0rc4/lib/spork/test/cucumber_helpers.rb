SPORK_CUCUMBER_DIR = File.expand_path( "../../../features/", File.dirname(__FILE__))
require("#{SPORK_CUCUMBER_DIR}/support/background_job.rb")
require("#{SPORK_CUCUMBER_DIR}/support/spork_world.rb")
require("#{SPORK_CUCUMBER_DIR}/support/bundler_helpers.rb")
Dir.glob("#{SPORK_CUCUMBER_DIR}/steps/**/*.rb").each { |f| load f }
