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

describe Admin::Plugins::PluginsController do

  describe :routes do

    it "should resolve the route" do
      params_from(:get, "/admin/plugins").should == {:controller => "admin/plugins/plugins", :action => "index"}
      route_for(:controller => "admin/plugins/plugins", :action => "index").should == "admin/plugins"
      plugins_listing_path.should == "/admin/plugins"
    end

    it "should resolve plugin settings route" do
      params_from(:get, "/admin/plugin/edit").should == {:controller => "admin/plugins/plugins", :action => "edit"}
      route_for(:controller => "admin/plugins/plugins", :action => "edit").should == "admin/plugin/edit"
      plugin_edit_path.should == "/admin/plugin/edit"
    end

    it "should resolve plugin settings route" do
      params_from(:post, "/admin/plugin/save").should == {:controller => "admin/plugins/plugins", :action => "save"}
      plugin_save_path.should == "/admin/plugin/save"
    end
  end

  describe :index do

    before :each do
      controller.stub!(:default_plugin_manager).and_return(@plugin_manager = mock('plugin_manager'))
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
    end

    it "should populate the current list of plugins and the external plugins path" do
      @plugin_manager.should_receive(:plugins).and_return([@plugin_1, @plugin_2])
      controller.should_receive(:system_environment).and_return(@system_environment = mock("system_environment"))
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

    def plugin(id, name)
      about = com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor::About.new(name, nil, nil, nil, nil, [])
      GoPluginDescriptor.new(id, nil, about, nil, nil, true)
    end

    def plugin_descriptors(plugin)
      GoPluginDescriptorModel::convertToDescriptorWithAllValues plugin
    end
  end

  describe :edit do
    before :each do
      controller.stub!(:default_plugin_manager).and_return(@plugin_manager = mock('plugin_manager'))
    end

    it 'should populate settings template' do
      settings_template = '<div></div>'
      @plugin_manager.should_receive(:loadPluginSettings).with("sample").and_return(settings_template)
      get :edit, :plugin_id =>'sample'
      assigns[:tab_name].should == "plugins-listing"
      assigns[:settings_template].should == settings_template
    end

    it 'should set error when plugin manager throws exception' do
      @plugin_manager.should_receive(:loadPluginSettings).with("sample").and_raise("error")
      get :edit, :plugin_id =>'sample'
      assigns[:error].should == 'error'
    end
  end

  describe :save do
    before :each do
      controller.stub!(:default_plugin_manager).and_return(@plugin_manager = mock('plugin_manager'))
    end

    it 'should save settings template' do
      @plugin_manager.should_receive(:savePluginSettings).with('sample',"{\"key1\":\"value1\",\"key2\":{\"key2subKey1\":\"key2subKey1Value\"}}")
      post :save, :plugin_id =>'sample', :settings => {:key1 => 'value1', :key2 => {:key2subKey1 => 'key2subKey1Value'}}
    end
  end
end