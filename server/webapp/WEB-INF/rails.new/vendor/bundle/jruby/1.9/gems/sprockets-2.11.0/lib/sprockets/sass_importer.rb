require 'sass'

module Sprockets
  # This custom importer adds sprockets dependency tracking on to Sass
  # `@import` statements. This makes the Sprockets and Sass caching
  # systems work together.
  class SassImporter < Sass::Importers::Filesystem
    def initialize(context, root)
      @context = context
      super root.to_s
    end

    def find_relative(*args)
      engine = super
      if engine && (filename = engine.options[:filename])
        @context.depend_on(filename)
      end
      engine
    end

    def find(*args)
      engine = super
      if engine && (filename = engine.options[:filename])
        @context.depend_on(filename)
      end
      engine
    end
  end
end
