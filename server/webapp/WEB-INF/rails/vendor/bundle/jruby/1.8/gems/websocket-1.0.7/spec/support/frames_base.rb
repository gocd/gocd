module WebSocket
  module Frame
    class Base

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
