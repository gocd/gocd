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
