##########################GO-LICENSE-START################################
# Copyright 2017 ThoughtWorks, Inc.
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

module Admin
  module AuthorizationHelper
    def check_admin_user_and_401
      return unless security_service.isSecurityEnabled()
      unless security_service.isUserAdmin(current_user)
        Rails.logger.info("User '#{current_user.getUsername}' attempted to perform an unauthorized action!")
        render 'shared/config_error', status: 401
      end
    end

    def check_admin_or_template_admin_and_401
      template_name = params[:pipeline_name]
      return unless security_service.isSecurityEnabled

      if !security_service.isUserAdmin(current_user) && !security_service.isAuthorizedToEditTemplate(com.thoughtworks.go.config.CaseInsensitiveString.new(template_name), current_user)
        Rails.logger.info("User '#{current_user.getUsername}' attempted to perform an unauthorized action!")
        render 'shared/config_error', status: 401
      end
    end

    def check_view_access_to_template_and_401
      return unless security_service.isSecurityEnabled
      template_name = params[:template_name] || params[:name]
      unless security_service.isAuthorizedToViewTemplate(com.thoughtworks.go.config.CaseInsensitiveString.new(template_name), current_user)
        Rails.logger.info("User '#{current_user.getUsername}' attempted to perform an unauthorized action!")
        render 'shared/config_error', status: 401
      end
    end

    def check_admin_user_or_group_admin_user_and_401
      return unless security_service.isSecurityEnabled()
      unless security_service.isUserAdmin(current_user) || security_service.isUserGroupAdmin(current_user)
        Rails.logger.info("User '#{current_user.getUsername}' attempted to perform an unauthorized action!")
        render 'shared/config_error', status: 401
      end
    end
  end
end