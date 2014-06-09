##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
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

require 'rubygems'
require 'spork'
require 'rexml/document'

#uncomment the following line to use spork with the debugger
#require 'spork/ext/ruby-debug'

Spork.prefork do
  # Loading more in this block will cause your tests to run faster. However,
  # if you change any configuration or code from libraries loaded here, you'll
  # need to restart spork for it take effect.

end

Spork.each_run do
  # This code will be run each time you run your specs.

end

# This file is copied to spec/ when you run 'rails generate rspec:install'
ENV["RAILS_ENV"] ||= 'test'
require File.expand_path("../../config/environment", __FILE__)
require 'rspec/rails'
require 'rspec/autorun'
require 'capybara/rspec'
load 'spec/java_spec_imports.rb'

# Requires supporting ruby files with custom matchers and macros, etc,
# in spec/support/ and its subdirectories.
Dir[Rails.root.join("spec/support/**/*.rb")].each { |f| require f }

Dir["#{File.dirname(__FILE__)}/util/*.rb"].each { |f| load f }

RSpec.configure do |config|
  # If true, the base class of anonymous controllers will be inferred
  # automatically. This will be the default behavior in future versions of
  # rspec-rails.
  config.infer_base_class_for_anonymous_controllers = false

  # Run specs in random order to surface order dependencies. If you find an
  # order dependency and want to debug it, you can fix the order by providing
  # the seed, which is printed after each run.
  #     --seed 1234
  config.order = "random"

  # clear flash messages for every spec
  config.before(:each) do
    com.thoughtworks.go.server.web.FlashMessageService.useFlash(com.thoughtworks.go.server.web.FlashMessageService::Flash.new)
  end

  #config.include Capybara::DSL
end

include JavaImports
include JavaSpecImports

def java_date_utc(year, month, day, hour, minute, second)
  org.joda.time.DateTime.new(year, month, day, hour, minute, second, 0, org.joda.time.DateTimeZone::UTC).toDate()
end

def stub_server_health_messages
  assign(:current_server_health_states, com.thoughtworks.go.serverhealth.ServerHealthStates.new)
end

def stub_server_health_messages_for_controllers
  assigns[:current_server_health_states] = com.thoughtworks.go.serverhealth.ServerHealthStates.new
end

unless $has_loaded_one_time_enhancements
  GoCacheStore.class_eval do
    def write_with_recording(name, value, options = nil)
      writes[key(name, options)] = value
      write_without_recording(name, value, options)
    end

    alias_method_chain :write, :recording

    def read_with_recording(name, options = nil)
      value = read_without_recording(name, options)
      reads[key(name, options)] = value
    end

    alias_method_chain :read, :recording

    def clear_with_recording
      clear_without_recording
      writes.clear
      reads.clear
    end

    alias_method_chain :clear, :recording

    def writes
      @writes ||= {}
    end

    def reads
      @reads ||= {}
    end
  end

  $has_loaded_one_time_enhancements = true
end

def current_user
  @user ||= com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new("some-user"), "display name")
  @controller.stub(:current_user).and_return(@user)
  @user
end

