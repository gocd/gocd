require 'tilt'

module Sprockets
  class UglifierCompressor < Tilt::Template
    self.default_mime_type = 'application/javascript'

    def self.engine_initialized?
      defined?(::Uglifier)
    end

    def initialize_engine
      require_template_library 'uglifier'
    end

    def prepare
    end

    def evaluate(context, locals, &block)
      # Feature detect Uglifier 2.0 option support
      if Uglifier::DEFAULTS[:copyright]
        # Uglifier < 2.x
        Uglifier.new(:copyright => false).compile(data)
      else
        # Uglifier >= 2.x
        Uglifier.new(:comments => :none).compile(data)
      end
    end
  end
end
