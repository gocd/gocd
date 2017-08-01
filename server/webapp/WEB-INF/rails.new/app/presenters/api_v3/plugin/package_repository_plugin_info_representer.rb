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
  module Plugin
    class PackageRepositoryPluginInfoRepresenter < BasePluginInfoRepresenter

      property :extension_settings,
               exec_context: :decorator,
               skip_nil: true,
               decorator: ExtensionRepresenter

      def extension_settings
        {package_settings: plugin.getPackageSettings, repository_settings: plugin.getRepositorySettings}
      end

    end
  end
end