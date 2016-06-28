module WebSocket
  module Frame
    class Incoming
      class Client < Incoming
        def incoming_masking?
          false
        end

        def outgoing_masking?
          @handler.masking?
        end
      end
    end
  end
end
