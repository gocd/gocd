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

module ApiV3
  module Shared
    module Stages
      class JobRepresenter < BaseRepresenter
        alias_method :job, :represented

        error_representer({'runType' => 'run_instance_count'})
        property :name,
                 case_insensitive_string: true

        property :run_instance_count,
                 exec_context: :decorator

        property :timeout,
                 exec_context: :decorator

        property :elastic_profile_id, skip_nil: true

        collection :environment_variables,
                   exec_context: :decorator,
                   decorator: Shared::EnvironmentVariableRepresenter,
                   class: com.thoughtworks.go.config.EnvironmentVariableConfig
        collection :resources,
                   exec_context: :decorator,
                   skip_nil: true
        collection :tasks,
                   exec_context: :decorator,
                   decorator: Shared::Stages::Tasks::TaskRepresenter,
                   expect_hash: true,
                   class: lambda { |fragment, *|
                     Shared::Stages::Tasks::TaskRepresenter.task_class_for_type(fragment[:type]||fragment['type'])
                   }
        collection :tabs,
                   exec_context: :decorator,
                   decorator: TabConfigRepresenter,
                   expect_hash: true,
                   class: com.thoughtworks.go.config.Tab
        collection :artifacts,
                   exec_context: :decorator,
                   decorator: Shared::Stages::ArtifactRepresenter,
                   expect_hash: true,
                   class: lambda { |fragment, *|
                     Shared::Stages::ArtifactRepresenter.get_class_for_artifact_type(fragment[:type] || fragment['type'])
                   }

        collection :properties, exec_context: :decorator, decorator: Shared::Stages::PropertyConfigRepresenter, class: com.thoughtworks.go.config.ArtifactPropertyConfig, render_empty: false

        def run_instance_count
          if job.getRunInstanceCount.present?
            job.getRunInstanceCount.to_i
          elsif job.isRunOnAllAgents
            'all'
          else
            nil
          end
        end

        def run_instance_count=(val)
          return if val.blank? || val.to_s.strip.downcase == 'null'
          if val.to_s.strip.downcase == 'all'
            job.setRunOnAllAgents(true)
          else
            job.setRunInstanceCount(val.to_s)
          end
        end

        def timeout
          if job.timeout == '0'
            'never'
          elsif job.timeout.present?
            job.timeout.to_i
          else
            nil
          end
        end

        def timeout=(val)
          return if val.blank? || val.to_s.strip.downcase == 'null'

          if val.to_s.strip.downcase == 'never'
            job.timeout = '0'
          elsif job.timeout = nil
          else
            job.timeout = val.to_s
          end
        end

        def artifacts
          job.artifactConfigs
        end

        def artifacts=(value)
          job.setArtifactConfigs(com.thoughtworks.go.config.ArtifactConfigs.new(value))
        end

        def environment_variables
          job.getVariables
        end

        def environment_variables=(array_of_variables)
          job.setVariables(EnvironmentVariablesConfig.new(array_of_variables))
        end

        def resources
          job.resourceConfigs.collect(&:name)
        end

        def resources=(values)
          job.setResourceConfigs(ResourceConfigs.new(values.map { |name| ResourceConfig.new(name) }))
        end

        def tasks
          job.getTasks
        end

        def tasks=(value)
          job.setTasks(com.thoughtworks.go.config.Tasks.new(value.to_java(com.thoughtworks.go.domain.Task)))
        end

        def tabs
          job.getTabs
        end

        def tabs=(value)
          job.setTabs(com.thoughtworks.go.config.Tabs.new(value.to_java(com.thoughtworks.go.config.Tab)))
        end

        def properties
          job.getProperties
        end

        def properties=(value)
          job.setProperties(com.thoughtworks.go.config.ArtifactPropertiesConfig.new(value))
        end
      end
    end
  end
end
