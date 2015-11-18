require 'sass'
require 'sass/rails/cache_store'
require 'sass/rails/helpers'
require 'sprockets/sass_functions'
require 'tilt'

module Sass
  module Rails
    class SassTemplate < Tilt::Template
      def self.default_mime_type
        'text/css'
      end

      def self.engine_initialized?
        true
      end

      def initialize_engine
      end

      def prepare
      end

      def syntax
        :sass
      end

      def evaluate(context, locals, &block)
        cache_store = CacheStore.new(context.environment)

        options = {
          :filename => eval_file,
          :line => line,
          :syntax => syntax,
          :cache_store => cache_store,
          :importer => importer_class.new(context.pathname.to_s),
          :load_paths => context.environment.paths.map { |path| importer_class.new(path.to_s) },
          :sprockets => {
            :context => context,
            :environment => context.environment
          }
        }

        sass_config = context.sass_config.merge(options)

        engine = ::Sass::Engine.new(data, sass_config)
        css = engine.render

        engine.dependencies.map do |dependency|
          context.depend_on(dependency.options[:filename])
        end

        css
      rescue ::Sass::SyntaxError => e
        context.__LINE__ = e.sass_backtrace.first[:line]
        raise e
      end

      private

      def importer_class
        SassImporter
      end
    end

    class ScssTemplate < SassTemplate
      def syntax
        :scss
      end
    end
  end
end
