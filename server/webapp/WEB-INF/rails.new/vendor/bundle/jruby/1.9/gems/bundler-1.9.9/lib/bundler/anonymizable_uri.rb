module Bundler
  class AnonymizableURI
    attr_reader :original_uri,
                :without_credentials

    def initialize(original_uri, fallback_auth = nil)
      @original_uri = apply_auth(original_uri, fallback_auth).freeze
      @without_credentials = remove_auth(@original_uri).freeze
    end

  private

    def apply_auth(uri, auth = nil)
      if auth && uri.userinfo.nil?
        uri = uri.dup
        uri.userinfo = auth
      end

      uri
    end

    def remove_auth(uri)
      if uri.userinfo
        uri = uri.dup
        uri.user = uri.password = nil
      end

      uri
    end

  end
end
