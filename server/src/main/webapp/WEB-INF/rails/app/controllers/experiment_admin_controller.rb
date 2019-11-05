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

class ExperimentAdminController < AdminController
  include ::Admin::AuthorizationHelper

  protected
  def save_popup(md5, save_action, render_error_options_or_proc = {:action => :new, :layout => false}, url_options = {}, flash_success_message = "Saved successfully.", &load_data)
    render_error_options_or_proc.reverse_merge(:layout => false) unless render_error_options_or_proc.is_a?(Proc)
    save(md5, render_error_options_or_proc, save_action, flash_success_message, load_data) do |message|
      render(:plain => 'Saved successfully', :location => url_options_with_flash(message, {:action => :index, :class => 'success'}.merge(url_options)))
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

  private

  def save(md5, render_error_options_or_proc, save_action, success_message, load_data)
    @update_result = HttpLocalizedOperationResult.new
    @cruise_config = go_config_service.getCurrentConfig

    # TODO read the group as a param
    # group = @cruise_config.findGroupOfPipeline(@pipeline).group

    # correct_md5 = entity_hashing_service.md5ForEntity(@pipeline, group)
    puts "params: #{params}"
    pipeline_config_service.updatePipelineConfig(current_user, @pipeline, params[:pipeline_group_name], params[:pipeline_md5], @update_result)
    # update_response = go_config_service.updateConfigFromUI(save_action, md5, current_user, @update_result)
    # @cruise_config, @node, @subject, @config_after = update_response.getCruiseConfig(), update_response.getNode(), update_response.getSubject(), update_response.configAfterUpdate()

    # if @update_result.isSuccessful()
      # success_message = "#{success_message} #{'The configuration was modified by someone else, but your changes were merged successfully.'}" if update_response.wasMerged()
      # yield success_message
    unless @update_result.isSuccessful
      @config_file_conflict = (@update_result.httpCode() == 409)
      flash.now[:error] = @update_result.message()
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
      # all_errors_on_other_objects = update_response.getCruiseConfig().getAllErrorsExceptFor(@subject)
      all_errors_on_other_objects = []
      if render_error_options_or_proc.is_a?(Proc)
        render_error_options_or_proc.call(@update_result, all_errors_on_other_objects)
      else
        render_error(@update_result, all_errors_on_other_objects, render_error_options_or_proc)
      end
    end
  end

end
