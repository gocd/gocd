#
# Copyright 2019 ThoughtWorks, Inc.
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


#
# Calls the service used by the API (pipeline_config_service.updatePipelineConfig) instead of doing a full config save
#
class FastAdminController < AdminController
  include ::Admin::AuthorizationHelper

  protected
  def fast_save_popup(render_error_options_or_proc = {:action => :new, :layout => false}, url_options = {}, flash_success_message = "Saved successfully.", &load_data)
    render_error_options_or_proc.reverse_merge(:layout => false) unless render_error_options_or_proc.is_a?(Proc)
    fast_save(render_error_options_or_proc, flash_success_message, load_data) do |message|
      render(:plain => 'Saved successfully', :location => url_options_with_flash(message, {:action => :index, :class => 'success'}.merge(url_options)))
    end
  end

  def set_save_redirect_url url
    @onsuccess_redirect_uri = url
  end

  def fast_save_page(redirect_url, render_error_options_or_proc, success_message = "Saved successfully.", &load_data)
    set_save_redirect_url redirect_url
    fast_save(render_error_options_or_proc, success_message, load_data) do |message|
      url = com.thoughtworks.go.util.UrlUtil.urlWithQuery(@onsuccess_redirect_uri, "fm", set_flash_message(message, "success"))
      redirect_to(url)
    end
  end

  def render_error(result, errors, options)
    @errors = flatten_all_errors(errors)
    flash.now[:error] = result.httpCode == 422 ? "Save failed, see errors below" : result.message
    performed? || render_error_with_options(options.merge({:status => result.httpCode()}))
  end

  private

  def fast_save(render_error_options_or_proc, success_message, load_data)
    @cruise_config = go_config_service.getCurrentConfig
    @update_result = pipeline_config_service.updatePipelineConfig(current_user, @pipeline, params[:pipeline_group_name], params[:pipeline_digest])

    unless @update_result.isSuccessful
      @config_file_conflict = (@update_result.httpCode() == 409)
      flash.now[:error] = @update_result.httpCode == 422 ? "Save failed, see errors below" : @update_result.message
      response.headers[GO_CONFIG_ERROR_HEADER] = flash[:error]
    end

    begin
      load_data.call
    rescue
      Rails.logger.error $!
      render_assertion_failure({})
    end
    return if @error_rendered

    if @update_result.isSuccessful
      yield success_message
    else
      all_errors_on_other_objects = []
      if render_error_options_or_proc.is_a?(Proc)
        render_error_options_or_proc.call(@update_result, all_errors_on_other_objects)
      else
        render_error(@update_result, all_errors_on_other_objects, render_error_options_or_proc)
      end
    end
  end

end
