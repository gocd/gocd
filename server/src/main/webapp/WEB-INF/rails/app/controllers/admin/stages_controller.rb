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
  class StagesController < AdminController
    include PipelineConfigLoader
    helper AdminHelper
    helper FlashMessagesHelper
    helper TaskHelper
    include AuthenticationHelper

    before_action :check_admin_user_and_403, only: [:config_change]
    load_pipeline_except_for :create, :update, :destroy, :increment_index, :decrement_index, :config_change

    layout "application", :except => [:new, :create, :update]

    def index
      params[:current_tab] = "stages"
      load_stage_usage
      load_template_list
      render with_layout({}, "details")
    end

    def new
      assert_load :task_view_models, task_view_service.getTaskViewModels()
      assert_load :stage, new_stage
      render layout: false
    end

    def create
      assert_load :stage, new_stage
      @stage.setConfigAttributes(params[:stage], task_view_service)
      save_popup(params[:config_md5], Class.new(::ConfigUpdate::SaveAsPipelineOrTemplateAdmin) do
        include ::ConfigUpdate::PipelineOrTemplateNode
        include ::ConfigUpdate::RefsAsUpdatedRefs

        def initialize params, user, security_service, stage, pluggable_task_service
          super(params, user, security_service)
          @stage = stage
          @pluggable_task_service = pluggable_task_service
        end

        def subject(pipeline)
          @stage
        end

        def update(pipeline)
          task = @stage.getJobs().first().getTasks().first()
          @pluggable_task_service.validate(task) if task.instance_of? com.thoughtworks.go.config.pluggabletask.PluggableTask
          pipeline.addStageWithoutValidityAssertion(@stage)
        end
      end.new(params, current_user.getUsername(), security_service, @stage, pluggable_task_service), {:action => :new, :layout => false}, {:current_tab => params[:current_tab]}) do
        assert_load(:pipeline, @node)
        assert_load(:stage, @subject)
        assert_load(:task_view_models, task_view_service.getTaskViewModelsWith(@stage.allBuildPlans().first().tasks().first())) unless @update_result.isSuccessful()
      end
    end

    def update
      save_popup(params[:config_md5], Class.new(::ConfigUpdate::SaveAsPipelineOrTemplateAdmin) do
        include ConfigUpdate::StageNode
        include ConfigUpdate::NodeAsSubject

        def updatedNode(cruise_config)
          load_from_pipeline_stage_named(load_pipeline_or_template(cruise_config), CaseInsensitiveString.new(params[:stage][:name] || params[:stage_name]))
        end

        def update(stage)
          stage.setConfigAttributes(params[:stage])
        end
      end.new(params, current_user.getUsername(), security_service), {:action => params[:current_tab], :layout => nil}, {:current_tab => params[:current_tab], :action => :edit, :stage_name => params[:stage][:name] || params[:stage_name]}) do
        @should_not_render_layout = true
        assert_load(:pipeline, ConfigUpdate::LoadConfig.for(params).load_pipeline_or_template(@cruise_config))
        assert_load(:stage, @node)

        load_data_for_permissions
        load_pause_info
      end

    end

    def edit
        load_stage
        load_data_for_permissions
        render(with_layout(:action => params[:current_tab])) unless @error_rendered
    end

    def destroy
      save_page(params[:config_md5], admin_stage_listing_path(:pipeline_name => params[:pipeline_name]), with_layout({:action => :index}, "details"), Class.new(::ConfigUpdate::SaveAsPipelineOrTemplateAdmin) do
        include ::ConfigUpdate::PipelineOrTemplateNode
        include ::ConfigUpdate::PipelineStageSubject
        include ::ConfigUpdate::RefsAsUpdatedRefs

        def update(pipeline)
          stage = subject(pipeline)
          pipeline.remove(stage)
        end
      end.new(params, current_user.getUsername(), security_service)) do
        load_for_listing
      end
    end

    def increment_index
      change_index { |pipeline, stage| pipeline.incrementIndex(stage) }
    end

    def decrement_index
      change_index { |pipeline, stage| pipeline.decrementIndex(stage) }
    end

    def use_template
      params[:current_tab] = "stages"
      save_page(params[:config_md5], admin_stage_listing_path(:current_tab => "stages"), with_layout({:action => :index}, "details"), Class.new(::ConfigUpdate::SaveAsPipelineOrTemplateAdmin) do
        include ::ConfigUpdate::PipelineNode
        include ::ConfigUpdate::NodeAsSubject
        include ::ConfigUpdate::RefsAsUpdatedRefs

        def update(pipeline)
          pipeline.setConfigAttributes(params[:pipeline])
        end
      end.new(params, current_user.getUsername(), security_service)) do
        load_for_listing
      end
    end

    def config_change
      @changes = go_config_service.configChangesFor(params[:later_md5], params[:earlier_md5], result = HttpLocalizedOperationResult.new)
      @config_change_error_message = result.isSuccessful ? ('This is the first entry in the config versioning. Please refer config tab to view complete configuration during this run.' if @changes == nil) : result.message()
    end

    private

    def load_stage
      stage_name = ::ConfigUpdate::LoadConfig.for(params).stage_name
      assert_load(:stage, @pipeline.getStage(stage_name), "No stage named '#{stage_name}' exists for pipeline '#{@pipeline.name()}'.")
    end

    def load_stage_usage
      unless params[:stage_parent] == "templates"
        @pipeline && assert_load(:stage_usage, @cruise_config.getStagesUsedAsMaterials(@pipeline))
      end
    end

    def load_data_for_permissions
      return if params[:current_tab] != "permissions"
      if params[:stage_parent] == "templates"
        assert_load(:autocomplete_users, user_service.allUsernames().to_json)
        assert_load(:autocomplete_roles, user_service.allRoleNames(@cruise_config).to_json)
        return
      end
      assert_load(:pipeline_group, @cruise_config.findGroup(@cruise_config.getGroups().findGroupNameByPipeline(@pipeline.name())))
      assert_load(:autocomplete_users, user_service.usersThatCanOperateOnStage(@cruise_config, @pipeline).to_json)
      assert_load(:autocomplete_roles, user_service.rolesThatCanOperateOnStage(@cruise_config, @pipeline).to_json)
    end

    def new_stage
      new_job = JobConfig.new(CaseInsensitiveString.new(""), ResourceConfigs.new, ArtifactTypeConfigs.new, com.thoughtworks.go.config.Tasks.new([AntTask.new].to_java(Task)))
      StageConfig.new(CaseInsensitiveString.new(""), JobConfigs.new([new_job].to_java(JobConfig)))
    end

    def change_index &action
      save_page(params[:config_md5], admin_stage_listing_path(), with_layout({:action => :index}, "details"), Class.new(::ConfigUpdate::SaveAsPipelineOrTemplateAdmin) do
        include ::ConfigUpdate::PipelineOrTemplateNode
        include ::ConfigUpdate::PipelineStageSubject
        include ::ConfigUpdate::RefsAsUpdatedRefs

        def initialize params, current_user, security_service, action
          super(params, current_user, security_service)
          @action = action
        end

        def update(pipeline)
          stage = subject(pipeline)
          @action.call(pipeline, stage)
        end
      end.new(params, current_user.getUsername(), security_service, action)) do
        load_for_listing
      end
    end

    def load_for_listing
      assert_load(:pipeline, @node)
      load_stage_usage
      load_pause_info
      load_template_list
    end

    def load_template_list
       assert_load(:template_list, template_config_service.getTemplateViewModels(current_user.getUsername()))
    end

    def with_layout options = {}, layout_name = "stage"
      options.merge(:layout => "#{params[:stage_parent]}/#{layout_name}")
    end
  end
end
