##########################GO-LICENSE-START################################
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
##########################GO-LICENSE-END##################################

module Admin
  module Plugins
    module PluginsHelper
      include JavaImports
      def can_edit_plugin_settings?(plugin_id)
        meta_data_store.hasPlugin(plugin_id) && is_admin_user? && !meta_data_store.preferenceFor(plugin_id).getTemplate().blank?
      end

      private
      def is_admin_user?
        security_service.isUserAdmin(current_user)
      end

      def meta_data_store
        PluginSettingsMetadataStore.getInstance()
      end
    end
  end
end
