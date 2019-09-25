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

  def cancel_stage_using_pipeline_stage_name
    pipeline_name = params[:pipeline_name]
    stage_name = params[:stage_name]
    result = HttpLocalizedOperationResult.new
    schedule_service.cancelAndTriggerRelevantStages(pipeline_name,stage_name, current_user, result)
    render_localized_operation_result result
  end

  private
  def render_not_found()
    render plain: "Not Found!", status: 404
  end
end
