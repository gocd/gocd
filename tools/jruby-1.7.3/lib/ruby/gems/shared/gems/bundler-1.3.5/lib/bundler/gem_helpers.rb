module Bundler
  module GemHelpers

    GENERIC_CACHE = {}
    GENERICS = [
      Gem::Platform.new('java'),
      Gem::Platform.new('mswin32'),
      Gem::Platform.new('x86-mingw32'),
      Gem::Platform::RUBY
    ]

    def generic(p)
      return p if p == Gem::Platform::RUBY

      GENERIC_CACHE[p] ||= begin
        found = GENERICS.find do |p2|
          p2.is_a?(Gem::Platform) && p.os == p2.os
        end
        found || Gem::Platform::RUBY
      end
    end
  end
end
