##########################################################################
# Copyright 2015 ThoughtWorks, Inc.
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
##########################################################################

module ApiV1
  class StagesController < ApiV1::BaseController

    before_action :check_user_and_404
    before_action :check_user_can_see_pipeline

    def show
      pipeline_name    = params[:pipeline_name]
      pipeline_counter = params[:pipeline_counter].to_i
      stage_name       = params[:stage_name]
      stage_counter    = params[:stage_counter]
      result           = HttpOperationResult.new

      stage_model = stage_service.findStageWithIdentifier(pipeline_name, pipeline_counter, stage_name, stage_counter, string_username, result)

      if stage_model.instance_of?(com.thoughtworks.go.domain.NullStage)
        raise RecordNotFound
      else

        if result.isSuccess
          render DEFAULT_FORMAT => StageRepresenter.new(stage_model).to_hash(url_builder: self)
        else
          render_http_operation_result(result)
        end
      end
    end

    def history
      pipeline_name        = params[:pipeline_name]
      stage_name           = params[:stage_name]
      offset               = params[:offset].to_i
      page_size            = 10
      stage_instance_count = stage_service.getCount(pipeline_name, stage_name)
      result               = HttpOperationResult.new

      pagination    = Pagination.pageStartingAt(offset, stage_instance_count, page_size)
      stage_history = stage_service.findDetailedStageHistoryByOffset(pipeline_name, stage_name, pagination, CaseInsensitiveString.str(current_user.getUsername()), result)

      if stage_history.instance_of?(com.thoughtworks.go.presentation.pipelinehistory.NullStageHistoryItem)
        raise RecordNotFound
      else
        if result.isSuccess()
          render DEFAULT_FORMAT => StageHistoryRepresenter.new(stage_history, {pipeline_name: pipeline_name, stage_name: stage_name}).to_hash(url_builder: self)
        else
          render_http_operation_result(result)
        end
      end
    end

  end

end
