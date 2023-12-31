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

# Methods added to this helper will be available to all templates in the application.
module ApplicationHelper
  include Services
  include JavaImports
  include ActionView::Helpers::JavaScriptHelper

  GO_MESSAGE_KEYS = [:error, :notice, :success]

  def url_for_path(java_path)
    path = java_path.sub(/^\//, "")
    url, params = root_path.split("?")
    url = "#{url.gsub(/\/$/, "")}/#{path}"
    if params
      if url =~ /\?/
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

  def url_for_job(job)
    url_for_job_identifier(job.getIdentifier())
  end

  def url_for_pipeline(pipeline_name)
    url_for_path("/pipeline/activity/#{pipeline_name}")
  end

  def path_for_stage(stage_identifier)
    stage_identifier = stage_identifier.getIdentifier() if stage_identifier.respond_to? :getIdentifier
    stage_detail_tab_path_for pipeline_name: stage_identifier.getPipelineName(),
                              pipeline_counter: stage_identifier.getPipelineCounter(),
                              stage_name: stage_identifier.getStageName(),
                              stage_counter: stage_identifier.getStageCounter()
  end

  def duration_to_string(duration)
    org.joda.time.format.PeriodFormat.getDefault().print(duration.toPeriod())
  end

  def load_flash_message(key)
    flash_message_service.get(key.to_s) || session.delete(key) || (default_key?(key) ? load_from_flash : nil)
  end

  def load_from_flash
    GO_MESSAGE_KEYS.each do |key|
      val = flash[key]
      if val
        return com.thoughtworks.go.presentation.FlashMessageModel.new(Array(val).join(". ").to_s, key.to_s)
      end
    end
    nil
  end

  def default_key?(key)
    key == :notice
  end

  def random_dom_id(prefix = "")
    prefix + java.util.UUID.randomUUID().to_s
  end

  def submit_button(name, options = {})
    options = HashWithIndifferentAccess.new(options)
    options.reverse_merge!(type: 'submit')
    options[:value] ||= name
    content_tag(:button, button_content(name), button_options(options), false)
  end

  def add_class(css_class_string, *new_classes)
    (Array(css_class_string) + new_classes).join(" ")
  end

  def button_content(name, *other_content)
    value = other_content.empty? ? name.upcase : name.upcase + other_content.first.to_s
    content_tag(:span, value, nil, false)
  end

  def button_options(options)
    options[:class] = add_class(options[:class], ['submit', options[:type], options[:disabled] ? 'disabled' : nil].compact.uniq)
    options[:disabled] && (options[:disabled] = 'disabled')
    options
  end

  def current_user
    instance_variable_get(:@user)
  end

  def can_view_admin_page?
    security_service.canViewAdminPage(current_user)
  end

  def is_user_a_group_admin?
    security_service.isUserGroupAdmin(current_user)
  end

  def is_user_an_admin?
    security_service.isUserAdmin(current_user)
  end

  def is_user_authorized_to_view_templates?
    security_service.isAuthorizedToViewTemplates(current_user)
  end

  def current_gocd_version
    com.thoughtworks.go.CurrentGoCDVersion.getInstance().getGocdDistVersion()
  end

  def render_json(options = {})
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
    unless flash[:error].nil?
      @page_name = @page_name + " page_error"
    end

    @page_name
  end

  def version
    @@version ||= com.thoughtworks.go.CurrentGoCDVersion.getInstance().formatted()
  end

  def docs_url(suffix)
    CurrentGoCDVersion.docs_url(suffix)
  end

  def full_version
    @@full_version ||= com.thoughtworks.go.CurrentGoCDVersion.getInstance().fullVersion()
  end

  def id_for(obj, prefix = nil)
    "#{prefix || obj.class}_#{obj.object_id}"
  end

  def link_blocking_post_to_server(options = {})
    [:name, :url, :html].each { |key| raise "Expected key: #{key}. Didn't find it. Found: #{options.keys.inspect}" unless options.key?(key) }

    tag_options = raw tag.tag_options(options[:html], true)

    %Q|<a href="#" #{tag_options} onclick="#{javascript_post_to_server(options)}; return false;">#{options[:name]}</a>|
  end

  def javascript_post_to_server(options)
    "Util.ajaxUpdate('#{escape_javascript(url_for(options[:url]))}', '#{escape_javascript(options[:idForSpinner])}')"
  end

  def content_wrapper_tag
    "<div class=\"content_wrapper_outer\"><div class=\"content_wrapper_inner\">".html_safe
  end

  def end_content_wrapper
    "</div></div>".html_safe
  end

  def access_forbidden
    @status == 403
  end

  def is_server_in_maintenance_mode?
    maintenance_mode_service.isMaintenanceMode
  end

  def maintenance_mode_updated_on
    maintenance_mode_service.updatedOn if is_server_in_maintenance_mode?
  end

  def maintenance_mode_updated_by
    maintenance_mode_service.updatedBy if is_server_in_maintenance_mode?
  end

  def edit_path_for_pipeline(pipeline_name)
    "/go/admin/pipelines/#{pipeline_name}/general"
  end

  def compare_pipelines_path(options)
    options = options.with_indifferent_access
    "/go/compare/#{options[:pipeline_name]}/#{options[:from_counter]}/with/#{options[:to_counter]}"
  end

  def supports_analytics_dashboard?
    !default_plugin_info_finder.allPluginInfos(PluginConstants::ANALYTICS_EXTENSION).detect do |combined_plugin_info|
      combined_plugin_info.extensionFor(PluginConstants::ANALYTICS_EXTENSION).getCapabilities().supportsDashboardAnalytics()
    end.nil?
  end

  def supports_vsm_analytics?
    return false if show_analytics_only_for_admins? && !is_user_an_admin?

    !default_plugin_info_finder.allPluginInfos(PluginConstants::ANALYTICS_EXTENSION).detect do |combined_plugin_info|
      combined_plugin_info.extensionFor(PluginConstants::ANALYTICS_EXTENSION).getCapabilities().supportsVSMAnalytics()
    end.nil?
  end

  def vsm_analytics_chart_info
    plugin_info = first_plugin_which_supports_vsm_analytics
    if plugin_info
      supported_analytics = plugin_info.getCapabilities().supportedVSMAnalytics().get(0)
      {
        "type" => 'vsm',
        "id" => supported_analytics.getId(),
        "title" => supported_analytics.getTitle(),
        "plugin_id" => plugin_info.getDescriptor().id(),
        "url" => show_analytics_path({plugin_id: plugin_info.getDescriptor().id(), type: 'vsm', id: supported_analytics.getId()})
      }
    end
  end

  def stage_width_percent(total_number_of_stages, is_last_running_stage,total_width)
    last_running_width = is_last_running_stage ? 0 : 0
    round_to(((total_width / total_number_of_stages) - last_running_width), 4).to_s + "%"
  end

  private

  def round_to(float, precision)
    (float * 10**precision).round.to_f / 10**precision
  end

  def show_analytics_only_for_admins?
    system_environment.enableAnalyticsOnlyForAdmins
  end

  def first_plugin_which_supports_vsm_analytics
    first_analytics_combined_plugin_info = default_plugin_info_finder.allPluginInfos(PluginConstants::ANALYTICS_EXTENSION).find do |combined_plugin_info|
      extension_info = combined_plugin_info.extensionFor(PluginConstants::ANALYTICS_EXTENSION)
      extension_info.getCapabilities().supportsVSMAnalytics()
    end

    first_analytics_combined_plugin_info.extensionFor(PluginConstants::ANALYTICS_EXTENSION) if first_analytics_combined_plugin_info
  end

  def run_stage_path(options)
    options = options.with_indifferent_access
    options.reverse_merge!(params.slice(:pipeline_name, :pipeline_counter, :stage_name))
    "/go/api/stages/#{options[:pipeline_name]}/#{options[:pipeline_counter]}/#{options[:stage_name]}/run"
  end

  def cancel_stage_path(pipeline_instance_modal)
    active_stage = pipeline_instance_modal.activeStage
    "/go/api/stages/#{active_stage.getPipelineName}/#{active_stage.getPipelineCounter}/#{active_stage.getName}/#{active_stage.getCounter}/cancel"
  end

  def agent_detail_path(options)
    "/go/agents/#{options[:uuid]}"
  end
end
