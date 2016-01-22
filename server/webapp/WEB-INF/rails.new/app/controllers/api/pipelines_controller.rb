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

class Api::PipelinesController < Api::ApiController
  include ComparisonHelper
  def url(params={})#FIXME: we should not have such common method names(we work with all helpers mixed in each response, which means we need to be have more specific method names to avoid conflicts)
    api_pipeline_stage_feed_url(params)
  end

  def resource_url(params={})#FIXME: we should not have such common method names(we work with all helpers mixed in each response, which means we need to be have more specific method names to avoid conflicts)
    stage_url(params)
  end

  def page_url(stage_locator, options = {})#FIXME: we should not have such common method names(we work with all helpers mixed in each response, which means we need to be have more specific method names to avoid conflicts)
    stage_identifier = StageIdentifier.new(stage_locator)
    stage_detail_tab_url({:pipeline_name => stage_identifier.getPipelineName(),
                      :pipeline_counter => stage_identifier.getPipelineCounter().to_s,
                      :stage_name => stage_identifier.getStageName(),
                      :stage_counter => stage_identifier.getStageCounter()}.merge(options))
  end

  def history
    pipeline_name = params[:pipeline_name]
    offset = params[:offset].to_i
    page_size = 10
    pipeline_instance_count = pipeline_history_service.totalCount(pipeline_name)
    result = HttpOperationResult.new

    pagination = Pagination.pageStartingAt(offset, pipeline_instance_count, page_size)
    pipeline_history = pipeline_history_service.loadMinimalData(pipeline_name, pagination, current_user, result)

    if result.canContinue()
      pipeline_history_api_model = PipelineHistoryAPIModel.new(pagination, pipeline_history)
      render json: pipeline_history_api_model
    else
      render_error_response(result.detailedMessage(), result.httpCode(), true)
    end
  end

  def instance_by_counter
    pipeline_name = params[:pipeline_name]
    pipeline_counter = params[:pipeline_counter].to_i
    result = HttpOperationResult.new

    pipeline_instance_model = pipeline_history_service.findPipelineInstance(pipeline_name, pipeline_counter, current_user, result)

    if result.canContinue()
      pipeline_instance_api_model = PipelineInstanceAPIModel.new(pipeline_instance_model)
      render json: pipeline_instance_api_model
    else
      render_error_response(result.detailedMessage(), result.httpCode(), true)
    end
  end

  def status
    pipeline_name = params[:pipeline_name]
    result = HttpOperationResult.new

    pipeline_status = pipeline_history_service.getPipelineStatus(pipeline_name, CaseInsensitiveString.str(current_user.getUsername()), result)

    if result.canContinue()
      render json: PipelineStatusAPIModel.new(pipeline_status)
    else
      render_error_response(result.detailedMessage(), result.httpCode(), true)
    end
  end

  helper_method :url, :resource_url, :page_url

  def pipeline_instance
    pipeline = pipeline_history_service.load(params[:id].to_i, current_user, result = HttpOperationResult.new)
    if (result.canContinue())
      @doc = xml_api_service.write(PipelineXmlViewModel.new(pipeline), "#{request.protocol}#{request.host_with_port}/go")
    end
    render_operation_result_if_failure(result)
  end

  def pipelines
    @pipelines = pipeline_history_service.latestInstancesForConfiguredPipelines(current_user)
  end

  def stage_feed
    @title = @pipeline_name = params[:name]
    if !go_config_service.hasPipelineNamed(CaseInsensitiveString.new(@pipeline_name))
      render_error_response "Pipeline not found", 404, true
      return
    end
    @feed = Feed.new(current_user, pipeline_stages_feed_service.feedResolverFor(@pipeline_name), result = HttpLocalizedOperationResult.new, params)
    if (!result.isSuccessful)
      render_localized_operation_result(result)
      return
    end
    render content_type: 'application/atom+xml'
  end

  def releaseLock
    operation_result = HttpOperationResult.new
    pipeline_unlock_api_service.unlock(params[:pipeline_name], current_user, operation_result)
    render_operation_result(operation_result)
  end

  def schedule
    pipeline_name = params[:pipeline_name]
    revisions = merge_revisions(pipeline_name, params["materials"]||{}, params["original_fingerprint"]||{}, params['material_fingerprint']||{})
    pipeline_scheduler.manualProduceBuildCauseAndSave(pipeline_name, @user, ScheduleOptions.new(revisions, params[:variables]||{}, params[:secure_variables]||{}), result = HttpOperationResult.new)
    render_operation_result(result)
  end

  def card_activity
    card_numbers_set = changeset_service.getCardNumbersBetween(params[:pipeline_name], params[:from_pipeline_counter].to_i, params[:to_pipeline_counter].to_i, current_user, result = HttpLocalizedOperationResult.new, show_bisect?)
    if !result.isSuccessful
      render_localized_operation_result result
      return
    end
    render text: card_numbers_set.to_a.join(",")
  end

  def pause
    result = HttpLocalizedOperationResult.new
    pipeline_name = params[:pipeline_name]
    pause_cause = params[:pauseCause]
    pipeline_pause_service.pause(pipeline_name, pause_cause, current_user, result)
    render_localized_operation_result result
  end

  def unpause
    result = HttpLocalizedOperationResult.new
    pipeline_name = params[:pipeline_name]
    pipeline_pause_service.unpause(pipeline_name, current_user, result)
    render_localized_operation_result result
  end

  private
  def merge_revisions(pipeline_name, new_revisions_using_name, original_fingerprint, new_revisions_with_fingerprint)
    new_revisions_using_name.delete_if { |key, value| value.blank? }.each do |material_name, revision|
      material_config = go_config_service.findMaterialWithName(CaseInsensitiveString.new(pipeline_name), CaseInsensitiveString.new(material_name))
      fingerprint = material_config ? material_config.getPipelineUniqueFingerprint() : material_name
      new_revisions_with_fingerprint[fingerprint] = revision
    end
    original_fingerprint.merge(new_revisions_with_fingerprint.delete_if { |key, value| value.blank? })
  end
end
