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
    class PipelineInstanceRepresenter < ApiV1::BaseRepresenter
      alias_method :pipeline_instance, :represented

      link :self do |opts|
        opts[:url_builder].pipeline_instance_by_counter_api_url(pipeline_instance.getName(), pipeline_instance.getCounter())
      end

      link :doc do
        'https://api.go.cd/#get-pipeline-instance'
      end

      link :history_url do |opts|
        opts[:url_builder].pipeline_history_url(pipeline_instance.getName())
      end

      link :vsm_url do |opts|
        opts[:url_builder].vsm_show_url(pipeline_instance.getName(), :pipeline_counter => pipeline_instance.getCounter())

      end
      link :compare_url do |opts|
        opts[:url_builder].compare_pipelines_url(:from_counter => pipeline_instance.getCounter()-1, :to_counter => pipeline_instance.getCounter(), :pipeline_name => pipeline_instance.getName())
      end

      link :build_cause_url do |opts|
        opts[:url_builder].build_cause_url(:pipeline_counter => pipeline_instance.getCounter(), :pipeline_name => pipeline_instance.getName())
      end

      property :getLabel, as: :label
      property :getScheduledDate, as: :schedule_at
      property :getApprovedBy, as: :triggered_by
      collection :stages, embedded: true, exec_context: :decorator, decorator: StageRepresenter

      def stages
        pipeline_instance.getStageHistory().collect do |stage|
          [stage, {
                  pipeline_name:    pipeline_instance.getName(),
                  pipeline_counter: pipeline_instance.getCounter(),
                  render_previous:  true
                }
          ]
        end
      end
    end
  end
end
