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

class FailuresController < ApplicationController
  include ParamEncoder

  layout false
  decode_params :suite_name, :test_name, :only => 'show'

  def show
    job_id = JobIdentifier.new(StageIdentifier.new(params[:pipeline_name], params[:pipeline_counter].to_i, params[:stage_name], params[:stage_counter]), params[:job_name])
    result = HttpLocalizedOperationResult.new
    @failure_details = failure_service.failureDetailsFor(job_id, params[:suite_name], params[:test_name], current_user, result)
    render_localized_operation_result(result) unless result.isSuccessful()
  end
end