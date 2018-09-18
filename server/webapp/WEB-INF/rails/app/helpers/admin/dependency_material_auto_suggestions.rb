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

module Admin
  module DependencyMaterialAutoSuggestions
    include JavaImports

    def pipeline_stages_json(cruise_config, current_user, security_service, params)
      pipelines = cruise_config.getAllLocalPipelineConfigs()
      pipeline_stages_array = Array.new
      pipelines.each do |pipeline|
        unless pipeline.name() == CaseInsensitiveString.new(params[:pipeline_name])
          pipeline.each do |stage|
            pipeline_stages_array.push({:pipeline=> pipeline.name().to_s, :stage => stage.name().to_s}) if security_service.hasViewOrOperatePermissionForPipeline(current_user, pipeline.name().to_s)
          end
        end
      end
      pipeline_stages_array.sort_by {|item| item[:pipeline].downcase}.to_json
    end
  end
end
