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

module ApiV1
  module Admin
    module MergedEnvironments
      class MergedEnvironmentConfigRepresenter < ApiV1::BaseRepresenter
        alias_method :environment, :represented

        link :self do |opts|
          opts[:url_builder].apiv1_admin_merged_environment_show_url(environment_name: environment.name)
        end

        link :doc do |opts|
          'https://api.gocd.org/#merged-environment-config'
        end

        link :find do |opts|
          opts[:url_builder].apiv1_admin_merged_environment_show_url(environment_name: '__environment_name__').gsub(/__environment_name__/, ':environment_name')
        end

        property :name, case_insensitive_string: true

        collection :origin, as: :origins,
                   skip_parse: true,
                   getter: lambda {|options|
                     origin = self.getOrigin
                     (origin.instance_of? FileConfigOrigin) ? [origin] : origin
                   },
                   decorator: lambda {|origin, *|
                     if origin.instance_of? FileConfigOrigin
                       Shared::ConfigOrigin::ConfigXmlOriginRepresenter
                     else
                       Shared::ConfigOrigin::ConfigRepoOriginRepresenter
                     end
                   }

        collection :pipelines,
                   exec_context: :decorator,
                   decorator: ApiV1::Admin::MergedEnvironments::PipelineConfigSummaryRepresenter

        collection :agents,
                   exec_context: :decorator,
                   decorator: ApiV1::Admin::MergedEnvironments::AgentSummaryRepresenter

        collection :environment_variables,
                   exec_context: :decorator,
                   decorator: ApiV1::Admin::MergedEnvironments::EnvironmentVariableRepresenter,
                   expect_hash: true

        def environment_variables
          environment.getVariables.map do |env_var|
            {env_var: env_var, environment: environment}
          end
        end

        def agents
          environment.getAgents.map do |agent|
            {agent: agent, environment: environment}
          end
        end

        def pipelines
          environment.getPipelines.map do |pipeline|
            {pipeline: pipeline, environment: environment}
          end
        end
      end
    end
  end
end
