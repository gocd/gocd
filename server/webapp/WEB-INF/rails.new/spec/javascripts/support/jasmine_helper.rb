##########################GO-LICENSE-START################################
# Copyright 2017 ThoughtWorks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##########################GO-LICENSE-END##################################
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

