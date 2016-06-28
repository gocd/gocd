module WebSocket
  module Handshake
    module Handler
      class Client < Base
        private

        # @see WebSocket::Handshake::Handler::Base#header_line
        def header_line
          path = @handshake.path
          path += '?' + @handshake.query if @handshake.query
          "GET #{path} HTTP/1.1"
        end

        # @see WebSocket::Handshake::Handler::Base#header_handshake_keys
        def handshake_keys
          super + @handshake.headers.to_a
        end
      end
    end
  end
end
