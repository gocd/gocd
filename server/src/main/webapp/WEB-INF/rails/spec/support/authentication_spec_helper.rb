#
# Copyright Thoughtworks, Inc.
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

module AuthenticationSpecHelper
  def login_as_user
    enable_security
    allow(controller).to receive(:current_user).and_return(@user = Username.new(CaseInsensitiveString.new(SecureRandom.hex)))
    allow(@security_service).to receive(:isUserAdmin).with(@user).and_return(false)
    allow(@security_service).to receive(:isUserGroupAdmin).with(@user).and_return(false)
    allow(@security_service).to receive(:isUserAdminOfGroup).with(anything, anything).and_return(false)
    allow(@security_service).to receive(:isAuthorizedToViewAndEditTemplates).with(@user).and_return(false)
    allow(@security_service).to receive(:isAuthorizedToEditTemplate).with(an_instance_of(CaseInsensitiveString), @user).and_return(false)
    allow(@security_service).to receive(:isAuthorizedToViewTemplate).with(an_instance_of(CaseInsensitiveString), @user).and_return(false)
    allow(@security_service).to receive(:isAuthorizedToViewTemplates).with(@user).and_return(false)
  end

  def disable_security
    allow(controller).to receive(:security_service).and_return(@security_service = double('security-service'))
    allow(@security_service).to receive(:isSecurityEnabled).and_return(false)
    allow(@security_service).to receive(:isUserAdmin).and_return(true)
  end

  def enable_security
    allow(controller).to receive(:security_service).and_return(@security_service = double('security-service'))
    allow(@security_service).to receive(:isSecurityEnabled).and_return(true)
  end

  def login_as_admin
    enable_security
    allow(controller).to receive(:current_user).and_return(@user = Username.new(CaseInsensitiveString.new(SecureRandom.hex)))
    allow(@security_service).to receive(:isUserAdmin).with(@user).and_return(true)
    allow(@security_service).to receive(:isAuthorizedToViewTemplate).with(an_instance_of(CaseInsensitiveString), @user).and_return(true)
    allow(@security_service).to receive(:isAuthorizedToViewTemplates).with(@user).and_return(true)
    allow(@security_service).to receive(:isAuthorizedToEditTemplate).with(an_instance_of(CaseInsensitiveString), @user).and_return(true)
    allow(@security_service).to receive(:isAuthorizedToViewAndEditTemplates).with(anything).and_return(true)
  end

  def login_as_group_admin
    enable_security
    allow(controller).to receive(:current_user).and_return(@user = Username.new(CaseInsensitiveString.new(SecureRandom.hex)))
    allow(@security_service).to receive(:isUserAdmin).with(@user).and_return(false)
    allow(@security_service).to receive(:isUserGroupAdmin).with(@user).and_return(true)
    allow(@security_service).to receive(:isUserAdminOfGroup).with(anything, anything).and_return(true)
  end

  def login_as_anonymous
    allow(controller).to receive(:current_user).and_return(@user = Username::ANONYMOUS)
    allow(@security_service).to receive(:isUserAdmin).with(@user).and_return(false)
    allow(@security_service).to receive(:isUserGroupAdmin).with(@user).and_return(false)
    allow(@security_service).to receive(:isAuthorizedToViewAndEditTemplates).with(@user).and_return(false)
    allow(@security_service).to receive(:isAuthorizedToEditTemplate).with(an_instance_of(CaseInsensitiveString), @user).and_return(false)
    allow(@security_service).to receive(:isAuthorizedToViewTemplate).with(an_instance_of(CaseInsensitiveString), @user).and_return(false)
    allow(@security_service).to receive(:isAuthorizedToViewTemplates).with(@user).and_return(false)
  end
end

