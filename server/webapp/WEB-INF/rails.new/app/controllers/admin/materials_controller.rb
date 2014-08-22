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

module Admin
  class MaterialsController < AdminController
    layout "pipelines/details", :only => [:index, :destroy]

    include PauseInfoLoader
    include PipelineConfigLoader

    load_pipeline_except_for :create, :update, :destroy, :create_package_and_associate

    def index
      params[:current_tab] = 'materials'
    end

    def new
      load_new_material(@cruise_config)
      load_other_form_objects(@processed_cruise_config)
      render layout: false
    end

    def create
      load_new_material(@cruise_config)
      save_popup(params[:config_md5], get_create_command, {:action => :new, :layout => false}, {:controller => '/admin/materials', :stage_parent => "pipelines", :current_tab => params[:current_tab]}) do
        assert_load :pipeline, get_node_for_create
        assert_load :material, @subject
        load_pause_info
        load_other_form_objects(@cruise_config)
      end
    end

    def edit
      get_material_from_pipeline
      load_other_form_objects(@processed_cruise_config)
      render layout: false unless performed?
    end

    def update
      save_popup(params[:config_md5], get_update_command, {:action => :edit, :layout => false}, {:controller => '/admin/materials', :stage_parent => "pipelines", :current_tab => params[:current_tab]}) do
        assert_load :pipeline, ConfigUpdate::LoadConfig.for(params).load_pipeline(@cruise_config)
        assert_load :material, @subject unless @subject.nil?
        load_other_form_objects(@cruise_config)
        load_pause_info
      end
    end

    def destroy
      save_page(params[:config_md5], admin_material_index_path(:pipeline_name => params[:pipeline_name]), {:action => :index}, Class.new(::ConfigUpdate::SaveAsPipelineAdmin) do
        include ::ConfigUpdate::PipelineForMaterialNode
        include ::ConfigUpdate::PipelineMaterialSubject

        def initialize params, user, security_service
          super(params, user, security_service)
        end

        def update(pipeline)
          material_config = pipeline.materialConfigs().getByFingerPrint(params[:finger_print])
          pipeline.removeMaterialConfig(material_config)
        end
      end.new(params, current_user.getUsername(), security_service)) do
        assert_load :pipeline, @node
        load_pause_info
      end
    end

    private
    def get_update_command
      Class.new(::ConfigUpdate::SaveAsPipelineAdmin) do
        include ::ConfigUpdate::LoadConfig
        include ::ConfigUpdate::NodeAsSubject

        def initialize params, user, security_service
          super(params, user, security_service)
        end

        def node(cruise_config)
          pipeline = load_pipeline(cruise_config)
          #Since the fingerprint that got submitted could be different from the new fingerprint after the setConfigAttributes, use the index.
          if @index
            material_config = pipeline.materialConfigs().get(@index)
          else
            material_config = load_material_config_for_pipeline(cruise_config)
            @index = pipeline.materialConfigs().indexOf(material_config)
          end
          @cruise_config = cruise_config
          material_config
        end

        def update(material_config)
          material_config.setConfigAttributes(params[:material])
        end
      end.new(params, current_user.getUsername(), security_service)
    end

    def get_node_for_create
      @node
    end

    def get_create_command
      Class.new(::ConfigUpdate::SaveAsPipelineAdmin) do
        include ::ConfigUpdate::PipelineForMaterialNode

        def initialize params, user, security_service, material, cruise_config
          super(params, user, security_service)
          @material = material
          @cruise_config = cruise_config
        end

        def subject(pipeline)
          pipeline.materialConfigs().last()
        end

        def update(pipeline)
          @material.setConfigAttributes(params[:material])
          pipeline.addMaterialConfig(@material)
        end
      end.new(params, current_user.getUsername(), security_service, @material, go_config_service.getConfigForEditing())
    end

    def get_material_from_pipeline
      finger_print = params[:finger_print]
      assert_load :material, @pipeline.materialConfigs().getByFingerPrint(finger_print)
    end

    def load_other_form_objects(cruise_config)
    end
  end
end