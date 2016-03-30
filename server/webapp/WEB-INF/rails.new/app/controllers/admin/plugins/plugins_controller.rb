##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
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

class Admin::Plugins::PluginsController < AdminController

  before_filter :set_tab_name

  def index
    @plugin_descriptors = default_plugin_manager.plugins()
                              .collect { |descriptor| GoPluginDescriptorModel::convertToDescriptorWithAllValues descriptor }
                              .sort { |plugin1, plugin2| plugin1.about().name().downcase <=> plugin2.about().name().downcase }
    @external_plugin_location = system_environment.getExternalPluginAbsolutePath()
    @upload_feature_enabled = system_environment.isPluginUploadEnabled()
    assert_load :meta_data_store, meta_data_store
  end

  def upload
    render :status => 403, :text => "Feature is not enabled" and return unless system_environment.isPluginUploadEnabled()
    if params[:plugin].nil?
      respond_to do |format|
        format.html { flash[:error] = "Please select a file to upload." and redirect_to action: "index" }
        format.js
      end
    else
      upload_response = default_plugin_manager.addPlugin(java.io.File.new(params[:plugin].path), params[:plugin].original_filename)
      respond_to do |format|
        if upload_response.isSuccess
          format.html { flash[:notice] =  upload_response.success and redirect_to action: "index" }
        else
          format.html { flash[:error] = upload_response.errors.values.join("\n") and redirect_to action: "index"}
        end
        format.js
      end
    end
  end

  def edit_settings
    plugin_settings = plugin_service.getPluginSettingsFor(params[:plugin_id])
    render_settings_page(plugin_settings, 200)
  end

  def update_settings
    plugin_settings = plugin_service.getPluginSettingsFor(params[:plugin_id], params[:plugin_settings])
    plugin_service.validatePluginSettingsFor(plugin_settings)
    if plugin_settings.hasErrors()
      flash.now[:error] = l.string('SAVE_FAILED')
      render_settings_page(plugin_settings, 400)
    else
      plugin_service.savePluginSettingsFor(plugin_settings)
      render(:text => 'Saved successfully', :location => url_options_with_flash(l.string('SAVED_SUCCESSFULLY'), {:action => :index, :class => 'success'}))
    end
  end

  private
  def set_tab_name
    @tab_name = 'plugins-listing'
  end

  def meta_data_store
    PluginSettingsMetadataStore.getInstance()
  end

  def render_settings_page(plugin_settings, status_code)
    assert_load :meta_data_store, meta_data_store
    assert_load :plugin_settings, plugin_settings
    render template: "/admin/plugins/plugins/settings", status: status_code, layout: false
  end
end