require 'tilt'

module Sprockets
  class SassCompressor < Tilt::Template
    self.default_mime_type = 'text/css'

    def self.engine_initialized?
      defined?(::Sass::Engine)
    end

    def initialize_engine
      require_template_library 'sass'
    end

    def prepare
    end

    def evaluate(context, locals, &block)
      ::Sass::Engine.new(data, {
        :syntax => :scss,
        :cache => false,
        :read_cache => false,
        :style => :compressed
      }).render
    end
  end
end
