##########################################################################
# Copyright 2016 ThoughtWorks, Inc.
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

module ApiV4
  module Admin
    module Templates
      class PipelineConfigSummaryRepresenter < BaseRepresenter
        alias_method :pipeline, :represented

        link :self do |opts|
          opts[:url_builder].apiv3_admin_pipeline_url(represented.getPipelineName)
        end

        link :doc do |opts|
          'https://api.gocd.org/#pipeline-config'
        end

        link :find do |opts|
          opts[:url_builder].apiv3_admin_pipeline_url(pipeline_name: '__pipeline_name__').gsub(/__pipeline_name__/, ':pipeline_name')
        end

        property :name, case_insensitive_string: true, exec_context: :decorator
        property :can_edit, exec_context: :decorator

        private

        def name
          represented.getPipelineName
        end

        def can_edit
          represented.canUserEditPipeline()
        end
      end
    end
  end
end
