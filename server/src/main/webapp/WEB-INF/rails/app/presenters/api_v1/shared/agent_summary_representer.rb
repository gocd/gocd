#
# Copyright 2019 ThoughtWorks, Inc.
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
#

module ApiV1
  module Shared
    class AgentSummaryRepresenter < ApiV1::BaseRepresenter
      alias_method :agent, :represented

      link :self do |opts|
        spark_url_for(opts, SparkRoutes::AgentsAPI.uuid(agent.getUuid()))
      end

      link :doc do |opts|
        CurrentGoCDVersion.api_docs_url('#agents')
      end

      link :find do |opts|
        spark_url_for(opts, SparkRoutes::AgentsAPI.find())
      end

      property :uuid
    end
  end
end

