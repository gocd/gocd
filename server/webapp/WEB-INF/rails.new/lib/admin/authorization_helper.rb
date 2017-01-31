module Admin
  module AuthorizationHelper
    def check_admin_user_and_401
      return unless security_service.isSecurityEnabled()
      unless security_service.isUserAdmin(current_user)
        Rails.logger.info("User '#{current_user.getUsername}' attempted to perform an unauthorized action!")
        render 'shared/config_error'
      end
    end

    def check_admin_or_template_admin_and_401
      template_name = params[:pipeline_name]
      return unless security_service.isSecurityEnabled

      if !security_service.isUserAdmin(current_user) && !security_service.isAuthorizedToEditTemplate(template_name, current_user)
        Rails.logger.info("User '#{current_user.getUsername}' attempted to perform an unauthorized action!")
        render 'shared/config_error'
      end
    end
  end
end