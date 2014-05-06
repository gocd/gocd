module WebSocket
  module Frame
    class Incoming
      class Client < Incoming

        private

        def incoming_masking?
          false
        end

        def outgoing_masking?
          masking?
        end

      end
    end
  end
end
