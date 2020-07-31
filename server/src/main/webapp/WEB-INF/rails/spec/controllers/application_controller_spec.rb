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
    it "should load go config service" do
      expect(Spring).to receive(:bean).with('goConfigService').and_return(mock_go_config_service = "MOCK GO CONFIG")
      expect(controller.go_config_service).to eq(mock_go_config_service)
    end

    it "should load pipeline stages feed service" do
      expect(Spring).to receive(:bean).with('pipelineStagesFeedService').and_return(mock_feed_service = Object.new)
      expect(controller.pipeline_stages_feed_service).to eq(mock_feed_service)
    end

    it "should load material service" do
      expect(Spring).to receive(:bean).with('materialService').and_return(mock_material_service = Object.new)
      expect(controller.material_service).to eq(mock_material_service)
    end

    it "should load pipeline_history_service" do
      expect(Spring).to receive(:bean).with('pipelineHistoryService').and_return(mock_phs = Object.new)
      expect(controller.pipeline_history_service).to eq(mock_phs)
    end

    it "should load agent_service" do
      expect(Spring).to receive(:bean).with('agentService').and_return(agent_service = Object.new)
      expect(controller.agent_service).to eq(agent_service)
    end

    it "should load xml_api_service" do
      expect(Spring).to receive(:bean).with('xmlApiService').and_return(xml_api_service = Object.new)
      expect(controller.xml_api_service).to eq(xml_api_service)
    end

    it "should load pipeline_lock_service" do
      expect(Spring).to receive(:bean).with('pipelineUnlockApiService').and_return(service = Object.new)
      expect(controller.pipeline_unlock_api_service).to eq(service)
    end

    it "should load schedule_service" do
      expect(Spring).to receive(:bean).with('scheduleService').and_return(service = Object.new)
      expect(controller.schedule_service).to eq(service)
    end

    it "should load user_service" do
      expect(Spring).to receive(:bean).with('userService').and_return(service = Object.new)
      expect(controller.user_service).to eq(service)
    end

    it "should load flash_message_service" do
      expect(controller.flash_message_service).to be_a(com.thoughtworks.go.server.web.FlashMessageService)
    end

    it "should load user_search_service" do
      expect(Spring).to receive(:bean).with('userSearchService').and_return(service = Object.new)
      expect(controller.user_search_service).to eq(service)
    end

    it "should load version_info_service" do
      expect(Spring).to receive(:bean).with('versionInfoService').and_return(service = Object.new)
      expect(controller.version_info_service).to eq(service)
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

  describe "config MD5 stuff" do
    it "should understand loaded config-file md5" do
      @controller.instance_variable_set('@cruise_config_md5', "md5_value")
      expect(@controller.send(:cruise_config_md5)).to eq("md5_value")
    end

    it "should understand loaded config-file md5" do
      expect do
        @controller.send(:cruise_config_md5)
      end.to raise_error("md5 for config file has not been loaded yet")
    end

    it "should get servlet request" do
      expect(controller).to receive(:request).and_return(req = double('request'))
      expect(req).to receive(:env).and_return(env = {})
      env['java.servlet_request'] = :holy_cow
      expect(controller.send(:servlet_request)).to eq(:holy_cow)
    end
  end

  describe "do for every request" do
    controller do
      def index
        render plain: "Hello"
      end
    end

    it "should populate the config file validity for every request" do
      go_config_service = stub_service(:go_config_service)
      expect(go_config_service).to receive(:checkConfigFileValid).and_return(com.thoughtworks.go.config.validation.GoConfigValidity.valid())

      get :index

      expect(assigns[:config_valid]).to eq(true)
    end

    it "should set the siteUrl and secureSiteUrl on the thread" do
      get :index

      # These values are configured in the spec_helper's setup_base_urls method.
      # But, this controller puts them on the current thread.
      expect(Thread.current[:ssl_base_url]).to eq("https://ssl.host:443")
      expect(Thread.current[:base_url]).to eq("http://test.host")
    end
  end

  describe "current_user_id for gadget rendering server" do
    it "should return nil if current user is 'anonymous'" do
      controller.instance_variable_set('@user', Username.new(CaseInsensitiveString.new("anonymous")))
      expect(@controller.current_user_id).to eq(nil)
    end

    it "should return the current user if not 'anonymous'" do
      controller.instance_variable_set('@user', Username.new(CaseInsensitiveString.new("admin")))
      expect(@controller.current_user_id).to eq("admin")
    end
  end

  describe "current_user_id_for_oauth for gadget rendering server" do
    it "should always return the current user even if it's anonymous" do
      controller.instance_variable_set('@user', Username.new(CaseInsensitiveString.new("anonymous")))
      expect(@controller.current_user_id_for_oauth).to eq("anonymous")
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
      config_service = stub_service(:go_config_service)
      allow(config_service).to receive(:checkConfigFileValid).and_return(com.thoughtworks.go.config.validation.GoConfigValidity.valid)
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

    it "should render_operation_result_if_failure" do
      result = HttpOperationResult.new
      result.forbidden("Unauthorized", "the real cause", nil)
      expect(controller).to receive(:render_if_error).with("Unauthorized { the real cause }\n", 403).and_return(true)

      controller.render_operation_result_if_failure(result)
    end

    it "should not render_operation_result when result hasn't failed" do
      result = double('result', httpCode: 302)
      expect(controller).to receive(:render).never

      controller.render_operation_result_if_failure(result)
    end

    describe "requiring full path" do
      before(:each) do
        setup_base_urls
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

      it "should return only the path to a given resource and not the whole url" do
        expect(controller.url_for(controller: 'go_errors', action: :inactive, foo: "junk", only_path: false)).to eq("http://test.host/errors/inactive?foo=junk")
      end

      it "should cache the url if options is an active-record object" do
        obj = TestObject.new

        def controller.test_object_url(*args)
          raise 'should not invoke this, because it is stubbed!'
        end

        expect(controller).to receive(:test_object_url).with(obj).and_return("some-url")
        expect(controller.url_for(obj)).to eq("some-url")
      end

      it "should return full path when requested explicitly" do
        expect(controller.url_for(controller: 'go_errors', action: :inactive, foo: "junk",
                                  only_path: false)).to eq("http://test.host/errors/inactive?foo=junk")
      end
    end


    describe "url cache" do
      before do
        Services.go_cache.clear
        draw_test_controller_route
        def controller.default_url_options
          super.reverse_merge(UrlBuilder.default_url_options)
        end
      end

      after do
        Services.go_cache.clear
        controller.go_cache.clear
      end

      it "should cache the url" do
        Services.go_cache.clear
        expect(controller.url_for(controller: 'go_errors', action: :inactive)).to eq("http://test.host/errors/inactive")
        key = Services.go_cache.getKeys.grep(/#{Regexp.quote(com.thoughtworks.go.listener.BaseUrlChangeListener::URLS_CACHE_KEY)}#{Regexp.quote(GoCache::SUB_KEY_DELIMITER)}/).last
        expect(key).to be_present
        Services.go_cache.put(key, "some-random-url")
        expect(controller.url_for(controller: 'go_errors', action: :inactive)).to eq("some-random-url")
        expect(Services.go_cache.get(key)).to eq('some-random-url')
      end

      it "should cache the url irrespective of option key type" do
        Services.go_cache.clear
        url_options = {controller: 'go_errors', action: :inactive, foo: 'bar', boo: 'baz'}
        expect(controller.url_for(url_options)).to eq("http://test.host/errors/inactive?boo=baz&foo=bar")
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

    describe "local request recognition" do
      before :all do
        import java.net.InetAddress unless defined? InetAddress
      end

      it "should recognize local request" do
        @controller.request.env["SERVER_NAME"] = "server_name"
        @controller.request.env["REMOTE_ADDR"] = "client_ip"
        localhost = InetAddress.getLocalHost
        @controller.request.env["SERVER_NAME"] = localhost.getHostName
        @controller.request.env["REMOTE_ADDR"] = localhost.getHostAddress
        expect(@controller.send(:request_from_localhost?)).to be_truthy

        @controller.request.env["SERVER_NAME"] = localhost.getHostName
        @controller.request.env["REMOTE_ADDR"] = "8.8.8.8"
        expect(@controller.send(:request_from_localhost?)).to be_falsey
      end
    end
  end
end
