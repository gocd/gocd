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

module Gadgets
  class PipelineController < ::ApplicationController
    layout 'application', :except => [:content, :index]

    def index
      expires_in 24.hours, :public => true
      render layout: false
    end

    def content
      pipeline_name = params[:pipeline_name]

      if (pipeline_name == nil)
        render_error_response(l.string("GADGET_PIPELINE_NAME_MISSING", [:pipeline_name]), 400, true)
        return
      end

      if (pipeline_name.empty?)
        render_error_response(l.string("GADGET_PIPELINE_NAME_EMPTY", [:pipeline_name]), 400, true)
        return
      end

      if (!go_config_service.hasPipelineNamed(CaseInsensitiveString.new(pipeline_name)))
        render_error_response(l.string("PIPELINE_NOT_FOUND", [pipeline_name]), 404, true)
        return
      end

      if (!security_service.hasViewPermissionForPipeline(current_user, pipeline_name))
        render_error_response(l.string("NO_VIEW_PERMISSION_ON_PIPELINE", [current_user.getDisplayName(), pipeline_name]), 403, true)
        return
      end

      @pipeline = pipeline(pipeline_history_service.getActivePipelineInstance(current_user, pipeline_name), pipeline_name)

      render layout: false
    end

    private

    def pipeline(models, pipeline_name)
      models.get(0).getPipelineModel(pipeline_name)
    end
  end
end

