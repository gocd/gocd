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

class AdminController < ApplicationController
  include ::Admin::ConfigContextHelper
  include ::Admin::AuthorizationHelper

  layout "admin"
  prepend_before_filter :default_as_empty_list, :only => [:update]
  before_filter :enable_admin_error_template
  before_filter :load_context

  GO_CONFIG_ERROR_HEADER = 'Go-Config-Error'

  protected
  def save_popup(md5, save_action, render_error_options_or_proc = {:action => :new, :layout => false}, url_options = {}, flash_success_message = "Saved successfully.", &load_data)
    render_error_options_or_proc.reverse_merge(:layout => false) unless render_error_options_or_proc.is_a?(Proc)
    save(md5, render_error_options_or_proc, save_action, flash_success_message, load_data) do |message|
      render(:text => 'Saved successfully', :location => url_options_with_flash(message, {:action => :index, :class => 'success'}.merge(url_options)))
    end
  end

  def set_save_redirect_url url
    @onsuccess_redirect_uri = url
  end

  def save_page(md5, redirect_url, render_error_options_or_proc, save_action, success_message = "Saved successfully.", &load_data)
    set_save_redirect_url redirect_url
    save(md5, render_error_options_or_proc, save_action, success_message, load_data) do |message|
      url = com.thoughtworks.go.util.UrlUtil.urlWithQuery(@onsuccess_redirect_uri, "fm", set_flash_message(message, "success"))
      redirect_to(url)
    end
  end

  protected

  def render_error_with_options(options)
    render(options)
  end

  def assert_loaded(*args)
    options = args.extract_options!
    successful = true

    args.inject(@asserted_variables ||= {}) do |map, name|
      unless (var = instance_variable_get("@#{name}"))
        Rails.logger.warn("Could not load '#{name}', rendering failure #{caller[0..10].join("\n")}")
        if(@should_not_render_layout)
          options = options.merge(:layout => nil)
        end
        render_assertion_failure(options)
        successful = false
      end
      map[name] = var
    end
    successful
  end

  def render_assertion_failure(options)
    return if @error_rendered
    @message = options.delete(:message) || l.string("ERROR_OCCURRED_WHILE_UPDATING_CONFIG")
    @error_rendered = true
    options[:status] ||= (@update_result && @update_result.httpCode()) || 404
    render({:template => "shared/config_error.html", :layout => action_has_layout? ? "application" : nil}.merge(options))
    false
  end

  def assert_load(name, value, message = nil, status = nil)
    instance_variable_set("@#{name}", value)
    assert_loaded(name, {:message => message, :status => status})
  end

  def assert_load_eval(name, message = nil, status = nil)
    instance_variable_set("@#{name}", yield)
  rescue
    instance_variable_set("@#{name}", nil)
  ensure
    return assert_loaded(name, {:message => message, :status => status})
  end

  def flatten_all_errors(errors)
    errors.collect { |e| e.getAll().to_a }.flatten.uniq
  end

  def render_error(result, errors, options)
    @errors = flatten_all_errors(errors)
    flash.now[:error] = result.message(localizer)
    performed? || render_error_with_options(options.merge({:status => result.httpCode()}))
  end

  private

  def save(md5, render_error_options_or_proc, save_action, success_message, load_data)
    @update_result = HttpLocalizedOperationResult.new
    update_response = go_config_service.updateConfigFromUI(save_action, md5, current_user, @update_result)
    @cruise_config, @node, @subject, @config_after = update_response.getCruiseConfig(), update_response.getNode(), update_response.getSubject(), update_response.configAfterUpdate()

    unless @update_result.isSuccessful()
      @config_file_conflict = (@update_result.httpCode() == 409)
      flash.now[:error] = @update_result.message(localizer)
      response.headers[GO_CONFIG_ERROR_HEADER] = flash[:error]
    end

    begin
      load_data.call
    rescue
      Rails.logger.error $!
      render_assertion_failure({})
    end
    return if @error_rendered

    if @update_result.isSuccessful()
      success_message = "#{success_message} #{l.string("CONFIG_MERGED")}" if update_response.wasMerged()
      yield success_message
    else
      all_errors_on_other_objects = update_response.getCruiseConfig().getAllErrorsExceptFor(@subject)
      if render_error_options_or_proc.is_a?(Proc)
        render_error_options_or_proc.call(@update_result, all_errors_on_other_objects)
      else
        render_error(@update_result, all_errors_on_other_objects, render_error_options_or_proc)
      end
    end
  end

  def enable_admin_error_template
    self.error_template_for_request = 'shared/config_error'
  end

  def load_context
    assert_load :config_context, create_config_context(go_config_service.registry)
  end
end