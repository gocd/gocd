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

# This file is copied to ~/spec when you run 'ruby script/generate rspec'
# from the project root directory.
ENV["RAILS_ENV"] ||= 'test'
require File.dirname(__FILE__) + "/../config/environment"
require 'spec/autorun'
require 'spec/rails'
require 'rexml/document'
load 'spec/java_spec_imports.rb'
require 'pp'

# Requires supporting files with custom matchers and macros, etc,
# in ./support/ and its subdirectories.
Dir["#{File.dirname(__FILE__)}/support/**/*.rb"].each { |f| load f }

Dir["#{File.dirname(__FILE__)}/util/*.rb"].each { |f| load f }

Spec::Runner.configure do |config|
  # If you're not using ActiveRecord you should remove these
  # lines, delete config/database.yml and disable :active_record
  # in your config/boot.rb
#  config.use_transactional_fixtures = true
#  config.use_instantiated_fixtures  = false
#  config.fixture_path = RAILS_ROOT + '/spec/fixtures/'

  # == Fixtures
  #
  # You can declare fixtures for each example_group like this:
  #   describe "...." do
  #     fixtures :table_a, :table_b
  #
  # Alternatively, if you prefer to declare them only once, you can
  # do so right here. Just uncomment the next line and replace the fixture
  # names with your fixtures.
  #
  # config.global_fixtures = :table_a, :table_b
  #
  # If you declare global fixtures, be aware that they will be declared
  # for all of your examples, even those that don't use them.
  #
  # You can also declare which fixtures to use (for example fixtures for test/fixtures):
  #
  # config.fixture_path = RAILS_ROOT + '/spec/fixtures/'
  #
  # == Mock Framework
  #
  # RSpec uses it's own mocking framework by default. If you prefer to
  # use mocha, flexmock or RR, uncomment the appropriate line:
  #
  # config.mock_with :mocha
  # config.mock_with :flexmock
  # config.mock_with :rr
  #
  # == Notes
  #
  # For more information take a look at Spec::Runner::Configuration and Spec::Runner
  config.before(:each, :behaviour_type => :controller) do
    @controller.instance_eval { flash.stub!(:sweep) }
  end
end

include JavaImports
include JavaSpecImports

module Spec
  module Rails
    class SpecServer
      def in_memory_database?
        false
      end
    end
  end
end

def java_date_utc(year, month, day, hour, minute, second)
  org.joda.time.DateTime.new(year, month, day, hour, minute, second, 0, org.joda.time.DateTimeZone::UTC).toDate()
end


def cdata_wraped_regexp_for(value)
  /<!\[CDATA\[#{value}\]\]>/
end

def assert_fixture_equal(fixture, response)
  generated = extract_test("<div class='under_test'>" + response + "</div>")
  jsunit = extract_test(File.read(File.join(RAILS_ROOT, "..", "..", "..", "jsunit", "tests", fixture)))

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

def stub_server_health_messages
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

  [Spec::Rails::Example::ControllerExampleGroup, Spec::Rails::Example::ViewExampleGroup].each do |klass|
    klass.class_eval do
      def setup
        com.thoughtworks.go.server.web.FlashMessageService.useFlash(com.thoughtworks.go.server.web.FlashMessageService::Flash.new)
      end
    end
  end

  $has_loaded_one_time_enhancements = true
end




def with_caching(perform_caching)
  old_perform_caching = ActionController::Base.perform_caching
  begin
    ActionController::Base.perform_caching = perform_caching
    yield
  ensure
    ActionController::Base.perform_caching = old_perform_caching
  end
end

def check_fragment_caching(obj1, obj2, cache_key_proc)
  ActionController::Base.cache_store.clear
  ActionController::Base.perform_caching = false

  yield obj1
  obj_1_not_cached_body = @response.body
  ActionController::Base.cache_store.writes.length.should == 0
  @controller.send :erase_results
  ActionController::Base.cache_store.read(*cache_key_proc[obj2]).should be_nil
  ActionController::Base.perform_caching = true

  yield obj2
  ActionController::Base.cache_store.read(*cache_key_proc[obj2]).should_not be_nil
  ActionController::Base.cache_store.writes.length.should == 1
  @controller.send :erase_results

  yield obj2
  ActionController::Base.cache_store.writes.length.should == 1
  @controller.send :erase_results

  ActionController::Base.cache_store.read(*cache_key_proc[obj1]).should be_nil
  yield obj1
  ActionController::Base.cache_store.writes.length.should == 2
  ActionController::Base.cache_store.read(*cache_key_proc[obj1]).should_not be_nil
  assert_equal obj_1_not_cached_body, @response.body
ensure
  ActionController::Base.perform_caching = false
end

def assert_redirected_with_flash(url, msg, flash_class, params = [])
  assert_redirect(url)
  params.each { |param| response.redirect_url.should =~ /#{param}/ }
  flash_guid = $1 if response.redirect_url =~ /[?&]fm=([\w-]+)?(&.+){0,}$/
  flash = controller.flash_message_service.get(flash_guid)
  flash.to_s.should == msg
  flash.flashClass().should == flash_class
end

def assert_redirected_with_notice(url, msg, flash_class)
  assert_redirect(url)
  session[:notice].should == FlashMessageModel.new(msg, flash_class)
end

def assert_redirect(url)
  response.status.should == "302 Found"
  response.redirect_url.should =~ %r{#{url}}
end

def stub_service_on(obj, service_getter)
  service = mock(service_getter.to_s.camelize)
  obj.stub!(service_getter).and_return(service)
  service
end

def stub_service(service_getter)
  stub_service_on(@controller, service_getter)
end

def current_user
  @user ||= com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new("some-user"), "display name")
  @controller.stub(:current_user).and_return(@user)
  @user
end

def stub_localized_result
  result = com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult.new
  com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult.stub(:new).and_return(result)
  result
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

ApplicationController.class_eval do
  def should_receive_render_with(*expected)
    self.should_receive(:render).with(*expected) do |*actual|
      actual.should == expected
      @performed_render = true
    end
  end

  def should_receive_redirect_to(expected_url)
    self.should_receive(:redirect_to).with(expected_url) do |actual_url|
      actual_url.should =~ expected_url
      @performed_redirect = true
    end
  end
end

ActionController::TestCase.class_eval do
  setup :base_urls

  def base_urls
    setup_base_urls
  end
end

def uuid_pattern
  hex = "[a-f0-9]"
  "#{hex}{8}-#{hex}{4}-#{hex}{4}-#{hex}{4}-#{hex}{12}"
end

Spec::Example::SharedExampleGroup.clear