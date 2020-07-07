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

Rails.application.routes.draw do
  unless defined?(CONSTANTS)
    CONFIG_REPO_ID_FORMAT = ROLE_NAME_FORMAT = USER_NAME_FORMAT = GROUP_NAME_FORMAT = TEMPLATE_NAME_FORMAT = PIPELINE_NAME_FORMAT = STAGE_NAME_FORMAT = ENVIRONMENT_NAME_FORMAT = /[\w\-][\w\-.]*/
    JOB_NAME_FORMAT = /[\w\-.]+/
    PIPELINE_COUNTER_FORMAT = STAGE_COUNTER_FORMAT = /-?\d+/
    NON_NEGATIVE_INTEGER = /\d+/
    PIPELINE_LOCATOR_CONSTRAINTS = {:pipeline_name => PIPELINE_NAME_FORMAT, :pipeline_counter => PIPELINE_COUNTER_FORMAT}
    STAGE_LOCATOR_CONSTRAINTS = {:stage_name => STAGE_NAME_FORMAT, :stage_counter => STAGE_COUNTER_FORMAT}.merge(PIPELINE_LOCATOR_CONSTRAINTS)
    ENVIRONMENT_NAME_CONSTRAINT = {:name => ENVIRONMENT_NAME_FORMAT}
    MERGED_ENVIRONMENT_NAME_CONSTRAINT = {:environment_name => ENVIRONMENT_NAME_FORMAT}
    PLUGIN_ID_FORMAT = /[\w\-.]+/
    ALLOW_DOTS = /[^\/]+/
    CONSTANTS = true
  end

  # This is used to generate _url and _path in application_helper#url_for_path
  get '/', to: redirect('/go/pipelines'), as: :root

  get "admin/pipelines/snippet" => "admin/pipelines_snippet#index", as: :pipelines_snippet
  get "admin/pipelines/snippet/:group_name" => "admin/pipelines_snippet#show", constraints: {group_name: GROUP_NAME_FORMAT}, as: :pipelines_snippet_show
  get "admin/pipelines/snippet/:group_name/edit" => "admin/pipelines_snippet#edit", constraints: {group_name: GROUP_NAME_FORMAT}, as: :pipelines_snippet_edit
  put "admin/pipelines/snippet/:group_name" => "admin/pipelines_snippet#update", constraints: {group_name: GROUP_NAME_FORMAT}, as: :pipelines_snippet_update
  get "admin/config_change/between/:later_md5/and/:earlier_md5" => 'admin/stages#config_change', as: :admin_config_change

  get "admin/commands" => "admin/commands#index", as: :admin_commands
  get "admin/commands/show" => "admin/commands#show", as: :admin_command_definition
  get "admin/commands/lookup" => "admin/commands#lookup", :format => "text", as: :admin_command_lookup

  get "admin/config_xml" => "admin/configuration#show", as: :config_view
  put "admin/config_xml" => "admin/configuration#update", as: :config_update
  get "admin/config_xml/edit" => "admin/configuration#edit", as: :config_edit

  resources :analytics, only: [:index], controller: "analytics"
  get 'analytics/:plugin_id/:type/:id' => 'analytics#show', constraints: {plugin_id: PLUGIN_ID_FORMAT, id: PIPELINE_NAME_FORMAT}, as: :show_analytics

  scope 'pipelines' do
    defaults :no_layout => true do
      get ':pipeline_name/:pipeline_counter/build_cause' => 'pipelines#build_cause', constraints: PIPELINE_LOCATOR_CONSTRAINTS, as: :build_cause
    end

    match 'show', to: 'pipelines#show', via: %w(get post), as: :pipeline
    match 'select_pipelines', to: 'pipelines#select_pipelines', via: %w(get post), as: :pipeline_select_pipelines

    %w(index build_cause).each do |controller_action_method|
      get "#{controller_action_method}" => "pipelines##{controller_action_method}"
    end
  end

  get "pipelines(.:format)" => 'pipelines#index', defaults: {:format => "html"}, as: :pipeline_dashboard
  get 'home' => 'home#index'

  get "pipelines/value_stream_map/:pipeline_name/:pipeline_counter(.:format)" => "value_stream_map#show", constraints: {:pipeline_name => PIPELINE_NAME_FORMAT, :pipeline_counter => PIPELINE_COUNTER_FORMAT}, defaults: {:format => :html}, as: :vsm_show
  get "materials/value_stream_map/:material_fingerprint/:revision(.:format)" => "value_stream_map#show_material", defaults: {:format => :html}, constraints: {:revision => /[^\/]+(?=\.html\z|\.json\z)|[^\/]+/}, as: :vsm_show_material

  {'application/vnd.go.cd.v1+json' => :apiv1, 'application/vnd.go.cd+json' => :latest}.each do |header, as|
    scope :api, as: as, format: false do
      api_version(:module => 'ApiV1', header: {name: 'Accept', value: header}) do
        get 'version_infos/stale', controller: :version_infos, action: :stale, as: :stale_version_info
        get 'version_infos/latest_version', controller: :version_infos, action: :latest_version, as: :latest_version_info
        patch 'version_infos/go_server', controller: :version_infos, action: :update_server, as: :update_server_version_info

        match '*url', via: :all, to: 'errors#not_found'
      end
    end
  end

  namespace :admin do
    namespace :security do
      resources :auth_configs, only: [:index], controller: :auth_configs, as: :auth_configs
      resources :roles, only: [:index], controller: :roles, as: :roles
    end
  end

  namespace :api, as: "" do
    defaults :no_layout => true do

      defaults :format => 'xml' do
        #job api's
        get 'jobs/scheduled.xml' => 'jobs#scheduled'
      end
    end
  end

  post 'pipelines/:pipeline_name/:pipeline_counter/:stage_name/:stage_counter/rerun-jobs' => 'stages#rerun_jobs', as: :rerun_jobs, constraints: STAGE_LOCATOR_CONSTRAINTS
  get "pipelines/:pipeline_name/:pipeline_counter/:stage_name/:stage_counter" => "stages#overview", as: "stage_detail_tab_default", constraints: STAGE_LOCATOR_CONSTRAINTS

  %w(overview pipeline materials jobs stats stats_iframe stage_config).each do |controller_action_method|
    get "pipelines/:pipeline_name/:pipeline_counter/:stage_name/:stage_counter/#{controller_action_method}" => "stages##{controller_action_method}", as: "stage_detail_tab_#{controller_action_method}", constraints: STAGE_LOCATOR_CONSTRAINTS
  end

  get "history/stage/:pipeline_name/:pipeline_counter/:stage_name/:stage_counter" => 'stages#history', as: :stage_history, constraints: STAGE_LOCATOR_CONSTRAINTS

  scope 'internal' do
    # redirects to first-stage details page of the specific pipeline run
    get 'pipelines/:pipeline_name/:pipeline_counter' => 'stages#redirect_to_first_stage', as: :internal_stage_detail_tab, constraints: PIPELINE_LOCATOR_CONSTRAINTS
  end

  get 'preferences/notifications', controller: 'preferences', action: 'notifications'

  # bring back routes, these are referenced from rails pages...
  get "agents/:uuid" => 'agent_details#show', as: :agent_detail, constraints: {uuid: ALLOW_DOTS}
  get "agents/:uuid/job_run_history" => 'agent_details#job_run_history', as: :job_run_history_on_agent, constraints: {uuid: ALLOW_DOTS}

  get "errors/inactive" => 'go_errors#inactive'

end
