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

require 'rails_helper'

describe Admin::Plugins::PluginsController do

  before :each do
    allow(controller).to receive(:default_plugin_manager).and_return(@plugin_manager = double('plugin_manager'))
    allow(controller).to receive(:system_environment).and_return(@system_environment = double("system_environment", :isPluginUploadEnabled => true))
    allow(controller).to receive(:plugin_service).and_return(@plugin_service = double('plugin service'))
  end

  describe "routes" do
    it "should resolve the route_for_index" do
      expect({:get => "/admin/old_plugins"}).to route_to(:controller => "admin/plugins/plugins", :action => "index")
      expect(plugins_listing_path).to eq("/admin/old_plugins")
    end

    it "should resolve_the_route_for_upload" do
      expect({:post => "/admin/old_plugins"}).to route_to(:controller => "admin/plugins/plugins", :action => "upload")
      expect(upload_plugin_path).to eq("/admin/old_plugins")
    end

    it "should resolve_the_route_for_get plugin settings" do
      expect({:get => "/admin/old_plugins/settings/plugin.id"}).to route_to(:controller => "admin/plugins/plugins", :action => "edit_settings", :plugin_id => "plugin.id")
      expect(edit_settings_path(:plugin_id => 'plugin.id')).to eq("/admin/old_plugins/settings/plugin.id")
    end

    it "should resolve_the_route_for_update plugin settings" do
      expect({:post => "/admin/old_plugins/settings/plugin.id"}).to route_to(:controller => "admin/plugins/plugins", :action => "update_settings", :plugin_id => "plugin.id")
      expect(update_settings_path(:plugin_id => 'plugin.id')).to eq("/admin/old_plugins/settings/plugin.id")
    end
  end

  describe "upload" do
    it "should show success message when upload is successful" do
      expect(@plugin_manager).to receive(:addPlugin).with(an_instance_of(java.io.File), 'plugins_controller_spec.rb')
        .and_return(@plugin_response = double('upload_response'))
      expect(@plugin_response).to receive(:isSuccess).and_return(true)
      expect(@plugin_response).to receive(:success).and_return("successfully uploaded!")
      file = Rack::Test::UploadedFile.new(__FILE__, "image/jpeg")

      post :upload, :plugin => file

      expect(flash[:notice]).to eq("successfully uploaded!")
    end

    it "should show error message when upload is unsuccessful" do
      expect(@plugin_manager).to receive(:addPlugin).with(an_instance_of(java.io.File), 'plugins_controller_spec.rb')
        .and_return(@plugin_response = double('upload_response'))
      expect(@plugin_response).to receive(:isSuccess).and_return(false)
      expect(@plugin_response).to receive(:errors).and_return({415 => "invalid file"})
      file = Rack::Test::UploadedFile.new(__FILE__, "image/jpeg")

      post :upload, :plugin => file

      expect(flash[:error]).to eq("invalid file")
    end

    it "should show error message when no file is selected" do
      expect(@plugin_manager).not_to receive(:addPlugin)

      post :upload, :plugin => nil

      expect(flash[:error]).to eq("Please select a file to upload.")
    end

    it "should redirect to #index" do
      expect(@plugin_manager).to receive(:addPlugin).with(an_instance_of(java.io.File), 'plugins_controller_spec.rb')
        .and_return(@plugin_response = double('upload_response'))
      expect(@plugin_response).to receive(:isSuccess).and_return(true)
      expect(@plugin_response).to receive(:success).and_return("successfully uploaded!")
      file = Rack::Test::UploadedFile.new(__FILE__, "image/jpeg")

      post :upload, :plugin => file

      expect(response).to redirect_to "/admin/old_plugins"
    end

    it "should refuse to upload when feature is turned off" do
      expect(@system_environment).to receive(:isPluginUploadEnabled).and_return(false)

      post :upload, :plugin => nil

      expect(response.status).to eq(403)
      expect(response.body).to eq("Feature is not enabled")
    end

  end

  describe "index" do
    before :each do
      @plugin_1 = plugin("id", "name")
      @plugin_2 = plugin("yum", "yum plugin")
      @plugin_3 = plugin("A-id", "Name")
      @plugin_4 = plugin("Yum-id", "Yum Exec Plugin")
      @plugin_5 = plugin("Another-id", nil)
      @plugin_6 = plugin("plugin.jar", nil)
      expect(@system_environment).to receive(:getExternalPluginAbsolutePath).and_return("some_path")
      expect(@system_environment).to receive(:isPluginUploadEnabled).and_return(true)
    end

    it "should populate the tab name" do
      expect(@plugin_manager).to receive(:plugins).and_return([@plugin_1, @plugin_2])

      get :index

      expect(assigns[:tab_name]).to eq("plugins-listing")
      assert_template layout: "admin"
    end

    it "should populate the current list of plugins and the external plugins path" do
      expect(@plugin_manager).to receive(:plugins).and_return([@plugin_1, @plugin_2])

      get :index

      expect(assigns[:plugin_descriptors]).to eq([plugin_descriptors(@plugin_1), plugin_descriptors(@plugin_2)])
      expect(assigns[:external_plugin_location]).to eq("some_path")
    end

    it "should populate the current list of plugins in case insensitive alphabetical order when plugin names are given" do
      expect(@plugin_manager).to receive(:plugins).and_return([@plugin_1, @plugin_2, @plugin_3, @plugin_4])

      get :index

      expect(assigns[:plugin_descriptors]).to eq([plugin_descriptors(@plugin_1), plugin_descriptors(@plugin_3), plugin_descriptors(@plugin_4), plugin_descriptors(@plugin_2)])
    end

    it "should populate the current list of plugins in case insensitive alphabetical order when plugin names are not given" do
      expect(@plugin_manager).to receive(:plugins).and_return([@plugin_1, @plugin_2, @plugin_3, @plugin_4, @plugin_5, @plugin_6])

      get :index

      expect(assigns[:plugin_descriptors]).to eq([plugin_descriptors(@plugin_5), plugin_descriptors(@plugin_1), plugin_descriptors(@plugin_3), plugin_descriptors(@plugin_6), plugin_descriptors(@plugin_4), plugin_descriptors(@plugin_2)])
    end

    it "should populate the feature toggle flag for upload plugin" do

      expect(@plugin_manager).to receive(:plugins).and_return([])

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

  describe "edit_settings" do
    before :each do
      expect(@plugin_service).to receive(:getPluginSettingsFor).with('plugin.id').and_return(@plugin_settings = double('plugin settings'))
    end

    it "should render settings template with required data" do
      get :edit_settings, :plugin_id => 'plugin.id'

      expect(assigns[:meta_data_store]).to eq(PluginSettingsMetadataStore.getInstance())
      expect(assigns[:plugin_settings]).to eq(@plugin_settings)
      assert_template "admin/plugins/plugins/settings"
      assert_template layout: false
    end
  end

  describe "update_settings" do
    before :each do
      expect(@plugin_service).to receive(:getPluginSettingsFor).with('plugin.id', anything()).and_return(@plugin_settings = double('plugin settings'))
      expect(@plugin_service).to receive(:validatePluginSettingsFor).with(@plugin_settings)
    end

    it "should render settings template with required data on error" do
      expect(@plugin_settings).to receive(:hasErrors).and_return(true)

      post :update_settings, :plugin_id => 'plugin.id'

      expect(assigns[:meta_data_store]).to eq(PluginSettingsMetadataStore.getInstance())
      expect(assigns[:plugin_settings]).to eq(@plugin_settings)
      assert_template "admin/plugins/plugins/settings"
      assert_template layout: false
    end

    it "should redirect to plugin listing on success" do
      expect(@plugin_settings).to receive(:hasErrors).and_return(false)
      expect(@plugin_service).to receive(:savePluginSettingsFor).with(@plugin_settings)

      post :update_settings, :plugin_id => 'plugin.id'

      expect(response.body).to eq('Saved successfully')
      expect(URI.parse(response.location).path).to eq(plugins_listing_path)
    end
  end

  describe "can_edit_plugin_settings?" do
    before :each do
      @meta_data_store = double('meta_data_store')
      @secuity_service = double('security_service')
      @user = 'user'

      allow(controller).to receive(:meta_data_store) { @meta_data_store }
      allow(controller).to receive(:security_service) { @secuity_service }
      allow(controller).to receive(:current_user) { @user }
    end

    it 'should be editable for admin user if metadata store has plugin settings with a view template' do
      preference = double('preference')

      expect(@secuity_service).to receive(:isUserAdmin).with(@user).and_return(true)
      expect(@meta_data_store).to receive(:hasPlugin).with('plugin_id').and_return(true)
      expect(@meta_data_store).to receive(:preferenceFor).with('plugin_id').and_return(preference)
      expect(preference).to receive(:getTemplate).and_return('view_template')

      expect(controller.can_edit_plugin_settings?('plugin_id')).to be_truthy
    end

    it 'should not be editable in absence of plugin settings in store' do
      expect(@meta_data_store).to receive(:hasPlugin).with('plugin_id').and_return(false)

      expect(controller.can_edit_plugin_settings?('plugin_id')).to be_falsey
    end

    it 'should not be editable for a non admin user' do
      expect(@secuity_service).to receive(:isUserAdmin).with(@user).and_return(false)
      expect(@meta_data_store).to receive(:hasPlugin).with('plugin_id').and_return(true)

      expect(controller.can_edit_plugin_settings?('plugin_id')).to be_falsey
    end

    it 'should not be editable in absence of template in plugin settings' do
      preference = double('preference')

      expect(@secuity_service).to receive(:isUserAdmin).with(@user).and_return(true)
      expect(@meta_data_store).to receive(:hasPlugin).with('plugin_id').and_return(true)
      expect(@meta_data_store).to receive(:preferenceFor).with('plugin_id').and_return(preference)
      expect(preference).to receive(:getTemplate).and_return(nil)

      expect(controller.can_edit_plugin_settings?('plugin_id')).to be_falsey
    end
  end
end
