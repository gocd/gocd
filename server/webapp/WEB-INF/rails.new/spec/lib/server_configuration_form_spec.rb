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
      @security_config = SecurityConfig.new(false)
      @mail_host = MailHost.new("blrstdcrspair02", 9999, "pavan", "strong_password", true, true, "from@from.com", "admin@admin.com")

      form = ServerConfigurationForm.from_server_config(com.thoughtworks.go.config.ServerConfig.new(@security_config, @mail_host))
      expect(form.allow_auto_login).to eq "true"
      expect(form.should_allow_auto_login).to eq true
    end

    it "should set allow_auto_login to 'false' when form is being created from_server_config and isAllowOnlyKnownUsersToLogin is true" do
      @security_config = SecurityConfig.new(true)
      @mail_host = MailHost.new("blrstdcrspair02", 9999, "pavan", "strong_password", true, true, "from@from.com", "admin@admin.com")

      form = ServerConfigurationForm.from_server_config(com.thoughtworks.go.config.ServerConfig.new(@security_config, @mail_host))
      expect(form.allow_auto_login).to eq "false"
      expect(form.should_allow_auto_login).to eq false
    end
  end
end
