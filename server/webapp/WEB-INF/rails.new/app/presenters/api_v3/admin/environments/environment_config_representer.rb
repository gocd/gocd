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

module ApiV3
  module Admin
    module Environments
      class EnvironmentConfigRepresenter < ApiV3::BaseRepresenter
        alias_method :environment, :represented

        error_representer({
                            'variables' => 'environment_variables',
                          })

        link :self do |opts|
          opts[:url_builder].apiv3_admin_environment_url(name: environment.name)
        end

        link :doc do |opts|
          'https://api.gocd.io/#environment-config'
        end

        link :find do |opts|
          opts[:url_builder].apiv3_admin_environment_url(name: '__environment_name__').gsub(/__environment_name__/, ':environment_name')
        end

        property :name, case_insensitive_string: true

        collection :pipelines,
                   exec_context: :decorator,
                   decorator: ApiV3::Admin::Environments::PipelineConfigSummaryRepresenter,
                   class: com.thoughtworks.go.config.EnvironmentPipelineConfig

        collection :agents,
                   exec_context: :decorator,
                   class: EnvironmentAgentConfig,
                   decorator: ApiV3::Shared::AgentSummaryRepresenter

        collection :environment_variables,
                   exec_context: :decorator,
                   decorator: ApiV3::Shared::EnvironmentVariableRepresenter,
                   expect_hash: true,
                   class: EnvironmentVariableConfig

        def environment_variables
          environment.getVariables()
        end

        def agents
          environment.getAgents.to_a
        end

        def agents=(agents)
          environment.setAgents(agents)
        end

        def environment_variables=(array_of_variables)
          environment.setVariables(EnvironmentVariablesConfig.new(array_of_variables))
        end

        def pipelines
          environment.getPipelines.to_a
        end

        def pipelines=(pipelines)
          environment.setPipelines(pipelines)
        end
      end
    end
  end
end
