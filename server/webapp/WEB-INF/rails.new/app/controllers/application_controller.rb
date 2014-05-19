class ApplicationController < ActionController::Base
  include Services

  # Prevent CSRF attacks by raising an exception.
  # For APIs, you may want to use :null_session instead.
  protect_from_forgery with: :exception

  before_filter :set_current_user

  before_filter :set_current_user, :populate_health_messages

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

  # health messages
  def populate_health_messages
      @current_server_health_states = server_health_service.getAllValidLogs(go_config_service.getCurrentConfig())
  end
end
