module WebSocket
  module Handshake
    # Construct or parse a server WebSocket handshake.
    #
    # @example
    #   handshake = WebSocket::Handshake::Server.new
    #
    #   # Parse client request
    #   @handshake << <<EOF
    #   GET /demo HTTP/1.1\r
    #   Upgrade: websocket\r
    #   Connection: Upgrade\r
    #   Host: example.com\r
    #   Sec-WebSocket-Origin: http://example.com\r
    #   Sec-WebSocket-Version: 13\r
    #   Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r
    #   \r
    #   EOF
    #
    #   # All data received?
    #   @handshake.finished?
    #
    #   # No parsing errors?
    #   @handshake.valid?
    #
    #   # Create response
    #   @handshake.to_s # HTTP/1.1 101 Switching Protocols
    #                   # Upgrade: websocket
    #                   # Connection: Upgrade
    #                   # Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=
    #
    class Server < Base

      # Initialize new WebSocket Server
      #
      # @param [Hash] args Arguments for server
      #
      # @option args [Boolean] :secure If true then server will use wss:// protocol
      #
      # @example
      #   Websocket::Handshake::Server.new(:secure => true)
      def initialize(args = {})
        super
        @secure = !!args[:secure]
      end

      # Add text of request from Client. This method will parse content immediately and update version, state and error(if neccessary)
      #
      # @param [String] data Data to add
      #
      # @example
      #   @handshake << <<EOF
      #   GET /demo HTTP/1.1
      #   Upgrade: websocket
      #   Connection: Upgrade
      #   Host: example.com
      #   Sec-WebSocket-Origin: http://example.com
      #   Sec-WebSocket-Version: 13
      #   Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==
      #
      #   EOF
      def <<(data)
        @data << data
        if parse_data
          set_version
        end
      end

      # Should send content to client after finished parsing?
      # @return [Boolean] true
      def should_respond?
        true
      end

      # Host of server according to client header
      # @return [String] host
      def host
        @headers["host"].to_s.split(":")[0].to_s
      end

      # Port of server according to client header
      # @return [String] port
      def port
        @headers["host"].to_s.split(":")[1]
      end

      private

      # Set version of protocol basing on client requets. AFter cotting method calls include_version.
      def set_version
        @version = @headers['sec-websocket-version'].to_i if @headers['sec-websocket-version']
        @version ||= @headers['sec-websocket-draft'].to_i if @headers['sec-websocket-draft']
        @version ||= 76 if @leftovers != ""
        @version ||= 75
        include_version
      end

      # Include set of methods for selected protocol version
      # @return [Boolean] false if protocol number is unknown, otherwise true
      def include_version
        case @version
          when 75 then extend Handler::Server75
          when 76, 0..3 then extend Handler::Server76
          when 4..13 then extend Handler::Server04
          else set_error(:unknown_protocol_version) and return false
        end
        return true
      end

      PATH = /^(\w+) (\/[^\s]*) HTTP\/1\.1$/

      # Parse first line of Client response.
      # @param [String] line Line to parse
      # @return [Boolean] True if parsed correctly. False otherwise
      def parse_first_line(line)
        line_parts = line.match(PATH)
        set_error(:invalid_header) and return unless line_parts
        method = line_parts[1].strip
        set_error(:get_request_required) and return unless method == "GET"

        resource_name = line_parts[2].strip
        @path, @query = resource_name.split('?', 2)

        return true
      end

    end
  end
end
