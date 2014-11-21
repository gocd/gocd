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

java_import 'org.springframework.dao.DataRetrievalFailureException'

class Api::StagesController < Api::ApiController
  include ApplicationHelper

  def index
    return render_not_found unless number?(params[:id])

    stage_id = Integer(params[:id])
    begin
      @stage = stage_service.stageById(stage_id)
      @doc = xml_api_service.write(StageXmlViewModel.new(@stage), "#{request.protocol}#{request.host_with_port}/go")
      respond_to do |format|
        format.xml
      end
    rescue Exception => e
      logger.error(e)
      return render_not_found
    end
  end

  def cancel
    stage_id = Integer(params[:id])
    result = HttpLocalizedOperationResult.new
    schedule_service.cancelAndTriggerRelevantStages(stage_id, current_user, result)
    render_localized_operation_result result
  end

  def cancel_stage_using_pipeline_stage_name
    pipeline_name = params[:pipeline_name]
    stage_name = params[:stage_name]
    result = HttpLocalizedOperationResult.new
    schedule_service.cancelAndTriggerRelevantStages(pipeline_name,stage_name, current_user, result)
    render_localized_operation_result result
  end

  def history
    pipeline_name = params[:pipeline_name]
    stage_name = params[:stage_name]
    offset = params[:offset].to_i
    page_size = 10
    stage_instance_count = stage_service.getCount(pipeline_name, stage_name)
    result = HttpOperationResult.new

    pagination = Pagination.pageStartingAt(offset, stage_instance_count, page_size)
    stage_history = stage_service.findDetailedStageHistoryByOffset(pipeline_name, stage_name, pagination, CaseInsensitiveString.str(current_user.getUsername()), result)

    if result.canContinue()
      stage_history_api_model = StageHistoryAPIModel.new(pagination, stage_history)
      render json: stage_history_api_model
    else
      render_error_response(result.detailedMessage(), result.httpCode(), true)
    end
  end

  def instance_by_counter
    pipeline_name = params[:pipeline_name]
    pipeline_counter = params[:pipeline_counter].to_i
    stage_name = params[:stage_name]
    stage_counter = params[:stage_counter]
    result = HttpOperationResult.new

    stage_model = stage_service.findStageWithIdentifier(pipeline_name, pipeline_counter, stage_name, stage_counter, CaseInsensitiveString.str(current_user.getUsername()), result)

    if result.canContinue()
      stage_api_model = StageAPIModel.new(stage_model)
      render json: stage_api_model
    else
      render_error_response(result.detailedMessage(), result.httpCode(), true)
    end
  end

  private
  def render_not_found()
    render text: "Not Found!", status: 404
  end
end
