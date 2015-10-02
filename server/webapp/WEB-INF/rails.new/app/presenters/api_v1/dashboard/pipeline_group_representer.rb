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
  module Dashboard

    class PipelineGroupRepresenter < ApiV1::BaseRepresenter
      alias_method :pipeline_dashboard, :represented

      link :self do |opts|
        opts[:url_builder].pipeline_group_config_list_api_url
      end

      link :doc do
        'http://api.go.cd/current/#pipeline-groups'
      end

      property :getName, as: :name
      collection :pipelines, embedded: true, exec_context: :decorator, decorator: PipelineRepresenter

      def pipelines
        pipeline_dashboard.getPipelineModels()
      end
    end
  end
end