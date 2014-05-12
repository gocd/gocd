require 'rubygems'
require 'pathname'
require 'rspec/expectations'
require 'timeout'

APP_ROOT    = Pathname.new(File.expand_path('../../', File.dirname(__FILE__)))
SANDBOX_DIR = APP_ROOT + "tmp/sandbox"

require(APP_ROOT + "features/support/background_job.rb")
require(APP_ROOT + "features/support/spork_world.rb")

World do
  SporkWorld.new
end

# FileUtils.rm_rf SporkWorld::SANDBOX_DIR
Before { reset_sandbox_dir }
After  { terminate_background_jobs }
