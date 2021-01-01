#
# Copyright 2021 ThoughtWorks, Inc.
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

class ApplicationController < ActionController::Base
  include Services
  helper SparkUrlAware
  include JavaImports

  class << self
    def inherited(klass)
      super
      descendants.each do |each_subclass|
        each_subclass.helper :all
      end
    end
  end

  # Prevent CSRF attacks by raising an exception.
  # For APIs, you may want to use :null_session instead.
  protect_from_forgery with: :exception, :except => :unresolved

  attr_accessor :error_template_for_request

  before_action :set_current_user, :local_access_only, :populate_config_validity, :set_site_urls_in_thread

  helper_method :current_user_id_for_oauth

  LOCAL_ONLY_ACTIONS = Hash.new([]).merge("api/server" => ["info"])

  if Rails.env.development?
    before_action do |controller|
      response.headers["X-Controller-Action"] = "#{params[:controller]}##{params[:action]}"
    end
  end

  if Rails.env.production?
    include ActionRescue
  end

  # in most places in java, we expect `params` to be a Map, which it currently is not.
  def params
    @_params ||= super.tap {|p| p.permit!}.to_unsafe_h
  end

  # user
  def set_current_user
    @user = SessionUtils::currentUsername()
    # See SessionUtils to get for context
    @user_id = session["GOCD_SECURITY_CURRENT_USER_ID"]
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

  def local_access_only
    LOCAL_ONLY_ACTIONS[params[:controller]].include?(params[:action]) ? allow_local_only : true
  end

  def allow_local_only
    return true if request_from_localhost?
    render_if_error("Forbidden", 403)
    false
  end

  def request_from_localhost?
    SystemUtil.isLocalhost(request.env["SERVER_NAME"], request.env["REMOTE_ADDR"])
  end

  def unresolved
    render_error_response 'The url you are trying to reach appears to be incorrect.', 404, false
  end

  def error_template_for_request
    @error_template_for_request || 'shared/error'
  end

  #FIXME could be moved to another helper
  def render_localized_operation_result(result)
    message = result.message()
    render_if_error(message, result.httpCode()) || render_text_with_status(message, result.httpCode())
  end

  def render_operation_result_if_failure(result)
    result.httpCode() >= 400 && render_operation_result(result)
  end

  def render_operation_result(result)
    render_if_error(result.detailedMessage(), result.httpCode()) || render_text_with_status(result.detailedMessage(), result.httpCode())
  end

  def render_if_error message, status
    return if (status < 400)
    render_error_response message, status, no_layout?
    return true
  end

  def render_error_response message, status, is_text
    if is_text
      render_text_with_status(message, status)
    else
      render_error_template(message, status)
    end
  end

  def render_message(message, status = :ok, data = {})
    render :json_hal_v1 => {message: message.strip}.merge(data), status: status
  end

  def render_error_template(message, status)
    @status, @message = status, message
    render error_template_for_request, status: @status, layout: 'application'
  end

  def render_text_with_status(message, status)
    unless message == nil || message.last == "\n"
      message = message + "\n"
    end
    render plain: message, status: status
  end

  def default_url_options
    # bug with the rails test framework where it does not setup the params before invoking this causing a NPE
    return {} unless params
    super.merge(params["autoRefresh"] ? {:autoRefresh => params["autoRefresh"]} : {})
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

  def cruise_config_md5
    raise "md5 for config file has not been loaded yet" if @cruise_config_md5.nil?
    @cruise_config_md5
  end

  helper_method :cruise_config_md5, :servlet_request

  def populate_config_validity
    @config_valid = go_config_service.checkConfigFileValid().isValid()
  end

  def set_site_urls_in_thread
    server = go_config_service_for_url.getCurrentConfig().server()
    Thread.current[:base_url] = server.getSiteUrl().getUrl()
    Thread.current[:ssl_base_url] = server.getHttpsUrl().getUrl()
  end

  def servlet_request
    request.env['java.servlet_request']
  end

  def is_user_an_admin?
    security_service.isUserAdmin(current_user)
  end

  def render_not_found_error
    render json: {message: 'Either the resource you requested was not found, or you are not authorized to perform this action.'}, status: 404
  end

  private

  def no_layout?
    params[:no_layout] == true || params[:no_layout] == 'true'
  end
end
