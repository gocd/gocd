module Bundler
  class Fetcher
    class Base
      attr_reader :downloader
      attr_reader :remote_uri
      attr_reader :fetch_uri
      attr_reader :display_uri

      def initialize(downloader, remote_uri, fetch_uri, display_uri)
        raise 'Abstract class' if self.class == Base
        @downloader = downloader
        @remote_uri = remote_uri
        @fetch_uri = fetch_uri
        @display_uri = display_uri
      end

      def api_available?
        api_fetcher?
      end

      def api_fetcher?
        false
      end

    end
  end
end
