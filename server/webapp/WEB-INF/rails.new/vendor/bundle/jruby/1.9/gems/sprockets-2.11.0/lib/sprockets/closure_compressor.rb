require 'tilt'

module Sprockets
  class ClosureCompressor < Tilt::Template
    self.default_mime_type = 'application/javascript'

    def self.engine_initialized?
      defined?(::Closure::Compiler)
    end

    def initialize_engine
      require_template_library 'closure-compiler'
    end

    def prepare
    end

    def evaluate(context, locals, &block)
      Closure::Compiler.new.compile(data)
    end
  end
end
