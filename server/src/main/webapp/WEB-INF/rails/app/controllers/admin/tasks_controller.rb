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
  class TasksController < FastAdminController
    include JobConfigLoader
    include TaskHelper
    CLONER = GoConfigCloner.new
    load_job_except_for :destroy, :increment_index, :decrement_index

    before_action :load_task, :only => [:edit]
    before_action :load_autocomplete_hash_for_fetch_task, :only => [:new, :edit]

    def index
      assert_load :tasks, @job.getTasks()
      assert_load :task_view_models, task_view_service.getTaskViewModels()
      render with_layout
    end

    def edit
      assert_load :artifact_plugin_to_fetch_view, default_plugin_info_finder.pluginIdToFetchViewTemplate()
      @task_view_model = task_view_service.getViewModel(@task, 'edit')
      assert_load :on_cancel_task_vms, task_view_service.getOnCancelTaskViewModels(@task)
      assert_load :config_store, config_store
      render :template => "/admin/tasks/plugin/edit", :layout => false
    end

    def new
      type = params[:type]
      assert_load :artifact_plugin_to_fetch_view, default_plugin_info_finder.pluginIdToFetchViewTemplate()
      assert_load :task, task_view_service.taskInstanceFor(type)
      assert_load :task_view_model, task_view_service.getViewModel(@task, 'new')
      assert_load :on_cancel_task_vms, task_view_service.getOnCancelTaskViewModels(@task)
      assert_load :config_store, config_store
      render "/admin/tasks/plugin/new", :layout => false
    end

    def create
      if params[:stage_parent] == 'templates' or Toggles.isToggleOff(Toggles.FAST_PIPELINE_SAVE)
        old_create
        return
      end

      pipeline_name = params[:pipeline_name]
      original_pipeline_config = pipeline_config_service.getPipelineConfig(pipeline_name)
      @pipeline = CLONER.deep_clone(original_pipeline_config)
      @stage = @pipeline.getStage(params[:stage_name])
      @job = @stage.jobConfigByConfigName(params[:job_name])

      type = params[:type]
      assert_load :task, task_view_service.taskInstanceFor(type)
      @task.setSelectedTaskType(params[:task][:selectedTaskType]) if 'fetch'.eql?(type)
      @task.setConfigAttributes(params[:task], task_view_service)
      @job.addTask(@task.is_a?(com.thoughtworks.go.config.FetchTaskAdapter) ? @task.getAppropriateTask : @task)

      failure_handler = action_failure_handler(@task, 'new')
      fast_save_popup(failure_handler, {:controller => '/admin/tasks', :current_tab => params[:current_tab]}) do
        assert_load :pipeline_md5, params[:pipeline_md5]
        assert_load :pipeline_group_name, params[:pipeline_group_name]
        assert_load :pipeline_name, params[:pipeline_name]
        assert_load :artifact_plugin_to_fetch_view, default_plugin_info_finder.pluginIdToFetchViewTemplate()
        @config_store = config_store
        if is_fetch_task? params[:type]
          pipeline_name = CaseInsensitiveString.new(params[:pipeline_name])
          stage_name = CaseInsensitiveString.new(params[:stage_name])
          looking_at_template = false
          map = com.thoughtworks.go.server.presentation.FetchArtifactViewHelper.new(system_environment, @cruise_config, pipeline_name, stage_name, looking_at_template).autosuggestMap()
          assert_load :pipeline_json, mk_as_json(map)
        end
      end
    end

    def old_create
      type = params[:type]
      assert_load :task, task_view_service.taskInstanceFor(type)
      @task.setSelectedTaskType(params[:task][:selectedTaskType]) if 'fetch'.eql?(type)
      @task.setConfigAttributes(params[:task], task_view_service)
      stage_parent_config = stage_parent_config(params[:stage_parent], params[:pipeline_name])
      action = TaskSaveOrUpdateAction.new('new', params, current_user.getUsername(), security_service, @task, pluggable_task_service, external_artifacts_service, stage_parent_config, go_config_service)

      failure_handler = action_failure_handler(@task, 'new')
      save_popup(params[:config_md5], action, failure_handler, {:controller => '/admin/tasks', :current_tab => params[:current_tab]}) do
        assert_load :job, @node
        assert_load :task, @subject
        assert_load :artifact_plugin_to_fetch_view, default_plugin_info_finder.pluginIdToFetchViewTemplate()
        load_modify_task_variables
      end
    end

    def action_failure_handler(task, action)
      failure_handler = proc do |result, all_errors|
        @errors = flatten_all_errors(all_errors)
        @task_view_model = task_view_service.getViewModel(task, action)
        @on_cancel_task_vms = task_view_service.getOnCancelTaskViewModels(task)
        @config_store = config_store
        render :template => "/admin/tasks/plugin/#{action}", :status => result.httpCode(), :layout => false
      end
      failure_handler
    end

    class TaskSaveOrUpdateAction < ::ConfigUpdate::SaveAsPipelineOrTemplateAdmin
      include ::ConfigUpdate::JobNode
      include ::ConfigUpdate::JobTaskSubject

      def initialize action, params, user, security_service, task, pluggable_task_service, external_artifacts_service, stage_parent_config, go_config_service
        super(params, user, security_service)
        @action = action
        @task = get_task(task)
        @pluggable_task_service = pluggable_task_service
        @external_artifacts_service = external_artifacts_service
        @go_config_service = go_config_service
        @stage_parent_config = stage_parent_config
      end

      def subject(job)
        if @task.is_a?(com.thoughtworks.go.config.AbstractFetchTask)
          return com.thoughtworks.go.config.FetchTaskAdapter.new(@task)
        end
        @task
      end

      def update(job)
        job.addTask(@task) if @action == 'new'
        job.getTasks().replace(task_index, @task) if @action == 'edit'

        @pluggable_task_service.validate(@task) if @task.instance_of? com.thoughtworks.go.config.pluggabletask.PluggableTask
        @pluggable_task_service.validate(@task.cancelTask()) if (!@task.cancelTask().nil?) && (@task.cancelTask().instance_of? com.thoughtworks.go.config.pluggabletask.PluggableTask)
        if @task.instance_of?(com.thoughtworks.go.config.FetchPluggableArtifactTask)
          if @stage_parent_config
            # need not call validate on oncancel task as it can't be fetch task from the UI.
            # The task or the config is not preprocessed. So, it may be that for some cases, the plugin validations are deferred to runtime
            @external_artifacts_service.validateFetchExternalArtifactTask(@task, @stage_parent_config, @go_config_service.getCurrentConfig())
          end
        end
      end


      private

      def get_task(task)
        return task.getAppropriateTask if task.is_a?(com.thoughtworks.go.config.FetchTaskAdapter)
        task
      end
    end

    def stage_parent_config(stage_parent, pipeline_or_template_name)
      if 'pipelines'.eql?(stage_parent)
        go_config_service.pipelineConfigNamed(CaseInsensitiveString.new(pipeline_or_template_name))
      else
        go_config_service.templateConfigNamed(CaseInsensitiveString.new(pipeline_or_template_name))
      end
    end

    def destroy
      save_page(params[:config_md5], admin_tasks_listing_path(:stage_parent => params[:stage_parent]), with_layout(:action => :index), Class.new(::ConfigUpdate::SaveAsPipelineOrTemplateAdmin) do
        include ::ConfigUpdate::JobNode
        include ::ConfigUpdate::JobTaskSubject

        def update(job)
          tasks = job.getTasks()
          tasks.remove(task_index)
        end

      end.new(params, current_user.getUsername(), security_service)) do
        load_tasks
        @task = @subject
      end
    end

    def update
      if params[:stage_parent] == 'templates' or Toggles.isToggleOff(Toggles.FAST_PIPELINE_SAVE)
        old_update
        return
      end

      pipeline_name = params[:pipeline_name]
      original_pipeline_config = pipeline_config_service.getPipelineConfig(pipeline_name)
      @pipeline = CLONER.deep_clone(original_pipeline_config)
      @stage = @pipeline.getStage(params[:stage_name])
      @job = @stage.jobConfigByConfigName(params[:job_name])

      type = params[:type]
      assert_load :task, task_view_service.taskInstanceFor(type)
      @task.setSelectedTaskType(params[:task][:selectedTaskType]) if 'fetch'.eql?(type)
      @task.setConfigAttributes(params[:task], task_view_service)

      @job.getTasks().set(params[:task_index].to_i, @task.is_a?(com.thoughtworks.go.config.FetchTaskAdapter) ? @task.getAppropriateTask : @task)

      failure_handler = action_failure_handler(@task, 'edit')
      fast_save_popup(failure_handler, {:controller => '/admin/tasks', :current_tab => params[:current_tab]}) do
        assert_load :pipeline_md5, params[:pipeline_md5]
        assert_load :pipeline_group_name, params[:pipeline_group_name]
        assert_load :pipeline_name, params[:pipeline_name]
        assert_load :artifact_plugin_to_fetch_view, default_plugin_info_finder.pluginIdToFetchViewTemplate()
        @config_store = config_store
        if is_fetch_task? params[:type]
          pipeline_name = CaseInsensitiveString.new(params[:pipeline_name])
          stage_name = CaseInsensitiveString.new(params[:stage_name])
          looking_at_template = false
          map = com.thoughtworks.go.server.presentation.FetchArtifactViewHelper.new(system_environment, @cruise_config, pipeline_name, stage_name, looking_at_template).autosuggestMap()
          assert_load :pipeline_json, mk_as_json(map)
        end
      end
    end

    def old_update
      type = params[:type]
      assert_load :task, task_view_service.taskInstanceFor(type)
      @task.setSelectedTaskType(params[:task][:selectedTaskType]) if 'fetch'.eql?(type)
      @task.setConfigAttributes(params[:task], task_view_service)

      stage_parent_config = stage_parent_config(params[:stage_parent], params[:pipeline_name])
      update_action = TaskSaveOrUpdateAction.new('edit', params, current_user.getUsername(), security_service, @task, pluggable_task_service, external_artifacts_service, stage_parent_config, go_config_service)

      save_popup(params[:config_md5], update_action, action_failure_handler(@task, 'edit'), {:controller => '/admin/tasks', :current_tab => params[:current_tab]}) do
        assert_load :task, @subject
        load_modify_task_variables
        assert_load :artifact_plugin_to_fetch_view, default_plugin_info_finder.pluginIdToFetchViewTemplate
      end
    end

    def increment_index
      change_index {|tasks, task_idx| tasks.incrementIndex(task_idx)}
    end

    def decrement_index
      change_index {|tasks, task_idx| tasks.decrementIndex(task_idx)}
    end

    private

    def config_store
      com.thoughtworks.go.plugin.access.pluggabletask.PluggableTaskConfigStore.store()
    end

    def load_modify_task_variables
      assert_load :pipeline, ::ConfigUpdate::LoadConfig.for(params).load_pipeline_or_template(@cruise_config)
      load_autocomplete_options_from_config(@config_after)
    end

    def load_autocomplete_options_from_config cruise_config
      if is_fetch_task? params[:type]
        pipeline_name = @pipeline.name()
        stage_name = @stage.name()
        looking_at_template = ::ConfigUpdate::LoadConfig.for(params).looking_at_template?
        map = com.thoughtworks.go.server.presentation.FetchArtifactViewHelper.new(system_environment, cruise_config, pipeline_name, stage_name, looking_at_template).autosuggestMap()
        assert_load :pipeline_json, mk_as_json(map)
      end
    end

    def mk_as_json graph
      pipeline_array = []
      graph.each do |pipeline_name, stage_graph|
        pipeline_stage_array = []
        stage_graph.each do |stage_name, job_graph|
          job_artifact_array = []
          job_graph.each do |job_name, artifacts_map|
            artifact_plugin_map = {}
            artifacts_map.each do |artifact_id, plugin_id|
              artifact_plugin_map[artifact_id] = plugin_id
            end
            job_artifact_array.push({:job => job_name.to_s, :artifacts => artifact_plugin_map})
          end
          job_artifact_array.sort! {|one, other| one[:job] <=> other[:job]}
          pipeline_stage_array.push({:stage => stage_name.to_s, :jobs => job_artifact_array})
        end
        pipeline_stage_array.sort! {|one, other| one[:stage] <=> other[:stage]}
        pipeline_array.push({:pipeline => pipeline_name.to_s, :stages => pipeline_stage_array})
      end
      pipeline_array.sort! {|one, other| pipeline_ordering(one, other)}
      pipeline_array.to_json
    end

    def pipeline_ordering one, other
      depth_difference = (one[:pipeline].count("/") - other[:pipeline].count("/"))
      depth_difference == 0 ? (one[:pipeline] <=> other[:pipeline]) : depth_difference
    end

    def load_autocomplete_hash_for_fetch_task
      load_autocomplete_options_from_config(@processed_cruise_config)
    end

    def load_task
      assert_load :tasks, @job.getTasksForView()
      task_idx = params[:task_index].to_i
      (@tasks.size() > task_idx) ? assert_load(:task, @tasks.get(task_idx)) : render_assertion_failure({:message => 'Task not found.'})
    end

    def change_index &action
      save_page(params[:config_md5], admin_tasks_listing_path, with_layout(:action => :index), Class.new(::ConfigUpdate::SaveAsPipelineOrTemplateAdmin) do
        include ::ConfigUpdate::JobNode
        include ::ConfigUpdate::JobTaskSubject

        def initialize(params, user, security_service, action)
          super(params, user, security_service)
          @action = action
        end

        def update(job)
          tasks = job.getTasks()
          @action.call(tasks, task_index)
        end
      end.new(params, current_user.getUsername(), security_service, action)) do
        load_tasks
      end
    end

    def load_tasks
      loader = ::ConfigUpdate::LoadConfig.for(params)
      assert_load :pipeline, loader.load_pipeline_or_template(@cruise_config)
      assert_load :stage, loader.load_stage(@cruise_config)
      assert_load :job, @node
      assert_load :tasks, @job.getTasks()
      load_pause_info unless loader.looking_at_template?
    end

    def with_layout options = {}
      options.merge(:layout => "#{params[:stage_parent]}/job")
    end
  end
end
