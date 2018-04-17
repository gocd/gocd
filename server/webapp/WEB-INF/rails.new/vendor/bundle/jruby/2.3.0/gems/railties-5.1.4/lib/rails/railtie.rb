require "rails/initializable"
require "active_support/inflector"
require "active_support/core_ext/module/introspection"
require "active_support/core_ext/module/delegation"

module Rails
  # <tt>Rails::Railtie</tt> is the core of the Rails framework and provides
  # several hooks to extend Rails and/or modify the initialization process.
  #
  # Every major component of Rails (Action Mailer, Action Controller, Active
  # Record, etc.) implements a railtie. Each of them is responsible for their
  # own initialization. This makes Rails itself absent of any component hooks,
  # allowing other components to be used in place of any of the Rails defaults.
  #
  # Developing a Rails extension does _not_ require implementing a railtie, but
  # if you need to interact with the Rails framework during or after boot, then
  # a railtie is needed.
  #
  # For example, an extension doing any of the following would need a railtie:
  #
  # * creating initializers
  # * configuring a Rails framework for the application, like setting a generator
  # * adding <tt>config.*</tt> keys to the environment
  # * setting up a subscriber with <tt>ActiveSupport::Notifications</tt>
  # * adding Rake tasks
  #
  # == Creating a Railtie
  #
  # To extend Rails using a railtie, create a subclass of <tt>Rails::Railtie</tt>.
  # This class must be loaded during the Rails boot process, and is conventionally
  # called <tt>MyNamespace::Railtie</tt>.
  #
  # The following example demonstrates an extension which can be used with or
  # without Rails.
  #
  #   # lib/my_gem/railtie.rb
  #   module MyGem
  #     class Railtie < Rails::Railtie
  #     end
  #   end
  #
  #   # lib/my_gem.rb
  #   require 'my_gem/railtie' if defined?(Rails)
  #
  # == Initializers
  #
  # To add an initialization step to the Rails boot process from your railtie, just
  # define the initialization code with the +initializer+ macro:
  #
  #   class MyRailtie < Rails::Railtie
  #     initializer "my_railtie.configure_rails_initialization" do
  #       # some initialization behavior
  #     end
  #   end
  #
  # If specified, the block can also receive the application object, in case you
  # need to access some application-specific configuration, like middleware:
  #
  #   class MyRailtie < Rails::Railtie
  #     initializer "my_railtie.configure_rails_initialization" do |app|
  #       app.middleware.use MyRailtie::Middleware
  #     end
  #   end
  #
  # Finally, you can also pass <tt>:before</tt> and <tt>:after</tt> as options to
  # +initializer+, in case you want to couple it with a specific step in the
  # initialization process.
  #
  # == Configuration
  #
  # Railties can access a config object which contains configuration shared by all
  # railties and the application:
  #
  #   class MyRailtie < Rails::Railtie
  #     # Customize the ORM
  #     config.app_generators.orm :my_railtie_orm
  #
  #     # Add a to_prepare block which is executed once in production
  #     # and before each request in development.
  #     config.to_prepare do
  #       MyRailtie.setup!
  #     end
  #   end
  #
  # == Loading Rake Tasks and Generators
  #
  # If your railtie has Rake tasks, you can tell Rails to load them through the method
  # +rake_tasks+:
  #
  #   class MyRailtie < Rails::Railtie
  #     rake_tasks do
  #       load 'path/to/my_railtie.tasks'
  #     end
  #   end
  #
  # By default, Rails loads generators from your load path. However, if you want to place
  # your generators at a different location, you can specify in your railtie a block which
  # will load them during normal generators lookup:
  #
  #   class MyRailtie < Rails::Railtie
  #     generators do
  #       require 'path/to/my_railtie_generator'
  #     end
  #   end
  #
  # == Application and Engine
  #
  # An engine is nothing more than a railtie with some initializers already set. And since
  # <tt>Rails::Application</tt> is an engine, the same configuration described here can be
  # used in both.
  #
  # Be sure to look at the documentation of those specific classes for more information.
  class Railtie
    autoload :Configuration, "rails/railtie/configuration"

    include Initializable

    ABSTRACT_RAILTIES = %w(Rails::Railtie Rails::Engine Rails::Application)

    class << self
      private :new
      delegate :config, to: :instance

      def subclasses
        @subclasses ||= []
      end

      def inherited(base)
        unless base.abstract_railtie?
          subclasses << base
        end
      end

      def rake_tasks(&blk)
        register_block_for(:rake_tasks, &blk)
      end

      def console(&blk)
        register_block_for(:load_console, &blk)
      end

      def runner(&blk)
        register_block_for(:runner, &blk)
      end

      def generators(&blk)
        register_block_for(:generators, &blk)
      end

      def abstract_railtie?
        ABSTRACT_RAILTIES.include?(name)
      end

      def railtie_name(name = nil)
        @railtie_name = name.to_s if name
        @railtie_name ||= generate_railtie_name(self.name)
      end

      # Since Rails::Railtie cannot be instantiated, any methods that call
      # +instance+ are intended to be called only on subclasses of a Railtie.
      def instance
        @instance ||= new
      end

      def respond_to_missing?(*args)
        instance.respond_to?(*args) || super
      end

      # Allows you to configure the railtie. This is the same method seen in
      # Railtie::Configurable, but this module is no longer required for all
      # subclasses of Railtie so we provide the class method here.
      def configure(&block)
        instance.configure(&block)
      end

      private
        def generate_railtie_name(string)
          ActiveSupport::Inflector.underscore(string).tr("/", "_")
        end

        # If the class method does not have a method, then send the method call
        # to the Railtie instance.
        def method_missing(name, *args, &block)
          if instance.respond_to?(name)
            instance.public_send(name, *args, &block)
          else
            super
          end
        end

        # receives an instance variable identifier, set the variable value if is
        # blank and append given block to value, which will be used later in
        # `#each_registered_block(type, &block)`
        def register_block_for(type, &blk)
          var_name = "@#{type}"
          blocks = instance_variable_defined?(var_name) ? instance_variable_get(var_name) : instance_variable_set(var_name, [])
          blocks << blk if blk
          blocks
        end
    end

    delegate :railtie_name, to: :class

    def initialize #:nodoc:
      if self.class.abstract_railtie?
        raise "#{self.class.name} is abstract, you cannot instantiate it directly."
      end
    end

    def configure(&block) #:nodoc:
      instance_eval(&block)
    end

    # This is used to create the <tt>config</tt> object on Railties, an instance of
    # Railtie::Configuration, that is used by Railties and Application to store
    # related configuration.
    def config
      @config ||= Railtie::Configuration.new
    end

    def railtie_namespace #:nodoc:
      @railtie_namespace ||= self.class.parents.detect { |n| n.respond_to?(:railtie_namespace) }
    end

    protected

      def run_console_blocks(app) #:nodoc:
        each_registered_block(:console) { |block| block.call(app) }
      end

      def run_generators_blocks(app) #:nodoc:
        each_registered_block(:generators) { |block| block.call(app) }
      end

      def run_runner_blocks(app) #:nodoc:
        each_registered_block(:runner) { |block| block.call(app) }
      end

      def run_tasks_blocks(app) #:nodoc:
        extend Rake::DSL
        each_registered_block(:rake_tasks) { |block| instance_exec(app, &block) }
      end

    private

      # run `&block` in every registered block in `#register_block_for`
      def each_registered_block(type, &block)
        klass = self.class
        while klass.respond_to?(type)
          klass.public_send(type).each(&block)
          klass = klass.superclass
        end
      end
  end
end
