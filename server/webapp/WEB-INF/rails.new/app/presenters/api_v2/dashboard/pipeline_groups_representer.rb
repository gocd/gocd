##########################################################################
# Copyright 2017 ThoughtWorks, Inc.
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

module ApiV2
  module Dashboard
    class PipelineGroupsRepresenter < ApiV2::BaseRepresenter

      alias_method :all_pipelines_across_groups, :represented

      link :self do |opts|
        opts[:url_builder].apiv2_show_dashboard_url
      end

      link :doc do
        'https://api.go.cd/current/#dashboard'
      end

      collection :pipeline_groups, embedded: true, exec_context: :decorator, decorator: PipelineGroupRepresenter

      def pipeline_groups
        all_pipelines_across_groups
            .group_by {|pipeline| pipeline.groupName()}
            .collect {|group_name, pipelines_in_group| {:name => group_name, :pipelines => pipelines_in_group}}
      end
    end
  end
end