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
  class StageRepresenter < ApiV1::BaseRepresenter
    alias_method :stage, :represented

    link :self do |opts|
      if stage.getIdentifier
        opts[:url_builder].apiv1_stage_instance_by_counter_api_url(pipeline_name:    stage.getIdentifier.getPipelineName,
                                                                   pipeline_counter: stage.getIdentifier.getPipelineCounter,
                                                                   stage_name:       stage.getName,
                                                                   stage_counter:    stage.getCounter)
      end
    end

    link :doc do |opts|
      'https://api.go.cd/#get-stage-instance'
    end

    property :getName, as: :name
    property :result, exec_context: :decorator
    property :getCounter, as: :counter
    property :getApprovalType, as: :stage_type
    property :rerun_of, exec_context: :decorator, decorator: StageSummaryRepresenter

    property :triggered_by, exec_context: :decorator, decorator: lambda { |thing, *|
                            if thing == com.thoughtworks.go.util.GoConstants::DEFAULT_APPROVED_BY
                              ChangesRepresenter
                            else
                              UserSummaryRepresenter
                            end
                          }
    property :pipeline, exec_context: :decorator, decorator: PipelineSummaryRepresenter
    collection :jobs, embedded: true, exec_context: :decorator, decorator: JobSummaryRepresenter

    def result
      stage.getResult()
    end

    def pipeline
      if stage.getIdentifier()
        OpenStruct.new({
                         pipeline_name:    stage.getIdentifier().getPipelineName,
                         pipeline_counter: stage.getIdentifier().getPipelineCounter,
                         pipeline_label:   stage.getIdentifier().getPipelineLabel
                       })
      end
    end

    def jobs
      if stage.respond_to?(:getBuildHistory)
        stage.getBuildHistory()
      else
        stage.getJobInstances()
      end
    end

    def triggered_by
      stage.getApprovedBy()
    end

    def rerun_of
      if stage.getRerunOfCounter
        OpenStruct.new({
                         pipeline_name:    stage.getIdentifier().getPipelineName,
                         pipeline_counter: stage.getIdentifier().getPipelineCounter,
                         stage_name:       stage.getName,
                         stage_counter:    stage.getRerunOfCounter
                       })
      end
    end

    class ChangesRepresenter < ApiV1::BaseRepresenter
      def to_hash(opts={})
        represented
      end
    end
  end
end
