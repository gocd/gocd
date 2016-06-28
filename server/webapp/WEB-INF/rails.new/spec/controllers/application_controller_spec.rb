##########################GO-LICENSE-START################################
# Copyright 2016 ThoughtWorks, Inc.
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

require 'spec_helper'
require 'java'

describe ApplicationController do

  before do
    UserHelper.stub(:getUserId).and_return(1)
  end

  describe "error_handling" do
    before do
      class << controller
        include ActionRescue
      end
    end

    it "should handle InvalidAuthenticityToken by redirecting to root_url" do
      exception = ActionController::InvalidAuthenticityToken.new
      exception.should_receive(:backtrace).and_return([])
      nil.bomb rescue exception

      expect(Rails.logger).to receive(:error).with(anything)
      expect(controller).to receive(:redirect_to).with(root_url)

      controller.rescue_action(exception)
    end
  end

  describe :java_routes do
    it "should generate run stage url" do
      expect(run_stage_path(pipeline_name: "pipeline_name", stage_name: "stage_name",
                            pipeline_counter: 10)).to eq("/run/pipeline_name/10/stage_name")
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

    it "should load shine_dao" do
      expect(Spring).to receive(:bean).with('shineDao').and_return(shine_dao = Object.new)
      expect(controller.shine_dao).to eq(shine_dao)
    end

    it "should load xml_api_service" do
      expect(Spring).to receive(:bean).with('xmlApiService').and_return(xml_api_service = Object.new)
      expect(controller.xml_api_service).to eq(xml_api_service)
    end

    it "should load dependency_material_service" do
      expect(Spring).to receive(:bean).with('dependencyMaterialService').and_return(dependency_material_service = Object.new)
      expect(controller.dependency_material_service).to eq(dependency_material_service)
    end

    it "should load failure_service" do
      expect(Spring).to receive(:bean).with('failureService').and_return(service = Object.new)
      expect(controller.failure_service).to eq(service)
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
      controller.flash_message_service.should be_a(com.thoughtworks.go.server.web.FlashMessageService)
    end

    it "should load user_search_service" do
      expect(Spring).to receive(:bean).with('userSearchService').and_return(service = Object.new)
      expect(controller.user_search_service).to eq(service)
    end

    it "should load viewRenderingService" do
      expect(Spring).to receive(:bean).with('viewRenderingService').and_return(service = Object.new)
      expect(controller.view_rendering_service).to eq(service)
    end

    it "should have task_view_service method available" do
      expect(Spring).to receive(:bean).with('taskViewService').and_return(service = Object.new)
      expect(controller.task_view_service).to eq(service)
    end

    it "should load mingle_config_service" do
      expect(Spring).to receive(:bean).with('mingleConfigService').and_return(service = Object.new)
      expect(controller.mingle_config_service).to eq(service)
    end

    it "should load version_info_service" do
      expect(Spring).to receive(:bean).with('versionInfoService').and_return(service = Object.new)
      expect(controller.version_info_service).to eq(service)
    end
  end

  describe :default_as_empty_list do
    it "should default given params as empty list only if not given" do
      @controller.params = HashWithIndifferentAccess.new
      @controller.params[:default_as_empty_list] = ["foo", "bar>baz", "quux>bang>boom", "user>name", "hello"]
      @controller.params[:hi] = "bye"
      @controller.params[:hello] = "world"
      @controller.params[:user] = {:name => "foo"}
      expect(@controller.send(:default_as_empty_list)).to be_true
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
      @controller.send(:cruise_config_md5).should == "md5_value"
    end

    it "should understand loaded config-file md5" do
      lambda do
        @controller.send(:cruise_config_md5)
      end.should raise_error("md5 for config file has not been loaded yet")
    end

    it "should get servlet request" do
      expect(controller).to receive(:request).and_return(req = double('request'))
      expect(req).to receive(:env).and_return(env = {})
      env['java.servlet_request'] = :holy_cow
      controller.send(:servlet_request).should == :holy_cow
    end
  end

  describe "do for every request" do
    controller do
      def index
        render text: "Hello"
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
      Thread.current[:ssl_base_url].should == "https://ssl.host:443"
      Thread.current[:base_url].should == "http://test.host"
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
        render text: "Some test action"
      end
    end

    before(:each) do
      @routes.draw do
        get "/anonymous/test_action"
      end
      config_service = stub_service(:go_config_service)
      config_service.stub(:checkConfigFileValid).and_return(com.thoughtworks.go.config.validation.GoConfigValidity.valid)
    end

    it "should set the current user as @user on set_current_user" do
      username = Username.new(CaseInsensitiveString.new("bob"), "bobby")
      UserHelper.should_receive(:getUserName).and_return(username)

      get :test_action

      expect(assigns[:user]).to eq(username)
    end

    it "should set the current user entity id as @user_id on set_current_user" do
      UserHelper.stub(:getUserName).and_return(Username.new(CaseInsensitiveString.new("bob"), "bobby"))
      session[com.thoughtworks.go.server.util.UserHelper.getSessionKeyForUserId] = 123

      get :test_action

      expect(assigns[:user_id]).to eq(123)
    end

    it "should set the current user entity id as nil on set_current_user when authentication is turned off" do
      UserHelper.stub(:getUserName).and_return(Username.new(CaseInsensitiveString.new("bob"), "bobby"))
      session[com.thoughtworks.go.server.util.UserHelper.getSessionKeyForUserId] = nil

      get :test_action

      expect(assigns[:user_id]).to eq(nil)
    end

    it "should get @user for current_user" do
      controller.instance_variable_set('@user', "foo")
      expect(controller.current_user).to eq("foo")
    end

    it "should render_operation_result_if_failure" do
      result = HttpOperationResult.new
      result.unauthorized("Unauthorized", "the real cause", nil)
      expect(controller).to receive(:render_if_error).with("Unauthorized { the real cause }\n", 401).and_return(true)

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
        def id
        end

        def self.model_name
          self
        end

        def self.singular_route_key
          "test_object"
        end
      end

      it "should return only the path to a given resource and not the whole url" do
        expect(controller.url_for(controller: "java", action: "null", foo: "junk")).to eq("/rails/java/null?foo=junk")
      end

      it "should cache the url if options is an active-record object" do
        obj = TestObject.new
        controller.should_receive(:test_object_url).with(obj).and_return("some-url")
        expect(controller.url_for(obj)).to eq("some-url")
      end

      it "should return full path when requested explicitly" do
        expect(controller.url_for(controller: "java", action: "null", foo: "junk",
            only_path: false)).to eq("http://test.host/rails/java/null?foo=junk")
      end

      it "should use ssl base url from server config when requested" do
        expect(controller.url_for(controller: "java", action: "null", foo: "junk",
            only_path: false, protocol: 'https')).to eq("https://ssl.host:443/rails/java/null?foo=junk")
        expect(controller.url_for(controller: "java", action: "null", foo: "junk",
            only_path: false, protocol: 'http')).to eq("http://test.host/rails/java/null?foo=junk")
        expect(controller.url_for(controller: "java", action: "null", foo: "junk",
            only_path: false)).to eq("http://test.host/rails/java/null?foo=junk")
      end
    end

    describe "url cache" do
      before do
        draw_test_controller_route

        ActionController::Base.class_eval do
          @@url_cache_miss_count = 0
          alias_method :url_for_original, :url_for

          def url_for(options)
            @@url_cache_miss_count += 1
            s = string_for(params) + " - "
            s += string_for(options)
            "#{@@url_cache_miss_count} - #{s} - #{request.protocol} - #{request.host_with_port}"
          end

          def string_for(map)
            str = []
            map.keys.sort { |val, other| val.to_s <=> other.to_s }.each do |key|
              str << "#{key}=#{map[key]}"
            end
            str.join("|")
          end
        end
        stub_service(:server_config_service).stub(:siteUrlFor) do |url, _force_ssl|
          url
        end
      end

      after do
        ActionController::Base.class_eval do
          remove_method :url_for
          alias_method :url_for, :url_for_original
          remove_method :url_for_original
        end
        controller.go_cache.clear
      end

      it "should cache the url based on options" do
        controller.params.clear
        controller.url_for('foo' => 'foo', 'bar' => 'bar').should == "1 -  - bar=bar|foo=foo|only_path=true - http:// - test.host"
        controller.url_for('bar' => 'bar', 'foo' => 'foo').should == "1 -  - bar=bar|foo=foo|only_path=true - http:// - test.host"
      end

      it "should cache the url based on params" do
        controller.stub(:params).and_return(foo: "foo", bar: "bar")
        expect(controller.url_for).to eq("1 - bar=bar|foo=foo - only_path=true - http:// - test.host")
        controller.stub(:params).and_return(bar: "bar", foo: "foo")
        expect(controller.url_for).to eq("1 - bar=bar|foo=foo - only_path=true - http:// - test.host")
      end

      it "should cache the url based on params and options" do
        controller.stub(:params).and_return(bar: "bar", baz: "baz")
        expect(controller.url_for(foo: "foo", quux: "quux")).to eq("1 - bar=bar|baz=baz - foo=foo|only_path=true|quux=quux - http:// - test.host")

        controller.stub(:params).and_return(baz: "baz", bar: "bar")
        expect(controller.url_for(quux: "quux", foo: "foo")).to eq("1 - bar=bar|baz=baz - foo=foo|only_path=true|quux=quux - http:// - test.host")

        expect(controller.url_for(foo: "foo")).to eq("2 - bar=bar|baz=baz - foo=foo|only_path=true - http:// - test.host")

        controller.stub(:params).and_return(bar: "bar")
        expect(controller.url_for(foo: "foo")).to eq("3 - bar=bar - foo=foo|only_path=true - http:// - test.host")
      end

      it "should cache the url based on values" do
        expect(controller.url_for(foo: "foo", quux: "foo")).to eq("1 -  - foo=foo|only_path=true|quux=foo - http:// - test.host")
        expect(controller.url_for(foo: "foo", quux: "quux")).to eq("2 -  - foo=foo|only_path=true|quux=quux - http:// - test.host")
      end

      it "should cache the url based on keys" do
        expect(controller.url_for(foo: "foo")).to eq("1 -  - foo=foo|only_path=true - http:// - test.host")
        expect(controller.url_for(foo1: "foo")).to eq("2 -  - foo1=foo|only_path=true - http:// - test.host")
      end

      it "should cache the url based on host_and_port request reached on" do
        controller.request.stub(:host_with_port).and_return("local-host:8153")
        expect(controller.url_for(foo: "foo")).to eq("1 -  - foo=foo|only_path=true - http:// - local-host:8153")
        controller.request.stub(:host_with_port).and_return("local-ghost:8154")
        expect(controller.url_for(foo: "foo")).to eq("2 -  - foo=foo|only_path=true - http:// - local-ghost:8154")
        controller.request.stub(:host_with_port).and_return("local-ghost:8153")
        expect(controller.url_for(foo: "foo")).to eq("3 -  - foo=foo|only_path=true - http:// - local-ghost:8153")
      end

      it "should cache the url based on protocol request reached on" do
        controller.request.stub(:host_with_port).and_return("host")
        controller.request.stub(:protocol).and_return("http://")
        expect(controller.url_for(foo: "foo")).to eq("1 -  - foo=foo|only_path=true - http:// - host")

        controller.request.stub(:protocol).and_return("https://")
        expect(controller.url_for(foo: "foo")).to eq("2 -  - foo=foo|only_path=true - https:// - host")

        controller.request.stub(:protocol).and_return("spdy://")
        expect(controller.url_for(foo: "foo")).to eq("3 -  - foo=foo|only_path=true - spdy:// - host")
      end

      it "should not mistake pipeline_counter and stage_counter for caching" do
        controller.stub(:params).and_return(:controller => "stages", :action => "stage", "pipeline_name" => "foo",
                                            "pipeline_counter" => "2", "stage_name" => "bar", "stage_counter" => "1")
        expect(controller.url_for(format: "json")).to eq(
              "1 - action=stage|controller=stages|pipeline_counter=2|pipeline_name=foo|stage_counter=1|stage_name=bar - format=json|only_path=true - http:// - test.host")

        controller.stub(:params).and_return(:controller => "stages", :action => "stage", "pipeline_name" => "foo",
                                            "pipeline_counter" => "1", "stage_name" => "bar", "stage_counter" => "2")
        expect(controller.url_for(format: "json")).to eq(
              "2 - action=stage|controller=stages|pipeline_counter=1|pipeline_name=foo|stage_counter=2|stage_name=bar - format=json|only_path=true - http:// - test.host")
      end

      it "should sort symbols after string" do
        h = HashMapKey
        [h.new(:pavan), h.new("pavan"), h.new("JJ"), h.new(:JJ)].sort.should == [h.new("JJ"), h.new("pavan"), h.new(:JJ), h.new(:pavan)]
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
        controller.redirect_with_flash("Flash message", action: :index, params: { column: "a", sort: "b" }, class: "warning")
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
        expect(@controller.send(:request_from_localhost?)).to be_true

        @controller.request.env["SERVER_NAME"] = localhost.getHostName
        @controller.request.env["REMOTE_ADDR"] = "8.8.8.8"
        expect(@controller.send(:request_from_localhost?)).to be_false
      end
    end
  end
end
