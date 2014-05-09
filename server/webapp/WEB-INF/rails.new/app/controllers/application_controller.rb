class ApplicationController < ActionController::Base
  # Prevent CSRF attacks by raising an exception.
  # For APIs, you may want to use :null_session instead.
  protect_from_forgery with: :exception

  before_filter :set_current_user

  def self.service_with_alias_name(alias_name, bean_name)
    define_method alias_name do
      instance_variable_get("@#{alias_name}") || instance_variable_set("@#{alias_name}", Spring.bean(bean_name))
    end
    helper_method alias_name
  end

  def self.services(*args)
    args.each do |name|
      name = name.to_s
      service_with_alias_name(name, name.camelize(:lower))
    end
  end

  services(:agent_service, :artifacts_service, :backup_service, :changeset_service, :go_cache, :go_config_file_dao, :go_config_service, :go_license_service, :dependency_material_service, :environment_config_service, :environment_service, :environment_service,
           :job_instance_service, :job_presentation_service, :licensed_agent_count_validator, :localizer, :material_service, :pipeline_config_service, :pipeline_history_service, :pipeline_lock_service, :pipeline_scheduler, :pipeline_stages_feed_service,
           :pipeline_unlock_api_service, :properties_service, :security_service, :server_config_service, :server_health_service, :stage_service, :system_environment, :user_service, :user_search_service, :failure_service,
           :mingle_config_service, :schedule_service, :flash_message_service, :template_config_service, :shine_dao, :xml_api_service, :pipeline_pause_service, :luau_service,
           :task_view_service, :view_rendering_service, :role_service, :server_status_service, :pipeline_configs_service, :pipeline_service, :material_update_service,
           :system_service, :default_plugin_manager, :command_repository_service, :value_stream_map_service, :admin_service, :config_repository, :package_repository_service, :package_definition_service, :pipeline_sql_map_dao, :pluggable_task_service, :garage_service)

  service_with_alias_name(:go_config_service_for_url, "goConfigService")

  # user
  def set_current_user
    @user = com.thoughtworks.go.server.util.UserHelper.getUserName()
    @user_id = session[com.thoughtworks.go.server.util.UserHelper.getSessionKeyForUserId()]
  end

  def current_user
    @user
  end

  def current_user_entity_id
    @user_id
  end

  def string_username
    CaseInsensitiveString.str(current_user.getUsername())
  end

  def current_user_id_for_oauth
    string_username
  end

  def current_user_id
    current_user.getUsername() == CaseInsensitiveString.new("anonymous") ? nil : string_username
  end

  # flash message
  def redirect_with_flash(msg, options)
    redirect_to url_options_with_flash(msg, options)
  end

  def url_options_with_flash(msg, options)
    params = options[:params] || {}
    options.merge(:params => params.merge(:fm => set_flash_message(msg, options.delete(:class))))
  end

  def set_flash_message(msg, klass)
    flash_message_service.add(FlashMessageModel.new(msg, klass))
  end
end
