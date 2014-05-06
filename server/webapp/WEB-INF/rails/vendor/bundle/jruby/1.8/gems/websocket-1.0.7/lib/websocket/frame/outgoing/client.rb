module WebSocket
  module Frame
    class Outgoing
      class Client < Outgoing

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
