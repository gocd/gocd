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

# Methods added to this helper will be available to all templates in the application.
module ApplicationHelper
  include Services
  include RailsLocalizer

  GO_MESSAGE_KEYS = [:error, :notice, :success]

  def url_for_path(java_path, options = {})
    path = java_path.sub(/^\//, "")
    url = ((options[:only_path] == false) ? root_url : root_path)
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

  def path_for_stage(stage_identifier)
    stage_identifier = stage_identifier.getIdentifier() if stage_identifier.respond_to? :getIdentifier
    stage_detail_path :pipeline_name => stage_identifier.getPipelineName(),
                      :pipeline_counter => stage_identifier.getPipelineCounter(),
                      :stage_name => stage_identifier.getStageName(),
                      :stage_counter => stage_identifier.getStageCounter()
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
    options.reverse_merge!(:link => :enabled, :class => "", :anchor_class => "", :url => name)
    url = url_for_path(options[:url])
    css_class = "current" if ((@current_tab_name == name) || url.match(/#{url_for}$/))
    link_body = options[:link] != :enabled ? "<span>#{display_name}</span>" : link_to(display_name, url, :target => options[:target], :class => options[:anchor_class])
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

  def onclick_lambda(options)
    on_click_lambda = ''
    if options.has_key? :onclick_lambda
      options.reverse_merge!(:id => random_dom_id)
      on_click_lambda = "<script type='text/javascript'> Util.on_load(function() { Event.observe($('#{options[:id]}'), 'click', function(evt) { #{options.delete(:onclick_lambda)}(evt); }); }); </script>"
    end
    [on_click_lambda, options]
  end

  def submit_button name, options={}
    # DESIGN TODO: this is used for action/submit buttons on environments, pipeline dashboard, etc.  Probably not 100% complete to match the features above
    options = HashWithIndifferentAccess.new(options)
    options.reverse_merge!(:type => 'submit')
    options[:value] = name
    lambda_text, options_without_onclick = onclick_lambda(options)
    if( options[:type] == "image" )
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
    options[:type] = "button"
    image_url = '/images/g9/button_select_icon.png'
    if options[:text_color] == 'dark'
      image_url = '/images/g9/button_select_icon_dark.png'
    end
    content_tag(:button, button_content(name, tag(:img, :src => image_path(image_url))), button_options(options), false)
  end

  def header_select_button name, options
    options[:class] = add_class(options[:class], 'header_submit')
    options[:type] = "button"
    content_tag(:button, button_content(name), button_options(options), false)
  end

  def image_button name, options
    options[:class] = add_class(options[:class], 'image')
    options[:type] = "submit"
    options[:title] = name
    content_tag(:button, content_tag(:span, ' ', :title => name), button_options(options), false)
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
    options = options.merge({:locals => {:scope => {}}}) unless options.has_key? :locals
    render(options).to_json
  end

  def page_name
    @page_name = controller.controller_name.gsub("/", "_")
    if !flash[:error].nil?
      @page_name= @page_name + " page_error"
    end

    return @page_name
  end

  def version
    version_file = Rails.root.join("..", "vm", "admin", "admin_version.txt.vm")
    File.readlines(version_file)
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

  def end_form_tag
    "</form>"
  end

  def form_remote_tag(options = {})
    options[:form] = true

    options[:html] ||= {}
    options[:html][:onsubmit] =
        (options[:html][:onsubmit] ? options[:html][:onsubmit] + "; " : "") +
            "#{remote_function(options)}; return false;"

    form_tag(options[:html].delete(:action) || url_for(options[:url]), options[:html])
  end

  def form_tag(url_for_options = {}, options = {}, *parameters_for_url)
    html_options = html_options_for_form(url_for_options, options, *parameters_for_url)
    form_tag_html(html_options)
  end

  def check_for_cancelled_contents(state, options={} )
    # DESIGN TODO: this is used to see if an X should be placed inside an element (usually a status bar, or color_code block)
    if state.to_s == 'Cancelled'
      contents =  "<img src='#{image_path('/images/g9/stage_bar_cancelled_icon.png')}' alt='' />"
    else
      contents = ""
    end
    return contents
  end

  def content_wrapper_tag(options={})
    return "<div class=\"content_wrapper_outer\"><div class=\"content_wrapper_inner\">"
  end

  def end_content_wrapper()
    return "</div></div>"
  end

  def word_breaker(txt, break_at_length=15)
    loop_count = txt.length / break_at_length;
    new_txt = txt.clone
    i = 0
    while i < loop_count do
      i+=1
      new_txt.insert(i*break_at_length, "<wbr/>")
    end
    return new_txt
  end

  def selections
    Array(params[:selections]).map do |entry|
      TriStateSelection.new(*entry)
    end
  end

  def to_operation_result_json(localized_result, success_msg=localized_result.message(localizer))
    if localized_result.isSuccessful()
      {:success => success_msg}.to_json
    else
      {:error => localized_result.message(localizer)}.to_json
    end
  end

  def number? s
    Integer(s) rescue false
  end

  def smart_word_breaker(txt)
    splitTxt = txt.split(/-/)
    i=0
    while i < splitTxt.length do
      segment = splitTxt[i]
      break_at_length = 15
      divisions = (segment.length/break_at_length)
      if divisions >= 1
        j = 1
        while j <= divisions
          segment.insert(break_at_length*j, "<wbr/>")
          j += 1
        end
      end
      splitTxt[i] = segment
      i+=1
    end
    return splitTxt.join("-<wbr/>")
  end

  def make_https url
    uri = org.apache.commons.httpclient.URI.new(url, "UTF-8")
    org.apache.commons.httpclient.URI.new("https", uri.getUserinfo(), uri.getHost(), system_environment.getSslServerPort(), uri.getPath(), uri.getQuery(), uri.getFragment()).to_s
  end

  def config_md5_field
    "<input type=\"hidden\" name=\"cruise_config_md5\" value=\"#{cruise_config_md5}\"/>"
  end

  def unauthorized_access
    @status == 401
  end

  def required_label(form, name, text)
    text = text + "<span class='asterisk'>#{l.string("REQUIRED_FIELD")}</span>"
    form.label(name, text)
  end

  def required_label_text(text)
    text + "<span class='asterisk'>#{l.string("REQUIRED_FIELD")}</span>"
  end

  def label_with_hint(form, name, text, hint, required)
    if (required != nil)
      text = text + "<span class='asterisk'>#{l.string("REQUIRED_FIELD")}</span>"
    end
    text = text + "<span class='hint'>"+hint+"</span>"
    form.label(name, text)
  end

  def render_pluggable_template(task_view_model, options = {})
    # The view is self here since this method will be called only from views.
    options.merge!(:view => self)
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
end