module WebSocket
  module Handshake
    module Handler
      module Client

        include Base

        private

        # @see WebSocket::Handshake::Handler::Base#header_line
        def header_line
          path = @path
          path += "?" + @query if @query
          "GET #{path} HTTP/1.1"
        end

      end
    end
  end
end
