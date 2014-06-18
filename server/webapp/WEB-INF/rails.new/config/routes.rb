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
  unless defined?(CONSTANTS)
    USER_NAME_FORMAT = GROUP_NAME_FORMAT = TEMPLATE_NAME_FORMAT = PIPELINE_NAME_FORMAT = STAGE_NAME_FORMAT = /[\w\-][\w\-.]*/
    JOB_NAME_FORMAT = /[\w\-.]+/
    PIPELINE_COUNTER_FORMAT = STAGE_COUNTER_FORMAT = /-?\d+/
    NON_NEGATIVE_INTEGER = /\d+/
    PIPELINE_LOCATOR_CONSTRAINTS = {:pipeline_name => PIPELINE_NAME_FORMAT, :pipeline_counter => PIPELINE_COUNTER_FORMAT}
    STAGE_LOCATOR_CONSTRAINTS = {:stage_name => STAGE_NAME_FORMAT, :stage_counter => STAGE_COUNTER_FORMAT}.merge(PIPELINE_LOCATOR_CONSTRAINTS)
    CONSTANTS = true
  end

  root 'welcome#index' # put to get root_path. '/' is handled by java.

  get "admin/pipelines/snippet" => "admin/pipelines_snippet#index", as: :pipelines_snippet
  get "admin/pipelines/snippet/:group_name" => "admin/pipelines_snippet#show", constraints: {group_name: GROUP_NAME_FORMAT}, as: :pipelines_snippet_show
  get "admin/pipelines/snippet/:group_name/edit" => "admin/pipelines_snippet#edit", constraints: {group_name: GROUP_NAME_FORMAT}, as: :pipelines_snippet_edit
  put "admin/pipelines/snippet/:group_name" => "admin/pipelines_snippet#update", constraints: {group_name: GROUP_NAME_FORMAT}, as: :pipelines_snippet_update

  get 'admin/backup' => 'admin/backup#index', as: :backup_server
  post 'admin/backup' => 'admin/backup#perform_backup', as: :perform_backup
  delete 'admin/backup/delete_all' => 'admin/backup#delete_all', as: :delete_backup_history #NOT_IN_PRODUCTION don't remove this line, the build will remove this line when packaging the war

  get "admin/plugins" => "admin/plugins/plugins#index", as: :plugins_listing

  get "admin/:stage_parent/:pipeline_name/materials" => "admin/materials#index", constraints: {pipeline_name: PIPELINE_NAME_FORMAT}, :defaults => {:stage_parent => "pipelines"}, as: :admin_material_index
  delete "admin/:stage_parent/:pipeline_name/materials/:finger_print" => "admin/materials#destroy", as: :admin_material_delete

  get "admin/pipeline/new" => "admin/pipelines#new", as: :pipeline_new
  post "admin/pipelines" => "admin/pipelines#create", as: :pipeline_create
  get "admin/pipeline/:pipeline_name/clone" => "admin/pipelines#clone", constraints: {pipeline_name: PIPELINE_NAME_FORMAT}, as: :pipeline_clone
  post "admin/pipeline/save_clone" => "admin/pipelines#save_clone", as: :pipeline_save_clone
  get "admin/pipelines/:pipeline_name/pause_info.json" => "admin/pipelines#pause_info", :format => "json", constraints: {pipeline_name: PIPELINE_NAME_FORMAT}, as: :pause_info_refresh
  get "admin/pipelines/:pipeline_name/:current_tab" => "admin/pipelines#edit", constraints: {stage_parent: "pipelines", pipeline_name: PIPELINE_NAME_FORMAT, current_tab: /#{["general", "project_management", "environment_variables", "permissions", "parameters"].join("|")}/}, defaults: {stage_parent: "pipelines"}, as: :pipeline_edit
  put "admin/pipelines/:pipeline_name/:current_tab" => "admin/pipelines#update", constraints: {stage_parent: "pipelines", pipeline_name: PIPELINE_NAME_FORMAT, current_tab: /#{["general", "project_management", "environment_variables", "permissions", "parameters"].join("|")}/}, defaults: {stage_parent: "pipelines"}, as: :pipeline_update

  get "admin/:stage_parent/:pipeline_name/stages" => "admin/stages#index", constraints: {pipeline_name: PIPELINE_NAME_FORMAT, stage_parent: /(pipelines|templates)/}, as: :admin_stage_listing
  put "admin/:stage_parent/:pipeline_name/stages" => "admin/stages#use_template", constraints: {pipeline_name: PIPELINE_NAME_FORMAT, stage_parent: /(pipelines|templates)/}, :defaults => {:stage_parent => "pipelines"}, as: :admin_stage_use_template
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

  get "admin/:stage_parent/:pipeline_name/stages/:stage_name/job/:job_name/tasks" => "admin/tasks#index", constraints: {pipeline_name: PIPELINE_NAME_FORMAT, stage_name: STAGE_NAME_FORMAT, job_name: JOB_NAME_FORMAT}, :current_tab => "tasks", as: :admin_tasks_listing
  get "admin/:stage_parent/:pipeline_name/stages/:stage_name/job/:job_name/tasks/:type/new" => "admin/tasks#new", constraints: {pipeline_name: PIPELINE_NAME_FORMAT, stage_name: STAGE_NAME_FORMAT, job_name: JOB_NAME_FORMAT}, :current_tab => "tasks", as: :admin_task_new
  post "admin/:stage_parent/:pipeline_name/stages/:stage_name/job/:job_name/tasks/:type" => "admin/tasks#create", constraints: {pipeline_name: PIPELINE_NAME_FORMAT, stage_name: STAGE_NAME_FORMAT, job_name: JOB_NAME_FORMAT}, :current_tab => "tasks", as: :admin_task_create
  post "admin/:stage_parent/:pipeline_name/stages/:stage_name/job/:job_name/task/:task_index/index/increment" => "admin/tasks#increment_index", constraints: {pipeline_name: PIPELINE_NAME_FORMAT, stage_name: STAGE_NAME_FORMAT, job_name: JOB_NAME_FORMAT}, :current_tab => "tasks", as: :admin_task_increment_index
  post "admin/:stage_parent/:pipeline_name/stages/:stage_name/job/:job_name/task/:task_index/index/decrement" => "admin/tasks#decrement_index", constraints: {pipeline_name: PIPELINE_NAME_FORMAT, stage_name: STAGE_NAME_FORMAT, job_name: JOB_NAME_FORMAT}, :current_tab => "tasks", as: :admin_task_decrement_index
  get "admin/:stage_parent/:pipeline_name/stages/:stage_name/job/:job_name/tasks/:type/:task_index/edit" => "admin/tasks#edit", constraints: {pipeline_name: PIPELINE_NAME_FORMAT, stage_name: STAGE_NAME_FORMAT, job_name: JOB_NAME_FORMAT, task_index: NON_NEGATIVE_INTEGER}, :current_tab => "tasks", as: :admin_task_edit
  delete "admin/:stage_parent/:pipeline_name/stages/:stage_name/job/:job_name/tasks/:task_index" => "admin/tasks#destroy", constraints: {pipeline_name: PIPELINE_NAME_FORMAT, stage_name: STAGE_NAME_FORMAT, job_name: JOB_NAME_FORMAT, task_index: NON_NEGATIVE_INTEGER}, :current_tab => "tasks", as: :admin_task_delete
  put "admin/:stage_parent/:pipeline_name/stages/:stage_name/job/:job_name/tasks/:type/:task_index" => "admin/tasks#update", constraints: {pipeline_name: PIPELINE_NAME_FORMAT, stage_name: STAGE_NAME_FORMAT, job_name: JOB_NAME_FORMAT, task_index: NON_NEGATIVE_INTEGER}, :current_tab => "tasks", as: :admin_task_update

  get "admin/config_xml" => "admin/configuration#show", as: :config_view
  put "admin/config_xml" => "admin/configuration#update", as: :config_update
  get "admin/config_xml/edit" => "admin/configuration#edit", as: :config_edit

  get "admin/garage" => "admin/garage#index", as: :garage_index
  post "admin/garage/gc" => "admin/garage#gc", as: :garage_gc

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

  get "admin/package_definitions/:repo_id/new" => "admin/package_definitions#new", as: :package_definitions_new
  get "admin/package_definitions/:repo_id/new_for_new_pipeline_wizard" => "admin/package_definitions#new_for_new_pipeline_wizard", as: :package_definitions_new_for_new_pipeline_wizard
  get "admin/package_definitions/:repo_id/:package_id/pipelines_used_in" => "admin/package_definitions#pipelines_used_in", as: :pipelines_used_in
  get "admin/package_definitions/:repo_id/:package_id" => "admin/package_definitions#show", as: :package_definitions_show
  get "admin/package_definitions/:repo_id/:package_id/for_new_pipeline_wizard" => "admin/package_definitions#show_for_new_pipeline_wizard", as: :package_definitions_show_for_new_pipeline_wizard
  get "admin/package_definitions/:repo_id/:package_id/with_repository_list" => "admin/package_definitions#show_with_repository_list", as: :package_definitions_show_with_repository_list
  delete "admin/package_definitions/:repo_id/:package_id" => "admin/package_definitions#destroy", as: :package_definition_delete
  get "admin/package_definitions/check_connection" => "admin/package_definitions#check_connection", as: :package_definition_check_connection

  get "admin/package_repositories" => "admin/package_repositories#index", as: :package_repositories
  get "admin/package_repositories/new" => "admin/package_repositories#new", as: :package_repositories_new
  get "admin/package_repositories/check_connection" => "admin/package_repositories#check_connection", as: :package_repositories_check_connection
  get "admin/package_repositories/list" => "admin/package_repositories#list", as: :package_repositories_list
  get "admin/package_repositories/:id/edit" => "admin/package_repositories#edit", as: :package_repositories_edit
  post "admin/package_repositories" => "admin/package_repositories#create", as: :package_repositories_create
  put "admin/package_repositories/:id" => "admin/package_repositories#update", as: :package_repositories_update
  delete "admin/package_repositories/:id" => "admin/package_repositories#destroy", as: :package_repositories_delete
  get "admin/package_repositories/:plugin/config/" => "admin/package_repositories#plugin_config", as: :package_repositories_plugin_config
  get "admin/package_repositories/:id/:plugin/config/" => "admin/package_repositories#plugin_config_for_repo", as: :package_repositories_plugin_config_for_repo

  get 'agents/filter_autocomplete/:action' => 'agent_autocomplete#%{action}', constraints: {action: /resource|os|ip|name|status|environment/}, as: :agent_filter_autocomplete

  scope 'pipelines' do
    defaults :no_layout => true do
      post 'material_search' => 'pipelines#material_search'
      post 'show_for_trigger' => 'pipelines#show_for_trigger', as: :pipeline_show_with_option
      get ':pipeline_name/:pipeline_counter/build_cause' => 'pipelines#build_cause', constraints: PIPELINE_LOCATOR_CONSTRAINTS, as: :build_cause
    end

    get ':action' => 'pipelines#:action', constraints: {:action => /index|show|build_cause|select_pipelines/}
    get "" => 'pipelines#index', as: :pipeline_dashboard
    post ":action" => 'pipelines#:action', constraints: {:action => /select_pipelines/}, as: :pipeline
  end

  get 'home' => 'pipelines#index'
  get "pipelines/value_stream_map/:pipeline_name/:pipeline_counter(.:format)" => "value_stream_map#show", constraints: {:pipeline_name => PIPELINE_NAME_FORMAT, :pipeline_counter => PIPELINE_COUNTER_FORMAT}, defaults: {:format => :html}, as: :vsm_show

  defaults :no_layout => true do
    get 'materials/:id.xml' => 'application#unresolved', as: :material
    get 'materials/:materialId/changeset/:modificationId.xml' => 'application#unresolved', as: :modification
  end

  namespace :api, as: "" do
    defaults :no_layout => true do
      delete 'users/:username' => 'users#destroy', constraints: {username: USER_NAME_FORMAT}
      get 'plugins/status' => 'plugins#status'

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

      #agents api's
      get 'agents' => 'agents#index', format: 'json', as: :agents_information
      post 'agents/edit_agents' => 'agents#edit_agents', as: :api_disable_agent
      post 'agents/:uuid/:action' => 'agents#%{action}', constraints: {action: /enable|disable|delete/}, as: :agent_action

      defaults :format => 'text' do
        get 'support' => 'server#capture_support_info'
        get 'fanin_trace/:name' => 'fanin_trace#fanin_trace', constraints: {name: PIPELINE_NAME_FORMAT}
        get 'fanin/:name' => 'fanin_trace#fanin', constraints: {name: PIPELINE_NAME_FORMAT}
        get 'process_list' => 'process_list#process_list'
      end

      defaults :format => 'xml' do
        get 'users.xml' => 'users#index'
        get 'server.xml' => 'server#info'

        # stage api's
        get 'stages/:id.xml' => 'stages#index', as: :stage

        # pipeline api's
        get 'pipelines/:name/stages.xml' => 'pipelines#stage_feed', constraints: {name: PIPELINE_NAME_FORMAT}, as: :api_pipeline_stage_feed
        get 'pipelines/:name/:id.xml' => 'pipelines#pipeline_instance', constraints: {name: PIPELINE_NAME_FORMAT}, as: :api_pipeline_instance
        get 'card_activity/:pipeline_name/:from_pipeline_counter/to/:to_pipeline_counter' => 'pipelines#card_activity', constraints: {from_pipeline_counter: PIPELINE_COUNTER_FORMAT, to_pipeline_counter: PIPELINE_COUNTER_FORMAT, pipeline_name: PIPELINE_NAME_FORMAT}, as: :card_activity
        get 'pipelines.xml' => 'pipelines#pipelines', as: :api_pipelines

        #job api's
        get 'jobs/scheduled.xml' => 'jobs#scheduled'
        post 'jobs/:id.xml' => 'jobs#index'
      end
    end
  end

  # dummy mappings. for specs to pass
  get '/admin/templates' => 'test/test#index', as: :templates
  get '/server/messages.json' => 'test/test#index', as: :global_message
  get '/pipelines' => 'pipelines#index', as: :pipelines_for_test
  get '/agents' => 'agents#index', as: :agents_for_test
  get '/environments' => 'environments#index', as: :environments_for_test
  get "environments/new" => 'environments#new', as: :environment_new
  get '/compare/:pipeline_name/:from_counter/with/:to_counter' => 'test/test#index', constraints: {from_counter: PIPELINE_COUNTER_FORMAT, to_counter: PIPELINE_COUNTER_FORMAT, pipeline_name: PIPELINE_NAME_FORMAT}, as: :compare_pipelines
  get 'pipelines/:pipeline_name/:pipeline_counter/:stage_name/:stage_counter(/:action)' => 'test/test#%{action}', as: :stage_detail_tab, constraints: STAGE_LOCATOR_CONSTRAINTS, defaults: {action: 'overview'}
  get "pipelines/:pipeline_name/:pipeline_counter/:stage_name/:stage_counter(.:format)" => 'test/test#overview', as: :stage_detail, constraints: STAGE_LOCATOR_CONSTRAINTS

  get 'test' => 'test/test#index', as: :edit_server_config
  get 'test' => 'test/test#index', as: :gadgets_oauth_clients
  get 'test' => 'test/test#index', as: :user_listing
  get 'test' => 'test/test#index', as: :oauth_clients
  get 'test' => 'test/test#index', as: :dismiss_license_expiry_warning

  # catch all route
  match '*url', via: :all, to: 'application#unresolved'

# The priority is based upon order of creation: first created -> highest priority.
  # See how all your routes lay out with "rake routes".

  # You can have the root of your site routed with "root"
  # root 'welcome#index'

  # Example of regular route:
  #   get 'products/:id' => 'catalog#view'

  # Example of named route that can be invoked with purchase_url(id: product.id)
  #   get 'products/:id/purchase' => 'catalog#purchase', as: :purchase

  # Example resource route (maps HTTP verbs to controller actions automatically):
  #   resources :products

  # Example resource route with options:
  #   resources :products do
  #     member do
  #       get 'short'
  #       post 'toggle'
  #     end
  #
  #     collection do
  #       get 'sold'
  #     end
  #   end

  # Example resource route with sub-resources:
  #   resources :products do
  #     resources :comments, :sales
  #     resource :seller
  #   end

  # Example resource route with more complex sub-resources:
  #   resources :products do
  #     resources :comments
  #     resources :sales do
  #       get 'recent', on: :collection
  #     end
  #   end

  # Example resource route with concerns:
  #   concern :toggleable do
  #     post 'toggle'
  #   end
  #   resources :posts, concerns: :toggleable
  #   resources :photos, concerns: :toggleable

  # Example resource route within a namespace:
  #   namespace :admin do
  #     # Directs /admin/products/* to Admin::ProductsController
  #     # (app/controllers/admin/products_controller.rb)
  #     resources :products
  #   end
end
