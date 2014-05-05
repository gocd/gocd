# encoding: binary

module WebSocket
  module Frame
    module Handler
      module Handler07

        include Handler05

        private

        # Hash of frame names and it's opcodes
        FRAME_TYPES = {
          :continuation => 0,
          :text => 1,
          :binary => 2,
          :close => 8,
          :ping => 9,
          :pong => 10,
        }

        # Hash of frame opcodes and it's names
        FRAME_TYPES_INVERSE = FRAME_TYPES.invert

        # Convert frame type name to opcode
        # @param [Symbol] frame_type Frame type name
        # @return [Integer] opcode or nil
        # @raise [WebSocket::Error] if frame opcode is not known
        def type_to_opcode(frame_type)
          FRAME_TYPES[frame_type] || raise(WebSocket::Error, :unknown_frame_type)
        end

        # Convert frame opcode to type name
        # @param [Integer] opcode Opcode
        # @return [Symbol] Frame type name or nil
        # @raise [WebSocket::Error] if frame type name is not known
        def opcode_to_type(opcode)
          FRAME_TYPES_INVERSE[opcode] || raise(WebSocket::Error, :unknown_opcode)
        end

      end
    end
  end
end
