=begin
= $RCSfile$ -- Ruby-space definitions that completes C-space funcs for SSL

= Info
  'OpenSSL for Ruby 2' project
  Copyright (C) 2001 GOTOU YUUZOU <gotoyuzo@notwork.org>
  All rights reserved.

= Licence
  This program is licenced under the same licence as Ruby.
  (See the file 'LICENCE'.)

= Version
  $Id$
=end

require "openssl/buffering"
require 'fcntl' # used by OpenSSL::SSL::Nonblock (if loaded)

module OpenSSL
  module SSL

    # FIXME: Using the old non-ASN1 logic here because our ASN1 appears to
    # return the wrong types for some decoded objects.
    # @see https://github.com/jruby/jruby/issues/1102
    # @private
    def verify_certificate_identity(cert, hostname)
      should_verify_common_name = true
      cert.extensions.each { |ext|
        next if ext.oid != "subjectAltName"
        ext.value.split(/,\s+/).each { |general_name|
          # MRI 1.9.3 (since we parse ASN.1 differently)
          # when 2 # dNSName in GeneralName (RFC5280)
          if /\ADNS:(.*)/ =~ general_name
            should_verify_common_name = false
            return true if verify_hostname(hostname, $1)
          # MRI 1.9.3 (since we parse ASN.1 differently)
          # when 7 # iPAddress in GeneralName (RFC5280)
          elsif /\AIP(?: Address)?:(.*)/ =~ general_name
            should_verify_common_name = false
            return true if $1 == hostname
            # NOTE: bellow logic makes little sense as we read exts differently
            #value = $1 # follows GENERAL_NAME_print() in x509v3/v3_alt.c
            #if value.size == 4
            #  return true if value.unpack('C*').join('.') == hostname
            #elsif value.size == 16
            #  return true if value.unpack('n*').map { |e| sprintf("%X", e) }.join(':') == hostname
            #end
          end
        }
      }
      if should_verify_common_name
        cert.subject.to_a.each { |oid, value|
          if oid == "CN"
            return true if verify_hostname(hostname, value)
          end
        }
      end
      return false
    end
    module_function :verify_certificate_identity

    def verify_hostname(hostname, san) # :nodoc:
      # RFC 5280, IA5String is limited to the set of ASCII characters
      return false unless san.ascii_only?
      return false unless hostname.ascii_only?

      # See RFC 6125, section 6.4.1
      # Matching is case-insensitive.
      san_parts = san.downcase.split(".")

      # TODO: this behavior should probably be more strict
      return san == hostname if san_parts.size < 2

      # Matching is case-insensitive.
      host_parts = hostname.downcase.split(".")

      # RFC 6125, section 6.4.3, subitem 2.
      # If the wildcard character is the only character of the left-most
      # label in the presented identifier, the client SHOULD NOT compare
      # against anything but the left-most label of the reference
      # identifier (e.g., *.example.com would match foo.example.com but
      # not bar.foo.example.com or example.com).
      return false unless san_parts.size == host_parts.size

      # RFC 6125, section 6.4.3, subitem 1.
      # The client SHOULD NOT attempt to match a presented identifier in
      # which the wildcard character comprises a label other than the
      # left-most label (e.g., do not match bar.*.example.net).
      return false unless verify_wildcard(host_parts.shift, san_parts.shift)

      san_parts.join(".") == host_parts.join(".")
    end
    module_function :verify_hostname

    def verify_wildcard(domain_component, san_component) # :nodoc:
      parts = san_component.split("*", -1)

      return false if parts.size > 2
      return san_component == domain_component if parts.size == 1

      # RFC 6125, section 6.4.3, subitem 3.
      # The client SHOULD NOT attempt to match a presented identifier
      # where the wildcard character is embedded within an A-label or
      # U-label of an internationalized domain name.
      return false if domain_component.start_with?("xn--") && san_component != "*"

      parts[0].length + parts[1].length < domain_component.length &&
      domain_component.start_with?(parts[0]) &&
      domain_component.end_with?(parts[1])
    end
    module_function :verify_wildcard

    class SSLSocket
      include Buffering
      include SocketForwarder
      include Nonblock

      ##
      # Perform hostname verification after an SSL connection is established
      #
      # This method MUST be called after calling #connect to ensure that the
      # hostname of a remote peer has been verified.
      def post_connection_check(hostname)
        unless OpenSSL::SSL.verify_certificate_identity(peer_cert, hostname)
          raise SSLError, "hostname \"#{hostname}\" does not match the server certificate"
        end
        return true
      end

    end

    ##
    # SSLServer represents a TCP/IP server socket with Secure Sockets Layer.
    class SSLServer
      include SocketForwarder
      # When true then #accept works exactly the same as TCPServer#accept
      attr_accessor :start_immediately

      # Creates a new instance of SSLServer.
      # * +srv+ is an instance of TCPServer.
      # * +ctx+ is an instance of OpenSSL::SSL::SSLContext.
      def initialize(svr, ctx)
        @svr = svr
        @ctx = ctx
        unless ctx.session_id_context
          # see #6137 - session id may not exceed 32 bytes
          prng = ::Random.new($0.hash)
          session_id = prng.bytes(16).unpack('H*')[0]
          @ctx.session_id_context = session_id
        end
        @start_immediately = true
      end

      # Returns the TCPServer passed to the SSLServer when initialized.
      def to_io
        @svr
      end

      # See TCPServer#listen for details.
      def listen(backlog=5)
        @svr.listen(backlog)
      end

      # See BasicSocket#shutdown for details.
      def shutdown(how=Socket::SHUT_RDWR)
        @svr.shutdown(how)
      end

      # Works similar to TCPServer#accept.
      def accept
        # Socket#accept returns [socket, addrinfo].
        # TCPServer#accept returns a socket.
        # The following comma strips addrinfo.
        sock, = @svr.accept
        begin
          ssl = OpenSSL::SSL::SSLSocket.new(sock, @ctx)
          ssl.sync_close = true
          ssl.accept if @start_immediately
          ssl
        rescue SSLError => ex
          sock.close
          raise ex
        end
      end

      # See IO#close for details.
      def close
        @svr.close
      end
    end
  end
end
