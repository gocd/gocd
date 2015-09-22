require 'sass'
require 'active_support/core_ext/class/attribute'
require 'sprockets/railtie'

module Sass::Rails
  class Railtie < ::Rails::Railtie
    config.sass = ActiveSupport::OrderedOptions.new

    # Establish static configuration defaults
    # Emit scss files during stylesheet generation of scaffold
    config.sass.preferred_syntax = :scss
    # Write sass cache files for performance
    config.sass.cache            = true
    # Read sass cache files for performance
    config.sass.read_cache       = true
    # Display line comments above each selector as a debugging aid
    config.sass.line_comments    = true
    # Initialize the load paths to an empty array
    config.sass.load_paths       = []
    # Send Sass logs to Rails.logger
    config.sass.logger           = Sass::Rails::Logger.new

    # Set the default stylesheet engine
    # It can be overridden by passing:
    #     --stylesheet_engine=sass
    # to the rails generate command
    config.app_generators.stylesheet_engine config.sass.preferred_syntax

    if config.respond_to?(:annotations)
      config.annotations.register_extensions("scss", "sass") { |annotation| /\/\/\s*(#{annotation}):?\s*(.*)$/ }
    end

    # Remove the sass middleware if it gets inadvertently enabled by applications.
    config.after_initialize do |app|
      app.config.middleware.delete(Sass::Plugin::Rack) if defined?(Sass::Plugin::Rack)
    end

    initializer :setup_sass, group: :all do |app|
      # Only emit one kind of syntax because though we have registered two kinds of generators
      syntax     = app.config.sass.preferred_syntax.to_sym
      alt_syntax = syntax == :sass ? "scss" : "sass"
      app.config.generators.hide_namespace alt_syntax

      # Override stylesheet engine to the preferred syntax
      config.app_generators.stylesheet_engine syntax

      # Set the sass cache location
      config.sass.cache_location   = File.join(Rails.root, "tmp/cache/sass")

      # Establish configuration defaults that are evironmental in nature
      if config.sass.full_exception.nil?
        # Display a stack trace in the css output when in development-like environments.
        config.sass.full_exception = app.config.consider_all_requests_local
      end

      config.assets.configure do |env|
        env.register_engine '.sass', Sass::Rails::SassTemplate
        env.register_engine '.scss', Sass::Rails::ScssTemplate

        env.context_class.class_eval do
          class_attribute :sass_config
          self.sass_config = app.config.sass
        end
      end

      Sass.logger = app.config.sass.logger
    end

    initializer :setup_compression, group: :all do |app|
      if Rails.env.development?
        # Use expanded output instead of the sass default of :nested unless specified
        app.config.sass.style ||= :expanded
      else
        # config.assets.css_compressor may be set to nil in non-dev environments.
        # otherwise, the default is sass compression.
        app.config.assets.css_compressor = :sass unless app.config.assets.has_key?(:css_compressor)
      end
    end
  end
end
