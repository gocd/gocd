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

require 'rails_helper'

describe Admin::ServerController do
  include MockRegistryModule
  include ExtraSpecAssertions

  before do
    allow(controller).to receive(:set_current_user)
  end

  before(:each) do
    @encrypted_password = GoCipher.new.encrypt("encrypted_password")
    @mail_host = MailHost.new("blrstdcrspair02", 9999, "pavan", "strong_password", true, true, "from@from.com", "admin@admin.com")
    @should_allow_auto_login = true
    @security_config = SecurityConfig.new(false)
    @valid_mail_host_params = {:hostName => "blrstdcrspair02", :port => "9999", :username => "pavan", :password => "strong_password", :tls => "true", :from => "from@from.com", :adminMail => "admin@admin.com"}
    @valid_security_config_params = ({:allow_auto_login => (@should_allow_auto_login ? "true" : "false")})

    @valid_artifacts_setting = {:artifactsDir => "newArtifactDir", :purgeStart => "10", :purgeUpto => "20", :purgeArtifacts => "Size"}
    @valid_server_params = @valid_mail_host_params.merge(@valid_security_config_params).merge(@valid_artifacts_setting)
    @server_configuration_form = ServerConfigurationForm.new(@valid_server_params)


    allow(controller).to receive(:go_config_service).and_return(@go_config_service = double(GoConfigService))
    allow(controller).to receive(:user_service).and_return(@user_service = double(UserService))
    allow(controller).to receive(:system_service).and_return(@system_service = double(SystemService))

    allow(@user_service).to receive(:canUserTurnOffAutoLogin).and_return(true)
    allow(@go_config_service).to receive(:getMailHost).and_return(@mail_host)
    allow(@go_config_service).to receive(:security).and_return(@security_config)

    @cruise_config = com.thoughtworks.go.config.BasicCruiseConfig.new()
    @cruise_config.setServerConfig(com.thoughtworks.go.config.ServerConfig.new(@security_config, @mail_host))
    @cruise_config.server().setJobTimeout("42")
    @cruise_config.server().setCommandRepositoryLocation("foo")
    allow(@go_config_service).to receive(:getConfigForEditing).and_return(@cruise_config)

    allow(controller).to receive(:populate_config_validity)
    allow(@go_config_service).to receive(:registry).and_return(MockRegistryModule::MockRegistry.new)
    allow(controller).to receive(:server_config_service).and_return(@server_config_service = double(ServerConfigService))

    allow(controller).to receive(:l).and_return(localizer = Class.new do
      def method_missing method, *args
        com.thoughtworks.go.i18n.LocalizedMessage.string(args[0], args[1..-1].to_java(java.lang.Object)).localize(Spring.bean("localizer"))
      end
    end.new)
  end

  describe "index" do
    it "should assign server config details" do
      get :index

      expect(assigns[:server_configuration_form]).to eq(@server_configuration_form)
      expect(assigns[:tab_name]).to eq("server_configuration")
      expect(assigns[:allow_user_to_turn_off_auto_login]).to eq(true)
      assert_template layout: "admin"
    end

    it "should assign retrieve jobTimeout for index" do
      get :index

      expect(assigns[:server_configuration_form].jobTimeout).to eq("42")
      expect(assigns[:server_configuration_form].timeoutType).to eq(com.thoughtworks.go.config.ServerConfig::OVERRIDE_TIMEOUT)
    end

    it "should assign configured command repository location" do
      get :index

      expect(assigns[:server_configuration_form].commandRepositoryLocation).to eq("foo")
    end

    it "should assign command repo base directory location" do
      allow(controller).to receive(:system_environment).and_return(@system_environment = double(SystemEnvironment))
      expect(@system_environment).to receive(:getCommandRepositoryRootLocation).at_least(1).and_return("foo")

      get :index

      expect(assigns[:command_repository_base_dir_location]).to eq("foo/")
    end

    describe "with view" do
      render_views

      before do
        user = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new("foo"))
        allow(controller).to receive(:set_current_user) do
          controller.instance_variable_set :@user, user
        end
        allow(controller).to receive(:current_user).and_return(user)
        allow(controller).to receive(:security_service).and_return(@security_service = double(SecurityService))
        allow(@go_config_service).to receive(:isSecurityEnabled).and_return(true)
        allow(@security_service).to receive(:canViewAdminPage).with(user).and_return(true)
        allow(@security_service).to receive(:isUserAdmin).with(user).and_return(true)
      end

      it "should assign server config details in view" do
        allow(controller).to receive(:cruise_config_md5).and_return('foo_bar_baz')

        get :index
        expect(response.body).to have_selector("form input[type='hidden'][name='cruise_config_md5'][value='foo_bar_baz']", visible: :hidden)
      end
    end
  end

  describe "update" do
    it "should render success message returned by service while updating server config" do
      allow(@server_config_service).to receive(:siteUrlFor) { |url, forceSsl| url }
      expect(@server_config_service).to receive(:updateServerConfig) do |mailhost, artifact_dir, purgeStart, purgeEnd, jobTimeout, should_allow_auto_login, siteUrl, secureSiteUrl, null, operation_result|
        operation_result.setMessage(LocalizedMessage.composite([LocalizedMessage.string("SAVED_CONFIGURATION_SUCCESSFULLY"), LocalizedMessage.string("CONFIG_MERGED")].to_java(com.thoughtworks.go.i18n.Localizable)))
      end

      post :update, params: { :server_configuration_form => @valid_server_params, :cruise_config_md5 => "foo_bar_baz" }

      assert_redirected_with_flash("/admin/config/server", "Saved configuration successfully. The configuration was modified by someone else, but your changes were merged successfully.", 'success',[])
    end

    it "should assign server config details" do
      allow(@server_config_service).to receive(:siteUrlFor) { |url, forceSsl| url }
      stub_update_server_config(@mail_host, "newArtifactDir", 10, 20, nil, @should_allow_auto_login, nil, nil, nil, localized_success_message, "foo_bar_baz")

      post :update, params: { :server_configuration_form => @valid_server_params, :cruise_config_md5 => "foo_bar_baz" }

      assert_redirected_with_flash("/admin/config/server", "Saved configuration successfully.", 'success', [])
    end

    it "should validate mailHost params" do
      post :update, params: { :server_configuration_form => @valid_mail_host_params.except(:hostName) }
      assert_index_rendered_with_error("Hostname is required.")

      post :update, params: { :server_configuration_form => @valid_mail_host_params.except(:port) }
      assert_index_rendered_with_error("Port is required.")

      post :update, params: { :server_configuration_form => @valid_mail_host_params.merge(:port => "invalid-port") }
      assert_index_rendered_with_error("Invalid port.")

      post :update, params: { :server_configuration_form => @valid_mail_host_params.except(:from) }
      assert_index_rendered_with_error("From email address is required.")

      post :update, params: { :server_configuration_form => @valid_mail_host_params.except(:adminMail) }
      assert_index_rendered_with_error("Admin email address is required.")
    end

    it "should skip validating mailHost params if all of them are empty" do
      allow(@server_config_service).to receive(:siteUrlFor) { |url, forceSsl| url }
      stub_update_server_config(MailHost.new(com.thoughtworks.go.security.GoCipher.new),   "newArtifactDir", 10, 20, nil, @should_allow_auto_login, nil, nil, nil, localized_success_message, "foo_bar_baz")

      post :update, params: { :server_configuration_form => @valid_security_config_params.merge(@valid_artifacts_setting), :cruise_config_md5 => "foo_bar_baz" }

      assert_redirected_with_flash("/admin/config/server", "Saved configuration successfully.", 'success')
    end

    it "should drop purge-values when purging turned off" do
      allow(@server_config_service).to receive(:siteUrlFor) { |url, forceSsl| url }
      stub_update_server_config(MailHost.new(com.thoughtworks.go.security.GoCipher.new),   "newArtifactDir", nil, nil, nil, @should_allow_auto_login, nil, nil,nil,  localized_success_message, "foo_bar_baz")

      post :update, params: { :server_configuration_form => @valid_security_config_params.merge(@valid_artifacts_setting).merge(:purgeArtifacts => "Never"), :cruise_config_md5 => "foo_bar_baz" }

      assert_redirected_with_flash("/admin/config/server", "Saved configuration successfully.", 'success')
    end

    it "should send '0' for never terminate hung job" do
      allow(@server_config_service).to receive(:siteUrlFor) { |url, forceSsl| url }
      stub_update_server_config(MailHost.new(com.thoughtworks.go.security.GoCipher.new),   "newArtifactDir", nil, nil, '0', @should_allow_auto_login, nil, nil, nil, localized_success_message, "foo_bar_baz")

      post :update, params: { :server_configuration_form => @valid_security_config_params.merge(@valid_artifacts_setting).merge(:purgeArtifacts => "Never", :timeoutType => 'neverTimeout'), :cruise_config_md5 => "foo_bar_baz" }

      assert_redirected_with_flash("/admin/config/server", "Saved configuration successfully.", 'success')
    end

    it "should send overridden jobTimeout for terminate hung job" do
      allow(@server_config_service).to receive(:siteUrlFor) { |url, forceSsl| url }
      stub_update_server_config(MailHost.new(com.thoughtworks.go.security.GoCipher.new),   "newArtifactDir", nil, nil, '42', @should_allow_auto_login, nil, nil, nil,localized_success_message, "foo_bar_baz")

      post :update, params: { :server_configuration_form => @valid_security_config_params.merge(@valid_artifacts_setting).merge(:purgeArtifacts => "Never", :timeoutType => 'overrideTimeout', :jobTimeout => '42'), :cruise_config_md5 => "foo_bar_baz" }

      assert_redirected_with_flash("/admin/config/server", "Saved configuration successfully.", 'success')
    end

    it "should update the server site url and secure site url" do
      allow(@server_config_service).to receive(:siteUrlFor) { |url, forceSsl| url }
      stub_update_server_config(MailHost.new(com.thoughtworks.go.security.GoCipher.new),   "newArtifactDir", nil, nil, nil, @should_allow_auto_login, "http://site_url", "https://secure_site_url", nil, localized_success_message, "foo_bar_baz")
      post :update, params: { :server_configuration_form => @valid_security_config_params.merge(@valid_artifacts_setting).merge(:purgeArtifacts => "Never", :siteUrl => "http://site_url", :secureSiteUrl => "https://secure_site_url"), :cruise_config_md5 => "foo_bar_baz" }
      assert_redirected_with_flash("/admin/config/server", "Saved configuration successfully.", 'success')
    end

    it "should render error message if there is an error reported by the service" do
      result = nil
      expect(@server_config_service).to receive(:updateServerConfig) do |mailhost, artifact_dir, purgeStart, purgeEnd, jobTimeout, should_allow_auto_login, siteUrl, secureSiteUrl, null, operation_result|
        operation_result.notAcceptable(LocalizedMessage.string("INVALID_FROM_ADDRESS"))
        result = operation_result
      end

      post :update, params: { :server_configuration_form => @valid_mail_host_params }

      assert_index_rendered_with_error("From address is not a valid email address.")
    end

    it "should update command repository location" do
      allow(@server_config_service).to receive(:siteUrlFor) { |url, forceSsl| url }
      stub_update_server_config(@mail_host,   "newArtifactDir", 10, 20, nil, @should_allow_auto_login, nil, nil, "value",localized_success_message, nil)
      post :update, params: { :server_configuration_form => @valid_server_params.merge(:commandRepositoryLocation => "value") }
      assert_redirected_with_flash("/admin/config/server", "Saved configuration successfully.", 'success')
    end

    describe "during concurrent edit" do
      it "on validation failure, should set cruise_config_md5 to old-md5 sent in request instead of latest md5" do
        allow(@cruise_config).to receive(:getMd5).and_return "new-md5"

        post :update, params: { :server_configuration_form => @valid_server_params.merge(:port => "abcd"), :cruise_config_md5 => "old-md5" }

        expect(assigns[:cruise_config_md5]).to eq("old-md5")
      end
    end

    def assert_index_rendered_with_error(msg)
      flash = session[:notice]
      expect(flash.to_s).to eq(msg)
      expect(flash.flashClass()).to eq('error')
      expect(assigns[:tab_name]).to eq("server_configuration")
    end

    def localized_success_message
      LocalizedMessage.string("SAVED_CONFIGURATION_SUCCESSFULLY")
    end

    def stub_update_server_config(mailhost, artifact_dir, purgeStart, purgeEnd, jobTimeout, should_allow_auto_login, siteUrl, secureSiteUrl, repo_location, localizable_message,md5)
      expect(@server_config_service).to receive(:updateServerConfig) do |actual_mailhost, actual_artifact_dir, actual_purgeStart, actual_purgeEnd, actual_jobTimeout, actual_should_allow_auto_login, actual_siteUrl, actual_secureSiteUrl, actual_repo_location, actual_operation_result,actual_md5|
        expect(actual_mailhost).to eq(mailhost)
        expect(actual_artifact_dir).to eq(artifact_dir)
        expect(actual_purgeStart).to eq(purgeStart)
        expect(actual_purgeEnd).to eq(purgeEnd)
        expect(actual_jobTimeout).to eq(jobTimeout)
        expect(actual_should_allow_auto_login).to eq(should_allow_auto_login)
        expect(actual_siteUrl).to eq(siteUrl)
        expect(actual_secureSiteUrl).to eq(secureSiteUrl)
        expect(actual_repo_location).to eq(repo_location)
        expect(actual_md5).to eq(md5)
        actual_operation_result.setMessage(localizable_message)
      end
    end
  end

  describe "validate" do

    before :each do
      @default_localized_result = DefaultLocalizedResult.new
    end

    it "should validate email" do
      @default_localized_result.invalid("INVALID_EMAIL", ["@foo.com"].to_java(java.lang.String))
      expect(@server_config_service).to receive(:validateEmail).with("@foo.com").and_return(@default_localized_result)

      get :validate, params: { :email => "@foo.com" }

      expect(assigns[:result]).to eq(@default_localized_result)
    end

    it "should validate port" do
      @default_localized_result.invalid("INVALID_PORT", ["-1"].to_java(java.lang.String))
      expect(@server_config_service).to receive(:validatePort).with(-1).and_return(@default_localized_result)

      get :validate, params: { :port => "-1" }

      expect(assigns[:result]).to eq(@default_localized_result)
    end

    it "should validate hostname" do
      expect(@server_config_service).to receive(:validateHostName).with("foo.com").and_return(@default_localized_result)

      get :validate, params: { :hostName => "foo.com" }

      expect(assigns[:result]).to eq(@default_localized_result)
    end

    it "should return success if valid" do
      expect(@server_config_service).to receive(:validatePort).with(-1).and_return(@default_localized_result)
      @default_localized_result.invalid("INVALID_PORT", [].to_java(java.lang.Object))

      get :validate, params: { :port => "-1" }

      json = JSON.parse(response.body)
      expect(json["error"]).to eq("Invalid port.")
      expect(json["success"]).to eq(nil)
    end

    it "should return error if invalid" do
      expect(@server_config_service).to receive(:validateHostName).with("foo.com").and_return(@default_localized_result)

      get :validate, params: { :hostName => "foo.com" }

      json = JSON.parse(response.body)
      expect(json["error"]).to eq(nil)
      expect(json["success"]).to eq("Valid")
    end
  end

  describe "test_email" do
    it "should return error if sendTestEmail fails" do
      allow(controller).to receive(:server_config_service).and_return(@server_config_service = double(ServerConfigService))
      mail_host = MailHost.new("blrstdcrspair02", 9999, "pavan", "strong_password", true, true, "from@from.com", "admin@admin.com")

      res = nil
      expect(@server_config_service).to receive(:sendTestMail).with(mail_host, an_instance_of(HttpLocalizedOperationResult)) do |mail_host, op_result|
        op_result.badRequest(LocalizedMessage.string('INVALID_PORT'))
        res = op_result
      end

      post :test_email, params: { :server_configuration_form => @valid_mail_host_params }

      json = JSON.parse(response.body)
      expect(json["error"]).to eq("Invalid port.")
      expect(json["success"]).to eq(nil)
    end

    it "should validate port" do
      post :test_email, params: { :server_configuration_form => @valid_mail_host_params.except(:port) }

      json = JSON.parse(response.body)
      expect(json["error"]).to eq("Port is required.")
      expect(json["success"]).to eq(nil)
    end

    it "should send the test email by using the service" do
      mail_host = MailHost.new("blrstdcrspair02", 9999, "pavan", "strong_password", true, true, "from@from.com", "admin@admin.com")

      expect(@server_config_service).to receive(:sendTestMail).with(mail_host, an_instance_of(HttpLocalizedOperationResult))

      post :test_email, params: { :server_configuration_form => @valid_mail_host_params }

      json = JSON.parse(response.body)
      expect(json["error"]).to eq(nil)
      expect(json["success"]).to eq("Sent test email successfully.")
    end
  end
end
