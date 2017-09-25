module WebSocket
  module Handshake
    module Handler
      class Client75 < Client
        private

        # @see WebSocket::Handshake::Handler::Base#handshake_keys
        def handshake_keys
          keys = [
            %w(Upgrade WebSocket),
            %w(Connection Upgrade)
          ]
          host = @handshake.host
          host += ":#{@handshake.port}" if @handshake.port
          keys << ['Host', host]
          keys << ['Origin', @handshake.origin] if @handshake.origin
          keys += super
          keys
        end
      end
    end
  end
end
