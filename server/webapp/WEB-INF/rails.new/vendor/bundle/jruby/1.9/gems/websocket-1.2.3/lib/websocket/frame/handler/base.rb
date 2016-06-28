module WebSocket
  module Frame
    module Handler
      class Base
        def initialize(frame)
          @frame = frame
        end

        # Convert data to raw frame ready to send to client
        # @return [String] Encoded frame
        def encode_frame
          fail NotImplementedError
        end

        # Convert raw data to decoded frame
        # @return [WebSocket::Frame::Incoming] Frame if found, nil otherwise
        def decode_frame
          fail NotImplementedError
        end

        private

        # Check if frame is one of control frames
        # @param [Symbol] frame_type Frame type
        # @return [Boolean] True if given frame type is control frame
        def control_frame?(frame_type)
          ![:text, :binary, :continuation].include?(frame_type)
        end

        # Check if frame is one of data frames
        # @param [Symbol] frame_type Frame type
        # @return [Boolean] True if given frame type is data frame
        def data_frame?(frame_type)
          [:text, :binary].include?(frame_type)
        end
      end
    end
  end
end
