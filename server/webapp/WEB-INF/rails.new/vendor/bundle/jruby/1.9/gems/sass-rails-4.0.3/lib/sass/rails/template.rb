require "sprockets/sass_template"

module Sprockets
  class SassTemplate
    def evaluate(context, locals, &block)
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

      sass_config = context.environment.context_class.sass_config.merge(options)
      ::Sass::Engine.new(data, sass_config).render
    rescue ::Sass::SyntaxError => e
      context.__LINE__ = e.sass_backtrace.first[:line]
      raise e
    end
  end
end
