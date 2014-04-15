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

# Filters added to this controller apply to all controllers in the application.
# Likewise, all the methods added will be available for all controllers.
require 'hash_map_key'
require 'pp'

class ApplicationController < ActionController::Base

  helper :all # include all helpers, all the time
  protect_from_forgery # See ActionController::RequestForgeryProtection for details

  include JavaImports
  include RailsLocalizer
  include ::Oauth2::Provider::ApplicationControllerMethods

  attr_accessor_with_default :error_template_for_request, 'shared/error'

  helper_method :l, :view_cache_key

  before_filter :licensed_agent_limit, :set_current_user, :local_access_only, :populate_config_validity, :set_site_urls_in_thread, :populate_health_messages

  LOCAL_ONLY_ACTIONS = Hash.new([]).merge("api/server" => ["info"])

  # Scrub sensitive parameters from your log
  # filter_parameter_logging :password

  def string_username
    CaseInsensitiveString.str(current_user.getUsername())
  end

  def current_user_id_for_oauth
    string_username
  end

  def current_user_id
    current_user.getUsername() == CaseInsensitiveString.new("anonymous") ? nil : string_username
  end

  def local_access_only
    LOCAL_ONLY_ACTIONS[params[:controller]].include?(params[:action]) ? allow_local_only : true
  end

  if RAILS_ENV == 'production'
    include ActionRescue
  end

  def unresolved
    render_error_response l.urlNotKnown(url_for), 404, false
  end

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

  def set_current_user
    @user = com.thoughtworks.go.server.util.UserHelper.getUserName()
    @user_id = session[com.thoughtworks.go.server.util.UserHelper.getSessionKeyForUserId()]
  end

  def licensed_agent_limit
    licensed_agent_count_validator.updateServerHealth()
  end

  def current_user
    @user
  end

  def current_user_entity_id
    @user_id
  end

  helper_method :current_user

  def url_for(options = {})
    if options.respond_to?(:has_key?)
      options.reverse_merge!(:only_path => true)
      force_ssl = (options.delete(:protocol) == "https")
    end
    cache_key_part = force_ssl ? "-force_ssl=true" : ""
    cache_key = "rails_url_for-#{HashMapKey::hypen_safe_key_for(params)}-#{HashMapKey::hypen_safe_key_for(options)}#{cache_key_part}-#{request.host_with_port}-#{request.protocol}"
    unless url = go_cache.get(BaseUrlChangeListener::URLS_CACHE_KEY, cache_key)
      url = server_config_service.siteUrlFor(super(options), force_ssl || false)
      go_cache.put(BaseUrlChangeListener::URLS_CACHE_KEY, cache_key, url)
    end
    url
  end

  def render_if_error message, status
    return if (status < 400)
    render_error_response message, status, (params[:no_layout] == true)
    return true
  end

  def render_error_template(message, status)
    @status, @message = status, message
    render error_template_for_request, :status => @status, :layout => 'application'
  end

  def render_text_with_status(message, status)
    unless message == nil || message.last == "\n"
      message = message + "\n"
    end
    render :text => message, :status => status
  end

  def render_error_response message, status, is_text
    if is_text
      render_text_with_status(message, status)
    else
      render_error_template(message, status)
    end
  end

  def render_operation_result(result)
    render_if_error(result.detailedMessage(), result.httpCode()) || render_text_with_status(result.detailedMessage(), result.httpCode())
  end

  def render_localized_operation_result(result)
    message = result.message(Spring.bean('localizer'))
    render_if_error(message, result.httpCode()) || render_text_with_status(message, result.httpCode())
  end

  def render_operation_result_if_failure(result)
    result.httpCode() >= 400 && render_operation_result(result)
  end

  def default_url_options(options = nil)
    # bug with the rails test framework where it does not setup the params before invoking this causing a NPE
    return {} unless params
    params["autoRefresh"] ? {"autoRefresh" => params["autoRefresh"]} : {}
  end

  def allow_local_only
    return true if request_from_localhost?
    render_if_error("Unauthorized", 401)
    false
  end

  def request_from_localhost?
    SystemUtil.isLocalhost(request.env["SERVER_NAME"], request.env["REMOTE_ADDR"])
  end

  def view_cache_key
    @view_cache_key ||= com.thoughtworks.go.server.ui.ViewCacheKey.new
  end

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

  def set_success_flash(msg)
    set_flash_message(l.string(msg), "success")
  end

  def set_error_flash(msg, *args)
    set_flash_message(l.string(msg, args.to_java(java.lang.Object)), "error")
  end

  helper_method :servlet_request

  def servlet_request
    request.env['java.servlet_request']
  end


  def default_as_empty_list
    (params.delete(:default_as_empty_list) || []).each do |locator|
      do_param_defaulting(params, locator.split(/\>/))
    end
    return true
  end

  def do_param_defaulting sub_map, nested_keys
    nested_keys.empty? && return
    sub_map[nested_keys.first] ||= ((nested_keys.length > 1) ? {} : [])
    do_param_defaulting(sub_map[nested_keys.first], nested_keys[1..-1])
  end

  def register_defaultable_list nested_name
    "<input type=\"hidden\" name=\"default_as_empty_list[]\" value=\"#{nested_name}\"/>"
  end

  helper_method :register_defaultable_list

  def cruise_config_md5
    raise "md5 for config file has not been loaded yet" if @cruise_config_md5.nil?
    @cruise_config_md5
  end

  helper_method :cruise_config_md5

  def populate_config_validity
    @config_valid = go_config_service.checkConfigFileValid().isValid()
  end

  def set_site_urls_in_thread
    server = go_config_service_for_url.getCurrentConfig().server()
    Thread.current[:base_url] = server.getSiteUrl().getUrl()
    Thread.current[:ssl_base_url] = server.getHttpsUrl().getUrl()
  end

  def render_with_error_render_check *args
    (@error_rendered && performed?) || render_without_error_render_check(*args)
  end

  alias_method_chain :render, :error_render_check

  def redirect_to_with_error_render_check *args
    (@error_rendered && performed?) || redirect_to_without_error_render_check(*args)
  end

  alias_method_chain :redirect_to, :error_render_check

  def populate_health_messages
    @current_server_health_states = server_health_service.getAllValidLogs(go_config_service.getCurrentConfig())
  end

#  def load_plugin_errors
#    yield
#    [:error, :notice, :success].each do |key|
#      val = flash[key]
#      session[:notice] = FlashMessageModel.new(val.to_s, key.to_s) if val && !val.is_a?(FlashMessageModel)
#    end
#  end
end
