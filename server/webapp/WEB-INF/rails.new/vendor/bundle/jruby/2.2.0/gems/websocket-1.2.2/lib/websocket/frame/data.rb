module WebSocket
  module Frame
    class Data < String

      def initialize(*args)
        super(*convert_args(args))
      end

      def <<(*args)
        super(*convert_args(args))
      end

      # Convert all arguments to ASCII-8BIT for easier traversing
      def convert_args(args)
        args.each { |arg| arg.force_encoding('ASCII-8BIT') }
      end

      # Extract mask from 4 first bytes according to spec
      def set_mask
        raise WebSocket::Error::Frame::MaskTooShort if bytesize < 4
        @masking_key = self[0..3].bytes.to_a
      end

      # Remove mask flag - it will still be present in payload
      def unset_mask
        @masking_key = nil
      end

      # Extract `count` bytes starting from `start_index` and unmask it if needed.
      def getbytes(start_index, count)
        data = self[start_index, count]
        data = mask(data.bytes.to_a, @masking_key).pack('C*') if @masking_key
        data
      end

      # Mask whole payload using mask key
      def mask(payload, mask)
        return mask_native(payload, mask) if respond_to?(:mask_native)
        result = []
        payload.each_with_index do |byte, i|
          result[i] = byte ^ mask[i % 4]
        end
        result
      end

    end
  end
end
