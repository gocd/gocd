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
  class PipelineOperationController < ApiV1::BaseController
    before_action :check_pipeline_group_admin_user_and_401

    def pause
      result = HttpLocalizedOperationResult.new
      pipeline_name = params[:pipeline_name]
      pause_cause = params[:pause_cause]
      pipeline_pause_service.pause(pipeline_name, pause_cause, current_user, result)
      render_http_operation_result(result)
    end

    def unpause
      result = HttpLocalizedOperationResult.new
      pipeline_name = params[:pipeline_name]
      pipeline_pause_service.unpause(pipeline_name, current_user, result)
      render_http_operation_result(result)
    end

  end
end
