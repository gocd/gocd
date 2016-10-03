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

describe ServerConfigurationForm do

  describe "to_bases_collection" do
    it "should convert new line separated entry to bases collection" do
      input = " foo,bar, baz \nbase2\r\n\n base3 "
      form = ServerConfigurationForm.new({})
      actual = form.to_bases_collection input
      actual.size.should == 3
      actual[0].getValue().should == " foo,bar, baz "
      actual[1].getValue().should == "base2"
      actual[2].getValue().should == " base3 "
    end

    it "should ignore empty lines when constructing base config" do
      input = "  \nfoo  \n  \n  "
      form = ServerConfigurationForm.new({})
      actual = form.to_bases_collection input
      actual.size.should == 1
    end

    it "should not construct base config for empty" do
      input = "  \n  "
      form = ServerConfigurationForm.new({})
      actual = form.to_bases_collection input
      actual.size.should == 0
    end
  end

  describe "from_bases_collection" do
    it "should convert bases collection to new line separated entry" do
      bases = BasesConfig.new([BaseConfig.new('base1'), BaseConfig.new('base2')].to_java(BaseConfig))
      actual = ServerConfigurationForm.from_bases_collection(bases)
      actual.should == "base1\r\nbase2"
    end
  end

  describe "to_ldap_config" do
    it "should construct ldap config" do
      input = "foo,bar, baz\nbase2\n\nbase3\n\n\r\n\n\n\n\n\n\r\n"
      form = ServerConfigurationForm.new({:ldap_search_base => input})
      actual = form.to_ldap_config
      actual.searchBases().should_not == nil
      actual.searchBases().size().should == 3
    end
  end

  describe "allow_auto_login" do
    it "should default it to true when a new instance is created without a value being provided for it" do
      form = ServerConfigurationForm.new({})
      expect(form.allow_auto_login).to eq nil
      expect(form.should_allow_auto_login).to eq true
    end

    it "should set it when a new instance is created" do
      form = ServerConfigurationForm.new({:allow_auto_login => "true"})
      expect(form.allow_auto_login).to eq "true"
      expect(form.should_allow_auto_login).to eq true

      form = ServerConfigurationForm.new({:allow_auto_login => "false"})
      expect(form.allow_auto_login).to eq "false"
      expect(form.should_allow_auto_login).to eq false
    end

    it "should set allow_auto_login to 'true' when form is being created from_server_config and isAllowOnlyKnownUsersToLogin is false" do
      @ldap_config = LdapConfig.new("ldap://test.com", "test", "password", @encrypted_password, true,BasesConfig.new([BaseConfig.new('base1'), BaseConfig.new('base2')].to_java(BaseConfig)), "searchFilter")
      @password_file_config = PasswordFileConfig.new("path")
      @security_config = SecurityConfig.new(@ldap_config, @password_file_config, false)
      @mail_host = MailHost.new("blrstdcrspair02", 9999, "pavan", "strong_password", true, true, "from@from.com", "admin@admin.com")

      form = ServerConfigurationForm.from_server_config(com.thoughtworks.go.config.ServerConfig.new(@security_config, @mail_host))
      expect(form.allow_auto_login).to eq "true"
      expect(form.should_allow_auto_login).to eq true
    end

    it "should set allow_auto_login to 'false' when form is being created from_server_config and isAllowOnlyKnownUsersToLogin is true" do
      @ldap_config = LdapConfig.new("ldap://test.com", "test", "password", @encrypted_password, true,BasesConfig.new([BaseConfig.new('base1'), BaseConfig.new('base2')].to_java(BaseConfig)), "searchFilter")
      @password_file_config = PasswordFileConfig.new("path")
      @security_config = SecurityConfig.new(@ldap_config, @password_file_config, true)
      @mail_host = MailHost.new("blrstdcrspair02", 9999, "pavan", "strong_password", true, true, "from@from.com", "admin@admin.com")

      form = ServerConfigurationForm.from_server_config(com.thoughtworks.go.config.ServerConfig.new(@security_config, @mail_host))
      expect(form.allow_auto_login).to eq "false"
      expect(form.should_allow_auto_login).to eq false
    end
  end
end
