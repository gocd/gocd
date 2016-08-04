##########################################################################
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
##########################################################################

module ApiV3
  module AuthenticationHelper
    def check_user_and_404
      return unless security_service.isSecurityEnabled()
      if current_user.try(:isAnonymous)
        Rails.logger.info("User '#{current_user.getUsername}' attempted to perform an unauthorized action!")
        render_not_found_error
      end
    end

    def check_user_can_see_pipeline
      return unless security_service.isSecurityEnabled()
      unless security_service.hasViewPermissionForPipeline(string_username, params[:pipeline_name])
        Rails.logger.info("User '#{current_user.getUsername}' attempted to perform an unauthorized action!")
        render_unauthorized_error
      end
    end

    def check_admin_user_and_401
      return unless security_service.isSecurityEnabled()
      unless security_service.isUserAdmin(current_user)
        Rails.logger.info("User '#{current_user.getUsername}' attempted to perform an unauthorized action!")
        render_unauthorized_error
      end
    end

    def check_pipeline_group_admin_user_and_401
      groupName = params[:group] || go_config_service.findGroupNameByPipeline(com.thoughtworks.go.config.CaseInsensitiveString.new(params[:pipeline_name]))
      return unless security_service.isSecurityEnabled()
      unless is_user_an_admin_for_group?(current_user, groupName)
        Rails.logger.info("User '#{current_user.getUsername}' attempted to perform an unauthorized action!")
        render_unauthorized_error
      end
    end


    def verify_content_type_on_post
      if [:put, :post, :patch].include?(request.request_method_symbol) && !request.raw_post.blank? && request.content_mime_type != :json
        render ApiV3::BaseController::DEFAULT_FORMAT => { message: "You must specify a 'Content-Type' of 'application/json'" }, status: :unsupported_media_type
      end
    end

    def render_not_found_error
      render ApiV3::BaseController::DEFAULT_FORMAT => { :message => 'Either the resource you requested was not found, or you are not authorized to perform this action.' }, :status => 404
    end

    def render_bad_request(exception)
      render ApiV3::BaseController::DEFAULT_FORMAT => { :message => "Your request could not be processed. #{exception.message}" }, :status => 400
    end

    def render_unauthorized_error
      render ApiV3::BaseController::DEFAULT_FORMAT => { :message => 'You are not authorized to perform this action.' }, :status => 401
    end

    private
    def is_user_an_admin_for_group?(current_user, group_name)
      if go_config_service.groups().hasGroup(group_name)
        return security_service.isUserAdminOfGroup(current_user.getUsername, group_name)
      else
        return security_service.isUserAdmin(current_user)
      end
    end

  end
end