def assert_redirected_with_flash(url, msg, flash_class, params = [])
  assert_redirect(url)
  params.each { |param| response.redirect_url.should =~ /#{param}/ }
  flash_guid = $1 if response.redirect_url =~ /[?&]fm=([\w-]+)?(&.+){0,}$/
  flash = controller.flash_message_service.get(flash_guid)
  flash.to_s.should == msg
  flash.flashClass().should == flash_class
end

def assert_redirect(url)
  response.status.should == 302
  response.redirect_url.should =~ %r{#{url}}
end


def cdata_wraped_regexp_for(value)
  /<!\[CDATA\[#{value}\]\]>/
end

def setup_base_urls
  config_service = Spring.bean("goConfigService")
  if (config_service.currentCruiseConfig().server().getSiteUrl().getUrl().nil?)
    config_service.updateConfig(Class.new do
      def update config
        server = config.server()
        com.thoughtworks.go.util.ReflectionUtil.setField(server, "siteUrl", com.thoughtworks.go.domain.ServerSiteUrlConfig.new("http://test.host"))
        com.thoughtworks.go.util.ReflectionUtil.setField(server, "secureSiteUrl", com.thoughtworks.go.domain.ServerSiteUrlConfig.new("https://ssl.host:443"))
        return config
      end
    end.new)
  end
end

def check_fragment_caching(obj1, obj2, cache_key_proc)
  ActionController::Base.cache_store.clear
  ActionController::Base.perform_caching = false

  yield obj1
  obj_1_not_cached_body = response.body
  ActionController::Base.cache_store.writes.length.should == 0
  allow_double_render
  ActionController::Base.cache_store.read(*cache_key_proc[obj2]).should be_nil
  ActionController::Base.perform_caching = true

  yield obj2
  ActionController::Base.cache_store.read(*cache_key_proc[obj2]).should_not be_nil
  ActionController::Base.cache_store.writes.length.should == 1
  allow_double_render

  yield obj2
  ActionController::Base.cache_store.writes.length.should == 1
  allow_double_render

  ActionController::Base.cache_store.read(*cache_key_proc[obj1]).should be_nil
  yield obj1
  ActionController::Base.cache_store.writes.length.should == 2
  ActionController::Base.cache_store.read(*cache_key_proc[obj1]).should_not be_nil
  assert_equal obj_1_not_cached_body, response.body
ensure
  ActionController::Base.perform_caching = false
end

# erase_results does not exist, in Rails 3 and above.
# https://github.com/markcatley/responds_to_parent/pull/2/files
# http://www.dixis.com/?p=488
def allow_double_render
  self.instance_variable_set(:@_response_body, nil)
end

def fake_template_presence file_path, content
  controller.prepend_view_path(ActionView::FixtureResolver.new(file_path => content))
end

def stub_service_on(obj, service_getter)
  service = double(service_getter.to_s.camelize)
  obj.stub(service_getter).and_return(service)
  service
end

def stub_service(service_getter)
  stub_service_on(@controller, service_getter)
end

# TODO: SBD: Move out to a new file.
RSpec::Matchers.define :be_nil_or_empty do
  match do |actual|
    actual.nil? or actual.size == 0
  end
end

# TODO: SBD: DEFINITELY move this
def assert_fixture_equal(fixture, response)
  generated = extract_test("<div class='under_test'>" + response + "</div>")
  jsunit = extract_test(File.read(File.join(Rails.root, "..", "..", "..", "jsunit", "tests", fixture)))

#      File.open('/tmp/jsunit.xml', 'w') {|f| f.write(jsunit) }
#      File.open('/tmp/generated.xml', 'w') {|f| f.write(generated) }
#      puts `diff /tmp/generated.xml /tmp/jsunit.xml`


  generated.gsub(/\s+/, " ").should == jsunit.gsub(/\s+/, " ")
end

#
# Modify HTML to remove time dependent stuff so we can compare HTML files more reliably
#
def extract_test(xml)
  xml.gsub!(/[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}/, "UUID")
  xml.gsub!(/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(([-+]\d{2}:\d{2})|Z)/, "REPLACED_DATE")
  xml.gsub!(/\w{3} \w{3} \d{2} \d{2}:\d{2}:\d{2} \w{3} \d{4}/, "REPLACED_DATE_TIME")
  xml.gsub!(/\w{3} \w{3} \d{2} \d{2}:\d{2}:\d{2} \w{3}\+\d{2}:\d{2} \d{4}/, "REPLACED_DATE_TIME")
  xml.gsub!(/\d{13}/, "REPLACED_DATE_TIME_MILLIS")
  xml.gsub!(/[\d\w\s]*?\sminute[s]*\sago\s*/, "REPLACED_RELATIVE_TIME")
  xml.gsub!(/[\d\w\s]*?\shour[s]*\sago\s*/, "REPLACED_RELATIVE_TIME")
  xml.gsub!(/[\d\w\s]*?\sday[s]*\sago\s*/, "REPLACED_RELATIVE_TIME")
  xml.gsub!(/\s+/m, " ")
  xml.gsub!(/Windows 2003/, "OPERATING SYSTEM")
  xml.gsub!(/Linux/, "OPERATING SYSTEM")
  xml.gsub!(/SunOS/, "OPERATING SYSTEM")
  xml.gsub!(/Mac OS X/, "OPERATING SYSTEM")
  xml.gsub!(/<script.*?<\/script>/m, "")

  resp_doc = REXML::Document.new(xml)
  generated_content = resp_doc.root.elements["//div[@class='under_test']"]

  formatter = REXML::Formatters::Pretty.new
  formatter.compact = true
  out = ""
  formatter.write(generated_content, out)
  out
end
# END TODO: SBD: DEFINITELY move this
