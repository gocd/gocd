module Admin
  module Plugins
    module PluginsHelper
      def can_edit_plugin_settings?(plugin_id)
        meta_data_store.hasPlugin(plugin_id) && is_admin_user? && !meta_data_store.preferenceFor(plugin_id).getTemplate().blank?
      end
    end
  end
end
