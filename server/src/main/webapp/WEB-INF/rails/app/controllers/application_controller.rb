#
# Copyright 2024 Thoughtworks, Inc.
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

  before_action :set_current_user, :populate_config_validity

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

  def populate_config_validity
    @config_valid = go_config_service.checkConfigFileValid().isValid()
  end

  def is_user_an_admin?
    security_service.isUserAdmin(current_user)
  end

  private

  def no_layout?
    params[:no_layout] == true || params[:no_layout] == 'true'
  end
end
