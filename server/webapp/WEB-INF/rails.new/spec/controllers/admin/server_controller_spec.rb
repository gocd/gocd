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

require 'spec_helper'

describe Admin::ServerController do
  include MockRegistryModule

  before do
    controller.stub(:set_current_user)
  end

  before(:each) do
    @encrypted_password = GoCipher.new.encrypt("encrypted_password")
    @mail_host = MailHost.new("blrstdcrspair02", 9999, "pavan", "strong_password", true, true, "from@from.com", "admin@admin.com")
    @ldap_config = LdapConfig.new("ldap://test.com", "test", "password", @encrypted_password, true,BasesConfig.new([BaseConfig.new('base1'), BaseConfig.new('base2')].to_java(BaseConfig)), "searchFilter")
    @ldap_config_without_password = LdapConfig.new("ldap://test.com", "test", "********", @encrypted_password, false,BasesConfig.new([BaseConfig.new('base1'), BaseConfig.new('base2')].to_java(BaseConfig)), "searchFilter")
    @password_file_config = PasswordFileConfig.new("path")
    @should_allow_auto_login = true
    @security_config = SecurityConfig.new(@ldap_config, @password_file_config, false)
    @valid_mail_host_params = {:hostName => "blrstdcrspair02", :port => "9999", :username => "pavan", :password => "strong_password", :tls => "true", :from => "from@from.com", :adminMail => "admin@admin.com"}
    @valid_ldap_params = {:ldap_uri => "ldap://test.com", :ldap_username => "test", :ldap_password => "password", :ldap_encrypted_password => @encrypted_password,  :ldap_password_changed => "true",
                          :ldap_search_base =>"base1\r\nbase2", :ldap_search_filter => "searchFilter"}
    @valid_ldap_not_changed_password_params = {:ldap_uri => "ldap://test.com", :ldap_username => "test", :ldap_password => "********", :ldap_encrypted_password => @encrypted_password, :ldap_password_changed => "false",
                          :ldap_search_base =>"base1\r\nbase2", :ldap_search_filter => "searchFilter"}
    @valid_security_config_params = ({:allow_auto_login => (@should_allow_auto_login ? "1" : "0"), :password_file_path => "path"}).merge(@valid_ldap_params)

    @valid_artifacts_setting = {:artifactsDir => "newArtifactDir", :purgeStart => "10", :purgeUpto => "20", :purgeArtifacts => "Size"}
    @valid_server_params = @valid_mail_host_params.merge(@valid_security_config_params).merge(@valid_artifacts_setting)
    @server_configuration_form = ServerConfigurationForm.new(@valid_server_params)


    controller.stub(:go_config_service).and_return(@go_config_service = Object.new)
    controller.stub(:user_service).and_return(@user_service = Object.new)
    controller.stub(:system_service).and_return(@system_service = Object.new)

    @user_service.stub(:canUserTurnOffAutoLogin).and_return(true)
    @go_config_service.stub(:getMailHost).and_return(@mail_host)
    @go_config_service.stub(:security).and_return(@security_config)

    @cruise_config = com.thoughtworks.go.config.BasicCruiseConfig.new()
    @cruise_config.setServerConfig(com.thoughtworks.go.config.ServerConfig.new(@security_config, @mail_host))
    @cruise_config.server().setJobTimeout("42")
    @cruise_config.server().setCommandRepositoryLocation("foo")
    @go_config_service.stub(:getConfigForEditing).and_return(@cruise_config)

    controller.stub(:populate_config_validity)
    @go_config_service.stub(:registry).and_return(MockRegistryModule::MockRegistry.new)

    controller.stub(:l).and_return(localizer = Class.new do
      def method_missing method, *args
        com.thoughtworks.go.i18n.LocalizedMessage.string(args[0], args[1..-1].to_java(java.lang.Object)).localize(Spring.bean("localizer"))
      end
    end.new)
  end

  describe "validate_ldap" do
    it "should resolve /admin/config/server/validate_ldap" do
      {:post => "/admin/config/server/validate_ldap"}.should route_to(:controller => "admin/server", :action => "validate_ldap")
    end

    it "should return error if validate ldap fails" do
      controller.stub(:server_config_service).and_return(@server_config_service = Object.new)
      result = nil
      @server_config_service.should_receive(:validateLdapSettings).with(@ldap_config, an_instance_of(HttpLocalizedOperationResult)) do |ldap_settings, op_result|
        op_result.badRequest(LocalizedMessage.string('CANNOT_CONNECT_TO_LDAP'))
        result = op_result
      end

      post :validate_ldap, :server_configuration_form => @valid_ldap_params
      json = JSON.parse(response.body)
      json["error"].should == "Cannot connect to ldap, please check the settings. Reason: {0}"
      json["success"].should == nil
    end

    it "should report success on being able to connect to ldap" do
      controller.stub(:server_config_service).and_return(@server_config_service = Object.new)
      result = nil
      @server_config_service.should_receive(:validateLdapSettings).with(@ldap_config_without_password, an_instance_of(HttpLocalizedOperationResult))

      post :validate_ldap, :server_configuration_form => @valid_ldap_not_changed_password_params
      json = JSON.parse(response.body)
      json["error"].should == nil
      json["success"].should == "Connected to LDAP successfully."
    end

    it "should report success on being able to connect to ldap when the password is not changed" do
      controller.stub(:server_config_service).and_return(@server_config_service = Object.new)
      result = nil
      @server_config_service.should_receive(:validateLdapSettings).with(@ldap_config, an_instance_of(HttpLocalizedOperationResult))

      post :validate_ldap, :server_configuration_form => @valid_ldap_params
      json = JSON.parse(response.body)
      json["error"].should == nil
      json["success"].should == "Connected to LDAP successfully."
    end
  end

  describe "index" do
    it "should resolve route to server config" do
      {:get => "/admin/config/server"}.should route_to(:controller => "admin/server", :action => "index")
    end

    it "should assign server config details" do
      get :index

      assigns[:server_configuration_form].should == @server_configuration_form
      assigns[:tab_name].should == "server_configuration"
      assigns[:allow_user_to_turn_off_auto_login].should == true
      assert_template layout: "admin"
    end

    it "should assign retrieve jobTimeout for index" do
      get :index

      assigns[:server_configuration_form].jobTimeout.should == "42"
      assigns[:server_configuration_form].timeoutType.should == com.thoughtworks.go.config.ServerConfig::OVERRIDE_TIMEOUT
    end

    it "should assign configured command repository location" do
      get :index

      assigns[:server_configuration_form].commandRepositoryLocation.should == "foo"
    end

    it "should assign command repo base directory location" do
      controller.stub(:system_environment).and_return(@system_environment = Object.new)
      @system_environment.should_receive(:getCommandRepositoryRootLocation).at_least(1).and_return("foo")

      get :index

      assigns[:command_repository_base_dir_location].should == "foo/"
    end

    it "should construct server config form to display ldap searchbases" do
      get :index

      assigns[:server_configuration_form].ldap_search_base.should == "base1\r\nbase2"
    end

    describe "with view" do
      render_views

      before do
        user = com.thoughtworks.go.server.domain.Username.new(CaseInsensitiveString.new("foo"))
        controller.stub(:set_current_user) do
          controller.instance_variable_set :@user, user
        end
        controller.stub(:current_user).and_return(user)
        controller.stub(:security_service).and_return(@security_service = Object.new)
        @go_config_service.stub(:isSecurityEnabled).and_return(true)
        @security_service.stub(:canViewAdminPage).with(user).and_return(true)
        @security_service.stub(:isUserAdmin).with(user).and_return(true)
      end

      it "should assign server config details in view" do
        controller.stub(:cruise_config_md5).and_return('foo_bar_baz')

        get :index

        expect(response.body).to have_selector("form input[type='hidden'][name='cruise_config_md5'][value='foo_bar_baz']")
      end
    end
  end

  describe "update" do
    before(:each) do
      controller.stub(:server_config_service).and_return(@server_config_service = Object.new)
    end

    it "should resolve route to server config" do
      {:post => "/admin/config/server/update"}.should route_to(:controller => "admin/server", :action => "update")
    end

    it "should render success message returned by service while updating server config" do
      controller.stub(:server_config_service).and_return(server_config_service = Object.new)
      server_config_service.stub(:siteUrlFor).and_return { |url, forceSsl| url }
      server_config_service.should_receive(:updateServerConfig) do |mailhost, ldap, password, artifact_dir, purgeStart, purgeEnd, jobTimeout, should_allow_auto_login, siteUrl, secureSiteUrl, null, operation_result|
        operation_result.setMessage(LocalizedMessage.composite([LocalizedMessage.string("SAVED_CONFIGURATION_SUCCESSFULLY"), LocalizedMessage.string("CONFIG_MERGED")].to_java(com.thoughtworks.go.i18n.Localizable)))
      end

      post :update, :server_configuration_form => @valid_server_params, :cruise_config_md5 => "foo_bar_baz"

      assert_redirected_with_flash("/admin/config/server", "Saved configuration successfully. The configuration was modified by someone else, but your changes were merged successfully.", 'success',[])
    end

    it "should assign server config details" do
      @server_config_service.stub(:siteUrlFor).and_return { |url, forceSsl| url }
      stub_update_server_config(@mail_host, @ldap_config, @password_file_config, "newArtifactDir", 10, 20, nil, @should_allow_auto_login, nil, nil, nil, localized_success_message, "foo_bar_baz")

      post :update, :server_configuration_form => @valid_server_params, :cruise_config_md5 => "foo_bar_baz"

      assert_redirected_with_flash("/admin/config/server", "Saved configuration successfully.", 'success', [])
    end

    it "should validate mailHost params" do
      post :update, :server_configuration_form => @valid_mail_host_params.except(:hostName)
      assert_index_rendered_with_error("Hostname is required.")

      post :update, :server_configuration_form => @valid_mail_host_params.except(:port)
      assert_index_rendered_with_error("Port is required.")

      post :update, :server_configuration_form => @valid_mail_host_params.merge(:port => "invalid-port")
      assert_index_rendered_with_error("Invalid port.")

      post :update, :server_configuration_form => @valid_mail_host_params.except(:from)
      assert_index_rendered_with_error("From email address is required.")

      post :update, :server_configuration_form => @valid_mail_host_params.except(:adminMail)
      assert_index_rendered_with_error("Admin email address is required.")
    end

    it "should skip validating mailHost params if all of them are empty" do
      @server_config_service.stub(:siteUrlFor).and_return { |url, forceSsl| url }
      stub_update_server_config(MailHost.new(com.thoughtworks.go.security.GoCipher.new), @ldap_config, @password_file_config, "newArtifactDir", 10, 20, nil, @should_allow_auto_login, nil, nil, nil, localized_success_message, "foo_bar_baz")

      post :update, :server_configuration_form => @valid_security_config_params.merge(@valid_artifacts_setting), :cruise_config_md5 => "foo_bar_baz"

      assert_redirected_with_flash("/admin/config/server", "Saved configuration successfully.", 'success')
    end

    it "should drop purge-values when purging turned off" do
      @server_config_service.stub(:siteUrlFor).and_return { |url, forceSsl| url }
      stub_update_server_config(MailHost.new(com.thoughtworks.go.security.GoCipher.new), @ldap_config, @password_file_config, "newArtifactDir", nil, nil, nil, @should_allow_auto_login, nil, nil,nil,  localized_success_message, "foo_bar_baz")

      post :update, :server_configuration_form => @valid_security_config_params.merge(@valid_artifacts_setting).merge(:purgeArtifacts => "Never"), :cruise_config_md5 => "foo_bar_baz"

      assert_redirected_with_flash("/admin/config/server", "Saved configuration successfully.", 'success')
    end

    it "should send '0' for never terminate hung job" do
      @server_config_service.stub(:siteUrlFor).and_return { |url, forceSsl| url }
      stub_update_server_config(MailHost.new(com.thoughtworks.go.security.GoCipher.new), @ldap_config, @password_file_config, "newArtifactDir", nil, nil, '0', @should_allow_auto_login, nil, nil, nil, localized_success_message, "foo_bar_baz")

      post :update, :server_configuration_form => @valid_security_config_params.merge(@valid_artifacts_setting).merge(:purgeArtifacts => "Never", :timeoutType => 'neverTimeout'), :cruise_config_md5 => "foo_bar_baz"

      assert_redirected_with_flash("/admin/config/server", "Saved configuration successfully.", 'success')
    end

    it "should send overridden jobTimeout for terminate hung job" do
      @server_config_service.stub(:siteUrlFor).and_return { |url, forceSsl| url }
      stub_update_server_config(MailHost.new(com.thoughtworks.go.security.GoCipher.new), @ldap_config, @password_file_config, "newArtifactDir", nil, nil, '42', @should_allow_auto_login, nil, nil, nil,localized_success_message, "foo_bar_baz")

      post :update, :server_configuration_form => @valid_security_config_params.merge(@valid_artifacts_setting).merge(:purgeArtifacts => "Never", :timeoutType => 'overrideTimeout', :jobTimeout => '42'), :cruise_config_md5 => "foo_bar_baz"

      assert_redirected_with_flash("/admin/config/server", "Saved configuration successfully.", 'success')
    end

    it "should update the server site url and secure site url" do
      @server_config_service.stub(:siteUrlFor).and_return { |url, forceSsl| url }
      stub_update_server_config(MailHost.new(com.thoughtworks.go.security.GoCipher.new), @ldap_config, @password_file_config, "newArtifactDir", nil, nil, nil, @should_allow_auto_login, "http://site_url", "https://secure_site_url", nil, localized_success_message, "foo_bar_baz")
      post :update, :server_configuration_form => @valid_security_config_params.merge(@valid_artifacts_setting).merge(:purgeArtifacts => "Never", :siteUrl => "http://site_url", :secureSiteUrl => "https://secure_site_url"), :cruise_config_md5 => "foo_bar_baz"
      assert_redirected_with_flash("/admin/config/server", "Saved configuration successfully.", 'success')
    end

    it "should render error message if there is an error reported by the service" do
      controller.stub(:server_config_service).and_return(server_config_service = Object.new)
      result = nil
      server_config_service.should_receive(:updateServerConfig) do |mailhost, ldap, password, artifact_dir, purgeStart, purgeEnd, jobTimeout, should_allow_auto_login, siteUrl, secureSiteUrl, null, operation_result|
        operation_result.notAcceptable(LocalizedMessage.string("INVALID_FROM_ADDRESS"))
        result = operation_result
      end

      post :update, :server_configuration_form => @valid_mail_host_params

      assert_index_rendered_with_error("From address is not a valid email address.")
    end

    it "should update command repository location" do
      @server_config_service.stub(:siteUrlFor).and_return { |url, forceSsl| url }
      stub_update_server_config(@mail_host, @ldap_config, @password_file_config, "newArtifactDir", 10, 20, nil, @should_allow_auto_login, nil, nil, "value",localized_success_message, nil)
      post :update, :server_configuration_form => @valid_server_params.merge(:commandRepositoryLocation => "value")
      assert_redirected_with_flash("/admin/config/server", "Saved configuration successfully.", 'success')
    end

    describe "during concurrent edit" do
      it "on validation failure, should set cruise_config_md5 to old-md5 sent in request instead of latest md5" do
        @cruise_config.stub(:getMd5).and_return "new-md5"

        post :update, :server_configuration_form => @valid_server_params.merge(:port => "abcd"), :cruise_config_md5 => "old-md5"

        assigns[:cruise_config_md5].should == "old-md5"
      end
    end

    def assert_index_rendered_with_error(msg)
      flash = session[:notice]
      flash.to_s.should == msg
      flash.flashClass().should == 'error'
      assigns[:tab_name].should == "server_configuration"
    end

    def localized_success_message
      LocalizedMessage.string("SAVED_CONFIGURATION_SUCCESSFULLY")
    end

    def stub_update_server_config(mailhost, ldap, password, artifact_dir, purgeStart, purgeEnd, jobTimeout, should_allow_auto_login, siteUrl, secureSiteUrl, repo_location, localizable_message,md5)
      @server_config_service.should_receive(:updateServerConfig) do |actual_mailhost, actual_ldap, actual_password, actual_artifact_dir, actual_purgeStart, actual_purgeEnd, actual_jobTimeout, actual_should_allow_auto_login, actual_siteUrl, actual_secureSiteUrl, actual_repo_location, actual_operation_result,actual_md5|
        actual_mailhost.should == mailhost
        actual_ldap.should == ldap
        actual_password.should == password
        actual_artifact_dir.should == artifact_dir
        actual_purgeStart.should == purgeStart
        actual_purgeEnd.should == purgeEnd
        actual_jobTimeout.should == jobTimeout
        actual_should_allow_auto_login.should == should_allow_auto_login
        actual_siteUrl.should == siteUrl
        actual_secureSiteUrl.should == secureSiteUrl
        actual_repo_location.should == repo_location
        actual_md5.should == md5
        actual_operation_result.setMessage(localizable_message)
      end
    end
  end

  describe "validate" do

    before :each do
      controller.stub(:server_config_service).and_return(@server_config_service = Object.new)
      @default_localized_result = DefaultLocalizedResult.new
    end

    it "should resolve /admin/config/server/validate" do
      expect_any_instance_of(HeaderConstraint).to receive(:matches?).with(any_args).and_return(true)
      {:post => "/admin/config/server/validate"}.should route_to(:controller => "admin/server", :action => "validate")
    end

    it "should validate email" do
      @default_localized_result.invalid("INVALID_EMAIL", ["@foo.com"].to_java(java.lang.String))
      @server_config_service.should_receive(:validateEmail).with("@foo.com").and_return(@default_localized_result)

      get :validate, :email => "@foo.com"

      assigns[:result].should == @default_localized_result
    end

    it "should validate port" do
      @default_localized_result.invalid("INVALID_PORT", ["-1"].to_java(java.lang.String))
      @server_config_service.should_receive(:validatePort).with(-1).and_return(@default_localized_result)

      get :validate, :port => "-1"

      assigns[:result].should == @default_localized_result
    end

    it "should validate hostname" do
      @server_config_service.should_receive(:validateHostName).with("foo.com").and_return(@default_localized_result)

      get :validate, :hostName => "foo.com"

      assigns[:result].should == @default_localized_result
    end

    it "should return success if valid" do
      @server_config_service.should_receive(:validatePort).with(-1).and_return(@default_localized_result)
      @default_localized_result.invalid("INVALID_PORT", [].to_java(java.lang.Object))

      get :validate, :port => "-1"

      json = JSON.parse(response.body)
      json["error"].should == "Invalid port."
      json["success"].should == nil
    end

    it "should return error if invalid" do
      @server_config_service.should_receive(:validateHostName).with("foo.com").and_return(@default_localized_result)

      get :validate, :hostName => "foo.com"

      json = JSON.parse(response.body)
      json["error"].should == nil
      json["success"].should == "Valid"
    end
  end

  describe "test_email" do

    it "should resolve admin/config/server/test_email" do
      {:post => "/admin/config/server/test_email"}.should route_to(:controller => "admin/server", :action => "test_email")
    end

    it "should return error if sendTestEmail fails" do
      controller.stub(:server_config_service).and_return(@server_config_service = Object.new)
      mail_host = MailHost.new("blrstdcrspair02", 9999, "pavan", "strong_password", true, true, "from@from.com", "admin@admin.com")

      res = nil
      @server_config_service.should_receive(:sendTestMail).with(mail_host, an_instance_of(HttpLocalizedOperationResult)) do |mail_host, op_result|
        op_result.badRequest(LocalizedMessage.string('INVALID_PORT'))
        res = op_result
      end

      post :test_email, :server_configuration_form => @valid_mail_host_params

      json = JSON.parse(response.body)
      json["error"].should == "Invalid port."
      json["success"].should == nil
    end

    it "should validate port" do
      controller.stub(:server_config_service).and_return(@server_config_service = Object.new)

      post :test_email, :server_configuration_form => @valid_mail_host_params.except(:port)

      json = JSON.parse(response.body)
      json["error"].should == "Port is required."
      json["success"].should == nil
    end

    it "should send the test email by using the service" do
      controller.stub(:server_config_service).and_return(@server_config_service = Object.new)
      mail_host = MailHost.new("blrstdcrspair02", 9999, "pavan", "strong_password", true, true, "from@from.com", "admin@admin.com")

      @server_config_service.should_receive(:sendTestMail).with(mail_host, an_instance_of(HttpLocalizedOperationResult))

      post :test_email, :server_configuration_form => @valid_mail_host_params

      json = JSON.parse(response.body)
      json["error"].should == nil
      json["success"].should == "Sent test email successfully."
    end
  end
end
