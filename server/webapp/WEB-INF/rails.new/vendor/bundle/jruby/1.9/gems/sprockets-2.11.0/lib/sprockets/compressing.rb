module Sprockets
  # `Compressing` is an internal mixin whose public methods are exposed on
  # the `Environment` and `Index` classes.
  module Compressing
    def compressors
      deep_copy_hash(@compressors)
    end

    def register_compressor(mime_type, sym, klass)
      @compressors[mime_type][sym] = klass
    end

    # Return CSS compressor or nil if none is set
    def css_compressor
      @css_compressor if defined? @css_compressor
    end

    # Assign a compressor to run on `text/css` assets.
    #
    # The compressor object must respond to `compress`.
    def css_compressor=(compressor)
      unregister_bundle_processor 'text/css', css_compressor if css_compressor
      @css_compressor = nil
      return unless compressor

      if compressor.is_a?(Symbol)
        compressor = compressors['text/css'][compressor] || raise(Error, "unknown compressor: #{compressor}")
      end

      if compressor.respond_to?(:compress)
        klass = Class.new(Processor) do
          @name = "css_compressor"
          @processor = proc { |context, data| compressor.compress(data) }
        end
        @css_compressor = :css_compressor
      else
        @css_compressor = klass = compressor
      end

      register_bundle_processor 'text/css', klass
    end

    # Return JS compressor or nil if none is set
    def js_compressor
      @js_compressor if defined? @js_compressor
    end

    # Assign a compressor to run on `application/javascript` assets.
    #
    # The compressor object must respond to `compress`.
    def js_compressor=(compressor)
      unregister_bundle_processor 'application/javascript', js_compressor if js_compressor
      @js_compressor = nil
      return unless compressor

      if compressor.is_a?(Symbol)
        compressor = compressors['application/javascript'][compressor] || raise(Error, "unknown compressor: #{compressor}")
      end

      if compressor.respond_to?(:compress)
        klass = Class.new(Processor) do
          @name = "js_compressor"
          @processor = proc { |context, data| compressor.compress(data) }
        end
        @js_compressor = :js_compressor
      else
        @js_compressor = klass = compressor
      end

      register_bundle_processor 'application/javascript', klass
    end
  end
end
