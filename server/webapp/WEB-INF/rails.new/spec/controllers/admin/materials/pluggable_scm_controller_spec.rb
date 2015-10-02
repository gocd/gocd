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

describe Admin::Materials::PluggableScmController do
  include ConfigSaveStubbing
  include MockRegistryModule

  before do
    controller.stub(:populate_health_messages)
  end

  describe "routes should resolve and generate" do
    it "show_existing" do
      {:get => '/admin/pipelines/pipeline.name/materials/pluggable_scm/show_existing'}.should route_to(:controller => 'admin/materials/pluggable_scm', :action => 'show_existing', :pipeline_name => 'pipeline.name')
      send('admin_pluggable_scm_show_existing_path', :pipeline_name => 'foo.bar').should == '/admin/pipelines/foo.bar/materials/pluggable_scm/show_existing'
    end

    it "choose_existing" do
      {:post => '/admin/pipelines/pipeline.name/materials/pluggable_scm/choose_existing'}.should route_to(:controller => 'admin/materials/pluggable_scm', :action => 'choose_existing', :pipeline_name => 'pipeline.name')
      send('admin_pluggable_scm_choose_existing_path', :pipeline_name => 'foo.bar').should == '/admin/pipelines/foo.bar/materials/pluggable_scm/choose_existing'
    end

    it "new" do
      {:get => '/admin/pipelines/pipeline.name/materials/pluggable_scm/new/plugin.id-1'}.should route_to(:controller => 'admin/materials/pluggable_scm', :action => 'new', :pipeline_name => 'pipeline.name', :plugin_id => 'plugin.id-1')
      send('admin_pluggable_scm_new_path', :pipeline_name => 'foo.bar', :plugin_id => 'plugin-id').should == '/admin/pipelines/foo.bar/materials/pluggable_scm/new/plugin-id'
    end

    it "create" do
      {:post => '/admin/pipelines/pipeline.name/materials/pluggable_scm/plugin.id-1'}.should route_to(:controller => 'admin/materials/pluggable_scm', :action => 'create', :pipeline_name => 'pipeline.name', :plugin_id => 'plugin.id-1')
      send('admin_pluggable_scm_create_path', :pipeline_name => 'foo.bar', :plugin_id => 'plugin-id').should == '/admin/pipelines/foo.bar/materials/pluggable_scm/plugin-id'
    end

    it "edit" do
      {:get => '/admin/pipelines/pipeline.name/materials/pluggable_scm/finger_print/edit'}.should route_to(:controller => 'admin/materials/pluggable_scm', :action => 'edit', :pipeline_name => 'pipeline.name', :finger_print => 'finger_print')
      send('admin_pluggable_scm_edit_path', :pipeline_name => 'foo.bar', :finger_print => 'finger_print').should == '/admin/pipelines/foo.bar/materials/pluggable_scm/finger_print/edit'
    end

    it "update" do
      {:put => '/admin/pipelines/pipeline.name/materials/pluggable_scm/finger_print'}.should route_to(:controller => 'admin/materials/pluggable_scm', :action => 'update', :pipeline_name => 'pipeline.name', :finger_print => 'finger_print')
      send('admin_pluggable_scm_update_path', :pipeline_name => 'foo.bar', :finger_print => 'finger_print').should == '/admin/pipelines/foo.bar/materials/pluggable_scm/finger_print'
    end

    it "check_connection" do
      {:post => '/admin/materials/pluggable_scm/check_connection/plugin.id-1'}.should route_to(:controller => 'admin/materials/pluggable_scm', :action => 'check_connection', :plugin_id => 'plugin.id-1')
      send('admin_pluggable_scm_check_connection_path', :plugin_id => 'plugin-id').should == '/admin/materials/pluggable_scm/check_connection/plugin-id'
    end

    it "pipelines_used_in" do
      {:get => '/admin/materials/pluggable_scm/scm-id/pipelines_used_in'}.should route_to(:controller => 'admin/materials/pluggable_scm', :action => 'pipelines_used_in', :scm_id => 'scm-id')
      send('scm_pipelines_used_in_path', :scm_id => 'scm-id').should == '/admin/materials/pluggable_scm/scm-id/pipelines_used_in'
    end
  end

  describe "action" do
    before do
      SCMMetadataStore.getInstance().clear()

      @material = PluggableSCMMaterialConfig.new(nil, SCMMother.create('scm-id-1', 'scm-name-1', 'plugin-id', '1', Configuration.new([ConfigurationPropertyMother.create('url', false, 'scm-url-1')].to_java(ConfigurationProperty))), nil, nil)

      setup_data
      setup_metadata

      @go_config_service.stub(:registry).and_return(MockRegistryModule::MockRegistry.new)

      @pluggable_scm_service = stub_service(:pluggable_scm_service)
    end

    describe "show_existing" do
      before do
        @pipeline_pause_service.should_receive(:pipelinePauseInfo).with('pipeline-name').and_return(@pause_info)
        @go_config_service.should_receive(:loadForEdit).with('pipeline-name', @user, @result).and_return(@pipeline_config_for_edit)
      end

      it "should load all scms" do
        get :show_existing, :pipeline_name => 'pipeline-name'

        assigns[:material].getType().should == 'PluggableSCMMaterial'
        assigns[:scms].should == @cruise_config.getSCMs()
        assert_template layout: false
      end
    end

    describe "choose_existing" do
      before do
        @pipeline_pause_service.should_receive(:pipelinePauseInfo).with('pipeline-name').and_return(@pause_info)
        @go_config_service.should_receive(:loadForEdit).with('pipeline-name', @user, @result).and_return(@pipeline_config_for_edit)
      end

      it "should choose material" do
        stub_save_for_success

        @cruise_config.getSCMs().size.should == 1
        @pipeline.materialConfigs().size.should == 1

        post :choose_existing, :pipeline_name => 'pipeline-name', :config_md5 => 'md5-1', :material => choose_existing_payload('scm-id-1')

        @cruise_config.getSCMs().size.should == 1
        @pipeline.materialConfigs().size.should == 2
        @cruise_config.getAllErrors().size.should == 0
        @pipeline.materialConfigs().get(1).getFolder().should == 'scm-folder'
        response.body.should == 'Saved successfully'
        URI.parse(response.location).path.should == admin_material_index_path
      end

      it "should assign config_errors for display when choose material fails due to validation errors" do
        stub_save_for_validation_error do |result, cruise_config, node|
          cruise_config.errors().add('base', 'someError')
          result.badRequest(LocalizedMessage.string('UNAUTHORIZED_TO_EDIT_PIPELINE', ['pipeline-name']))
        end

        post :choose_existing, :pipeline_name => 'pipeline-name', :config_md5 => 'md5-1', :material => choose_existing_payload('scm-id-1')

        @cruise_config.getAllErrors().size.should == 1

        assigns[:errors].size.should == 1
        response.status.should == 400
        assert_template layout: false
      end
    end

    describe "new" do
      before do
        @pipeline_pause_service.should_receive(:pipelinePauseInfo).with('pipeline-name').and_return(@pause_info)
        @go_config_service.should_receive(:loadForEdit).with('pipeline-name', @user, @result).and_return(@pipeline_config_for_edit)
      end

      it "should load new material" do
        get :new, :pipeline_name => 'pipeline-name', :plugin_id => 'plugin-id'

        assert_material_is_initialized
        assigns[:meta_data_store].should == @meta_data_store
        assigns[:cruise_config].should == @cruise_config
        assert_template layout: false
      end
    end

    describe "create" do
      before :each do
        @pipeline_pause_service.should_receive(:pipelinePauseInfo).with('pipeline-name').and_return(@pause_info)
        @go_config_service.should_receive(:loadForEdit).with('pipeline-name', @user, @result).and_return(@pipeline_config_for_edit)
        @pluggable_scm_service.should_receive(:validate)
      end

      it "should add new material" do
        stub_save_for_success

        @cruise_config.getSCMs().size.should == 1
        @pipeline.materialConfigs().size.should == 1

        post :create, :pipeline_name => 'pipeline-name', :plugin_id => 'plugin-id', :config_md5 => 'md5-1', :material => create_payload

        @cruise_config.getSCMs().size.should == 2
        @pipeline.materialConfigs().size.should == 2
        @cruise_config.getAllErrors().size.should == 0
        assert_successful_save(@pipeline.materialConfigs().get(1))
        response.body.should == 'Saved successfully'
        URI.parse(response.location).path.should == admin_material_index_path
      end

      it "should assign config_errors for display when create fails due to validation errors" do
        stub_save_for_validation_error do |result, cruise_config, node|
          cruise_config.errors().add('base', 'someError')
          result.badRequest(LocalizedMessage.string('UNAUTHORIZED_TO_EDIT_PIPELINE', ['pipeline-name']))
        end

        post :create, :pipeline_name => 'pipeline-name', :plugin_id => 'plugin-id', :config_md5 => 'md5-1', :material => create_payload

        @cruise_config.getAllErrors().size.should == 1

        assigns[:errors].size.should == 1
        response.status.should == 400
        assert_template layout: false
      end
    end

    describe "edit" do
      before do
        @pipeline_pause_service.should_receive(:pipelinePauseInfo).with('pipeline-name').and_return(@pause_info)
        @go_config_service.should_receive(:loadForEdit).with('pipeline-name', @user, @result).and_return(@pipeline_config_for_edit)
      end

      it "should edit an existing material" do
        get :edit, :pipeline_name => 'pipeline-name', :finger_print => @material.getPipelineUniqueFingerprint()

        assigns[:material].should == @material
        assigns[:meta_data_store].should == @meta_data_store
        assigns[:cruise_config].should == @cruise_config
        assert_template layout: false
      end
    end

    describe "update" do
      before :each do
        @pipeline_pause_service.should_receive(:pipelinePauseInfo).with('pipeline-name').and_return(@pause_info)
        @go_config_service.should_receive(:loadForEdit).with('pipeline-name', @user, @result).and_return(@pipeline_config_for_edit)
        @pluggable_scm_service.should_receive(:validate)
      end

      it "should update existing material" do
        stub_save_for_success

        @cruise_config.getSCMs().size.should == 1
        @pipeline.materialConfigs().size.should == 1

        put :update, :pipeline_name => 'pipeline-name', :config_md5 => 'md5-1', :material => update_payload('scm-id-1'), :finger_print => @material.getPipelineUniqueFingerprint()

        @cruise_config.getSCMs().size.should == 1
        @pipeline.materialConfigs().size.should == 1
        @cruise_config.getAllErrors().size.should == 0
        assert_successful_save(@pipeline.materialConfigs().get(0))
        assigns[:material].should_not == nil
        response.body.should == 'Saved successfully'
        URI.parse(response.location).path.should == admin_material_index_path
      end

      it "should assign config_errors for display when update fails due to validation errors" do
        stub_save_for_validation_error do |result, config, node|
          config.errors().add('base', 'someError')
          result.badRequest(LocalizedMessage.string('UNAUTHORIZED_TO_EDIT_PIPELINE', ['pipeline-name']))
        end

        put :update, :pipeline_name => "pipeline-name", :config_md5 => "md5-1", :material => update_payload('scm-id-1'), :finger_print => @material.getPipelineUniqueFingerprint()

        assigns[:errors].size.should == 1
        assigns[:material].should_not == nil
        response.status.should == 400
        assert_template layout: false
      end
    end

    describe "check_connection" do
      before :each do
        result = double('Result')
        result.stub(:isSuccessful).and_return(true)
        result.stub(:getMessages).and_return(['message 1', 'message 2'])
        @pluggable_scm_service.should_receive(:checkConnection).with(anything()) { result }
      end

      it "should check connection for pluggable SCM" do
        post :check_connection, :plugin_id => 'plugin-id', :material => create_payload

        response.body.should == "{\"status\":\"success\",\"messages\":[\"message 1\",\"message 2\"]}"
      end
    end

    describe "pipelines_used_in" do
      it "should show pipelines used in for pluggable SCM" do
        get :pipelines_used_in, :scm_id => 'scm-id-1'

        response.should render_template "admin/package_definitions/pipelines_used_in"
        assigns[:pipelines_with_group].size.should == 1
        assigns[:pipelines_with_group].get(0).first().name().to_s.should == 'pipeline-name'
        assigns[:pipelines_with_group].get(0).last().getGroup().should == 'defaultGroup'
      end
    end
  end

  def assert_material_is_initialized
    scm = com.thoughtworks.go.domain.scm.SCM.new
    scm.setPluginConfiguration(PluginConfiguration.new('plugin-id', '1'))
    pluggable_scm = PluggableSCMMaterialConfig.new
    pluggable_scm.setSCMConfig(scm)
    assigns[:material].should == pluggable_scm
  end

  def setup_data
    controller.stub(:populate_config_validity)

    @cruise_config = BasicCruiseConfig.new()
    scms = com.thoughtworks.go.domain.scm.SCMs.new
    scms.add(@material.getSCMConfig())
    @cruise_config.setSCMs(scms)

    @cruise_config_mother = GoConfigMother.new

    @pipeline = @cruise_config_mother.addPipeline(@cruise_config, 'pipeline-name', 'stage-name', MaterialConfigs.new([@material].to_java(MaterialConfig)), ['build-name'].to_java(java.lang.String))

    @pipeline_config_for_edit = ConfigForEdit.new(@pipeline, @cruise_config, @cruise_config)

    ReflectionUtil.setField(@cruise_config, 'md5', 'md5-1')
    @user = Username.new(CaseInsensitiveString.new('loser'))
    controller.stub(:current_user).and_return(@user)
    @result = stub_localized_result

    @go_config_service = stub_service(:go_config_service)
    @pipeline_pause_service = stub_service(:pipeline_pause_service)
    @pause_info = PipelinePauseInfo.paused('just for fun', 'loser')
    @go_config_service.stub(:getConfigForEditing).and_return(@cruise_config)
  end

  def setup_metadata
    scm_configurations = SCMConfigurations.new
    scm_configurations.add(SCMConfiguration.new('url'))
    scm_configurations.add(SCMConfiguration.new('branch'))
    scm_configurations.add(SCMConfiguration.new('optional-field'))

    scm_view = double('SCMView')
    scm_view.stub(:displayValue).and_return('display name')
    scm_view.stub(:template).and_return('plugin template')

    @meta_data_store = SCMMetadataStore.getInstance
    @meta_data_store.addMetadataFor('plugin-id', scm_configurations, scm_view)
  end

  def choose_existing_payload(scmId)
    {:scmId => scmId, :folder => 'scm-folder'}
  end

  def create_payload
    {:name => 'scm-name', :url => 'scm-url', :branch => 'scm-branch', 'optional-field' => '', :folder => 'scm-folder'}
  end

  def update_payload(scmId)
    {:scmId => scmId, :name => 'scm-name', :url => 'scm-url', :branch => 'scm-branch', 'optional-field' => '', :folder => 'scm-folder'}
  end

  def assert_successful_save(material_config)
    material_config.getFolder().should == 'scm-folder'
    scm_config = @cruise_config.getSCMs().find(material_config.getScmId())
    scm_config.getName().should == 'scm-name'
    scm_config.getPluginConfiguration().getId().should == 'plugin-id'
    scm_config.getPluginConfiguration().getVersion().should == '1'
    scm_configuration_map = scm_config.configAsMap()
    scm_configuration_map.get('url').get('value').should == 'scm-url'
    scm_configuration_map.get('branch').get('value').should == 'scm-branch'
    scm_configuration_map.get('optional-field').should == nil
  end
end
