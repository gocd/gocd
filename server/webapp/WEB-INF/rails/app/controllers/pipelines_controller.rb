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

class PipelinesController < ApplicationController
  include ApplicationHelper
  layout "application"

  skip_before_action :verify_authenticity_token, only: [:update_comment]

  def build_cause
    result = HttpOperationResult.new
    @pipeline_instance = pipeline_history_service.findPipelineInstance(params[:pipeline_name], params[:pipeline_counter].to_i, current_user, result)

    if result.canContinue()
      render "build_cause", layout: false
    else
      render_operation_result_if_failure(result)
    end
  end

  def update_comment
    result = HttpLocalizedOperationResult.new

    pipeline_history_service.updateComment(params[:pipeline_name], params[:pipeline_counter].to_i, params[:comment], current_user, result)
    if result.isSuccessful()
      render json: { status: 'success' }
    else
      render_localized_operation_result(result)
    end
  end

end
