#
# Copyright 2019 ThoughtWorks, Inc.
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
#

Rails.application.configure do
  # Settings specified here will take precedence over those in config/application.rb.

  # The test environment is used exclusively to run your application's
  # test suite. You never need to work with it otherwise. Remember that
  # your test database is "scratch space" for the test suite and is wiped
  # and recreated between test runs. Don't rely on the data there!
  config.cache_classes = true

  # Do not eager load code on boot. This avoids loading your whole application
  # just for the purpose of running a single test. If you are using a tool that
  # preloads Rails for running tests, you may have to set it to true.
  config.eager_load = false

  # Configure public file server for tests with Cache-Control for performance.
  config.public_file_server.enabled = true
  config.public_file_server.headers = {
    'Cache-Control' => "public, max-age=#{1.hour.to_i}"
  }

  config.assets.digest = false

  # Show full error reports and disable caching.
  config.consider_all_requests_local = true
  config.action_controller.perform_caching = false
  # Raise exceptions instead of rendering exception templates.
  config.action_dispatch.show_exceptions = false

  # Disable request forgery protection in test environment.
  config.action_controller.allow_forgery_protection = false

  # Print deprecation notices to the stderr.
  config.active_support.deprecation = :stderr

  # Raises error for missing translations
  # config.action_view.raise_on_missing_translations = true

  config.java_services_cache = :TestServiceCache
end

# Override load_context of Spring for rspec.
import org.springframework.context.support.ClassPathXmlApplicationContext

def Spring.load_context
  ctx_files = Dir[File.expand_path(File.join(Rails.root, "..", '..', '..', 'resources', "applicationContext*.xml"))].map { |path| "/#{File.basename(path)}" }
  ClassPathXmlApplicationContext.new(ctx_files.to_java(:string))
end

require "jasmine_selenium_runner/configure_jasmine"

class WithHeadless < JasmineSeleniumRunner::ConfigureJasmine
  def selenium_options
    options = super

    if browser =~ /^firefox/
      options = super
      options[:options] ||= Selenium::WebDriver::Firefox::Options.new
      options[:options].add_argument '-headless'
    elsif browser =~ /^chrome/
      options = super
      options[:options] ||= Selenium::WebDriver::Chrome::Options.new
      options[:options].add_argument '--headless'
    else
      raise "Don't know how to configure browser: #{browser}"
    end

    options
  end
end
