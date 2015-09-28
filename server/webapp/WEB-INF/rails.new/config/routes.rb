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

Go::Application.routes.draw do
  mount JasmineRails::Engine => '/jasmine-specs', as: :jasmine_old if defined?(JasmineRails)
  mount JasmineRails::Engine => '/jasmine-specs-new', as: :jasmine_new if defined?(JasmineRails)

  unless defined?(CONSTANTS)
    USER_NAME_FORMAT = GROUP_NAME_FORMAT = TEMPLATE_NAME_FORMAT = PIPELINE_NAME_FORMAT = STAGE_NAME_FORMAT = ENVIRONMENT_NAME_FORMAT = /[\w\-][\w\-.]*/
    JOB_NAME_FORMAT = /[\w\-.]+/
    PIPELINE_COUNTER_FORMAT = STAGE_COUNTER_FORMAT = /-?\d+/
    NON_NEGATIVE_INTEGER = /\d+/
    PIPELINE_LOCATOR_CONSTRAINTS = {:pipeline_name => PIPELINE_NAME_FORMAT, :pipeline_counter => PIPELINE_COUNTER_FORMAT}
    STAGE_LOCATOR_CONSTRAINTS = {:stage_name => STAGE_NAME_FORMAT, :stage_counter => STAGE_COUNTER_FORMAT}.merge(PIPELINE_LOCATOR_CONSTRAINTS)
    ENVIRONMENT_NAME_CONSTRAINT = {:name => ENVIRONMENT_NAME_FORMAT}
    ALLOW_DOTS = /[^\/]+/
    CONSTANTS = true
  end

  mount Oauth2Provider::Engine => '/oauth', :as => :oauth_engine

  root 'welcome#index' # put to get root_path. '/' is handled by java.

  get "about", controller: :about, action: :show, as: :about

  get "admin/pipelines/snippet" => "admin/pipelines_snippet#index", as: :pipelines_snippet
  get "admin/pipelines/snippet/:group_name" => "admin/pipelines_snippet#show", constraints: {group_name: GROUP_NAME_FORMAT}, as: :pipelines_snippet_show
  get "admin/pipelines/snippet/:group_name/edit" => "admin/pipelines_snippet#edit", constraints: {group_name: GROUP_NAME_FORMAT}, as: :pipelines_snippet_edit
  put "admin/pipelines/snippet/:group_name" => "admin/pipelines_snippet#update", constraints: {group_name: GROUP_NAME_FORMAT}, as: :pipelines_snippet_update

  get 'admin/backup' => 'admin/backup#index', as: :backup_server
  post 'admin/backup' => 'admin/backup#perform_backup', as: :perform_backup
  delete 'admin/backup/delete_all' => 'admin/backup#delete_all', as: :delete_backup_history #NOT_IN_PRODUCTION don't remove this line, the build will remove this line when packaging the war

  get "admin/plugins" => "admin/plugins/plugins#index", as: :plugins_listing
  post 'admin/plugins' => 'admin/plugins/plugins#upload', as: :upload_plugin
  get 'admin/plugins/settings/:plugin_id' => 'admin/plugins/plugins#edit_settings', constraints: {plugin_id: ALLOW_DOTS}, as: :edit_settings
  post 'admin/plugins/settings/:plugin_id' => 'admin/plugins/plugins#update_settings', constraints: {plugin_id: ALLOW_DOTS}, as: :update_settings

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

  get "admin/pipeline/new" => "admin/pipelines#new", as: :pipeline_new
  post "admin/pipelines" => "admin/pipelines#create", as: :pipeline_create
  get "admin/pipeline/:pipeline_name/clone" => "admin/pipelines#clone", constraints: {pipeline_name: PIPELINE_NAME_FORMAT}, as: :pipeline_clone
  post "admin/pipeline/save_clone" => "admin/pipelines#save_clone", as: :pipeline_save_clone
  get "admin/pipelines/:pipeline_name/pause_info.json" => "admin/pipelines#pause_info", :format => "json", constraints: {pipeline_name: PIPELINE_NAME_FORMAT}, as: :pause_info_refresh
  get "admin/:stage_parent/:pipeline_name/:current_tab" => "admin/pipelines#edit", constraints: {stage_parent: "pipelines", pipeline_name: PIPELINE_NAME_FORMAT, current_tab: /#{["general", "project_management", "environment_variables", "permissions", "parameters"].join("|")}/}, defaults: {stage_parent: "pipelines"}, as: :pipeline_edit
  put "admin/:stage_parent/:pipeline_name/:current_tab" => "admin/pipelines#update", constraints: {stage_parent: "pipelines", pipeline_name: PIPELINE_NAME_FORMAT, current_tab: /#{["general", "project_management", "environment_variables", "permissions", "parameters"].join("|")}/}, defaults: {stage_parent: "pipelines"}, as: :pipeline_update

  get "admin/:stage_parent/:pipeline_name/stages" => "admin/stages#index", constraints: {pipeline_name: PIPELINE_NAME_FORMAT, stage_parent: /(pipelines|templates)/}, as: :admin_stage_listing
  put "admin/:stage_parent/:pipeline_name/stages" => "admin/stages#use_template", constraints: {pipeline_name: PIPELINE_NAME_FORMAT, stage_parent: /(pipelines|templates)/}, as: :admin_stage_use_template
  get "admin/:stage_parent/:pipeline_name/stages/new" => "admin/stages#new", constraints: {pipeline_name: PIPELINE_NAME_FORMAT, stage_parent: /(pipelines|templates)/}, as: :admin_stage_new
  delete "admin/:stage_parent/:pipeline_name/stages/:stage_name" => "admin/stages#destroy", constraints: {pipeline_name: PIPELINE_NAME_FORMAT, stage_parent: /(pipelines|templates)/, stage_name: STAGE_NAME_FORMAT}, as: :admin_stage_delete
  post "admin/:stage_parent/:pipeline_name/stages" => "admin/stages#create", constraints: {pipeline_name: PIPELINE_NAME_FORMAT, stage_parent: /(pipelines|templates)/}, as: :admin_stage_create
  post "admin/:stage_parent/:pipeline_name/stages/:stage_name/index/increment" => "admin/stages#increment_index", constraints: {pipeline_name: PIPELINE_NAME_FORMAT, stage_parent: /(pipelines|templates)/, stage_name: STAGE_NAME_FORMAT}, as: :admin_stage_increment_index
  post "admin/:stage_parent/:pipeline_name/stages/:stage_name/index/decrement" => "admin/stages#decrement_index", constraints: {pipeline_name: PIPELINE_NAME_FORMAT, stage_parent: /(pipelines|templates)/, stage_name: STAGE_NAME_FORMAT}, as: :admin_stage_decrement_index
  get "admin/:stage_parent/:pipeline_name/stages/:stage_name/:current_tab" => "admin/stages#edit", constraints: {pipeline_name: PIPELINE_NAME_FORMAT, stage_parent: /(pipelines|templates)/, current_tab: /(settings|environment_variables|permissions)/, stage_name: STAGE_NAME_FORMAT }, as: :admin_stage_edit
  put "admin/:stage_parent/:pipeline_name/stages/:stage_name/:current_tab" => "admin/stages#update", constraints: {pipeline_name: PIPELINE_NAME_FORMAT, stage_parent: /(pipelines|templates)/, current_tab: /(settings|environment_variables|permissions)/, stage_name: STAGE_NAME_FORMAT }, as: :admin_stage_update

  get "admin/:stage_parent/:pipeline_name/stages/:stage_name/jobs" => "admin/jobs#index", constraints: {pipeline_name: PIPELINE_NAME_FORMAT, stage_name: STAGE_NAME_FORMAT, stage_parent: /(pipelines|templates)/}, as: :admin_job_listing
  get "admin/:stage_parent/:pipeline_name/stages/:stage_name/jobs/new" => "admin/jobs#new", constraints: {pipeline_name: PIPELINE_NAME_FORMAT, stage_name: STAGE_NAME_FORMAT, stage_parent: /(pipelines|templates)/}, as: :admin_job_new
  post "admin/:stage_parent/:pipeline_name/stages/:stage_name/jobs" => "admin/jobs#create", constraints: {pipeline_name: PIPELINE_NAME_FORMAT, stage_name: STAGE_NAME_FORMAT, stage_parent: /(pipelines|templates)/}, as: :admin_job_create
  put "admin/:stage_parent/:pipeline_name/stages/:stage_name/job/:job_name/:current_tab" => "admin/jobs#update", constraints: {pipeline_name: PIPELINE_NAME_FORMAT, stage_name: STAGE_NAME_FORMAT, stage_parent: /(pipelines|templates)/, job_name: JOB_NAME_FORMAT}, as: :admin_job_update #TODO: use job name format, so part splitting doesn't mess us up -JJ
  delete "admin/:stage_parent/:pipeline_name/stages/:stage_name/job/:job_name" => "admin/jobs#destroy", constraints: {pipeline_name: PIPELINE_NAME_FORMAT, stage_name: STAGE_NAME_FORMAT, stage_parent: /(pipelines|templates)/, job_name: JOB_NAME_FORMAT}, as: :admin_job_delete
  get "admin/:stage_parent/:pipeline_name/stages/:stage_name/job/:job_name/:current_tab" => "admin/jobs#edit", constraints: {pipeline_name: PIPELINE_NAME_FORMAT,  stage_name: STAGE_NAME_FORMAT, stage_parent: /(pipelines|templates)/, current_tab: /#{["settings", "environment_variables", "artifacts", "resources", "tabs"].join("|")}/, job_name: JOB_NAME_FORMAT}, as: :admin_job_edit
  #put "admin/:stage_parent/:pipeline_name/stages/:stage_name/job/:job_name/:current_tab" => "admin/jobs#update", constraints: {pipeline_name: PIPELINE_NAME_FORMAT,  stage_name: STAGE_NAME_FORMAT, stage_parent: /(pipelines|templates)/, current_tab: /#{["settings", "environment_variables", "artifacts", "resources", "tabs"].join("|")}/, job_name: JOB_NAME_FORMAT}, as: :admin_job_update

  get "admin/commands" => "admin/commands#index", as: :admin_commands
  get "admin/commands/show" => "admin/commands#show", as: :admin_command_definition
  get "admin/commands/lookup" => "admin/commands#lookup", :format => "text", as: :admin_command_lookup

  get "admin/:stage_parent/:pipeline_name/stages/:stage_name/job/:job_name/tasks" => "admin/tasks#index", constraints: {pipeline_name: PIPELINE_NAME_FORMAT, stage_name: STAGE_NAME_FORMAT, job_name: JOB_NAME_FORMAT, stage_parent: /(pipelines|templates)/}, :current_tab => "tasks", as: :admin_tasks_listing
  get "admin/:stage_parent/:pipeline_name/stages/:stage_name/job/:job_name/tasks/:type/new" => "admin/tasks#new", constraints: {pipeline_name: PIPELINE_NAME_FORMAT, stage_name: STAGE_NAME_FORMAT, job_name: JOB_NAME_FORMAT, stage_parent: /(pipelines|templates)/}, :current_tab => "tasks", as: :admin_task_new
  post "admin/:stage_parent/:pipeline_name/stages/:stage_name/job/:job_name/tasks/:type" => "admin/tasks#create", constraints: {pipeline_name: PIPELINE_NAME_FORMAT, stage_name: STAGE_NAME_FORMAT, job_name: JOB_NAME_FORMAT, stage_parent: /(pipelines|templates)/}, :current_tab => "tasks", as: :admin_task_create
  post "admin/:stage_parent/:pipeline_name/stages/:stage_name/job/:job_name/task/:task_index/index/increment" => "admin/tasks#increment_index", constraints: {pipeline_name: PIPELINE_NAME_FORMAT, stage_name: STAGE_NAME_FORMAT, job_name: JOB_NAME_FORMAT, stage_parent: /(pipelines|templates)/}, :current_tab => "tasks", as: :admin_task_increment_index
  post "admin/:stage_parent/:pipeline_name/stages/:stage_name/job/:job_name/task/:task_index/index/decrement" => "admin/tasks#decrement_index", constraints: {pipeline_name: PIPELINE_NAME_FORMAT, stage_name: STAGE_NAME_FORMAT, job_name: JOB_NAME_FORMAT, stage_parent: /(pipelines|templates)/}, :current_tab => "tasks", as: :admin_task_decrement_index
  get "admin/:stage_parent/:pipeline_name/stages/:stage_name/job/:job_name/tasks/:type/:task_index/edit" => "admin/tasks#edit", constraints: {pipeline_name: PIPELINE_NAME_FORMAT, stage_name: STAGE_NAME_FORMAT, job_name: JOB_NAME_FORMAT, task_index: NON_NEGATIVE_INTEGER, stage_parent: /(pipelines|templates)/}, :current_tab => "tasks", as: :admin_task_edit
  delete "admin/:stage_parent/:pipeline_name/stages/:stage_name/job/:job_name/tasks/:task_index" => "admin/tasks#destroy", constraints: {pipeline_name: PIPELINE_NAME_FORMAT, stage_name: STAGE_NAME_FORMAT, job_name: JOB_NAME_FORMAT, task_index: NON_NEGATIVE_INTEGER, stage_parent: /(pipelines|templates)/}, :current_tab => "tasks", as: :admin_task_delete
  put "admin/:stage_parent/:pipeline_name/stages/:stage_name/job/:job_name/tasks/:type/:task_index" => "admin/tasks#update", constraints: {pipeline_name: PIPELINE_NAME_FORMAT, stage_name: STAGE_NAME_FORMAT, job_name: JOB_NAME_FORMAT, task_index: NON_NEGATIVE_INTEGER, stage_parent: /(pipelines|templates)/}, :current_tab => "tasks", as: :admin_task_update

  get "admin/config_xml" => "admin/configuration#show", as: :config_view
  put "admin/config_xml" => "admin/configuration#update", as: :config_update
  get "admin/config_xml/edit" => "admin/configuration#edit", as: :config_edit

  get "admin/garage" => "admin/garage#index", as: :garage_index
  post "admin/garage/gc" => "admin/garage#gc", as: :garage_gc

  get "admin/config/server" => "admin/server#index", as: :edit_server_config
  post "admin/config/server/update" => "admin/server#update", as: :update_server_config
  get "admin/config/server/validate" => "admin/server#validate", as: :validate_server_config_params
  post "admin/config/server/test_email" => "admin/server#test_email", as: :send_test_email
  post "admin/config/server/validate_ldap" => "admin/server#validate_ldap", as: :validate_ldap_settings

  get "admin/pipelines" => "admin/pipeline_groups#index", as: :pipeline_groups
  get "admin/pipeline_group/new" => "admin/pipeline_groups#new", as: :pipeline_group_new
  post "admin/pipeline_group" => "admin/pipeline_groups#create", as: :pipeline_group_create
  put "admin/pipelines/move/:pipeline_name" => "admin/pipeline_groups#move", constraints: {pipeline_name: PIPELINE_NAME_FORMAT}, as: :move_pipeline_to_group
  delete "admin/pipelines/:pipeline_name" => "admin/pipeline_groups#destroy", constraints: {pipeline_name: PIPELINE_NAME_FORMAT}, as: :delete_pipeline
  get "admin/pipeline_group/:group_name/edit" => "admin/pipeline_groups#edit", constraints: {group_name: GROUP_NAME_FORMAT}, as: :pipeline_group_edit
  get "admin/pipeline_group/:group_name" => "admin/pipeline_groups#show", constraints: {group_name: GROUP_NAME_FORMAT}, as: :pipeline_group_show
  put "admin/pipeline_group/:group_name" => "admin/pipeline_groups#update", constraints: {group_name: GROUP_NAME_FORMAT}, as: :pipeline_group_update
  delete "admin/pipeline_group/:group_name" => "admin/pipeline_groups#destroy_group", constraints: {group_name: GROUP_NAME_FORMAT}, as: :pipeline_group_delete
  get "/admin/pipelines/possible_groups/:pipeline_name/:config_md5" => "admin/pipeline_groups#possible_groups", constraints: {pipeline_name: PIPELINE_NAME_FORMAT}, as: :possible_groups

  get "admin/templates" => "admin/templates#index", as: :templates
  get "admin/templates/new" => "admin/templates#new", as: :template_new
  post "admin/templates/create" => "admin/templates#create", as: :template_create
  delete "admin/templates/:pipeline_name" => "admin/templates#destroy", constraints: {pipeline_name: PIPELINE_NAME_FORMAT}, as: :delete_template
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
  get "admin/package_definitions/check_connection" => "admin/package_definitions#check_connection", as: :package_definition_check_connection

  get "admin/package_repositories/new" => "admin/package_repositories#new", as: :package_repositories_new
  get "admin/package_repositories/check_connection" => "admin/package_repositories#check_connection", as: :package_repositories_check_connection
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

  get 'agents/filter_autocomplete/:action' => 'agent_autocomplete#%{action}', constraints: {action: /resource|os|ip|name|status|environment/}, as: :agent_filter_autocomplete

  scope 'pipelines' do
    defaults :no_layout => true do
      get 'material_search' => 'pipelines#material_search'
      post 'show_for_trigger' => 'pipelines#show_for_trigger', as: :pipeline_show_with_option
      get ':pipeline_name/:pipeline_counter/build_cause' => 'pipelines#build_cause', constraints: PIPELINE_LOCATOR_CONSTRAINTS, as: :build_cause
    end

    get ':action' => 'pipelines#:action', constraints: {:action => /index|show|build_cause|select_pipelines/}
    post ':action' => 'pipelines#:action', constraints: {:action => /select_pipelines|show/}, as: :pipeline
  end

  get "pipelines(.:format)" => 'pipelines#index', defaults: {:format => "html"}, as: :pipeline_dashboard
  get 'home' => 'home#index'

  get "pipelines/value_stream_map/:pipeline_name/:pipeline_counter(.:format)" => "value_stream_map#show", constraints: {:pipeline_name => PIPELINE_NAME_FORMAT, :pipeline_counter => PIPELINE_COUNTER_FORMAT}, defaults: {:format => :html}, as: :vsm_show
  get "materials/value_stream_map/:material_fingerprint/:revision(.:format)" => "value_stream_map#show_material", defaults: {:format => :html}, constraints: {:revision => /[^\/]+(?=\.html\z|\.json\z)|[^\/]+/}, as: :vsm_show_material

  scope 'compare' do
    get ':pipeline_name/:from_counter/with/:to_counter' => 'comparison#show', constraints: {from_counter: PIPELINE_COUNTER_FORMAT, to_counter: PIPELINE_COUNTER_FORMAT, pipeline_name: PIPELINE_NAME_FORMAT}, as: :compare_pipelines
    get ':pipeline_name/page/:page' => 'comparison#page', constraints: {pipeline_name: PIPELINE_NAME_FORMAT}, as: :compare_pipelines_page
    get ':pipeline_name/list/compare_with/:other_pipeline_counter' => 'comparison#list', constraints: {pipeline_name: PIPELINE_NAME_FORMAT}, format: 'json', as: :compare_pipelines_list
    get ':pipeline_name/timeline/:page' => 'comparison#timeline', constraints: {pipeline_name: PIPELINE_NAME_FORMAT}, as: :compare_pipelines_timeline
  end

  get 'failures/:pipeline_name/:pipeline_counter/:stage_name/:stage_counter/:job_name/:suite_name/:test_name' => 'failures#show', constraints: STAGE_LOCATOR_CONSTRAINTS, :no_layout => true, as: :failure_details_internal

  get 'server/messages.json' => 'server#messages', :format => "json", as: :global_message

  scope 'config_view' do
    get "templates/:name" => "config_view/templates#show", as: :config_view_templates_show, constraints: {name: TEMPLATE_NAME_FORMAT}
  end

  defaults :no_layout => true do
    get 'materials/:id.xml' => 'application#unresolved', as: :material
    get 'materials/:materialId/changeset/:modificationId.xml' => 'application#unresolved', as: :modification
  end

  scope 'environments' do
    defaults :no_layout => true do
      post "create" => 'environments#create', as: :environment_create
      get "new" => 'environments#new', as: :environment_new
      put ":name" => 'environments#update', constraints: ENVIRONMENT_NAME_CONSTRAINT, as: :environment_update

      [:pipelines, :agents, :variables].each do |action|
        get ":name/edit/#{action}" => "environments#edit_#{action}", constraints: ENVIRONMENT_NAME_CONSTRAINT, as: "environment_edit_#{action}"
      end
    end
    get ":name/show" => 'environments#show', constraints: ENVIRONMENT_NAME_CONSTRAINT, as: :environment_show
  end
  match "environments(.:format)" => 'environments#index', defaults: {:format => :html}, via: [:post, :get], as: :environments

  scope :api, as: :apiv1 do
    api_version(:module => 'ApiV1', header: {name: 'Accept', value: 'application/vnd.go.cd.v1+json'}) do
      resources :backups, only: [:create]

      resources :users, param: :login_name, only: [:index, :show, :destroy], constraints: { login_name: USER_NAME_FORMAT } do
        patch :update, on: :member
      end

      resources :agents, param: :uuid, except: [:new, :create, :edit, :update] do
        patch :update, on: :member
      end

      get 'stages/:pipeline_name/:pipeline_counter/:stage_name/:stage_counter' => 'stages#show', constraints: {pipeline_name: PIPELINE_NAME_FORMAT, pipeline_counter: PIPELINE_COUNTER_FORMAT, stage_name: STAGE_NAME_FORMAT, stage_counter: STAGE_COUNTER_FORMAT}, as: :stage_instance_by_counter_api
      get 'stages/:pipeline_name/:stage_name' => 'stages#history', constraints: {pipeline_name: PIPELINE_NAME_FORMAT, stage_name: STAGE_NAME_FORMAT}, as: :stage_history_api

      get 'dashboard', controller: :dashboard, action: :dashboard, as: :show_dashboard

      match '*url', via: :all, to: 'errors#not_found'
    end
  end

  scope :api, as: :apiv2 do
    api_version(:module => 'ApiV2', header: {name: 'Accept', value: 'application/vnd.go.cd.v2+json'}) do
      resources :agents, param: :uuid, except: [:new, :create, :edit, :update] do
        patch :update, on: :member
      end

      match '*url', via: :all, to: 'errors#not_found'
    end
  end


  namespace :admin do
    resources :pipelines, only: [:edit], controller: :pipeline_configs, param: :pipeline_name, as: :pipeline_config
  end

  namespace :api, as: "" do
    defaults :no_layout => true do
      get 'plugins/status' => 'plugins#status'

      # state
      get 'state/status' => 'server_state#status'
      post 'state/active' => 'server_state#to_active'
      post 'state/passive' => 'server_state#to_passive'

      # history
      get 'pipelines/:pipeline_name/history/(:offset)' => 'pipelines#history', constraints: {pipeline_name: PIPELINE_NAME_FORMAT}, defaults: {:offset => '0'}, as: :pipeline_history
      get 'stages/:pipeline_name/:stage_name/history/(:offset)' => 'stages#history', constraints: {pipeline_name: PIPELINE_NAME_FORMAT, stage_name: STAGE_NAME_FORMAT}, defaults: {:offset => '0'}, as: :stage_history_api
      get 'jobs/:pipeline_name/:stage_name/:job_name/history/(:offset)' => 'jobs#history', constraints: {pipeline_name: PIPELINE_NAME_FORMAT, stage_name: STAGE_NAME_FORMAT, job_name: JOB_NAME_FORMAT}, defaults: {:offset => '0'}, as: :job_history_api
      get "agents/:uuid/job_run_history/(:offset)" => 'agents#job_run_history', defaults: {:offset => '0'}, as: :agent_job_run_history_api
      get "materials/:fingerprint/modifications/(:offset)" => 'materials#modifications', defaults: {:offset => '0'}, as: :material_modifications_api

      # instance
      get 'pipelines/:pipeline_name/instance/:pipeline_counter' => 'pipelines#instance_by_counter', constraints: {pipeline_name: PIPELINE_NAME_FORMAT, pipeline_counter: PIPELINE_COUNTER_FORMAT}, as: :pipeline_instance_by_counter_api
      get 'stages/:pipeline_name/:stage_name/instance/:pipeline_counter/:stage_counter' => 'stages#instance_by_counter', constraints: {pipeline_name: PIPELINE_NAME_FORMAT, pipeline_counter: PIPELINE_COUNTER_FORMAT, stage_name: STAGE_NAME_FORMAT, stage_counter: STAGE_COUNTER_FORMAT}, as: :stage_instance_by_counter_api

      # status
      get 'pipelines/:pipeline_name/status' => 'pipelines#status', constraints: {pipeline_name: PIPELINE_NAME_FORMAT}, as: :pipeline_status_api

      # config
      get 'config/pipeline_groups' => 'pipeline_groups#list_configs', as: :pipeline_group_config_list_api
      get 'config/materials' => 'materials#list_configs', as: :material_config_list_api
      get 'config/revisions/(:offset)' => 'configuration#config_revisions', defaults: {:offset => '0'}, as: :config_revisions_list_api
      get 'config/diff/:from_revision/:to_revision' => 'configuration#config_diff', as: :config_diff_api

      # stage api's
      post 'stages/:id/cancel' => 'stages#cancel', as: :cancel_stage
      post 'stages/:pipeline_name/:stage_name/cancel' => 'stages#cancel_stage_using_pipeline_stage_name', as: :cancel_stage_using_pipeline_stage_name

      # pipeline api's
      post 'pipelines/:pipeline_name/:action' => 'pipelines#%{action}', constraints: {pipeline_name: PIPELINE_NAME_FORMAT}, as: :api_pipeline_action
      post 'pipelines/:pipeline_name/pause' => 'pipelines#pause', constraints: {pipeline_name: PIPELINE_NAME_FORMAT}, as: :pause_pipeline
      post 'pipelines/:pipeline_name/unpause' => 'pipelines#unpause', constraints: {pipeline_name: PIPELINE_NAME_FORMAT}, as: :unpause_pipeline

      post 'material/notify/:post_commit_hook_material_type' => 'materials#notify', as: :material_notify

      post 'admin/command-repo-cache/reload' => 'commands#reload_cache', as: :admin_command_cache_reload

      post 'admin/start_backup' => 'admin#start_backup', as: :backup_api_url

      scope 'admin/feature_toggles' do
        defaults :no_layout => true, :format => :json do
          get "" => "feature_toggles#index", as: :api_admin_feature_toggles
          post "/:toggle_key" => "feature_toggles#update", constraints: {toggle_key: /[^\/]+/}, as: :api_admin_feature_toggle_update
        end
      end

      #agents api's
      get 'agents' => 'agents#index', format: 'json', as: :agents_information
      post 'agents/edit_agents' => 'agents#edit_agents', as: :api_disable_agent
      post 'agents/:uuid/:action' => 'agents#%{action}', constraints: {action: /enable|disable|delete/}, as: :agent_action

      defaults :format => 'text' do
        get 'support' => 'server#capture_support_info'
        get 'fanin_trace/:name' => 'fanin_trace#fanin_trace', constraints: {name: PIPELINE_NAME_FORMAT}
        get 'fanin_debug/:name/(:index)' => 'fanin_trace#fanin_debug', constraints: {name: PIPELINE_NAME_FORMAT}, defaults: {:offset => '0'}
        get 'fanin/:name' => 'fanin_trace#fanin', constraints: {name: PIPELINE_NAME_FORMAT}
      end

      defaults :format => 'json' do
        get 'process_list' => 'process_list#process_list'
      end

      defaults :format => 'xml' do
        get 'users.xml' => 'users#index'
        get 'server.xml' => 'server#info', as: :server

        # stage api's
        get 'stages/:id.xml' => 'stages#index', as: :stage

        # pipeline api's
        get 'pipelines/:name/stages.xml' => 'pipelines#stage_feed', constraints: {name: PIPELINE_NAME_FORMAT}, as: :api_pipeline_stage_feed
        get 'pipelines/:name/:id.xml' => 'pipelines#pipeline_instance', constraints: {name: PIPELINE_NAME_FORMAT}, as: :api_pipeline_instance
        get 'card_activity/:pipeline_name/:from_pipeline_counter/to/:to_pipeline_counter' => 'pipelines#card_activity', constraints: {from_pipeline_counter: PIPELINE_COUNTER_FORMAT, to_pipeline_counter: PIPELINE_COUNTER_FORMAT, pipeline_name: PIPELINE_NAME_FORMAT}, as: :card_activity
        get 'pipelines.xml' => 'pipelines#pipelines', as: :api_pipelines

        #job api's
        get 'jobs/scheduled.xml' => 'jobs#scheduled'
        get 'jobs/:id.xml' => 'jobs#index'
      end
    end
  end

  namespace :api do
    scope :config do
      namespace :internal do
        post 'pluggable_task/:plugin_id' => 'pluggable_task#validate', as: :pluggable_task_validation, constraints: { plugin_id: /[\w+\.\-]+/ }
      end
    end
  end

  post 'pipelines/:pipeline_name/:pipeline_counter/:stage_name/:stage_counter/rerun-jobs' => 'stages#rerun_jobs', as: :rerun_jobs, constraints: STAGE_LOCATOR_CONSTRAINTS
  post 'pipelines/:pipeline_name/:pipeline_counter/comment' => 'pipelines#update_comment', as: :update_comment, constraints: PIPELINE_LOCATOR_CONSTRAINTS, format: :json
  get 'pipelines/:pipeline_name/:pipeline_counter/:stage_name/:stage_counter/(:action)' => 'stages#overview', as: :stage_detail_tab, constraints: STAGE_LOCATOR_CONSTRAINTS
  get "history/stage/:pipeline_name/:pipeline_counter/:stage_name/:stage_counter" => 'stages#history', as: :stage_history, constraints: STAGE_LOCATOR_CONSTRAINTS
  get "config_change/between/:later_md5/and/:earlier_md5" => 'stages#config_change', as: :config_change

  get "/run/:pipeline_name/:pipeline_counter/:stage_name", :controller => "null", :action => "null", as: :run_stage, constraints: {:pipeline_name => PIPELINE_NAME_FORMAT, :pipeline_counter => PIPELINE_COUNTER_FORMAT, :stage_name => STAGE_NAME_FORMAT}

  resources :agents, :only =>  [:index], :defaults => {:format => "html"}
  post "agents/edit_agents", :controller => 'agents', :action => :edit_agents, as: :edit_agents
  post "agents/:action" , :controller => 'agents', constraints: {action: /(resource|environment)_selector/}, as: :agent_grouping_data

  scope 'admin/users', module:'admin' do
    defaults :no_layout => true do
      get 'new' => 'users#new', as: :users_new
      post 'create' =>   'users#create', as: :users_create
      post 'search' => 'users#search', as: :users_search
      post 'roles' => 'users#roles', as: :user_roles
      delete 'delete_all' => 'users#delete_all', as: :users_delete  #NOT_IN_PRODUCTION don't remove this line, the build will remove this line when packaging the war
    end
    post 'operate'  => 'users#operate', as: :user_operate
    get ''=>'users#users', as: :user_listing
  end

  get "agents/:uuid" => 'agent_details#show', as: :agent_detail
  get "agents/:uuid/job_run_history" => 'agent_details#job_run_history', as: :job_run_history_on_agent

  get "cas_errors/user_disabled" => 'cas_errors#user_disabled', as: :user_disabled_cas_error
  get "cas_errors/user_unknown" => 'cas_errors#user_unknown', as: :user_unknown_cas_error
  get "errors/inactive" => 'go_errors#inactive'

  get "gadgets/pipeline.xml" => "gadgets/pipeline#index", :format => 'xml', as: :pipeline_status_gadget
  get "gadgets/pipeline/content" => "gadgets/pipeline#content", :no_layout => true, as: :pipeline_status_gadget_content

  get "cctray.xml" => "cctray#index", :format => "xml", as: :cctray

  # dummy mappings. for specs to pass
  get 'test' => 'test/test#index', as: :oauth_clients
end
