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

require 'spec_helper'

describe Admin::Plugins::PluginsController do

  describe :routes do
    it "should resolve the route_for_index" do
      {:get => "/admin/plugins"}.should route_to(:controller => "admin/plugins/plugins", :action => "index")
      plugins_listing_path.should == "/admin/plugins"
    end

    it "should resolve_the_route_for_upload" do
      {:post => "/admin/plugins"}.should route_to(:controller => "admin/plugins/plugins", :action => "upload")
      upload_plugin_path.should == "/admin/plugins"
    end

    it "should resolve_the_route_for_get plugin settings" do
      {:get => "/admin/plugins/settings/plugin.id"}.should route_to(:controller => "admin/plugins/plugins", :action => "edit_settings", :plugin_id => "plugin.id")
      edit_settings_path(:plugin_id => 'plugin.id').should == "/admin/plugins/settings/plugin.id"
    end

    it "should resolve_the_route_for_update plugin settings" do
      {:post => "/admin/plugins/settings/plugin.id"}.should route_to(:controller => "admin/plugins/plugins", :action => "update_settings", :plugin_id => "plugin.id")
      update_settings_path(:plugin_id => 'plugin.id').should == "/admin/plugins/settings/plugin.id"
    end
  end

  describe :upload do
    before :each do
      controller.stub(:default_plugin_manager).and_return(@plugin_manager = double('plugin_manager'))
      expect(Toggles).to receive(:isToggleOn).with(Toggles.PLUGIN_UPLOAD_FEATURE_TOGGLE_KEY).and_return(true)
    end

    it "should show success message when upload is successful" do
      @plugin_manager.should_receive(:addPlugin).with(an_instance_of(java.io.File), 'plugins_controller_spec.rb')
        .and_return(@plugin_response = double('upload_response'))
      @plugin_response.should_receive(:isSuccess).and_return(true)
      @plugin_response.should_receive(:success).and_return("successfully uploaded!")
      file = Rack::Test::UploadedFile.new(__FILE__, "image/jpeg")

      post :upload, :plugin => file

      expect(flash[:notice]).to eq("successfully uploaded!")
    end

    it "should show error message when upload is unsuccessful" do
      @plugin_manager.should_receive(:addPlugin).with(an_instance_of(java.io.File), 'plugins_controller_spec.rb')
        .and_return(@plugin_response = double('upload_response'))
      @plugin_response.should_receive(:isSuccess).and_return(false)
      @plugin_response.should_receive(:errors).and_return({415 => "invalid file"})
      file = Rack::Test::UploadedFile.new(__FILE__, "image/jpeg")

      post :upload, :plugin => file

      expect(flash[:error]).to eq("invalid file")
    end

    it "should show error message when no file is selected" do
      @plugin_manager.should_not_receive(:addPlugin)

      post :upload, :plugin => nil

      expect(flash[:error]).to eq("Please select a file to upload.")
    end

    it "should redirect to #index" do
      @plugin_manager.should_receive(:addPlugin).with(an_instance_of(java.io.File), 'plugins_controller_spec.rb')
        .and_return(@plugin_response = double('upload_response'))
      @plugin_response.should_receive(:isSuccess).and_return(true)
      @plugin_response.should_receive(:success).and_return("successfully uploaded!")
      file = Rack::Test::UploadedFile.new(__FILE__, "image/jpeg")

      post :upload, :plugin => file

      response.should redirect_to "/admin/plugins"
    end

    it "should refuse to upload when feature is turned off" do
      RSpec::Mocks.proxy_for(Toggles).reset
      expect(Toggles).to receive(:isToggleOn).with(Toggles.PLUGIN_UPLOAD_FEATURE_TOGGLE_KEY).and_return(false)

      post :upload, :plugin => nil

      expect(response.status).to eq(403)
      expect(response.body).to eq("Feature is not enabled")
    end

  end

  describe :index do
    before :each do
      controller.stub(:default_plugin_manager).and_return(@plugin_manager = double('plugin_manager'))
      @plugin_1 = plugin("id", "name")
      @plugin_2 = plugin("yum", "yum plugin")
      @plugin_3 = plugin("A-id", "Name")
      @plugin_4 = plugin("Yum-id", "Yum Exec Plugin")
      @plugin_5 = plugin("Another-id", nil)
      @plugin_6 = plugin("plugin.jar", nil)
    end

    it "should populate the tab name" do
      @plugin_manager.should_receive(:plugins).and_return([@plugin_1, @plugin_2])

      get :index

      assigns[:tab_name].should == "plugins-listing"
      assert_template layout: "admin"
    end

    it "should populate the current list of plugins and the external plugins path" do
      @plugin_manager.should_receive(:plugins).and_return([@plugin_1, @plugin_2])
      controller.should_receive(:system_environment).and_return(@system_environment = double("system_environment"))
      @system_environment.should_receive(:getExternalPluginAbsolutePath).and_return("some_path")

      get :index

      assigns[:plugin_descriptors].should == [plugin_descriptors(@plugin_1), plugin_descriptors(@plugin_2)]
      assigns[:external_plugin_location].should == "some_path"
    end

    it "should populate the current list of plugins in case insensitive alphabetical order when plugin names are given" do
      @plugin_manager.should_receive(:plugins).and_return([@plugin_1, @plugin_2, @plugin_3, @plugin_4])

      get :index

      assigns[:plugin_descriptors].should == [plugin_descriptors(@plugin_1), plugin_descriptors(@plugin_3), plugin_descriptors(@plugin_4), plugin_descriptors(@plugin_2)]
    end

    it "should populate the current list of plugins in case insensitive alphabetical order when plugin names are not given" do
      @plugin_manager.should_receive(:plugins).and_return([@plugin_1, @plugin_2, @plugin_3, @plugin_4, @plugin_5, @plugin_6])

      get :index

      assigns[:plugin_descriptors].should == [plugin_descriptors(@plugin_5), plugin_descriptors(@plugin_1), plugin_descriptors(@plugin_3), plugin_descriptors(@plugin_6), plugin_descriptors(@plugin_4), plugin_descriptors(@plugin_2)]
    end

    it "should populate the feature toggle flag for upload plugin" do
      expect(Toggles).to receive(:isToggleOn).with(Toggles.PLUGIN_UPLOAD_FEATURE_TOGGLE_KEY).and_return(true)
      @plugin_manager.should_receive(:plugins).and_return([])

      get :index

      expect(assigns[:upload_feature_enabled]).to eq(true)
    end

    def plugin(id, name)
      about = com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor::About.new(name, nil, nil, nil, nil, [])
      GoPluginDescriptor.new(id, nil, about, nil, nil, true)
    end

    def plugin_descriptors(plugin)
      GoPluginDescriptorModel::convertToDescriptorWithAllValues plugin
    end
  end

  describe :edit_settings do
    before :each do
      controller.stub(:plugin_service).and_return(@plugin_service = double('plugin service'))
      expect(@plugin_service).to receive(:getPluginSettingsFor).with('plugin.id').and_return(@plugin_settings = double('plugin settings'))
    end

    it "should render settings template with required data" do
      get :edit_settings, :plugin_id => 'plugin.id'

      assigns[:meta_data_store].should == PluginSettingsMetadataStore.getInstance()
      assigns[:plugin_settings].should == @plugin_settings
      assert_template "admin/plugins/plugins/settings"
      assert_template layout: false
    end
  end

  describe :update_settings do
    before :each do
      controller.stub(:plugin_service).and_return(@plugin_service = double('plugin service'))
      expect(@plugin_service).to receive(:getPluginSettingsFor).with('plugin.id', anything()).and_return(@plugin_settings = double('plugin settings'))
      expect(@plugin_service).to receive(:validatePluginSettingsFor).with(@plugin_settings)
    end

    it "should render settings template with required data on error" do
      expect(@plugin_settings).to receive(:hasErrors).and_return(true)

      post :update_settings, :plugin_id => 'plugin.id'

      assigns[:meta_data_store].should == PluginSettingsMetadataStore.getInstance()
      assigns[:plugin_settings].should == @plugin_settings
      assert_template "admin/plugins/plugins/settings"
      assert_template layout: false
    end

    it "should redirect to plugin listing on success" do
      expect(@plugin_settings).to receive(:hasErrors).and_return(false)
      expect(@plugin_service).to receive(:savePluginSettingsFor).with(@plugin_settings)

      post :update_settings, :plugin_id => 'plugin.id'

      response.body.should == 'Saved successfully'
      URI.parse(response.location).path.should == plugins_listing_path
    end
  end
end
