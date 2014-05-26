class ApplicationController < ActionController::Base
  include Services
  include JavaImports

  # Prevent CSRF attacks by raising an exception.
  # For APIs, you may want to use :null_session instead.
  protect_from_forgery with: :exception

  before_filter :set_current_user, :populate_health_messages, :local_access_only

  LOCAL_ONLY_ACTIONS = Hash.new([]).merge("api/server" => ["info"])

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

  def local_access_only
    LOCAL_ONLY_ACTIONS[params[:controller]].include?(params[:action]) ? allow_local_only : true
  end

  def allow_local_only
    return true if request_from_localhost?
    render_if_error("Unauthorized", 401)
    false
  end

  def request_from_localhost?
    SystemUtil.isLocalhost(request.env["SERVER_NAME"], request.env["REMOTE_ADDR"])
  end

  def unresolved
    render_error_response l.urlNotKnown(url_for), 404, false
  end

  def render_if_error message, status
    return if (status < 400)
    render_error_response message, status, (params[:no_layout] == true)
    return true
  end

  def render_error_template(message, status)
    @status, @message = status, message
    render error_template_for_request, status: @status, layout: 'application'
  end

  def render_text_with_status(message, status)
    unless message == nil || message.last == "\n"
      message = message + "\n"
    end
    render text: message, status: status
  end

  def render_error_response message, status, is_text
    if is_text
      render_text_with_status(message, status)
    else
      render_error_template(message, status)
    end
  end

  def render_localized_operation_result(result)
    message = result.message(Spring.bean('localizer'))
    render_if_error(message, result.httpCode()) || render_text_with_status(message, result.httpCode())
  end
end
