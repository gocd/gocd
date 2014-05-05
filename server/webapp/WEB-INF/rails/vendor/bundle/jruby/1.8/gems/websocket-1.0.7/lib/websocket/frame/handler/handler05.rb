# encoding: binary

module WebSocket
  module Frame
    module Handler
      module Handler05

        include Handler04

        private

        def encode_frame
          if @code
            @data = Data.new([@code].pack('n') + @data.to_s)
            @code = nil
          end
          super
        end

        # Since handler 5 masking should be enabled by default
        def masking?; true; end

      end
    end
  end
end
