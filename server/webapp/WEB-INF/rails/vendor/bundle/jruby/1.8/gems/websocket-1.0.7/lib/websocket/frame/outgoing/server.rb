module WebSocket
  module Frame
    class Outgoing
      class Server < Outgoing

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
