require 'bundler/fetcher/base'

module Bundler
  class Fetcher
    class Index < Base
      def specs(_gem_names)
        old_sources = Bundler.rubygems.sources
        Bundler.rubygems.sources = [remote_uri.to_s]
        Bundler.rubygems.fetch_all_remote_specs
      rescue Gem::RemoteFetcher::FetchError, OpenSSL::SSL::SSLError => e
        case e.message
        when /certificate verify failed/
          raise CertificateFailureError.new(display_uri)
        when /401/
          raise AuthenticationRequiredError, remote_uri
        when /403/
          if remote_uri.userinfo
            raise BadAuthenticationError, remote_uri
          else
            raise AuthenticationRequiredError, remote_uri
          end
        else
          Bundler.ui.trace e
          raise HTTPError, "Could not fetch specs from #{display_uri}"
        end
      ensure
        Bundler.rubygems.sources = old_sources
      end
    end
  end
end
