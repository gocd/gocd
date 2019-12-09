#
# Copyright 2019 ThoughtWorks, Inc.
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
#

module Admin
  class PipelinesController < FastAdminController
    helper ::Admin::AdminHelper
    helper ::Admin::PipelinesHelper
    helper FlashMessagesHelper
    include ::Admin::PipelinesHelper
    ERROR_PATTERN = /#{ParamSubstitutionHandler::NO_PARAM_FOUND_MSG.gsub("'%s'", "'([^']*)'")}/

    CLONER = GoConfigCloner.new

    include PipelineConfigLoader
    include ::Admin::DependencyMaterialAutoSuggestions

    before_action :load_config_for_edit, :only => [:new, :create]
    before_action :load_template_list, :only => [:new, :create]

    load_pipeline_except_for :update, :new, :create, :clone, :save_clone

    layout "pipelines/details", :except => [:new, :create]

    def new
      @pipeline = empty_pipeline
      @group_name = params[:group]
      @all_pipelines = @cruise_config.getAllPipelineNames()
      @pipeline_group = BasicPipelineConfigs.new([@pipeline].to_java(PipelineConfig))
      @original_cruise_config = @cruise_config
      assert_load :task_view_models, task_view_service.getTaskViewModels()
      load_autocomplete_suggestions
      render :new, :layout => "application"
    end

    def create
      pipeline = empty_pipeline
      pipeline.setConfigAttributes(params[:pipeline_group][:pipeline], task_view_service)
      save_action = Class.new(::ConfigUpdate::SaveAction) do
        attr_reader :group
        include ::ConfigUpdate::CheckCanCreatePipeline
        include ::ConfigUpdate::CruiseConfigNode
        include ::ConfigUpdate::LoadConfig
        include ::Admin::PipelinesHelper

        def initialize params, user, security_service, pipeline, package_definition_service, pluggable_task_service
          super(params, user, security_service)
          @pipeline = pipeline
          @package_definition_service = package_definition_service
          @pluggable_task_service = pluggable_task_service
        end

        def subject(cruise_config)
          @pipeline
        end

        def update(cruise_config)
          if !use_template?(params)
            task = @pipeline.getFirstStageConfig().getJobs().first().getTasks().first()
            @pluggable_task_service.validate(task) if task.instance_of? com.thoughtworks.go.config.pluggabletask.PluggableTask
            @pluggable_task_service.validate(task.cancelTask()) if (!task.cancelTask().nil?) && (task.cancelTask().instance_of? com.thoughtworks.go.config.pluggabletask.PluggableTask)
          elsif !@pipeline.hasTemplate()
            @pipeline.addError(PipelineConfig::TEMPLATE_NAME, "Template name must be specified.")
          end

          if @pipeline.material_configs.size() > 0 && @pipeline.material_configs.get(0).type == PackageMaterialConfig::TYPE
            handle_package_material_creation_or_association(cruise_config)
          end

          add_pipeline(cruise_config, params[:pipeline_group][:group], @pipeline)
        end

        def handle_package_material_creation_or_association(cruise_config)
          package_material = @pipeline.material_configs.get(0)
          if params[:material][:create_or_associate_pkg_def] == "create"
            package_definition = PackageDefinitionCreator.new(@package_definition_service, params[:material]).createNewPackageDefinition(cruise_config)
            package_material.setPackageDefinition(package_definition)
          elsif params[:material][:create_or_associate_pkg_def] == "associate"
            package_definition = PackageDefinitionCreator.new(@package_definition_service, params[:material]).getPackageDefinition(cruise_config)
            package_material.setPackageDefinition(package_definition)
          end
        end

        def add_pipeline cruise_config, group_name, pipeline
          cruise_config.addPipelineWithoutValidation(group_name, pipeline)
          @group = cruise_config.findGroupOfPipeline(pipeline)
        end
      end.new(params, current_user, security_service, pipeline, package_definition_service, pluggable_task_service)

      save_page(params[:config_md5], nil, {:action => :new, :layout => 'application'}, save_action, 'Pipeline successfully created.') do
        assert_load(:task_view_models, task_view_service.getTaskViewModels()) if !@update_result.isSuccessful()
        assert_load(:pipeline, @subject)

        if !@update_result.isSuccessful() && !use_template?(params)
          task = @pipeline.getFirstStageConfig().getJobs().first().getTasks().first()
          task_view_model = task_view_service.getViewModel(task, 'new')
          task_view_model1 = task_view_service.getModelOfType(@task_view_models, task.getTaskType())
          task_view_model1.setModel(task_view_model.getModel())
        end

        if use_template?(params) && !@pipeline.hasTemplate()
          @pipeline.addError(PipelineConfig::TEMPLATE_NAME, "Template name must be specified.")
        end

        group = save_action.group
        group = @cruise_config.findGroup(params[:pipeline_group][:group]) if group.nil?
        assert_load(:pipeline_group, group)
        assert_load(:group_name, group.getGroup())

        assert_load(:all_pipelines, go_config_service.getCurrentConfig().getAllPipelineNames())
        load_pause_info
        load_autocomplete_suggestions
        set_save_redirect_url(pipeline_edit_path(:pipeline_name => @pipeline.name(), :stage_parent=>"pipelines", :current_tab => "general")) if @update_result.isSuccessful()

        pipeline_pause_service.pause(@pipeline.name().to_s, "Under construction", current_user) if @update_result.isSuccessful() #The if check is important now as we want consistency across config and db save. If config save fails, we do not want to insert it in the DB.

        if @update_result.isSuccessful()
          pipeline_selections_service.update(cookies[:selected_pipelines], current_user_entity_id, @pipeline.name())
        end

        @original_cruise_config = @cruise_config
        if @pipeline.material_configs.size() > 0 && @pipeline.material_configs.get(0).type == PackageMaterialConfig::TYPE
          populate_package_material_data
        end
      end
    end

    def populate_package_material_data
      package_definition = @pipeline.material_configs.get(0).getPackageDefinition()
      if package_definition && package_definition.getRepository()
        plugin_id = package_definition.getRepository().getPluginConfiguration().getId()
        package_configurations = PackageMetadataStore.getInstance().getMetadata(plugin_id)
        @package_configuration = PackageViewModel.new(package_configurations, package_definition)
      end
    end

    def edit
      render :action => params[:current_tab]
    end

    def update
      if params[:current_tab] == "parameters"
        update_parameter_tab
      else
        update_other_tab
      end
    end

    private

    def update_other_tab
      save_tab({:action => params[:current_tab]})
    end

    def update_parameter_tab
      save_tab(
        lambda do |result, all_errors_on_other_objects|
          merge_params(@pipeline.getParams(), @original_params.params)
          render_error(result, all_errors_on_other_objects, {:action => params[:current_tab]})
        end)
    end

    private

    def empty_pipeline
      PipelineConfig.new(CaseInsensitiveString.new(""), com.thoughtworks.go.config.materials.MaterialConfigs.new, [default_stage_config].to_java(StageConfig))
    end

    def save_tab(error_rendering_options_or_proc)
      if Toggles.isToggleOff(Toggles.FAST_PIPELINE_SAVE)
        save_tab_old(error_rendering_options_or_proc)
        return
      end
      pipeline_name = params[:pipeline_name]
      @original_params = Struct.new(:params).new
      @original_pipeline_config = pipeline_config_service.getPipelineConfig(pipeline_name)
      @original_params.params = CLONER.deepClone(@original_pipeline_config.getParams())
      @pipeline = CLONER.deep_clone(@original_pipeline_config)
      @pipeline.setConfigAttributes(params['pipeline'])
      redirect_url = pipeline_edit_path(:pipeline_name => params[:pipeline_name], :stage_parent => "pipelines", :current_tab => params[:current_tab])
      fast_save_page(redirect_url, error_rendering_options_or_proc) do
        assert_load(:pipeline, @pipeline)
        assert_load(:pipeline_group_name, params[:pipeline_group_name])
        assert_load(:pipeline_md5, params[:pipeline_md5])
        load_pause_info
      end
    end

    def save_tab_old(error_rendering_options_or_proc)
      @original_params = Struct.new(:params).new
      save_page(params[:config_md5], pipeline_edit_path(:pipeline_name => params[:pipeline_name], :stage_parent=>"pipelines", :current_tab => params[:current_tab]), error_rendering_options_or_proc, Class.new(::ConfigUpdate::SaveAsPipelineAdmin) do
        include ConfigUpdate::PipelineNode
        include ConfigUpdate::NodeAsSubject

        def initialize(params, user, security_service, original_params)
          super(params, user, security_service)
          @original_params = original_params
        end

        def update(pipeline)
          @original_params.params = CLONER.deepClone(pipeline.getParams())
          pipeline.setConfigAttributes(params[:pipeline])
        end
      end.new(params, current_user.getUsername(), security_service, @original_params)) do
        assert_load(:pipeline, @node)
        load_pause_info
      end
    end

    def merge_params(changed_param_configs, original_param_configs, request_params = params[:pipeline][:params])
      config_param_errors_matching(ERROR_PATTERN).each do |param_name|
        if renamed = renamed_param?(param_name, request_params)
          renamed_config = changed_param_configs.getParamNamed(renamed[:name])
          renamed_config.addError(ParamConfig::NAME, "Parameter '#{param_name}' cannot be renamed because it is referenced by other elements")
        elsif param_deleted?(param_name, request_params)
          param_config = GoConfigCloner.new.deepClone(original_param_configs.getParamNamed(param_name))
          param_config.addError(ParamConfig::NAME, "Parameter cannot be deleted because it is referenced by other elements")
          changed_param_configs.add(original_param_configs.getIndex(param_name), param_config)
        else
          raise "Should not get here! Param '#{param_name}' was referenced elsewhere but was not found in the request params: #{request_params.inspect}"
        end
      end
    end

    def renamed_param?(param_name, request_params)
      request_params.find { |p| p[:original_name] == param_name }
    end

    def param_deleted?(param_name, request_params)
      !(request_params.any? { |p| p[:name] == param_name })
    end

    def config_param_errors_matching(pattern)
      node_for_errors = Toggles.isToggleOff(Toggles.FAST_PIPELINE_SAVE) ? @cruise_config : @pipeline
      node_for_errors.getAllErrors().
              collect { |config_error| config_error.getAll().to_a }.
              flatten.
              collect { |err_msg| err_msg =~ pattern ? $1 : nil }.
              compact
    end

    def load_config_for_edit
      assert_load(:cruise_config, CLONER.deepClone(go_config_service.getConfigForEditing()))
    end

    def load_template_list
      assert_load(:template_list, template_config_service.getTemplateViewModels(current_user.getUsername()))
    end

    def load_group_list
      group_array = Array.new
      group_list = Array.new
      security_service.modifiableGroupsForUser(current_user).each do |group|
        group_array.push({:group => group})
        group_list.push(group)
      end

      group_array = group_array.sort_by { |group| group[:group].downcase }
      group_list = group_list.sort_by(&:downcase)

      assert_load :groups_json, group_array.to_json
      assert_load :groups_list, group_list
    end

    def load_autocomplete_suggestions
      load_group_list()
      assert_load :pipeline_stages_json, pipeline_stages_json(go_config_service.getCurrentConfig(), current_user, security_service, params)
    end

  end
end
