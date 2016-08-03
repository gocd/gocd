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
  class StageSummaryRepresenter < ApiV1::BaseRepresenter
    alias_method :stage, :represented

    link :self do |opts|
      opts[:url_builder].apiv1_stage_instance_by_counter_api_url(pipeline_name:    stage.pipeline_name,
                                                                 pipeline_counter: stage.pipeline_counter,
                                                                 stage_counter:    stage.stage_counter,
                                                                 stage_name:       stage.stage_name)
    end

    link :doc do |opts|
      'https://api.go.cd/#get-stage-instance'
    end

    property :stage_name, as: :name
    property :stage_counter, as: :counter
  end
end
