Go::Application.routes.draw do
  unless defined?(CONSTANTS)
    USER_NAME_FORMAT = /[\w\-][\w\-.]*/
  end

  root 'welcome#index' # put to get root_path. '/' is handled by java.

  get 'admin/backup' => 'admin/backup#index', as: :backup_server
  post 'admin/backup' => 'admin/backup#perform_backup', as: :perform_backup
  delete 'admin/backup/delete_all' => 'admin/backup#delete_all', as: :delete_backup_history #NOT_IN_PRODUCTION don't remove this line, the build will remove this line when packaging the war

  namespace :api do
    defaults :no_layout => true do
      delete 'users/:username' => 'users#destroy', constraints: {username: USER_NAME_FORMAT}
      get 'support' => 'server#capture_support_info', :format => 'text'

      defaults :format => 'xml' do
        get 'users.xml' => 'users#index'
        get 'server.xml' => 'server#info'
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
  get 'test' => 'test/test#index', as: :plugins_listing
  get 'test' => 'test/test#index', as: :config_view
  get 'test' => 'test/test#index', as: :edit_server_config
  get 'test' => 'test/test#index', as: :gadgets_oauth_clients
  get 'test' => 'test/test#index', as: :package_repositories_new
  get 'test' => 'test/test#index', as: :user_listing
  get 'test' => 'test/test#index', as: :oauth_clients
  get 'test' => 'test/test#index', as: :package_repositories_list
  get 'test' => 'test/test#index', as: :dismiss_license_expiry_warning

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
