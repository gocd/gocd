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
      def initialize(options)
        @groups = options[:pipeline_groups]
        @user = options[:user]

        super(options)
      end

      link :self do |opts|
        opts[:url_builder].apiv2_show_dashboard_url
      end

      link :doc do
        'https://api.go.cd/current/#dashboard'
      end

      collection :pipeline_groups, embedded: true, exec_context: :decorator, decorator: PipelineGroupRepresenter

      collection :pipelines, embedded: true, exec_context: :decorator, decorator: PipelineRepresenter

      def pipeline_groups
        @groups.inject([]) {|r, e| r << {pipeline_group: e, user: @user}}
      end

      def pipelines
        @groups.inject([]) {|r, e| r + e.allPipelines()}.inject([]) {|r, e| r << {pipeline: e, user: @user}}
      end
    end
  end
end