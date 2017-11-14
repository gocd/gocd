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
  class TasksController < AdminController
    include JobConfigLoader
    include TaskHelper
    load_job_except_for :destroy, :increment_index, :decrement_index

    before_action :load_task, :only => [:edit]
    before_action :load_autocomplete_hash_for_fetch_task, :only => [:new, :edit]

    def index
      assert_load :tasks, @job.getTasks()
      assert_load :task_view_models, task_view_service.getTaskViewModels()
      render with_layout
    end

    def edit
      @task_view_model = task_view_service.getViewModel(@task, 'edit')
      assert_load :on_cancel_task_vms, task_view_service.getOnCancelTaskViewModels(@task)
      assert_load :config_store, config_store
      render :template => "/admin/tasks/plugin/edit", :layout => false
    end

    def new
      type = params[:type]
      assert_load :task, task_view_service.taskInstanceFor(type)
      assert_load :task_view_model, task_view_service.getViewModel(@task, 'new')
      assert_load :on_cancel_task_vms, task_view_service.getOnCancelTaskViewModels(@task)
      assert_load :config_store, config_store
      render "/admin/tasks/plugin/new", :layout => false
    end

    def create
      type = params[:type]
      assert_load :task, task_view_service.taskInstanceFor(type)
      @task.setConfigAttributes(params[:task], task_view_service)
      create_failure_handler = proc do |result, all_errors|
        @errors = flatten_all_errors(all_errors)
        @task_view_model = task_view_service.getViewModel(@task, 'new')
        @on_cancel_task_vms = task_view_service.getOnCancelTaskViewModels(@task)
        @config_store = config_store
        render :template => "/admin/tasks/plugin/new", :status => result.httpCode(), :layout => false
      end

      save_popup(params[:config_md5], Class.new(::ConfigUpdate::SaveAsPipelineOrTemplateAdmin) do
        include ::ConfigUpdate::JobNode

        def initialize params, user, security_service, task, pluggable_task_service
          super(params, user, security_service)
          @task = task
          @pluggable_task_service = pluggable_task_service
        end

        def subject(job)
          @task
        end

        def update(job)
          job.addTask(@task)
          @pluggable_task_service.validate(@task) if @task.instance_of? com.thoughtworks.go.config.pluggabletask.PluggableTask
          @pluggable_task_service.validate(@task.cancelTask()) if (!@task.cancelTask().nil?) && (@task.cancelTask().instance_of? com.thoughtworks.go.config.pluggabletask.PluggableTask)
        end
      end.new(params, current_user.getUsername(), security_service, @task, pluggable_task_service), create_failure_handler, {:controller => '/admin/tasks', :current_tab => params[:current_tab]}) do
        assert_load :job, @node
        assert_load :task, @subject
        load_modify_task_variables
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
      update_failure_handler = proc do |result, all_errors|
        @errors = flatten_all_errors(all_errors)
        @task_view_model = task_view_service.getViewModel(@task, 'edit')
        @on_cancel_task_vms = task_view_service.getOnCancelTaskViewModels(@task)
        @config_store = config_store
        render :template => "/admin/tasks/plugin/edit", :status => result.httpCode(), :layout => false
      end
      save_popup(params[:config_md5], Class.new(::ConfigUpdate::SaveAsPipelineOrTemplateAdmin) do
        include ::ConfigUpdate::TaskNode
        include ::ConfigUpdate::NodeAsSubject

        def initialize(params, user, security_service, task_view_service, pluggable_task_service)
          super(params, user, security_service)
          @task_view_service = task_view_service
          @pluggable_task_service = pluggable_task_service
        end

        def update(task)
          task.setConfigAttributes(params[:task], @task_view_service)
          @pluggable_task_service.validate(task) if task.instance_of? com.thoughtworks.go.config.pluggabletask.PluggableTask
          @pluggable_task_service.validate(task.cancelTask()) if (!task.cancelTask().nil?) && (task.cancelTask().instance_of? com.thoughtworks.go.config.pluggabletask.PluggableTask)
        end

      end.new(params, current_user.getUsername(), security_service, task_view_service, pluggable_task_service), update_failure_handler, {:controller => '/admin/tasks', :current_tab => params[:current_tab]}) do
        assert_load :task, @node
        load_modify_task_variables
      end
    end

    def increment_index
      change_index { |tasks, task_idx| tasks.incrementIndex(task_idx) }
    end

    def decrement_index
      change_index { |tasks, task_idx| tasks.decrementIndex(task_idx) }
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
        stage_graph.each do |stage_name, job_names|
          jobs = job_names.map {|name| {:job => name.to_s}}
          jobs.sort! {|one, other| one[:job] <=> other[:job]}
          pipeline_stage_array.push({:stage => stage_name.to_s, :jobs => jobs})
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
      assert_load :tasks, @job.getTasks()
      task_idx = params[:task_index].to_i
      (@tasks.size() > task_idx) ? assert_load(:task, @tasks.get(task_idx)) : render_assertion_failure({:message => l.string("TASK_NOT_FOUND")})
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