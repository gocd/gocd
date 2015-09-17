require 'tilt'

module Sprockets
  # This custom Tilt handler replaces the one built into Tilt. The
  # main difference is that it uses a custom importer that plays nice
  # with sprocket's caching system.
  #
  # See `SassImporter` for more infomation.
  class SassTemplate < Tilt::Template
    self.default_mime_type = 'text/css'

    def self.engine_initialized?
      defined?(::Sass::Engine) && defined?(::Sass::Script::Functions) &&
        ::Sass::Script::Functions < Sprockets::SassFunctions
    end

    def initialize_engine
      # Double check constant to avoid tilt warning
      unless defined? ::Sass
        require_template_library 'sass'
      end

      # Install custom functions. It'd be great if this didn't need to
      # be installed globally, but could be passed into Engine as an
      # option.
      ::Sass::Script::Functions.send :include, Sprockets::SassFunctions
    end

    def prepare
    end

    def syntax
      :sass
    end

    def evaluate(context, locals, &block)
      # Use custom importer that knows about Sprockets Caching
      cache_store = SassCacheStore.new(context.environment)

      options = {
        :filename => eval_file,
        :line => line,
        :syntax => syntax,
        :cache_store => cache_store,
        :importer => SassImporter.new(context, context.pathname),
        :load_paths => context.environment.paths.map { |path| SassImporter.new(context, path) },
        :sprockets => {
          :context => context,
          :environment => context.environment
        }
      }

      ::Sass::Engine.new(data, options).render
    rescue ::Sass::SyntaxError => e
      # Annotates exception message with parse line number
      context.__LINE__ = e.sass_backtrace.first[:line]
      raise e
    end
  end
end
