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

describe Admin::Materials::PluggableScmController do
  include ConfigSaveStubbing
  include MockRegistryModule

  describe "action" do
    before do
      SCMMetadataStore.getInstance().clear()

      @material = PluggableSCMMaterialConfig.new(nil, SCMMother.create('scm-id-1', 'scm-name-1', 'plugin-id', '1', Configuration.new([ConfigurationPropertyMother.create('url', false, 'scm-url-1')].to_java(ConfigurationProperty))), nil, nil)

      setup_data
      setup_metadata

      allow(@go_config_service).to receive(:registry).and_return(MockRegistryModule::MockRegistry.new)

      @pluggable_scm_service = stub_service(:pluggable_scm_service)
    end

    describe "show_existing" do
      before do
        expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with('pipeline-name').and_return(@pause_info)
        expect(@go_config_service).to receive(:loadForEdit).with('pipeline-name', @user, @result).and_return(@pipeline_config_for_edit)
      end

      it "should load all scms" do
        get :show_existing, params: { :pipeline_name => 'pipeline-name' }

        expect(assigns[:material].getType()).to eq('PluggableSCMMaterial')
        expect(assigns[:scms]).to eq(@cruise_config.getSCMs())
        assert_template layout: false
      end
    end

    describe "choose_existing" do
      before do
        expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with('pipeline-name').and_return(@pause_info)
        expect(@go_config_service).to receive(:loadForEdit).with('pipeline-name', @user, @result).and_return(@pipeline_config_for_edit)
      end

      it "should choose material" do
        stub_save_for_success

        expect(@cruise_config.getSCMs().size).to eq(1)
        expect(@pipeline.materialConfigs().size).to eq(1)

        post :choose_existing, params: { :pipeline_name => 'pipeline-name', :config_md5 => 'md5-1', :material => choose_existing_payload('scm-id-1') }

        expect(@cruise_config.getSCMs().size).to eq(1)
        expect(@pipeline.materialConfigs().size).to eq(2)
        expect(@cruise_config.getAllErrors().size).to eq(0)
        expect(@pipeline.materialConfigs().get(1).getFolder()).to eq('scm-folder')
        expect(response.body).to eq('Saved successfully')
        expect(URI.parse(response.location).path).to eq(admin_material_index_path)
      end

      it "should assign config_errors for display when choose material fails due to validation errors" do
        stub_save_for_validation_error do |result, cruise_config, node|
          cruise_config.errors().add('base', 'someError')
          result.badRequest(LocalizedMessage.string('UNAUTHORIZED_TO_EDIT_PIPELINE', ['pipeline-name']))
        end

        post :choose_existing, params: { :pipeline_name => 'pipeline-name', :config_md5 => 'md5-1', :material => choose_existing_payload('scm-id-1') }

        expect(@cruise_config.getAllErrors().size).to eq(1)

        expect(assigns[:errors].size).to eq(1)
        expect(response.status).to eq(400)
        assert_template layout: false
      end
    end

    describe "new" do
      before do
        expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with('pipeline-name').and_return(@pause_info)
        expect(@go_config_service).to receive(:loadForEdit).with('pipeline-name', @user, @result).and_return(@pipeline_config_for_edit)
      end

      it "should load new material" do
        get :new, params: { :pipeline_name => 'pipeline-name', :plugin_id => 'plugin-id' }

        assert_material_is_initialized
        expect(assigns[:meta_data_store]).to eq(@meta_data_store)
        expect(assigns[:cruise_config]).to eq(@cruise_config)
        assert_template layout: false
      end
    end

    describe "create" do
      before :each do
        expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with('pipeline-name').and_return(@pause_info)
        expect(@go_config_service).to receive(:loadForEdit).with('pipeline-name', @user, @result).and_return(@pipeline_config_for_edit)
        expect(@pluggable_scm_service).to receive(:validate)
      end

      it "should add new material" do
        stub_save_for_success

        expect(@cruise_config.getSCMs().size).to eq(1)
        expect(@pipeline.materialConfigs().size).to eq(1)

        post :create, params: { :pipeline_name => 'pipeline-name', :plugin_id => 'plugin-id', :config_md5 => 'md5-1', :material => create_payload }

        expect(@cruise_config.getSCMs().size).to eq(2)
        expect(@pipeline.materialConfigs().size).to eq(2)
        expect(@cruise_config.getAllErrors().size).to eq(0)
        assert_successful_save(@pipeline.materialConfigs().get(1))
        expect(response.body).to eq('Saved successfully')
        expect(URI.parse(response.location).path).to eq(admin_material_index_path)
      end

      it "should assign config_errors for display when create fails due to validation errors" do
        stub_save_for_validation_error do |result, cruise_config, node|
          cruise_config.errors().add('base', 'someError')
          result.badRequest(LocalizedMessage.string('UNAUTHORIZED_TO_EDIT_PIPELINE', ['pipeline-name']))
        end

        post :create, params: { :pipeline_name => 'pipeline-name', :plugin_id => 'plugin-id', :config_md5 => 'md5-1', :material => create_payload }

        expect(@cruise_config.getAllErrors().size).to eq(1)

        expect(assigns[:errors].size).to eq(1)
        expect(response.status).to eq(400)
        assert_template layout: false
      end
    end

    describe "edit" do
      before do
        expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with('pipeline-name').and_return(@pause_info)
        expect(@go_config_service).to receive(:loadForEdit).with('pipeline-name', @user, @result).and_return(@pipeline_config_for_edit)
      end

      it "should edit an existing material" do
        get :edit, params: { :pipeline_name => 'pipeline-name', :finger_print => @material.getPipelineUniqueFingerprint() }

        expect(assigns[:material]).to eq(@material)
        expect(assigns[:meta_data_store]).to eq(@meta_data_store)
        expect(assigns[:cruise_config]).to eq(@cruise_config)
        assert_template layout: false
      end
    end

    describe "update" do
      before :each do
        expect(@pipeline_pause_service).to receive(:pipelinePauseInfo).with('pipeline-name').and_return(@pause_info)
        expect(@go_config_service).to receive(:loadForEdit).with('pipeline-name', @user, @result).and_return(@pipeline_config_for_edit)
        expect(@pluggable_scm_service).to receive(:validate)
      end

      it "should update existing material" do
        stub_save_for_success

        expect(@cruise_config.getSCMs().size).to eq(1)
        expect(@pipeline.materialConfigs().size).to eq(1)

        put :update, params: { :pipeline_name => 'pipeline-name', :config_md5 => 'md5-1', :material => update_payload('scm-id-1'), :finger_print => @material.getPipelineUniqueFingerprint() }

        expect(@cruise_config.getSCMs().size).to eq(1)
        expect(@pipeline.materialConfigs().size).to eq(1)
        expect(@cruise_config.getAllErrors().size).to eq(0)
        assert_successful_save(@pipeline.materialConfigs().get(0))
        expect(assigns[:material]).not_to eq(nil)
        expect(response.body).to eq('Saved successfully')
        expect(URI.parse(response.location).path).to eq(admin_material_index_path)
      end

      it "should assign config_errors for display when update fails due to validation errors" do
        stub_save_for_validation_error do |result, config, node|
          config.errors().add('base', 'someError')
          result.badRequest(LocalizedMessage.string('UNAUTHORIZED_TO_EDIT_PIPELINE', ['pipeline-name']))
        end

        put :update, params: { :pipeline_name => "pipeline-name", :config_md5 => "md5-1", :material => update_payload('scm-id-1'), :finger_print => @material.getPipelineUniqueFingerprint() }

        expect(assigns[:errors].size).to eq(1)
        expect(assigns[:material]).not_to eq(nil)
        expect(response.status).to eq(400)
        assert_template layout: false
      end
    end

    describe "check_connection" do
      before :each do
        result = double('Result')
        allow(result).to receive(:isSuccessful).and_return(true)
        allow(result).to receive(:getMessages).and_return(['message 1', 'message 2'])
        expect(@pluggable_scm_service).to receive(:checkConnection).with(anything()) { result }
      end

      it "should check connection for pluggable SCM" do
        post :check_connection, params: { :plugin_id => 'plugin-id', :material => create_payload }

        expect(response.body).to eq("{\"status\":\"success\",\"messages\":[\"message 1\",\"message 2\"]}")
      end
    end

    describe "pipelines_used_in" do
      it "should show pipelines used in for pluggable SCM" do
        get :pipelines_used_in, params: { :scm_id => 'scm-id-1' }

        expect(response).to render_template "admin/package_definitions/pipelines_used_in"
        expect(assigns[:pipelines_with_group].size).to eq(1)
        expect(assigns[:pipelines_with_group].get(0).first().name().to_s).to eq('pipeline-name')
        expect(assigns[:pipelines_with_group].get(0).last().getGroup()).to eq('defaultGroup')
      end
    end
  end

  def assert_material_is_initialized
    scm = com.thoughtworks.go.domain.scm.SCM.new
    scm.setPluginConfiguration(PluginConfiguration.new('plugin-id', nil))
    pluggable_scm = PluggableSCMMaterialConfig.new
    pluggable_scm.setSCMConfig(scm)
    expect(assigns[:material]).to eq(pluggable_scm)
  end

  def setup_data
    allow(controller).to receive(:populate_config_validity)

    @cruise_config = BasicCruiseConfig.new()
    scms = com.thoughtworks.go.domain.scm.SCMs.new
    scms.add(@material.getSCMConfig())
    @cruise_config.setSCMs(scms)

    @cruise_config_mother = GoConfigMother.new

    @pipeline = @cruise_config_mother.addPipeline(@cruise_config, 'pipeline-name', 'stage-name', MaterialConfigs.new([@material].to_java(MaterialConfig)), ['build-name'].to_java(java.lang.String))

    @pipeline_config_for_edit = ConfigForEdit.new(@pipeline, @cruise_config, @cruise_config)

    ReflectionUtil.setField(@cruise_config, 'md5', 'md5-1')
    @user = Username.new(CaseInsensitiveString.new('loser'))
    allow(controller).to receive(:current_user).and_return(@user)
    @result = stub_localized_result

    @go_config_service = stub_service(:go_config_service)
    @pipeline_pause_service = stub_service(:pipeline_pause_service)
    @pause_info = PipelinePauseInfo.paused('just for fun', 'loser')
    allow(@go_config_service).to receive(:getConfigForEditing).and_return(@cruise_config)
  end

  def setup_metadata
    scm_configurations = SCMConfigurations.new
    scm_configurations.add(SCMConfiguration.new('url'))
    scm_configurations.add(SCMConfiguration.new('branch'))
    scm_configurations.add(SCMConfiguration.new('optional-field'))

    scm_view = double('SCMView')
    allow(scm_view).to receive(:displayValue).and_return('display name')
    allow(scm_view).to receive(:template).and_return('plugin template')

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
    expect(material_config.getFolder()).to eq('scm-folder')
    scm_config = @cruise_config.getSCMs().find(material_config.getScmId())
    expect(scm_config.getName()).to eq('scm-name')
    expect(scm_config.getPluginConfiguration().getId()).to eq('plugin-id')
    expect(scm_config.getPluginConfiguration().getVersion()).to eq('1')
    scm_configuration_map = scm_config.configAsMap()
    expect(scm_configuration_map.get('url').get('value')).to eq('scm-url')
    expect(scm_configuration_map.get('branch').get('value')).to eq('scm-branch')
    expect(scm_configuration_map.get('optional-field')).to eq(nil)
  end
end
