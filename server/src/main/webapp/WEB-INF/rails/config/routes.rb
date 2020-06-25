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

  ["svn", "git", "hg", "p4", "dependency", "tfs", "package"].each do |material_type|
    get "admin/pipelines/:pipeline_name/materials/#{material_type}/new" => "admin/materials/#{material_type}#new", constraints: {pipeline_name: PIPELINE_NAME_FORMAT}, as: "admin_#{material_type}_new"
    post "admin/pipelines/:pipeline_name/materials/#{material_type}" => "admin/materials/#{material_type}#create", constraints: {pipeline_name: PIPELINE_NAME_FORMAT}, as: "admin_#{material_type}_create"
    get "admin/pipelines/:pipeline_name/materials/#{material_type}/:finger_print/edit" => "admin/materials/#{material_type}#edit", constraints: {pipeline_name: PIPELINE_NAME_FORMAT}, as: "admin_#{material_type}_edit"
    put "admin/pipelines/:pipeline_name/materials/#{material_type}/:finger_print" => "admin/materials/#{material_type}#update", constraints: {pipeline_name: PIPELINE_NAME_FORMAT}, as: "admin_#{material_type}_update"
  end
  defaults :no_layout => true do
    get "admin/pipelines/:pipeline_name/materials/dependency/pipeline_name_search" => "admin/materials/dependency#pipeline_name_search", constraints: {pipeline_name: PIPELINE_NAME_FORMAT}, as: :admin_dependency_material_pipeline_name_search
    get "admin/pipelines/:pipeline_name/materials/dependency/load_stage_names_for" => "admin/materials/dependency#load_stage_names_for", constraints: {pipeline_name: PIPELINE_NAME_FORMAT}, as: :admin_dependency_material_load_stage_names_for
  end
  get "admin/:stage_parent/:pipeline_name/materials" => "admin/materials#index", constraints: {stage_parent: "pipelines", pipeline_name: PIPELINE_NAME_FORMAT}, defaults: {stage_parent: "pipelines"}, as: :admin_material_index
  delete "admin/:stage_parent/:pipeline_name/materials/:finger_print" => "admin/materials#destroy", constraints: {stage_parent: "pipelines", pipeline_name: PIPELINE_NAME_FORMAT}, defaults: {stage_parent: "pipelines"}, as: :admin_material_delete

  get "admin/commands" => "admin/commands#index", as: :admin_commands
  get "admin/commands/show" => "admin/commands#show", as: :admin_command_definition
  get "admin/commands/lookup" => "admin/commands#lookup", :format => "text", as: :admin_command_lookup

  get "admin/config_xml" => "admin/configuration#show", as: :config_view
  put "admin/config_xml" => "admin/configuration#update", as: :config_update
  get "admin/config_xml/edit" => "admin/configuration#edit", as: :config_edit

  get "admin/templates/:template_name/permissions" => "admin/templates#edit_permissions", constraints: {template_name: TEMPLATE_NAME_FORMAT}, as: :edit_template_permissions
  post "admin/templates/:template_name/permissions" => "admin/templates#update_permissions", constraints: {template_name: TEMPLATE_NAME_FORMAT}, as: :update_template_permissions
  get "admin/:stage_parent/:pipeline_name/:current_tab" => "admin/templates#edit", constraints: {stage_parent: "templates", pipeline_name: PIPELINE_NAME_FORMAT, current_tab: /#{["general"].join("|")}/}, defaults: {stage_parent: "templates"}, as: :template_edit
  put "admin/:stage_parent/:pipeline_name/:current_tab" => "admin/templates#update", constraints: {stage_parent: "templates", pipeline_name: PIPELINE_NAME_FORMAT, current_tab: /#{["general"].join("|")}/}, defaults: {stage_parent: "templates"}, as: :template_update

  get "admin/package_definitions/:repo_id/new" => "admin/package_definitions#new", as: :package_definitions_new
  get "admin/package_definitions/:repo_id/new_for_new_pipeline_wizard" => "admin/package_definitions#new_for_new_pipeline_wizard", as: :package_definitions_new_for_new_pipeline_wizard
  get "admin/package_definitions/:repo_id/:package_id/pipelines_used_in" => "admin/package_definitions#pipelines_used_in", as: :pipelines_used_in
  get "admin/package_definitions/:repo_id/:package_id" => "admin/package_definitions#show", as: :package_definitions_show
  get "admin/package_definitions/:repo_id/:package_id/for_new_pipeline_wizard" => "admin/package_definitions#show_for_new_pipeline_wizard", as: :package_definitions_show_for_new_pipeline_wizard
  get "admin/package_definitions/:repo_id/:package_id/with_repository_list" => "admin/package_definitions#show_with_repository_list", as: :package_definitions_show_with_repository_list
  delete "admin/package_definitions/:repo_id/:package_id" => "admin/package_definitions#destroy", as: :package_definition_delete
  post "admin/package_definitions/check_connection" => "admin/package_definitions#check_connection", as: :package_definition_check_connection, constraints: HeaderConstraint.new

  get "admin/package_repositories/new" => "admin/package_repositories#new", as: :package_repositories_new
  post "admin/package_repositories/check_connection" => "admin/package_repositories#check_connection", as: :package_repositories_check_connection, constraints: HeaderConstraint.new
  get "admin/package_repositories/list" => "admin/package_repositories#list", as: :package_repositories_list
  get "admin/package_repositories/:id/edit" => "admin/package_repositories#edit", as: :package_repositories_edit
  post "admin/package_repositories" => "admin/package_repositories#create", as: :package_repositories_create
  put "admin/package_repositories/:id" => "admin/package_repositories#update", as: :package_repositories_update
  delete "admin/package_repositories/:id" => "admin/package_repositories#destroy", as: :package_repositories_delete
  get "admin/package_repositories/:plugin/config/" => "admin/package_repositories#plugin_config", constraints: {:plugin => ALLOW_DOTS}, as: :package_repositories_plugin_config
  get "admin/package_repositories/:id/:plugin/config/" => "admin/package_repositories#plugin_config_for_repo", constraints: {:plugin => ALLOW_DOTS}, as: :package_repositories_plugin_config_for_repo

  get "admin/pipelines/:pipeline_name/materials/pluggable_scm/show_existing" => "admin/materials/pluggable_scm#show_existing", constraints: {pipeline_name: PIPELINE_NAME_FORMAT}, as: :admin_pluggable_scm_show_existing
  post "admin/pipelines/:pipeline_name/materials/pluggable_scm/choose_existing" => "admin/materials/pluggable_scm#choose_existing", constraints: {pipeline_name: PIPELINE_NAME_FORMAT}, as: :admin_pluggable_scm_choose_existing
  get "admin/pipelines/:pipeline_name/materials/pluggable_scm/new/:plugin_id" => "admin/materials/pluggable_scm#new", constraints: {pipeline_name: PIPELINE_NAME_FORMAT, plugin_id: ALLOW_DOTS}, as: :admin_pluggable_scm_new
  post "admin/pipelines/:pipeline_name/materials/pluggable_scm/:plugin_id" => "admin/materials/pluggable_scm#create", constraints: {pipeline_name: PIPELINE_NAME_FORMAT, plugin_id: ALLOW_DOTS}, as: :admin_pluggable_scm_create
  get "admin/pipelines/:pipeline_name/materials/pluggable_scm/:finger_print/edit" => "admin/materials/pluggable_scm#edit", constraints: {pipeline_name: PIPELINE_NAME_FORMAT}, as: :admin_pluggable_scm_edit
  put "admin/pipelines/:pipeline_name/materials/pluggable_scm/:finger_print" => "admin/materials/pluggable_scm#update", constraints: {pipeline_name: PIPELINE_NAME_FORMAT}, as: :admin_pluggable_scm_update
  post "admin/materials/pluggable_scm/check_connection/:plugin_id" => "admin/materials/pluggable_scm#check_connection", constraints: {plugin_id: ALLOW_DOTS}, as: :admin_pluggable_scm_check_connection
  get "admin/materials/pluggable_scm/:scm_id/pipelines_used_in" => "admin/materials/pluggable_scm#pipelines_used_in", as: :scm_pipelines_used_in

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

  get 'compare/:pipeline_name/:from_counter/with/:to_counter' => 'comparison#show', constraints: {from_counter: PIPELINE_COUNTER_FORMAT, to_counter: PIPELINE_COUNTER_FORMAT, pipeline_name: PIPELINE_NAME_FORMAT}, as: :compare_pipelines
  scope 'compare' do
    get ':pipeline_name/list/compare_with/:other_pipeline_counter' => 'comparison#list', constraints: {pipeline_name: PIPELINE_NAME_FORMAT}, format: 'json', as: :compare_pipelines_list
    get ':pipeline_name/timeline/:page' => 'comparison#timeline', constraints: {pipeline_name: PIPELINE_NAME_FORMAT}, as: :compare_pipelines_timeline
  end

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
