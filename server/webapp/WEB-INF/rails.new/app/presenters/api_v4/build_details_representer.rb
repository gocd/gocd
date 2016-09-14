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

module ApiV4
  class BuildDetailsRepresenter < BaseRepresenter

    link :job do |opts|
      opts[:url_builder].root_url + job_url if represented.isBuilding()
    end

    link :stage do |opts|
      opts[:url_builder].root_url + stage_url if represented.isBuilding()
    end

    link :pipeline do |opts|
      opts[:url_builder].root_url + pipeline_url if represented.isBuilding()
    end

    property :getPipelineName, as: :pipeline_name, skip_nil: true
    property :getStageName, as: :stage_name, skip_nil: true
    property :getJobName, as: :job_name, skip_nil: true

    def job_url
      'tab/build/detail/' + represented.getBuildLocator()
    end

    def pipeline_url
      'tab/pipeline/history/' + represented.getPipelineName()
    end

    def stage_url
      build_locator = represented.getBuildLocator()
      stage_identifier = build_locator.split('/')[0...-1].join('/')
      'pipelines/' + stage_identifier
    end
  end
end