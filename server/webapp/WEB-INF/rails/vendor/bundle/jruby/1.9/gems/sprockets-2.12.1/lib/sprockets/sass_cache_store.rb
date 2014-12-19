require 'sass'

module Sprockets
  class SassCacheStore < ::Sass::CacheStores::Base
    attr_reader :environment

    def initialize(environment)
      @environment = environment
    end

    def _store(key, version, sha, contents)
      environment.cache_set("sass/#{key}", {:version => version, :sha => sha, :contents => contents})
    end

    def _retrieve(key, version, sha)
      if obj = environment.cache_get("sass/#{key}")
        return unless obj[:version] == version
        return unless obj[:sha] == sha
        obj[:contents]
      else
        nil
      end
    end

    def path_to(key)
      key
    end
  end
end
