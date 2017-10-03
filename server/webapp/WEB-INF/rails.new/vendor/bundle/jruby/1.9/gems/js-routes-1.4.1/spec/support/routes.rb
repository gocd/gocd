def draw_routes

  BlogEngine::Engine.routes.draw do
    root to: "application#index"
    resources :posts
  end
  App.routes.draw do

    get 'support(/page/:page)', to: BlogEngine::Engine, as: 'support'

    resources :inboxes do
      resources :messages do
        resources :attachments
      end
    end

    root :to => "inboxes#index"

    namespace :admin do
      resources :users
    end

    scope "/returns/:return" do
      resources :objects
    end
    resources :returns

    scope "(/optional/:optional_id)" do
      resources :things
    end

    get "(/sep1/:first_optional)/sep2/:second_required/sep3/:third_required(/:forth_optional)",
      as: :thing_deep, controller: :things, action: :show

    if Rails.version < "5.0.0"
      get "/:controller(/:action(/:id))" => "classic#classic", :as => :classic
    end

    get "/other_optional/(:optional_id)" => "foo#foo", :as => :foo
    get '/other_optional(/*optional_id)' => 'foo#foo', :as => :foo_all

    get 'books/*section/:title' => 'books#show', :as => :book
    get 'books/:title/*section' => 'books#show', :as => :book_title

    mount BlogEngine::Engine => "/blog", :as => :blog_app

    get '/no_format' => "foo#foo", :format => false, :as => :no_format

    get '/json_only' => "foo#foo", :format => true, :constraints => {:format => /json/}, :as => :json_only

    get '/привет' => "foo#foo", :as => :hello
    get '(/o/:organization)/search/:q' => "foo#foo", as: :search

    resources :sessions, :only => [:new, :create, :destroy], :protocol => 'https'

    get '/' => 'sso#login', host: 'sso.example.com', as: :sso

    resources :portals, :port => 8080

    get '/with_defaults' => 'foo#foo', defaults: { bar: 'tested', format: :json }, format: :true

    namespace :api, format: true, defaults: {format: 'json'} do
      get "/purchases" => "purchases#index"
    end

    resources :budgies do
      get "descendents"
    end

    namespace :backend, path: '', constraints: {subdomain: 'backend'} do
      root to: 'backend#index'
    end

  end

end
