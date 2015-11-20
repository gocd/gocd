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
      cert.extensions.each{|ext|
        next if ext.oid != "subjectAltName"
        ext.value.split(/,\s+/).each{|general_name|
          if /\ADNS:(.*)/ =~ general_name
            should_verify_common_name = false
            reg = Regexp.escape($1).gsub(/\\\*/, "[^.]+")
            return true if /\A#{reg}\z/i =~ hostname
          # NOTE: somehow we need the IP: canonical form
          # seems there were failures elsewhere when not
          # not sure how that's possible possible to-do!
          elsif /\AIP(?: Address)?:(.*)/ =~ general_name
          #elsif /\AIP Address:(.*)/ =~ general_name
            should_verify_common_name = false
            return true if $1 == hostname
          end
        }
      }
      if should_verify_common_name
        cert.subject.to_a.each{|oid, value|
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
          raise SSLError, "hostname was not match with the server certificate"
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
