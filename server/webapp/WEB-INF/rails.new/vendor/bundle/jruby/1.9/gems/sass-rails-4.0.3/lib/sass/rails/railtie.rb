require 'sprockets/railtie'

module Sass::Rails
  class Railtie < ::Rails::Railtie
    module SassContext
      attr_accessor :sass_config
    end

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
    # It can be overridedden by passing:
    #     --stylesheet_engine=sass
    # to the rails generate command
    config.app_generators.stylesheet_engine config.sass.preferred_syntax

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

      if app.assets
        app.assets.context_class.extend(SassContext)
        app.assets.context_class.sass_config = app.config.sass
      end

      Sass.logger = app.config.sass.logger
    end

    initializer :setup_compression, group: :all do |app|
      unless Rails.env.development?
        app.config.assets.css_compressor ||= :sass
      else
        # Use expanded output instead of the sass default of :nested unless specified
        app.config.sass.style ||= :expanded
      end
    end
  end
end
