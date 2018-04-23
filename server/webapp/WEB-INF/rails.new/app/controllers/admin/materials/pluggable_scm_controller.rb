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

module Admin::Materials
  class PluggableScmController < AdminController
    include PipelineConfigLoader

    before_action :load_config_for_edit, :only => [:pipelines_used_in]
    load_pipeline_except_for :check_connection, :pipelines_used_in

    def show_existing
      assert_load :material, PluggableSCMMaterialConfig.new
      assert_load :scms, @cruise_config.getSCMs()
      render layout: false
    end

    def choose_existing
      assert_load :material, PluggableSCMMaterialConfig.new

      create_failure_handler = proc do |result, all_errors|
        @errors = flatten_all_errors(all_errors)
        @scms = @cruise_config.getSCMs()
        render :template => '/admin/materials/pluggable_scm/show_existing', :status => result.httpCode(), :layout => false
      end

      save_popup(params[:config_md5], get_choose_existing_command, create_failure_handler, { :controller => '/admin/materials', :stage_parent => 'pipelines', :current_tab => params[:current_tab] }) do
        assert_load :pipeline, @node.pipelineConfigByName(CaseInsensitiveString.new(params[:pipeline_name]))
        assert_load :material, @subject
      end
    end

    def new
      scm = com.thoughtworks.go.domain.scm.SCM.new
      scm.setPluginConfiguration(PluginConfiguration.new(params[:plugin_id], nil))
      pluggable_scm = PluggableSCMMaterialConfig.new
      pluggable_scm.setSCMConfig(scm)

      assert_load :material, pluggable_scm
      assert_load :meta_data_store, meta_data_store
      render layout: false
    end

    def create
      scm = com.thoughtworks.go.domain.scm.SCM.new
      scm.setPluginConfiguration(PluginConfiguration.new(params[:plugin_id], '1'))
      pluggable_scm = PluggableSCMMaterialConfig.new
      pluggable_scm.setSCMConfig(scm)
      assert_load :material, pluggable_scm

      create_failure_handler = proc do |result, all_errors|
        @errors = flatten_all_errors(all_errors)
        @meta_data_store = meta_data_store
        render :template => '/admin/materials/pluggable_scm/new', :status => result.httpCode(), :layout => false
      end

      save_popup(params[:config_md5], get_create_command, create_failure_handler, { :controller => '/admin/materials', :stage_parent => 'pipelines', :current_tab => params[:current_tab] }) do
        assert_load :pipeline, @node.pipelineConfigByName(CaseInsensitiveString.new(params[:pipeline_name]))
        assert_load :material, @subject
      end
    end

    def edit
      assert_load :material, @pipeline.materialConfigs().getByFingerPrint(params[:finger_print])
      assert_load :meta_data_store, meta_data_store
      render layout: false unless performed?
    end

    def update
      material = @pipeline.materialConfigs().getByFingerPrint(params[:finger_print])
      assert_load :material, material
      assert_load :index, @pipeline.materialConfigs().indexOf(material)

      create_failure_handler = proc do |result, all_errors|
        @errors = flatten_all_errors(all_errors)
        @meta_data_store = meta_data_store
        render :template => '/admin/materials/pluggable_scm/edit', :status => result.httpCode(), :layout => false
      end

      save_popup(params[:config_md5], get_update_command, create_failure_handler, { :controller => '/admin/materials', :stage_parent => "pipelines", :current_tab => params[:current_tab] }) do
        assert_load :pipeline, @node.pipelineConfigByName(CaseInsensitiveString.new(params[:pipeline_name]))
        assert_load :material, @subject
      end
    end

    def check_connection
      scm = com.thoughtworks.go.domain.scm.SCM.new
      scm.setPluginConfiguration(PluginConfiguration.new(params[:plugin_id], nil))
      scm.setConfigAttributes(params[:material])

      result = pluggable_scm_service.checkConnection(scm)
      render json: { status: result.isSuccessful() ? 'success' : 'failure', messages: result.getMessages() }
    end

    def pipelines_used_in
      scm_to_pipeline_map
      @pipelines_with_group = @package_to_pipeline_map.get(params[:scm_id])
      render 'admin/package_definitions/pipelines_used_in', layout: false
    end

    private

    def get_choose_existing_command
      Class.new(::ConfigUpdate::SaveAsPipelineAdmin) do
        include ::ConfigUpdate::LoadConfig

        def initialize params, user, security_service, material
          super(params, user, security_service)
          @material = material
        end

        def node(cruise_config)
          cruise_config
        end

        def subject(cruise_config)
          cruise_config.pipelineConfigByName(CaseInsensitiveString.new(params[:pipeline_name])).materialConfigs().last()
        end

        def update(cruise_config)
          scm = cruise_config.getSCMs().find(params[:material][:scmId])
          @material.setSCMConfig(scm)

          @material.setConfigAttributes(params[:material])

          cruise_config.pipelineConfigByName(CaseInsensitiveString.new(params[:pipeline_name])).addMaterialConfig(@material)
        end
      end.new(params, current_user.getUsername(), security_service, @material)
    end

    def get_create_command
      Class.new(::ConfigUpdate::SaveAsPipelineAdmin) do
        include ::ConfigUpdate::LoadConfig

        def initialize params, user, security_service, material, pluggable_scm_service
          super(params, user, security_service)
          @material = material
          @pluggable_scm_service = pluggable_scm_service
        end

        def node(cruise_config)
          cruise_config
        end

        def subject(cruise_config)
          cruise_config.pipelineConfigByName(CaseInsensitiveString.new(params[:pipeline_name])).materialConfigs().last()
        end

        def update(cruise_config)
          scm = @material.getSCMConfig()
          scm.setConfigAttributes(params[:material])
          scm.ensureIdExists
          params[:material][PluggableSCMMaterialConfig::SCM_ID] = scm.getSCMId

          @pluggable_scm_service.validate(scm)

          scm.clearEmptyConfigurations()

          @material.setConfigAttributes(params[:material])

          cruise_config.getSCMs().add(scm)
          cruise_config.pipelineConfigByName(CaseInsensitiveString.new(params[:pipeline_name])).addMaterialConfig(@material)
        end
      end.new(params, current_user.getUsername(), security_service, @material, pluggable_scm_service)
    end

    def get_update_command
      Class.new(::ConfigUpdate::SaveAsPipelineAdmin) do
        include ::ConfigUpdate::LoadConfig

        def initialize params, user, security_service, material, index, pluggable_scm_service
          super(params, user, security_service)
          @material = material
          @index = index
          @pluggable_scm_service = pluggable_scm_service
        end

        def node(cruise_config)
          cruise_config
        end

        def subject(cruise_config)
          pipeline = cruise_config.pipelineConfigByName(CaseInsensitiveString.new(params[:pipeline_name]))
          #Since the fingerprint that got submitted could be different from the new fingerprint after the setConfigAttributes, use the index.
          if @index
            material_config = pipeline.materialConfigs().get(@index)
          end
          material_config
        end

        def update(cruise_config)
          scm_id = @material.getScmId()
          scm = cruise_config.getSCMs().find(scm_id)
          scm.setConfigAttributes(params[:material])

          @pluggable_scm_service.validate(scm)

          scm.clearEmptyConfigurations()

          pipeline = cruise_config.pipelineConfigByName(CaseInsensitiveString.new(params[:pipeline_name]))
          material_config = pipeline.materialConfigs().get(@index)
          material_config.setConfigAttributes(params[:material])
        end
      end.new(params, current_user.getUsername(), security_service, @material, @index, pluggable_scm_service)
    end

    def meta_data_store
      SCMMetadataStore.getInstance()
    end

    def load_config_for_edit
      assert_load(:cruise_config, go_config_service.getConfigForEditing())
    end

    def scm_to_pipeline_map
      @package_to_pipeline_map = @cruise_config.getGroups().getPluggableSCMMaterialUsageInPipelines()
    end
  end
end