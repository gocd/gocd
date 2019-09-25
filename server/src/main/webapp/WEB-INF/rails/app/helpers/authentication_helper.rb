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

module AuthenticationHelper
  def check_user_and_404
    return unless security_service.isSecurityEnabled()
    if current_user.try(:isAnonymous)
      Rails.logger.info("User '#{current_user.getUsername}' attempted to perform an unauthorized action!")
      render_not_found_error
    end
  end

  def check_user_and_403
    return unless security_service.isSecurityEnabled()
    if current_user.try(:isAnonymous)
      Rails.logger.info("User '#{current_user.getUsername}' attempted to perform an unauthorized action!")
      render_forbidden_error
    end
  end

  def check_user_can_see_pipeline
    return unless security_service.isSecurityEnabled()
    unless security_service.hasViewPermissionForPipeline(current_user, params[:pipeline_name])
      Rails.logger.info("User '#{current_user.getUsername}' attempted to perform an unauthorized action!")
      render_forbidden_error
    end
  end

  def check_admin_user_and_403
    return unless security_service.isSecurityEnabled()
    unless security_service.isUserAdmin(current_user)
      Rails.logger.info("User '#{current_user.getUsername}' attempted to perform an unauthorized action!")
      render_forbidden_error
    end
  end

  def check_admin_or_template_admin_and_403
    template_name = params[:template_name]
    return unless security_service.isSecurityEnabled

    if template_name.blank? && !(security_service.isUserAdmin(current_user) || security_service.isAuthorizedToViewAndEditTemplates(current_user))
      Rails.logger.info("User '#{current_user.getUsername}' attempted to perform an unauthorized action!")
      render_forbidden_error
    end
    if !template_name.blank? && !security_service.isAuthorizedToEditTemplate(com.thoughtworks.go.config.CaseInsensitiveString.new(template_name), current_user)
      Rails.logger.info("User '#{current_user.getUsername}' attempted to perform an unauthorized action!")
      render_forbidden_error
    end
  end

  def check_view_access_to_template_and_403
    return unless security_service.isSecurityEnabled
    template_name = params[:template_name]
    if !template_name.blank? && !security_service.isAuthorizedToViewTemplate(com.thoughtworks.go.config.CaseInsensitiveString.new(template_name), current_user)
      Rails.logger.info("User '#{current_user.getUsername}' attempted to perform an unauthorized action!")
      render_forbidden_error
    end
    if template_name.blank? && !security_service.isAuthorizedToViewTemplates(current_user)
      Rails.logger.info("User '#{current_user.getUsername}' attempted to perform an unauthorized action!")
      render_forbidden_error
    end
  end

  def check_admin_user_or_group_admin_user_and_403
    return unless security_service.isSecurityEnabled()
    if !(security_service.isUserAdmin(current_user) || security_service.isUserGroupAdmin(current_user))
      Rails.logger.info("User '#{current_user.getUsername}' attempted to perform an unauthorized action!")
      render_forbidden_error
    end
  end

  def check_any_admin_user_and_403
    return unless security_service.isSecurityEnabled()
    if !(security_service.isUserAdmin(current_user) || security_service.isUserGroupAdmin(current_user) || security_service.isAuthorizedToViewAndEditTemplates(current_user))
      Rails.logger.info("User '#{current_user.getUsername}' attempted to perform an unauthorized action!")
      render_forbidden_error
    end
  end

  def check_pipeline_group_admin_user_and_403
    return unless security_service.isSecurityEnabled()
    groupName = params[:group] || go_config_service.findGroupNameByPipeline(com.thoughtworks.go.config.CaseInsensitiveString.new(params[:pipeline_name]))
    unless is_user_an_admin_for_group?(current_user, groupName)
      Rails.logger.info("User '#{current_user.getUsername}' attempted to perform an unauthorized action!")
      render_forbidden_error
    end
  end

  def verify_content_type_on_post
    if [:put, :post, :patch].include?(request.request_method_symbol) && !request.raw_post.blank? && request.content_mime_type != :json
      render_message("You must specify a 'Content-Type' of 'application/json'" ,:unsupported_media_type)
    end
  end

  def render_not_found_error
    render_message('Either the resource you requested was not found, or you are not authorized to perform this action.', 404)
  end

  def render_bad_request(exception)
    render_message("Your request could not be processed. #{exception.message}", 400)
  end

  def render_forbidden_error
    render_message('You are not authorized to perform this action.', 403)
  end

  private
  def is_user_an_admin_for_group?(current_user, group_name)
    if security_service.isUserAdmin(current_user)
      return true
    end
    if go_config_service.groups().hasGroup(group_name)
      return security_service.isUserAdminOfGroup(current_user.getUsername, group_name)
    else
      false
    end
  end
end
