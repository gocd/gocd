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
        .collect {|descriptor| GoPluginDescriptorModel::convertToDescriptorWithAllValues descriptor}
        .sort { |plugin1, plugin2| plugin1.about().name().downcase <=> plugin2.about().name().downcase }
    @external_plugin_location = system_environment.getExternalPluginAbsolutePath()
  end

  def upload
    default_plugin_manager.addPlugin(java.io.File.new(params[:plugin].path))
    redirect_to action: "index"
  end

  private
  def set_tab_name
    @tab_name = 'plugins-listing'
  end
end