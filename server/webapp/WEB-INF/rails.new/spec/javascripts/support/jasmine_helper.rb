#Use this file to set/override Jasmine configuration options
#You can remove it if you don't need it.
#This file is loaded *after* jasmine.yml is interpreted.
#
#Example: using a different boot file.
#Jasmine.configure do |config|
#   config.boot_dir = '/absolute/path/to/boot_dir'
#   config.boot_files = lambda { ['/absolute/path/to/boot_dir/file.js'] }
#end
#
#Example: prevent PhantomJS auto install, uses PhantomJS already on your path.
#Jasmine.configure do |config|
#   config.prevent_phantom_js_auto_install = true
#end
#
require 'jasmine'
require 'selenium-webdriver'
require File.expand_path(File.dirname(__FILE__) + '/jasmine_with_selenium_runner')


Jasmine.configure do |config|
  if ENV['BROWSER'] && ENV['BROWSER'] != "phantomjs"
    config.runner = lambda { |formatter, jasmine_server_url|
      filepath = File.join(Dir.pwd, 'spec', 'javascripts', 'support', 'config.yml')
      runner_config = YAML::load(ERB.new(File.read(filepath)).result(binding))
      JasmineWithSeleniumRunner.new(formatter, jasmine_server_url, runner_config)
    }
  end
end


