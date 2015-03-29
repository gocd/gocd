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
    @upload_feature_enabled = Toggles.isToggleOn(Toggles.PLUGIN_UPLOAD_FEATURE_TOGGLE_KEY)
  end

  def upload
    render :status => 403, :text => "Feature is not enabled" and return unless Toggles.isToggleOn(Toggles.PLUGIN_UPLOAD_FEATURE_TOGGLE_KEY)
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

  def edit
    begin
      @settings_template = default_plugin_manager.loadPluginSettings(params[:plugin_id]);
    rescue => e
      @error = "#{e.message}"
    end
  end

  def save
    begin
      default_plugin_manager.savePluginSettings(params[:plugin_id], params[:settings].to_json);
      redirect_to :action => "edit", :plugin_id => params[:plugin_id] and return
    rescue => e
      @error = "#{e.message}"
      render "edit"
    end
  end

  private
  def set_tab_name
    @tab_name = 'plugins-listing'
  end

end