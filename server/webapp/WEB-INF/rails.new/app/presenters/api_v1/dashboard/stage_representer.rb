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
    class StageRepresenter < ApiV1::BaseRepresenter

      attr_reader :pipeline_counter, :pipeline_name, :render_previous
      alias_method :stage, :represented

      def initialize(args)
        options = args.extract_options!
        super(args.first)
        @pipeline_counter = options.delete(:pipeline_counter)
        @pipeline_name    = options.delete(:pipeline_name)
        @render_previous  = options.delete(:render_previous)
      end

      link :self do |opts|
        opts[:url_builder].apiv1_stage_instance_by_counter_api_url(pipeline_name: @pipeline_name, pipeline_counter: @pipeline_counter,
                                                                   stage_name:    stage.getName, stage_counter: stage.getCounter)
      end

      link :doc do
        'http://api.go.cd/current/#get-stage-instance'
      end

      property :getName, as: :name
      property :status, exec_context: :decorator

      property :previous_stage, embedded: false, exec_context: :decorator, decorator: StageRepresenter, skip_nil: true

      def status
        stage.getState()
      end

      def previous_stage
        if stage.hasPreviousStage
          stage_presenter_opts = {
            pipeline_name:    pipeline_name,
            pipeline_counter: stage.getPreviousStage().getIdentifier().getPipelineCounter()
          }
          [stage.getPreviousStage(), stage_presenter_opts]
        end
      end

      private

      def url_params
        {
          pipeline_name:    pipeline_name,
          pipeline_counter: pipeline_counter,
          stage_name:       stage.getName(),
          stage_counter:    stage.getCounter()
        }
      end
    end
  end
end
