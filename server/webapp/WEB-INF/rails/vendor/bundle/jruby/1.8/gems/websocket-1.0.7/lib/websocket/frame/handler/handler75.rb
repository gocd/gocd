# encoding: binary

module WebSocket
  module Frame
    module Handler
      module Handler75

        include Base

        # @see WebSocket::Frame::Base#supported_frames
        def supported_frames
          [:text, :close]
        end

        private

        # @see WebSocket::Frame::Handler::Base#encode_frame
        def encode_frame
          case @type
            when :close then "\xff\x00"
            when :text then
              ary = ["\x00", @data, "\xff"]
              ary.collect{ |s| s.encode('UTF-8', 'UTF-8', :invalid => :replace) if s.respond_to?(:encode) }
              ary.join
          end
        end

        # @see WebSocket::Frame::Handler::Base#decode_frame
        def decode_frame
          return if @data.size == 0

          pointer = 0
          frame_type = @data.getbyte(pointer)
          pointer += 1

          if (frame_type & 0x80) == 0x80
            # If the high-order bit of the /frame type/ byte is set
            length = 0

            loop do
              return if !@data.getbyte(pointer)
              b = @data.getbyte(pointer)
              pointer += 1
              b_v = b & 0x7F
              length = length * 128 + b_v
              break unless (b & 0x80) == 0x80
            end

            set_error(:frame_too_long) and return if length > ::WebSocket.max_frame_size

            unless @data.getbyte(pointer+length-1) == nil
              # Straight from spec - I'm sure this isn't crazy...
              # 6. Read /length/ bytes.
              # 7. Discard the read bytes.
              @data = @data[(pointer+length)..-1]

              # If the /frame type/ is 0xFF and the /length/ was 0, then close
              if length == 0
                self.class.new(:version => version, :type => :close, :decoded => true)
              end
            end
          else
            # If the high-order bit of the /frame type/ byte is _not_ set

            set_error(:invalid_frame) and return if @data.getbyte(0) != 0x00

            # Addition to the spec to protect against malicious requests
            set_error(:frame_too_long) and return if @data.size > ::WebSocket.max_frame_size

            msg = @data.slice!(/\A\x00[^\xff]*\xff/)
            if msg
              msg.gsub!(/\A\x00|\xff\z/, '')
              msg.force_encoding('UTF-8') if msg.respond_to?(:force_encoding)
              self.class.new(:version => version, :type => :text, :data => msg, :decoded => true)
            end
          end
        end

      end
    end
  end
end
