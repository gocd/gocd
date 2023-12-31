#
# Copyright 2024 Thoughtworks, Inc.
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

require 'rails_helper'
require 'java'

describe ApplicationController do

  before do
    allow(SessionUtils).to receive(:getUserId).and_return(1)
  end

  describe "error_handling" do
    before do
      class << controller
        include ActionRescue
      end
    end

    it "should handle InvalidAuthenticityToken by redirecting to root_url" do
      exception = ActionController::InvalidAuthenticityToken.new
      expect(exception).to receive(:backtrace).and_return([])
      nil.bomb rescue exception

      expect(Rails.logger).to receive(:error).with(anything)
      expect(controller).to receive(:redirect_to).with(:root)

      controller.rescue_action(exception)
    end
  end

  describe "java_routes" do
    include ApplicationHelper
    def params
      {}
    end
    it "should generate run stage url" do
      expect(run_stage_path(pipeline_name: "pipeline_name", stage_name: "stage_name",
                            pipeline_counter: 10)).to eq("/go/api/stages/pipeline_name/10/stage_name/run")
    end
  end

  describe "services" do
    it "should load go_config_service" do
      expect(Spring).to receive(:bean).with('goConfigService').and_return(mock_go_config_service = "MOCK GO CONFIG")
      expect(controller.go_config_service).to eq(mock_go_config_service)
    end

    it "should load pipeline_history_service" do
      expect(Spring).to receive(:bean).with('pipelineHistoryService').and_return(mock_phs = Object.new)
      expect(controller.pipeline_history_service).to eq(mock_phs)
    end

    it "should load schedule_service" do
      expect(Spring).to receive(:bean).with('scheduleService').and_return(service = Object.new)
      expect(controller.schedule_service).to eq(service)
    end

    it "should load flash_message_service" do
      expect(controller.flash_message_service).to be_a(com.thoughtworks.go.server.web.FlashMessageService)
    end
  end

  describe "default_as_empty_list" do
    it "should default given params as empty list only if not given" do
      @controller.params = HashWithIndifferentAccess.new
      @controller.params[:default_as_empty_list] = ["foo", "bar>baz", "quux>bang>boom", "user>name", "hello"]
      @controller.params[:hi] = "bye"
      @controller.params[:hello] = "world"
      @controller.params[:user] = {:name => "foo"}
      expect(@controller.send(:default_as_empty_list)).to be_truthy
      expect(@controller.params).to eq(HashWithIndifferentAccess.new({:hi => "bye", :hello => "world", :user => {:name => "foo"}, :foo => [], :bar => {:baz => []}, :quux => {:bang => {:boom => []}}}))
    end

    it "should always return true, because it needs to be used as a filter" do
      expect(@controller.send(:default_as_empty_list)).to eq(true)
      @controller.params[:default_as_empty_list] = ["foo", "bar>baz"]
      @controller.params[:hello] = "world"
      expect(@controller.send(:default_as_empty_list)).to eq(true)
    end
  end

  describe "do for every request" do
    controller do
      def index
        render plain: "Hello"
      end
    end

    it "should populate the config file validity for every request" do
      get :index

      expect(assigns[:config_valid]).to eq(true)
    end
  end

  context "with license agent validity stubbed" do
    controller do
      def test_action
        render plain: "Some test action"
      end
    end

    before(:each) do
      @routes.draw do
        get "/anonymous/test_action"
      end
    end

    it "should set the current user as @user on set_current_user" do
      username = Username.new(CaseInsensitiveString.new("bob"), "bobby")
      expect(SessionUtils).to receive(:currentUsername).and_return(username)

      get :test_action

      expect(assigns[:user]).to eq(username)
    end

    it "should set the current user entity id as @user_id on set_current_user" do
      allow(SessionUtils).to receive(:currentUsername).and_return(Username.new(CaseInsensitiveString.new("bob"), "bobby"))
      session["GOCD_SECURITY_CURRENT_USER_ID"] = 123

      get :test_action

      expect(assigns[:user_id]).to eq(123)
    end

    it "should set the current user entity id as nil on set_current_user when authentication is turned off" do
      allow(SessionUtils).to receive(:currentUsername).and_return(Username.new(CaseInsensitiveString.new("bob"), "bobby"))
      session["GOCD_SECURITY_CURRENT_USER_ID"] = nil

      get :test_action

      expect(assigns[:user_id]).to eq(nil)
    end

    it "should get @user for current_user" do
      controller.instance_variable_set('@user', "foo")
      expect(controller.current_user).to eq("foo")
    end

    describe "requiring full path" do
      before(:each) do
        draw_test_controller_route
      end

      # Fake that part of a Rails model object that is needed by url_for.
      class TestObject
        include ActiveModel::Model

        def id
          self
        end

        def persisted?
          true
        end
      end

      it "should cache the url if options is an active-record object" do
        obj = TestObject.new

        def controller.test_object_url(*args)
          raise 'should not invoke this, because it is stubbed!'
        end

        expect(controller).to receive(:test_object_url).with(obj).and_return("some-url")
        expect(controller.url_for(obj)).to eq("some-url")
      end
    end


    describe "url cache" do
      before do
        Services.go_cache.clear
        draw_test_controller_route
      end

      after do
        Services.go_cache.clear
        controller.go_cache.clear
      end

      it "should cache the url" do
        Services.go_cache.clear
        expect(controller.url_for(controller: 'non_api', action: 'not_found_action')).to eq("http://test.host/rails/non_api_404")
        key = Services.go_cache.getKeys.grep(/#{Regexp.quote(com.thoughtworks.go.listener.BaseUrlChangeListener::URLS_CACHE_KEY)}#{Regexp.quote(GoCache::SUB_KEY_DELIMITER)}/).last
        expect(key).to be_present
        Services.go_cache.put(key, "some-random-url")
        expect(controller.url_for(controller: 'non_api', action: 'not_found_action')).to eq("some-random-url")
        expect(Services.go_cache.get(key)).to eq('some-random-url')
      end

      it "should cache the url irrespective of option key type" do
        Services.go_cache.clear
        url_options = {controller: 'non_api', action: 'not_found_action', foo: 'bar', boo: 'baz'}
        expect(controller.url_for(url_options)).to eq("http://test.host/rails/non_api_404?boo=baz&foo=bar")
        key = Services.go_cache.getKeys.grep(/#{Regexp.quote(com.thoughtworks.go.listener.BaseUrlChangeListener::URLS_CACHE_KEY)}#{Regexp.quote(GoCache::SUB_KEY_DELIMITER)}/).last
        expect(key).to be_present
        Services.go_cache.put(key, "some-random-url")
        expect(controller.url_for(Hash[url_options.stringify_keys.to_a.shuffle])).to eq("some-random-url")
        expect(Services.go_cache.get(key)).to eq('some-random-url')
      end

      it "should contain flash message in the session upon redirect and forwards the params" do
        guid = nil
        expect(controller).to receive(:redirect_to) do |options|
          expect(options[:class]).to eq(nil)
          expect(options[:action]).to eq(:index)
          expect(options[:params][:column]).to eq("a")
          expect(options[:params][:sort]).to eq("b")
          guid = options[:params][:fm]
        end
        controller.redirect_with_flash("Flash message", action: :index, params: {column: "a", sort: "b"}, class: "warning")
        flash = controller.flash_message_service.get(guid)
        expect(flash.to_s).to eq("Flash message")
        expect(flash.flashClass).to eq("warning")
      end

      it "should contain flash message in the session upon redirect with empty params" do
        guid = nil
        expect(controller).to receive(:redirect_to) do |options|
          expect(options[:action]).to eq(:index)
          guid = options[:params][:fm]
        end
        controller.redirect_with_flash("Flash message", action: :index)
        flash = controller.flash_message_service.get(guid)
        expect(flash.to_s).to eq("Flash message")
        expect(flash.flashClass).to eq(nil)
      end
    end
  end
end
