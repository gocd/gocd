# encoding: binary

module WebSocket
  module Frame
    module Handler
      class Handler05 < Handler04

        # Since handler 5 masking should be enabled by default
        def masking?; true; end

      end
    end
  end
end
