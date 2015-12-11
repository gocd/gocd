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
  module Config
    class StageRepresenter < ApiV1::BaseRepresenter
      alias_method :stage, :represented

      property :name, case_insensitive_string: true
      property :fetch_materials
      property :clean_working_dir, as: :clean_working_directory
      property :artifact_cleanup_prohibited, as: :never_cleanup_artifacts
      property :approval,
               decorator: ApiV1::Config::ApprovalRepresenter,
               class:     com.thoughtworks.go.config.Approval
      collection :environment_variables,
                 exec_context: :decorator,
                 decorator:    ApiV1::Config::EnvironmentVariableRepresenter,
                 expect_hash:  true,
                 class:        com.thoughtworks.go.config.EnvironmentVariableConfig
      collection :jobs,
                 exec_context: :decorator,
                 decorator:    ApiV1::Config::JobRepresenter,
                 expect_hash:  true,
                 class:        com.thoughtworks.go.config.JobConfig
      property :errors, exec_context: :decorator, decorator: ApiV1::Config::ErrorRepresenter, skip_parse: true, skip_render: lambda { |object, options| object.empty? }


      delegate :name, :name=, to: :stage

      # delegate :fetch_materials, :fetch_materials=, to: :stage

      # delegate :clean_working_dir, :clean_working_dir=, to: :stage

      # delegate :artifact_cleanup_prohibited, :artifact_cleanup_prohibited=, to: :stage


      def jobs
        stage.getJobs()
      end

      def jobs=(value)
        stage.setJobs(JobConfigs.new(value.to_java(JobConfig)))
      end

      def environment_variables
        stage.getVariables()
      end

      def environment_variables=(array_of_variables)
        stage.setVariables(EnvironmentVariablesConfig.new(array_of_variables))
      end

      def errors
        stage.errors.addAll(jobs.errors)
        stage.errors.addAll(environment_variables.errors)
        stage.errors
      end

    end
  end
end
