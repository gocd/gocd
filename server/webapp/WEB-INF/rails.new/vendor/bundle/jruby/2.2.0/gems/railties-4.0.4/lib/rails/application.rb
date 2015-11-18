require 'fileutils'
require 'active_support/core_ext/object/blank'
require 'active_support/key_generator'
require 'rails/engine'

module Rails
  # In Rails 3.0, a Rails::Application object was introduced which is nothing more than
  # an Engine but with the responsibility of coordinating the whole boot process.
  #
  # == Initialization
  #
  # Rails::Application is responsible for executing all railties and engines
  # initializers. It also executes some bootstrap initializers (check
  # Rails::Application::Bootstrap) and finishing initializers, after all the others
  # are executed (check Rails::Application::Finisher).
  #
  # == Configuration
  #
  # Besides providing the same configuration as Rails::Engine and Rails::Railtie,
  # the application object has several specific configurations, for example
  # "cache_classes", "consider_all_requests_local", "filter_parameters",
  # "logger" and so forth.
  #
  # Check Rails::Application::Configuration to see them all.
  #
  # == Routes
  #
  # The application object is also responsible for holding the routes and reloading routes
  # whenever the files change in development.
  #
  # == Middlewares
  #
  # The Application is also responsible for building the middleware stack.
  #
  # == Booting process
  #
  # The application is also responsible for setting up and executing the booting
  # process. From the moment you require "config/application.rb" in your app,
  # the booting process goes like this:
  #
  #   1)  require "config/boot.rb" to setup load paths
  #   2)  require railties and engines
  #   3)  Define Rails.application as "class MyApp::Application < Rails::Application"
  #   4)  Run config.before_configuration callbacks
  #   5)  Load config/environments/ENV.rb
  #   6)  Run config.before_initialize callbacks
  #   7)  Run Railtie#initializer defined by railties, engines and application.
  #       One by one, each engine sets up its load paths, routes and runs its config/initializers/* files.
  #   8)  Custom Railtie#initializers added by railties, engines and applications are executed
  #   9)  Build the middleware stack and run to_prepare callbacks
  #   10) Run config.before_eager_load and eager_load! if eager_load is true
  #   11) Run config.after_initialize callbacks
  #
  class Application < Engine
    autoload :Bootstrap,      'rails/application/bootstrap'
    autoload :Configuration,  'rails/application/configuration'
    autoload :Finisher,       'rails/application/finisher'
    autoload :Railties,       'rails/engine/railties'
    autoload :RoutesReloader, 'rails/application/routes_reloader'

    class << self
      def inherited(base)
        raise "You cannot have more than one Rails::Application" if Rails.application
        super
        Rails.application = base.instance
        Rails.application.add_lib_to_load_path!
        ActiveSupport.run_load_hooks(:before_configuration, base.instance)
      end
    end

    attr_accessor :assets, :sandbox
    alias_method :sandbox?, :sandbox
    attr_reader :reloaders

    delegate :default_url_options, :default_url_options=, to: :routes

    def initialize
      super
      @initialized      = false
      @reloaders        = []
      @routes_reloader  = nil
      @app_env_config   = nil
      @ordered_railties = nil
      @railties         = nil
    end

    # Returns true if the application is initialized.
    def initialized?
      @initialized
    end

    # Implements call according to the Rack API. It simply
    # dispatches the request to the underlying middleware stack.
    def call(env)
      env["ORIGINAL_FULLPATH"] = build_original_fullpath(env)
      env["ORIGINAL_SCRIPT_NAME"] = env["SCRIPT_NAME"]
      super(env)
    end

    # Reload application routes regardless if they changed or not.
    def reload_routes!
      routes_reloader.reload!
    end


    # Return the application's KeyGenerator
    def key_generator
      # number of iterations selected based on consultation with the google security
      # team. Details at https://github.com/rails/rails/pull/6952#issuecomment-7661220
      @caching_key_generator ||= begin
        if config.secret_key_base
          key_generator = ActiveSupport::KeyGenerator.new(config.secret_key_base, iterations: 1000)
          ActiveSupport::CachingKeyGenerator.new(key_generator)
        else
          ActiveSupport::LegacyKeyGenerator.new(config.secret_token)
        end
      end
    end

    # Stores some of the Rails initial environment parameters which
    # will be used by middlewares and engines to configure themselves.
    # Currently stores:
    #
    #   * "action_dispatch.parameter_filter"             => config.filter_parameters
    #   * "action_dispatch.redirect_filter"              => config.filter_redirect
    #   * "action_dispatch.secret_token"                 => config.secret_token
    #   * "action_dispatch.secret_key_base"              => config.secret_key_base
    #   * "action_dispatch.show_exceptions"              => config.action_dispatch.show_exceptions
    #   * "action_dispatch.show_detailed_exceptions"     => config.consider_all_requests_local
    #   * "action_dispatch.logger"                       => Rails.logger
    #   * "action_dispatch.backtrace_cleaner"            => Rails.backtrace_cleaner
    #   * "action_dispatch.key_generator"                => key_generator
    #   * "action_dispatch.http_auth_salt"               => config.action_dispatch.http_auth_salt
    #   * "action_dispatch.signed_cookie_salt"           => config.action_dispatch.signed_cookie_salt
    #   * "action_dispatch.encrypted_cookie_salt"        => config.action_dispatch.encrypted_cookie_salt
    #   * "action_dispatch.encrypted_signed_cookie_salt" => config.action_dispatch.encrypted_signed_cookie_salt
    #
    def env_config
      @app_env_config ||= begin
        if config.secret_key_base.blank?
          ActiveSupport::Deprecation.warn "You didn't set config.secret_key_base. " +
            "Read the upgrade documentation to learn more about this new config option."

          if config.secret_token.blank?
            raise "You must set config.secret_key_base in your app's config."
          end
        end

        super.merge({
          "action_dispatch.parameter_filter" => config.filter_parameters,
          "action_dispatch.redirect_filter" => config.filter_redirect,
          "action_dispatch.secret_token" => config.secret_token,
          "action_dispatch.secret_key_base" => config.secret_key_base,
          "action_dispatch.show_exceptions" => config.action_dispatch.show_exceptions,
          "action_dispatch.show_detailed_exceptions" => config.consider_all_requests_local,
          "action_dispatch.logger" => Rails.logger,
          "action_dispatch.backtrace_cleaner" => Rails.backtrace_cleaner,
          "action_dispatch.key_generator" => key_generator,
          "action_dispatch.http_auth_salt" => config.action_dispatch.http_auth_salt,
          "action_dispatch.signed_cookie_salt" => config.action_dispatch.signed_cookie_salt,
          "action_dispatch.encrypted_cookie_salt" => config.action_dispatch.encrypted_cookie_salt,
          "action_dispatch.encrypted_signed_cookie_salt" => config.action_dispatch.encrypted_signed_cookie_salt
        })
      end
    end

    ## Rails internal API

    # This method is called just after an application inherits from Rails::Application,
    # allowing the developer to load classes in lib and use them during application
    # configuration.
    #
    #   class MyApplication < Rails::Application
    #     require "my_backend" # in lib/my_backend
    #     config.i18n.backend = MyBackend
    #   end
    #
    # Notice this method takes into consideration the default root path. So if you
    # are changing config.root inside your application definition or having a custom
    # Rails application, you will need to add lib to $LOAD_PATH on your own in case
    # you need to load files in lib/ during the application configuration as well.
    def add_lib_to_load_path! #:nodoc:
      path = File.join config.root, 'lib'
      $LOAD_PATH.unshift(path) if File.exist?(path)
    end

    def require_environment! #:nodoc:
      environment = paths["config/environment"].existent.first
      require environment if environment
    end

    def routes_reloader #:nodoc:
      @routes_reloader ||= RoutesReloader.new
    end

    # Returns an array of file paths appended with a hash of
    # directories-extensions suitable for ActiveSupport::FileUpdateChecker
    # API.
    def watchable_args #:nodoc:
      files, dirs = config.watchable_files.dup, config.watchable_dirs.dup

      ActiveSupport::Dependencies.autoload_paths.each do |path|
        dirs[path.to_s] = [:rb]
      end

      [files, dirs]
    end

    # Initialize the application passing the given group. By default, the
    # group is :default but sprockets precompilation passes group equals
    # to assets if initialize_on_precompile is false to avoid booting the
    # whole app.
    def initialize!(group=:default) #:nodoc:
      raise "Application has been already initialized." if @initialized
      run_initializers(group, self)
      @initialized = true
      self
    end

    def initializers #:nodoc:
      Bootstrap.initializers_for(self) +
      railties_initializers(super) +
      Finisher.initializers_for(self)
    end

    def config #:nodoc:
      @config ||= Application::Configuration.new(find_root_with_flag("config.ru", Dir.pwd))
    end

    def to_app #:nodoc:
      self
    end

    def helpers_paths #:nodoc:
      config.helpers_paths
    end

  protected

    alias :build_middleware_stack :app

    def run_tasks_blocks(app) #:nodoc:
      railties.each { |r| r.run_tasks_blocks(app) }
      super
      require "rails/tasks"
      config = self.config
      task :environment do
        ActiveSupport.on_load(:before_initialize) { config.eager_load = false }

        require_environment!
      end
    end

    def run_generators_blocks(app) #:nodoc:
      railties.each { |r| r.run_generators_blocks(app) }
      super
    end

    def run_runner_blocks(app) #:nodoc:
      railties.each { |r| r.run_runner_blocks(app) }
      super
    end

    def run_console_blocks(app) #:nodoc:
      railties.each { |r| r.run_console_blocks(app) }
      super
    end

    # Returns the ordered railties for this application considering railties_order.
    def ordered_railties #:nodoc:
      @ordered_railties ||= begin
        order = config.railties_order.map do |railtie|
          if railtie == :main_app
            self
          elsif railtie.respond_to?(:instance)
            railtie.instance
          else
            railtie
          end
        end

        all = (railties - order)
        all.push(self)   unless (all + order).include?(self)
        order.push(:all) unless order.include?(:all)

        index = order.index(:all)
        order[index] = all
        order.reverse.flatten
      end
    end

    def railties_initializers(current) #:nodoc:
      initializers = []
      ordered_railties.each do |r|
        if r == self
          initializers += current
        else
          initializers += r.initializers
        end
      end
      initializers
    end

    def reload_dependencies? #:nodoc:
      config.reload_classes_only_on_change != true || reloaders.map(&:updated?).any?
    end

    def default_middleware_stack #:nodoc:
      ActionDispatch::MiddlewareStack.new.tap do |middleware|
        app = self

        if rack_cache = load_rack_cache
          require "action_dispatch/http/rack_cache"
          middleware.use ::Rack::Cache, rack_cache
        end

        if config.force_ssl
          middleware.use ::ActionDispatch::SSL, config.ssl_options
        end

        middleware.use ::Rack::Sendfile, config.action_dispatch.x_sendfile_header

        if config.serve_static_assets
          middleware.use ::ActionDispatch::Static, paths["public"].first, config.static_cache_control
        end

        middleware.use ::Rack::Lock unless allow_concurrency?
        middleware.use ::Rack::Runtime
        middleware.use ::Rack::MethodOverride
        middleware.use ::ActionDispatch::RequestId

        # Must come after Rack::MethodOverride to properly log overridden methods
        middleware.use ::Rails::Rack::Logger, config.log_tags
        middleware.use ::ActionDispatch::ShowExceptions, show_exceptions_app
        middleware.use ::ActionDispatch::DebugExceptions, app
        middleware.use ::ActionDispatch::RemoteIp, config.action_dispatch.ip_spoofing_check, config.action_dispatch.trusted_proxies

        unless config.cache_classes
          middleware.use ::ActionDispatch::Reloader, lambda { app.reload_dependencies? }
        end

        middleware.use ::ActionDispatch::Callbacks
        middleware.use ::ActionDispatch::Cookies

        if config.session_store
          if config.force_ssl && !config.session_options.key?(:secure)
            config.session_options[:secure] = true
          end
          middleware.use config.session_store, config.session_options
          middleware.use ::ActionDispatch::Flash
        end

        middleware.use ::ActionDispatch::ParamsParser
        middleware.use ::Rack::Head
        middleware.use ::Rack::ConditionalGet
        middleware.use ::Rack::ETag, "no-cache"
      end
    end

    def allow_concurrency?
      if config.allow_concurrency.nil?
        config.cache_classes
      else
        config.allow_concurrency
      end
    end

    def load_rack_cache
      rack_cache = config.action_dispatch.rack_cache
      return unless rack_cache

      begin
        require 'rack/cache'
      rescue LoadError => error
        error.message << ' Be sure to add rack-cache to your Gemfile'
        raise
      end

      if rack_cache == true
        {
          metastore: "rails:/",
          entitystore: "rails:/",
          verbose: false
        }
      else
        rack_cache
      end
    end

    def show_exceptions_app
      config.exceptions_app || ActionDispatch::PublicExceptions.new(Rails.public_path)
    end

    def build_original_fullpath(env) #:nodoc:
      path_info    = env["PATH_INFO"]
      query_string = env["QUERY_STRING"]
      script_name  = env["SCRIPT_NAME"]

      if query_string.present?
        "#{script_name}#{path_info}?#{query_string}"
      else
        "#{script_name}#{path_info}"
      end
    end
  end
end
