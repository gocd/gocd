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
  class JobsController < AdminController
    include StageConfigLoader
    helper AdminHelper
    helper UiPatternHelper
    helper FlashMessagesHelper

    load_stage_except_for :create, :update, :destroy

    before_action :load_jobs, :only => [:index, :edit]

    layout "application", :except => [:new, :create, :update]

    def index
      render with_layout({}, "stage")
    end

    def new
      assert_load :task_view_models, task_view_service.getTaskViewModels()
      load_artifact_related_data
      load_resources_and_elastic_profile_ids_for_autocomplete
      load_store_ids_for_autocomplete
      assert_load :job, JobConfig.new(CaseInsensitiveString.new(""), ResourceConfigs.new, ArtifactTypeConfigs.new, com.thoughtworks.go.config.Tasks.new([AntTask.new].to_java(Task)))
      render layout: false
    end

    def edit
      load_artifact_related_data
      load_resources_and_elastic_profile_ids_for_autocomplete
      load_store_ids_for_autocomplete
      assert_load :job, @jobs.getJob(CaseInsensitiveString.new(params[:job_name]))
      render with_layout(:action => params[:current_tab]) unless @error_rendered
    end

    def create
      assert_load :job, JobConfig.new
      @job.setConfigAttributes(params[:job], task_view_service)
      save_popup(params[:config_md5], Class.new(::ConfigUpdate::SaveAsPipelineOrTemplateAdmin) do
        include ::ConfigUpdate::JobsNode
        include ::ConfigUpdate::RefsAsUpdatedRefs

        def initialize(params, user, security_service, job, pluggable_task_service, external_artifacts_service, go_config_service)
          super(params, user, security_service)
          @job = job
          @pluggable_task_service = pluggable_task_service
          @external_artifacts_service = external_artifacts_service
          @go_config_service = go_config_service
        end

        def subject(jobs)
          @job
        end

        def update(jobs)
          task = @job.getTasks().first()
          @pluggable_task_service.validate(task) if task.instance_of? com.thoughtworks.go.config.pluggabletask.PluggableTask
          @job.artifactConfigs().getPluggableArtifactConfigs().each do |external_artifact_config|
            @external_artifacts_service.validateExternalArtifactConfig(external_artifact_config, @go_config_service.artifactStores().find(external_artifact_config.getStoreId), false)
          end
          jobs.addJobWithoutValidityAssertion(@job)
        end
      end.new(params, current_user.getUsername(), security_service, @job, pluggable_task_service, external_artifacts_service, go_config_service), failure_handler({:action => :new, :layout => false}), {:current_tab => params[:current_tab]}) do
        assert_load :job, @subject
        assert_load :task_view_models, task_view_service.getTaskViewModelsWith(@job.tasks().first()) unless @update_result.isSuccessful()
        load_artifact_related_data
      end
    end

    def update
      save_popup(params[:config_md5], Class.new(::ConfigUpdate::SaveAsPipelineOrTemplateAdmin) do
        include ::ConfigUpdate::JobNode
        include ::ConfigUpdate::NodeAsSubject
        include ::ConfigUpdate::RefsAsUpdatedRefs

        def initialize params, user, security_service, external_artifacts_service, go_config_service
          super(params, user, security_service)
          @external_artifacts_service = external_artifacts_service
          @go_config_service = go_config_service
        end

        def updatedNode(cruise_config)
          stage = load_stage(cruise_config)
          load_job_from_stage_named(stage, CaseInsensitiveString.new(params[:job][:name] || params[:job_name]))
        end

        def update(job)
          job.setConfigAttributes(params[:job])
          job.artifactConfigs().getPluggableArtifactConfigs().each do |external_artifact_config|
            @external_artifacts_service.validateExternalArtifactConfig(external_artifact_config, @go_config_service.artifactStores().find(external_artifact_config.getStoreId), false)
          end
        end
      end.new(params, current_user.getUsername(), security_service, external_artifacts_service, go_config_service), failure_handler({:action => params[:current_tab], :layout => nil}), {:current_tab => params[:current_tab], :action => :edit, :job_name => params[:job][:name] || params[:job_name]}) do
        @should_not_render_layout = true
        load_pipeline_and_stage
        assert_load :job, @node
        load_artifact_related_data
      end
    end

    def destroy
      save_page(params[:config_md5], admin_job_listing_path, with_layout({:action => :index, :current_tab => "jobs"}, "stage"), Class.new(::ConfigUpdate::SaveAsPipelineOrTemplateAdmin) do
        include ::ConfigUpdate::JobsNode
        include ::ConfigUpdate::JobsJobSubject

        def update(jobs)
          job = jobs.getJob(job_name)
          jobs.remove(job)
        end
      end.new(params, current_user.getUsername(), security_service)) do
        load_pipeline_and_stage
        assert_load :jobs, @node
      end
    end

    private
    def load_artifact_related_data
      assert_load :plugin_name_to_id, default_plugin_info_finder.pluginDisplayNameToPluginId('artifact').to_hash
      assert_load :artifact_meta_data_store, artifact_meta_data_store
      assert_load :store_id_to_plugin_id, artifact_store_service.storeIdToPluginId().to_hash
      assert_load :artifact_plugin_to_view, default_plugin_info_finder.pluginIdToViewTemplate().to_hash
    end

    def artifact_meta_data_store
      ArtifactMetadataStore.instance()
    end

    def load_jobs
      assert_load :jobs, @stage.getJobs()
    end

    def load_pipeline_and_stage
      from_params = ::ConfigUpdate::LoadConfig.for(params)
      assert_load :pipeline, from_params.load_pipeline_or_template(@cruise_config)
      assert_load :stage, from_params.load_stage(@cruise_config)
      load_pause_info
    end

    def with_layout options = {}, layout_name = "job"
      options.merge(:layout => "#{params[:stage_parent]}/#{layout_name}")
    end

    def load_resources_and_elastic_profile_ids_for_autocomplete
      resources = @processed_cruise_config.getAllResources().map(&:getName) + agent_service.getListOfResourcesAcrossAgents()
      resources = resources.uniq
      assert_load :autocomplete_resources, resources.sort.to_json
      assert_load :elastic_profile_ids, elastic_profile_service.listAll().keys.sort.to_json
    end

    def load_store_ids_for_autocomplete
      assert_load :artifact_store_ids, artifact_store_service.listAll().keys.sort.to_json
    end

    def failure_handler(render_options)
      proc do |update_result, all_errors_on_other_objects|
        load_pipeline_and_stage
        assert_load :processed_cruise_config, @config_after
        load_resources_and_elastic_profile_ids_for_autocomplete
        load_store_ids_for_autocomplete
        render_error(update_result, all_errors_on_other_objects, render_options)
      end
    end

  end
end
