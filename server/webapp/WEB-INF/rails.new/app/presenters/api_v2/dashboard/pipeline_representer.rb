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
    class PipelineRepresenter < ApiV2::BaseRepresenter
      def initialize(options)
        @pipeline = options[:pipeline]
        @user = options[:user]

        super(@pipeline)
      end

      link :self do |opts|
        opts[:url_builder].pipeline_history_url(@pipeline.name())
      end

      link :doc do
        'https://api.go.cd/current/#pipelines'
      end

      link :settings_path do |opts|
        opts[:url_builder].pipeline_edit_url(@pipeline.name(), current_tab: :'general')
      end

      link :trigger do |opts|
        opts[:url_builder].api_pipeline_action_url(@pipeline.name(), action: :'schedule')
      end

      link :trigger_with_options do |opts|
        opts[:url_builder].api_pipeline_action_url(@pipeline.name(), action: :'schedule')
      end

      link :pause do |opts|
        opts[:url_builder].pause_pipeline_url(@pipeline.name())
      end

      link :unpause do |opts|
        opts[:url_builder].unpause_pipeline_url(@pipeline.name())
      end

      property :name, exec_context: :decorator
      property :lastUpdatedTimeStamp, as: :last_updated_timestamp
      property :locked, exec_context: :decorator
      property :pause_info, exec_context: :decorator do
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
      property :can_operate, exec_context: :decorator
      property :can_administer, exec_context: :decorator
      property :can_unlock, exec_context: :decorator
      property :can_pause, exec_context: :decorator
      collection :instances, embedded: true, exec_context: :decorator, decorator: PipelineInstanceRepresenter

      def name
        @pipeline.name().toString()
      end

      def pause_info
        @pipeline.model().getPausedInfo()
      end

      def locked
        @pipeline.model().getLatestPipelineInstance().isCurrentlyLocked
      end

      def instances
        @pipeline.model().getActivePipelineInstances().select do |pipeline_instance_model|
          !pipeline_instance_model.instance_of?(com.thoughtworks.go.presentation.pipelinehistory.EmptyPipelineInstanceModel)
        end
      end

      def can_operate
        @pipeline.isPipelineOperator(@user.getUsername().toString())
      end

      def can_administer
        @pipeline.canBeAdministeredBy(@user.getUsername().toString())
      end

      def can_unlock
        @pipeline.canBeOperatedBy(@user.getUsername().toString())
        end

      def can_pause
        @pipeline.canBeOperatedBy(@user.getUsername().toString())
      end
    end
  end
end
