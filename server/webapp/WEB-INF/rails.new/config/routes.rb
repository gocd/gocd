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
    USER_NAME_FORMAT = PIPELINE_NAME_FORMAT = STAGE_NAME_FORMAT = /[\w\-][\w\-.]*/
    PIPELINE_COUNTER_FORMAT = STAGE_COUNTER_FORMAT = /-?\d+/
    PIPELINE_LOCATOR_CONSTRAINTS = {:pipeline_name => PIPELINE_NAME_FORMAT, :pipeline_counter => PIPELINE_COUNTER_FORMAT}
    STAGE_LOCATOR_CONSTRAINTS = {:stage_name => STAGE_NAME_FORMAT, :stage_counter => STAGE_COUNTER_FORMAT}.merge(PIPELINE_LOCATOR_CONSTRAINTS)
    CONSTANTS = true
  end

  root 'welcome#index' # put to get root_path. '/' is handled by java.

  get 'admin/backup' => 'admin/backup#index', as: :backup_server
  post 'admin/backup' => 'admin/backup#perform_backup', as: :perform_backup
  delete 'admin/backup/delete_all' => 'admin/backup#delete_all', as: :delete_backup_history #NOT_IN_PRODUCTION don't remove this line, the build will remove this line when packaging the war

  get 'agents/filter_autocomplete/:action' => 'agent_autocomplete#%{action}', constraints: {action: /resource|os|ip|name|status|environment/}, as: :agent_filter_autocomplete

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
  get '/admin/pipelines' => 'test/test#index', as: :pipeline_groups
  get '/admin/templates' => 'test/test#index', as: :templates
  get '/server/messages.json' => 'test/test#index', as: :global_message
  get '/pipelines' => 'pipelines#index', as: :pipelines_for_test
  get '/agents' => 'agents#index', as: :agents_for_test
  get '/environments' => 'environments#index', as: :environments_for_test
  get '/compare/:pipeline_name/:from_counter/with/:to_counter' => 'test/test#index', constraints: {from_counter: PIPELINE_COUNTER_FORMAT, to_counter: PIPELINE_COUNTER_FORMAT, pipeline_name: PIPELINE_NAME_FORMAT}, as: :compare_pipelines
  get 'pipelines/:pipeline_name/:pipeline_counter/:stage_name/:stage_counter(/:action)' => 'test/test#%{action}', as: :stage_detail_tab, constraints: STAGE_LOCATOR_CONSTRAINTS, defaults: {action: 'overview'}
  get "pipelines/:pipeline_name/:pipeline_counter/:stage_name/:stage_counter.:format" => 'test/test#overview', as: :stage_detail, constraints: STAGE_LOCATOR_CONSTRAINTS, defaults: {format: 'html'}

  get 'test' => 'test/test#index', as: :plugins_listing
  get 'test' => 'test/test#index', as: :config_view
  get 'test' => 'test/test#index', as: :edit_server_config
  get 'test' => 'test/test#index', as: :gadgets_oauth_clients
  get 'test' => 'test/test#index', as: :package_repositories_new
  get 'test' => 'test/test#index', as: :user_listing
  get 'test' => 'test/test#index', as: :oauth_clients
  get 'test' => 'test/test#index', as: :package_repositories_list
  get 'test' => 'test/test#index', as: :dismiss_license_expiry_warning

  defaults :no_layout => true do
    post 'pipelines/material_search' => 'pipelines#material_search'
    post 'pipelines/show_for_trigger' => 'pipelines#show_for_trigger', as: :pipeline_show_with_option
    get 'pipelines/:pipeline_name/:pipeline_counter/build_cause' => 'pipelines#build_cause', constraints: PIPELINE_LOCATOR_CONSTRAINTS, as: :build_cause
  end
  get 'pipelines/:action' => 'pipelines#:action', constraints: {:action => /index|show|build_cause|select_pipelines/}
  get "pipelines" => 'pipelines#index', as: :pipeline_dashboard
  get 'home' => 'pipelines#index'

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
