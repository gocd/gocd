require 'digest/md5'

module WebSocket
  module Handshake
    module Handler
      module Client01

        include Client76

        private

        # @see WebSocket::Handshake::Handler::Base#handshake_keys
        def handshake_keys
          keys = super
          keys << ['Sec-WebSocket-Draft', @version]
          keys
        end

      end
    end
  end
end
