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
  module Plugins
    class PackageRepositoryRepresenter < BaseRepresenter
      alias_method :plugin, :represented

      collection :repository_properties,
                 exec_context: :decorator,
                 decorator:    ApiV1::Plugins::PropertyRepresenter

      collection :package_properties,
                 exec_context: :decorator,
                 decorator:    ApiV1::Plugins::PropertyRepresenter

      def repository_properties
        plugin.getRepositoryConfigurations
      end

      def package_properties
        plugin.getPackageConfigurations
      end
    end
  end
end
