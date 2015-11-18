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

    def verify_certificate_identity(cert, hostname)
      should_verify_common_name = true
      cert.extensions.each { |ext|
        next if ext.oid != "subjectAltName"
        ext.value.split(/,\s+/).each { |general_name|
          # MRI 1.9.3 (since we parse ASN.1 differently)
          # when 2 # dNSName in GeneralName (RFC5280)
          if /\ADNS:(.*)/ =~ general_name
            should_verify_common_name = false
            reg = Regexp.escape($1).gsub(/\\\*/, "[^.]+")
            return true if /\A#{reg}\z/i =~ hostname
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
            reg = Regexp.escape(value).gsub(/\\\*/, "[^.]+")
            return true if /\A#{reg}\z/i =~ hostname
          end
        }
      end
      return false
    end
    module_function :verify_certificate_identity

    class SSLSocket
      include Buffering
      include SocketForwarder
      include Nonblock

      def post_connection_check(hostname)
        unless OpenSSL::SSL.verify_certificate_identity(peer_cert, hostname)
          raise SSLError, "hostname does not match the server certificate"
        end
        return true
      end

    end

    class SSLServer
      include SocketForwarder
      attr_accessor :start_immediately

      def initialize(svr, ctx)
        @svr = svr
        @ctx = ctx
        unless ctx.session_id_context
          session_id = OpenSSL::Digest::MD5.hexdigest($0)
          @ctx.session_id_context = session_id
        end
        @start_immediately = true
      end

      def to_io
        @svr
      end

      def listen(backlog=5)
        @svr.listen(backlog)
      end

      def shutdown(how=Socket::SHUT_RDWR)
        @svr.shutdown(how)
      end

      def accept
        sock = @svr.accept
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

      def close
        @svr.close
      end
    end
  end
end
