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

module ApiV1
  module Dashboard
    class PipelineRepresenter < ApiV1::BaseRepresenter
      alias_method :pipeline, :represented

      link :self do |opts|
        opts[:url_builder].pipeline_history_url(pipeline.getName())
      end

      link :doc do
        'https://api.go.cd/#pipelines'
      end

      link :settings_path do |opts|
        opts[:url_builder].pipeline_edit_url(pipeline.getName(), current_tab: :'general')
      end

      link :trigger do |opts|
        opts[:url_builder].api_pipeline_action_url(pipeline.getName(), action: :'schedule')
      end

      link :trigger_with_options do |opts|
        opts[:url_builder].api_pipeline_action_url(pipeline.getName(), action: :'schedule')
      end

      link :pause do |opts|
        opts[:url_builder].pause_pipeline_url(pipeline.getName())
      end

      link :unpause do |opts|
        opts[:url_builder].unpause_pipeline_url(pipeline.getName())
      end

      property :getName, as: :name
      property :locked, exec_context: :decorator
      property :getPausedInfo, as: :pause_info do
        property :paused, as: :paused
        property :pauseBy,
                 as:         :paused_by,
                 getter:     lambda { |options| pauseBy.blank? ? nil : pauseBy },
                 render_nil: true
        property :pauseCause,
                 as:         :pause_reason,
                 getter:     lambda { |options| pauseCause.blank? ? nil : pauseCause },
                 render_nil: true
      end
      collection :instances, embedded: true, exec_context: :decorator, decorator: PipelineInstanceRepresenter

      def locked
        pipeline.getLatestPipelineInstance().isCurrentlyLocked
      end

      def instances
        pipeline.getActivePipelineInstances().select do |pipeline_instance_model|
          !pipeline_instance_model.instance_of?(com.thoughtworks.go.presentation.pipelinehistory.EmptyPipelineInstanceModel)
        end
      end

    end
  end
end
