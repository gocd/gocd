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
