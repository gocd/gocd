module WebSocket
  module Frame
    # @abstract Subclass and override to implement custom frames
    class Base

      attr_reader :data, :type, :version, :error

      # Initialize frame
      # @param args [Hash] Arguments for frame
      # @option args [String]  :data default data for frame
      # @option args [String]  :type Type of frame - available types are "text", "binary", "ping", "pong" and "close"(support depends on draft version)
      # @option args [Integer] :code Code for close frame. Supported by drafts > 05.
      # @option args [Integer] :version Version of draft. Currently supported version are 75, 76 and 00-13.
      def initialize(args = {})
        @type = args[:type].to_sym if args[:type]
        @code = args[:code]
        @data = Data.new(args[:data].to_s)
        @version = args[:version] || DEFAULT_VERSION
        include_version
      end

      # Check if some errors occured
      # @return [Boolean] True if error is set
      def error?
        !!@error
      end

      # Is selected type supported for selected handler?
      def support_type?
        supported_frames.include?(@type)
      end

      # Implement in submodules
      def supported_frames
        raise NotImplementedError
      end

      # Recreate inspect as #to_s was overwritten
      def inspect
        vars = self.instance_variables.map{|v| "#{v}=#{instance_variable_get(v).inspect}"}.join(", ")
        insp = "#{self.class}:0x%08x" % (self.__id__ * 2)
        "<#{insp} #{vars}>"
      end

      private

      # Include set of methods for selected protocol version
      # @return [Boolean] false if protocol number is unknown, otherwise true
      def include_version
        case @version
          when 75..76 then extend Handler::Handler75
          when 0..2 then extend Handler::Handler75
          when 3 then extend Handler::Handler03
          when 4 then extend Handler::Handler04
          when 5..6 then extend Handler::Handler05
          when 7..13 then extend Handler::Handler07
          else set_error(:unknown_protocol_version) and return
        end
      end

      # Changes state to error and sets error message
      # @param [String] message Error message to set
      def set_error(message)
        @error = message
      end

    end
  end
end
