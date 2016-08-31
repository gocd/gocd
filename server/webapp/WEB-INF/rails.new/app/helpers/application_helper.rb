##########################GO-LICENSE-START################################
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
##########################GO-LICENSE-END##################################

# Methods added to this helper will be available to all templates in the application.
module ApplicationHelper
  include Services
  include RailsLocalizer
  include JavaImports
  include PrototypeHelper

  GO_MESSAGE_KEYS = [:error, :notice, :success]

  def url_for_path(java_path, options = {})
    path        = java_path.sub(/^\//, "")
    url         = ((options[:only_path] == false) ? main_app.root_url : main_app.root_path)
    url, params = url.split("?")
    url = "#{url.gsub(/\/$/, "")}/#{path}"
    if params
      if url =~/\?/
        "#{url}&#{params}"
      else
        "#{url}?#{params}"
      end
    else
      url
    end
  end

  def url_for_job_identifier(id)
    url_for_path("/tab/build/detail/#{id.getPipelineName()}/#{id.getPipelineCounter()}/#{id.getStageName()}/#{id.getStageCounter()}/#{id.getBuildName()}")
  end

  def url_for_login
    url_for_path("/auth/login")
  end

  def url_for_job(job)
    url_for_job_identifier(job.getIdentifier())
  end

  def url_for_pipeline(pipeline_name, options = {})
    url_for_path("/tab/pipeline/history/#{pipeline_name}", options)
  end

  def path_for_stage(stage_identifier)
    stage_identifier = stage_identifier.getIdentifier() if stage_identifier.respond_to? :getIdentifier
    stage_detail_tab_path pipeline_name:    stage_identifier.getPipelineName(),
                          pipeline_counter: stage_identifier.getPipelineCounter(),
                          stage_name:       stage_identifier.getStageName(),
                          stage_counter:    stage_identifier.getStageCounter()
  end

  def stage_identifier_for_locator(stage_locator_string)
    stage_fragments = stage_locator_string.scan(/(.+)\/(\d+)\/(.+)\/(\d+)/).flatten
    com.thoughtworks.go.domain.StageIdentifier.new(stage_fragments[0], stage_fragments[1].to_i, stage_fragments[2], stage_fragments[3])
  end

  def tab_for(name, options = {})
    display_name = name.upcase
    tab_with_display_name(name, display_name, options)
  end

  def tab_with_display_name(name, display_name, options={})
    options.reverse_merge!(link: :enabled, class: "", anchor_class: "", url: name)
    url = url_for_path(options[:url])
    css_class = "current" if ((@current_tab_name == name) || url.match(/#{url_for}$/))
    link_body = options[:link] != :enabled ? "<span>#{display_name}</span>" : link_to(display_name, url, target: options[:target], class: options[:anchor_class])
    "<li id='cruise-header-tab-#{name.gsub(/\s+/, '-')}' class='#{css_class} #{options[:class]}'>\n" + link_body + "\n</li>"
  end

  def load_flash_message key
    flash_message_service.get(key.to_s) || session.delete(key) || (default_key?(key) ? load_from_flash : nil)
  end

  def load_from_flash
    GO_MESSAGE_KEYS.each do |key|
      val = flash[key]
      if val
        return com.thoughtworks.go.presentation.FlashMessageModel.new(Array(val).join(". ").to_s, key.to_s)
      end
    end
    return nil
  end

  def session_has key
    session[key] || (default_key?(key) && GO_MESSAGE_KEYS.inject(false) { |found, key| found || flash.key?(key) })
  end

  def default_key? key
    key == :notice
  end

  def random_dom_id(prefix = "")
    prefix + java.util.UUID.randomUUID().to_s
  end

  def sanitize_for_dom_id(value)
    value.gsub(".", "_dot_").tr("^a-zA-Z0-9_-", "_")
  end

  def onclick_lambda(options)
    on_click_lambda = ''
    if options.has_key? :onclick_lambda
      options.reverse_merge!(id: random_dom_id)
      on_click_lambda = "<script type='text/javascript'> Util.on_load(function() { Event.observe($('#{options[:id]}'), 'click', function(evt) { #{options.delete(:onclick_lambda)}(evt); }); }); </script>".html_safe
    end
    [on_click_lambda, options]
  end

  def submit_button name, options={}
    # DESIGN TODO: this is used for action/submit buttons on environments, pipeline dashboard, etc.  Probably not 100% complete to match the features above
    options = HashWithIndifferentAccess.new(options)
    options.reverse_merge!(type: 'submit')
    options.merge!(disabled: 'disabled') unless system_environment.isServerActive()
    options[:value] = name
    lambda_text, options_without_onclick = onclick_lambda(options)
    if (options[:type] == "image")
      button_body = image_button(name, options_without_onclick)
    else
      button_body = options[:type] == "select" ?
        select_button(name, options_without_onclick) :
        options[:type] == "header_select" ?
          header_select_button(name, options_without_onclick) :
          default_button(name, options_without_onclick)
    end
    button_body + lambda_text
  end

  def add_class css_class_string, *new_classes
    (Array(css_class_string) + new_classes).join(" ")
  end

  def select_button name, options
    options[:class] = add_class(options[:class], 'select')
    options[:type]  = "button"
    image_url       = 'g9/button_select_icon.png'
    if options[:text_color] == 'dark'
      image_url = 'g9/button_select_icon_dark.png'
    end
    content_tag(:button, button_content(name, tag(:img, src: image_path(image_url))), button_options(options), false)
  end

  def header_select_button name, options
    options[:class] = add_class(options[:class], 'header_submit')
    options[:type]  = "button"
    content_tag(:button, button_content(name), button_options(options), false)
  end

  def image_button name, options
    options[:class] = add_class(options[:class], 'image')
    options[:type]  = "submit"
    options[:title] = name
    content_tag(:button, content_tag(:span, ' ', title: name), button_options(options), false)
  end

  def default_button name, options
    content_tag(:button, button_content(name), button_options(options), false)
  end

  def button_content name, *other_content
    value = other_content.empty? ? name.upcase : name.upcase + other_content.first.to_s
    content_tag(:span, value, nil, false)
  end

  def button_options options
    options[:class] = add_class(options[:class], ['submit', options[:type], options[:disabled] ? 'disabled' : nil].compact.uniq)
    options[:disabled] && (options[:disabled] = 'disabled')
    options
  end

  def mycruise_available?
    go_config_service.isSecurityEnabled()
  end

  def use_compressed_js?
    system_environment.use_compressed_js()
  end

  # TODO: #130 - ugly hack. need to figure out what we can do. move dependent methods to controller as helper methods?
  def current_user
    instance_variable_get(:@user)
  end

  # TODO: #130 - ugly hack. need to figure out what we can do. move dependent methods to controller as helper methods?
  def cruise_config_md5
    config_md5 = instance_variable_get(:@cruise_config_md5)
    raise "md5 for config file has not been loaded yet" if config_md5.nil?
    config_md5
  end

  def can_view_admin_page?
    security_service.canViewAdminPage(current_user)
  end

  def has_operate_permission_for_agents?
    security_service.hasOperatePermissionForAgents(current_user)
  end

  def is_user_a_group_admin?
    security_service.isUserGroupAdmin(current_user)
  end

  def is_user_an_admin?
    security_service.isUserAdmin(current_user)
  end

  def is_user_a_template_admin?
    security_service.isAuthorizedToViewAndEditTemplates(current_user)
  end

  def is_user_a_template_admin_for_template? template_name
    security_service.isAuthorizedToEditTemplate(template_name, current_user)
  end

  def is_plugins_enabled?
    system_environment.get(com.thoughtworks.go.util.SystemEnvironment.PLUGIN_FRAMEWORK_ENABLED)
  end

  def render_json(options={})
    options = options.merge({locals: {scope: {}}}) unless options.has_key? :locals
    render(options).to_json
  end

  def page_name
    controller_name = params[:controller]
    #required for view specs
    if controller_name == nil
      controller_name = controller.controller_path
    end
    @page_name = controller_name.gsub("/", "_")
    if !flash[:error].nil?
      @page_name= @page_name + " page_error"
    end

    return @page_name
  end

  def version
    ApiV1::VersionRepresenter.version.full_version
  end

  def go_update
    version_info_service.getGoUpdate
  end

  def check_go_updates?
    version_info_service.isGOUpdateCheckEnabled
  end

  def json_escape data
    data.respond_to?(:java_class) && (data = data.to_s)
    data.is_a?(String) ? data.to_json.gsub(/\A"|"\Z/, "") : data.to_json
  end

  def id_for(obj, prefix = nil)
    "#{prefix || obj.class}_#{obj.object_id}"
  end

  def auto_refresh?
    params[:autoRefresh] != "false"
  end

  def pipeline_operations_blocking_form_remote_tag(options = {})
    pipeline_operations_form_remote_tag(options)
  end

  def pipeline_operations_form_remote_tag(options = {})
    options[:form] = true
    options[:html] ||= {}
    options[:html][:onsubmit] =
        (options[:html][:onsubmit] ? options[:html][:onsubmit] + "; " : "")
    form_tag(options[:html].delete(:action) || url_for(options[:url]), options[:html])
  end

  def merge_block_options(options)
    options[:before] = "AjaxRefreshers.disableAjax();" + (options[:before] || "")
    options[:complete] = "AjaxRefreshers.enableAjax();" + (options[:complete] || "")
    options[401] = "redirectToLoginPage('#{url_for_login}');" + (options[401] || "")
  end

  def blocking_form_remote_tag(options = {})
    merge_block_options(options)
    form_remote_tag(options)
  end

  def blocking_link_to_remote(name, options = {}, html_options = nil)
    merge_block_options(options)
    link_to_remote(name, options, html_options)
  end

  def blocking_link_to_remote_new(options = {})
    [:name, :url, :update, :html, :before].each {|key| raise "Expected key: #{key}. Didn't find it. Found: #{options.keys.inspect}" unless options.key?(key)}
    merge_block_options(options)
    options[:method] = "post"

    tag_options = tag_options(options[:html], true)
    %Q|<a href="#" #{tag_options} onclick="#{remote_function_new(options)}; return false;">#{options[:name]}</a>|
  end

  def remote_function_new(options)
    javascript_options = options_for_ajax(options)

    update = ''
    if options[:update] && options[:update].is_a?(Hash)
      update  = []
      update << "success:'#{options[:update][:success]}'" if options[:update][:success]
      update << "failure:'#{options[:update][:failure]}'" if options[:update][:failure]
      update  = '{' + update.join(',') + '}'
    elsif options[:update]
      update << "'#{options[:update]}'"
    end

    function = update.empty? ?
        "new Ajax.Request(" :
        "new Ajax.Updater(#{update}, "

    url_options = options[:url]
    url_options = url_options.merge(escape: false) if url_options.is_a?(Hash)
    function << "'#{escape_javascript(url_for(url_options))}'"
    function << ", #{javascript_options})"

    function = "#{options[:before]}; #{function}" if options[:before]
    function = "#{function}; #{options[:after]}"  if options[:after]
    function = "if (#{options[:condition]}) { #{function}; }" if options[:condition]
    function = "if (confirm('#{escape_javascript(options[:confirm])}')) { #{function}; }" if options[:confirm]

    return function
  end

  def link_to_remote_new(name, options = {}, html_options = nil)
    raise "Expected link name. Didn't find it." unless name
    [:method, :url].each {|key| raise "Expected key: #{key}. Didn't find it. Found: #{options.keys.inspect}" unless options.key?(key)}

    %Q|<a href="#" #{tag_options(html_options) unless html_options.nil?} onclick="new Ajax.Request('#{options[:url]}', {asynchronous:true, evalScripts:true, method:'#{options[:method]}', onSuccess:function(request){#{options[:success]}}}); return false;">#{name}</a>|
  end

  def end_form_tag
    "</form>".html_safe
  end

  def check_for_cancelled_contents(state, options={} )
    # DESIGN TODO: this is used to see if an X should be placed inside an element (usually a status bar, or color_code block)
    if state.to_s == 'Cancelled'
      contents =  "<img src='#{image_path('g9/stage_bar_cancelled_icon.png')}' alt='' />"
    else
      contents = ""
    end
    return contents.html_safe
  end

  def content_wrapper_tag(options={})
    "<div class=\"content_wrapper_outer\"><div class=\"content_wrapper_inner\">".html_safe
  end

  def end_content_wrapper()
    "</div></div>".html_safe
  end

  def selections
    Array(params[:selections]).map do |entry|
      TriStateSelection.new(*entry)
    end
  end

  def to_operation_result_json(localized_result, success_msg=localized_result.message(localizer))
    if localized_result.isSuccessful()
      {success: success_msg}.to_json
    else
      {error: localized_result.message(localizer)}.to_json
    end
  end

  def number? s
    Integer(s) rescue false
  end

  def make_https url
    uri = org.apache.commons.httpclient.URI.new(url, "UTF-8")
    org.apache.commons.httpclient.URI.new("https", uri.getUserinfo(), uri.getHost(), system_environment.getSslServerPort(), uri.getPath(), uri.getQuery(), uri.getFragment()).to_s
  end

  def config_md5_field
    hidden_field_tag('cruise_config_md5', cruise_config_md5)
  end

  def unauthorized_access
    @status == 401
  end

  def required_label(form, name, text)
    text = text + "<span class='asterisk'>#{l.string("REQUIRED_FIELD")}</span>"
    form.label(name, text.html_safe)
  end

  def required_label_text(text)
    text = text + "<span class='asterisk'>#{l.string("REQUIRED_FIELD")}</span>"
    text.html_safe
  end

  def label_with_hint(form, name, text, hint, required)
    if (required != nil)
      text = text + "<span class='asterisk'>#{l.string("REQUIRED_FIELD")}</span>"
    end
    text = text + "<span class='hint'>"+hint+"</span>"
    form.label(name, text.html_safe)
  end

  def render_pluggable_template(task_view_model, options = {})
    # The view is self here since this method will be called only from views.
    options.merge!(view: self)
    options.reject{|key, val| key.is_a?(String)}.map{|key, val| options[key.to_s] = val}
    view_rendering_service.render(task_view_model, options)
  end

  def render_pluggable_form_template(task_view_model, form_provider, options = {})
    options.merge!("formNameProvider" => form_provider)
    render_pluggable_template task_view_model, options
  end

  def form_name_provider form
    TopLevelFormNameProvider.new(form.instance_variable_get("@object_name"))
  end

  def is_ie8? user_agent
    !(user_agent =~ /MSIE 8.0/).blank?
  end

  def view_cache_key
    @view_cache_key ||= com.thoughtworks.go.server.ui.ViewCacheKey.new
  end

  def register_defaultable_list nested_name
    hidden_field_tag 'default_as_empty_list[]', nested_name, id: nil
  end

  def form_remote_tag_new(options = {})
    form_remote_tag(options)
  end

  def is_pipeline_config_spa_enabled?
    Toggles.isToggleOn(Toggles.PIPELINE_CONFIG_SINGLE_PAGE_APP)
  end

  def is_agents_spa_enabled?
    Toggles.isToggleOn(Toggles.AGENTS_SINGLE_PAGE_APP);
  end

  private
  def form_remote_tag(options = {})
    options[:form] = true

    options[:html] ||= {}
    options[:html][:onsubmit] =
        ((options[:html][:onsubmit] ? options[:html][:onsubmit] + "; " : "") +
            "#{remote_function(options)}; return false;").html_safe

    form_tag(options[:html].delete(:action) || url_for(options[:url]), options[:html])
  end

  # This method used to be in Rails 2.3. Was removed in Rails 3 or so. So, this is needed for compatibility.
  def remote_function options
    update =  options[:update]
    url = escape_javascript(url_for(options[:url]))
    retry_section = options.key?(202) ? "on202:function(request){#{options[202]}}, " : ""
    success_section = options.key?(:success) ? "onSuccess:function(request){#{options[:success]}}, " : ""
    complete_section = options.key?(:complete)? "onComplete:function(request){#{options[:complete]}}, "  : ""
    failure_section = options.key?(:failure)? "onFailure:function(request){#{options[:failure]}}, "  : ""
    before_section = options.key?(:before)? "#{options[:before]} "  : ""
    if update.nil? || update.empty? then
      %Q|#{options[:before]}; new Ajax.Request('#{url}', {asynchronous:true, evalScripts:true, #{retry_section}on401:function(request){#{options[401]}}, onComplete:function(request){#{options[:complete]}}, #{success_section}parameters:Form.serialize(this)})|
    else
      %Q|#{before_section}new Ajax.Updater({success:'#{options[:update][:success]}'}, '#{url}', {asynchronous:true, evalScripts:true, #{failure_section}#{complete_section}#{success_section}parameters:Form.serialize(this)})|

    end
  end
end