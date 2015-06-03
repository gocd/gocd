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
  class PipelineGroupsRepresenter < BaseRepresenter

    alias_method :pipeline_groups, :represented

    link :self do |opts|
      [
        {
          content_type: CONTENT_TYPE_API_V1,
          href:         opts[:url_builder].apiv1_show_dashboard_url
        },
        {
          content_type: CONTENT_TYPE_HTML,
          href:         opts[:url_builder].pipeline_dashboard_url
        }
      ]
    end

    link :doc do
      'http://www.go.cd/documentation/user/current/api/v1/pipelines.html'
    end

    collection :pipeline_groups, embedded: true, exec_context: :decorator, decorator: PipelineGroupRepresenter

  end
end
