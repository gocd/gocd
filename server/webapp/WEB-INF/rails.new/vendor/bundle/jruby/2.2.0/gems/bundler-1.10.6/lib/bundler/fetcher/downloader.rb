module Bundler
  class Fetcher
    class Downloader
      attr_reader :connection
      attr_reader :redirect_limit

      def initialize(connection, redirect_limit)
        @connection = connection
        @redirect_limit = redirect_limit
      end

      def fetch(uri, counter = 0)
        raise HTTPError, "Too many redirects" if counter >= redirect_limit

        response = request(uri)
        Bundler.ui.debug("HTTP #{response.code} #{response.message}")

        case response
        when Net::HTTPRedirection
          new_uri = URI.parse(response["location"])
          if new_uri.host == uri.host
            new_uri.user = uri.user
            new_uri.password = uri.password
          end
          fetch(new_uri, counter + 1)
        when Net::HTTPSuccess
          response.body
        when Net::HTTPRequestEntityTooLarge
          raise FallbackError, response.body
        when Net::HTTPUnauthorized
          raise AuthenticationRequiredError, uri.host
        else
          raise HTTPError, "#{response.class}: #{response.body}"
        end
      end

      def request(uri)
        Bundler.ui.debug "HTTP GET #{uri}"
        req = Net::HTTP::Get.new uri.request_uri
        if uri.user
          user = CGI.unescape(uri.user)
          password = uri.password ? CGI.unescape(uri.password) : nil
          req.basic_auth(user, password)
        end
        connection.request(uri, req)
      rescue OpenSSL::SSL::SSLError
        raise CertificateFailureError.new(uri)
      rescue *HTTP_ERRORS => e
        Bundler.ui.trace e
        case e.message
        when /host down:/, /getaddrinfo: nodename nor servname provided/
          raise NetworkDownError, "Could not reach host #{uri.host}. Check your network " \
          "connection and try again."
        else
          raise HTTPError, "Network error while fetching #{uri}"
        end
      end

    end
  end
end