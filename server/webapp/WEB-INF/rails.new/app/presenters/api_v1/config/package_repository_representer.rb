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

module ApiV1
  module Config
    class PackageRepositoryRepresenter < ApiV1::BaseRepresenter
      alias_method :package_repository, :represented

      error_representer

      link :self do |opts|
        opts[:url_builder].apiv1_admin_repository_url(repo_id: package_repository.id)
      end

      link :doc do |opts|
        'https://api.go.cd/#package-repository'
      end

      link :find do |opts|
        opts[:url_builder].apiv1_admin_repository_url(repo_id: '__repo_id__').gsub(/__repo_id__/, ':repo_id')
      end

      property :id, as: :repo_id, default: SecureRandom.hex
      property :name

      property :plugin_configuration,
               as: :plugin_metadata,
               decorator: ApiV1::Config::PluginConfigurationRepresenter,
               class: com.thoughtworks.go.domain.config.PluginConfiguration

      collection :configuration,
                 exec_context: :decorator,
                 decorator:    ApiV1::Config::PluginConfigurationPropertyRepresenter,
                 class: com.thoughtworks.go.domain.config.ConfigurationProperty

      collection :packages,
                 exec_context: :decorator,
                 embedded: true,
                 decorator:    ApiV1::Config::PackageHalRepresenter,
                 class:        com.thoughtworks.go.domain.packagerepository.PackageDefinition

      delegate :packages, :configuration, to: :package_repository

      def packages=(pkg)
      end

      def configuration=(value)
        package_repository.addConfigurations(value)
      end

    end
  end
end
