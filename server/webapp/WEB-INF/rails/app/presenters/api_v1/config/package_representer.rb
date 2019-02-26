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
    class PackageRepresenter < ApiV1::BaseRepresenter

      error_representer

      link :self do |opts|
        opts[:url_builder].apiv1_admin_package_url(package_id: package.id)
      end

      link :doc do |opts|
        'https://api.gocd.org/#packages'
      end

      link :find do |opts|
        opts[:url_builder].apiv1_admin_package_url(package_id: '__package_id__').gsub(/__package_id__/, ':package_id')
      end

      property :name, exec_context: :decorator
      property :id, exec_context: :decorator

      property :auto_update, exec_context: :decorator

      property :package_repo,
               exec_context: :decorator,
               decorator: ApiV1::Config::RepositorySummaryRepresenter,
               class: com.thoughtworks.go.domain.packagerepository.PackageRepository

      collection :configuration,
                 exec_context: :decorator,
                 decorator: ApiV1::Config::PluginConfigurationPropertyRepresenter,
                 class: com.thoughtworks.go.domain.config.ConfigurationProperty

      delegate :name, :name=, :id, :id=, :errors, :auto_update, :auto_update=,  to: :package

      def package
        represented[:package]
      end

      def repository
        represented[:repository]
      end
      
      def package_repo
        package.getRepository
      end

      def package_repo=(value)
        package.setRepository(repository)
      end

      def configuration
        package.getConfiguration
      end

      def configuration=(config_properties)
        package.addConfigurations(config_properties)
      end

    end
  end
end
