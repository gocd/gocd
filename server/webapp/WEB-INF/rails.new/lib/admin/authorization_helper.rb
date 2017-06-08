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