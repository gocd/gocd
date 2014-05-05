module WebSocket
  module Frame
    class Incoming
      class Server < Incoming

        private

        def incoming_masking?
          masking?
        end

        def outgoing_masking?
          false
        end

      end
    end
  end
end
