require 'bundler/gem_helpers'

module Bundler
  module MatchPlatform
    include GemHelpers

    def match_platform(p)
      Gem::Platform::RUBY == platform or
      platform.nil? or p == platform or
      generic(Gem::Platform.new(platform)) === p
    end
  end
end
