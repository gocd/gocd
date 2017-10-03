require 'tilt'

module Sprockets
  class YUICompressor < Tilt::Template
    def self.engine_initialized?
      defined?(::YUI)
    end

    def initialize_engine
      require_template_library 'yui/compressor'
    end

    def prepare
    end

    def evaluate(context, locals, &block)
      case context.content_type
      when 'application/javascript'
        YUI::JavaScriptCompressor.new.compress(data)
      when 'text/css'
        YUI::CssCompressor.new.compress(data)
      else
        data
      end
    end
  end
end
