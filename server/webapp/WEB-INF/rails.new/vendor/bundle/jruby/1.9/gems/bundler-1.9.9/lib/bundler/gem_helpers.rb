module Bundler
  module GemHelpers

    GENERIC_CACHE = {}
    GENERICS = [
      [Gem::Platform.new('java'), Gem::Platform.new('java')],
      [Gem::Platform.new('mswin32'), Gem::Platform.new('mswin32')],
      [Gem::Platform.new('mswin64'), Gem::Platform.new('mswin64')],
      [Gem::Platform.new('universal-mingw32'), Gem::Platform.new('universal-mingw32')],
      [Gem::Platform.new('x64-mingw32'), Gem::Platform.new('x64-mingw32')],
      [Gem::Platform.new('x86_64-mingw32'), Gem::Platform.new('x64-mingw32')],
      [Gem::Platform.new('mingw32'), Gem::Platform.new('x86-mingw32')]
    ]

    def generic(p)
      return p if p == Gem::Platform::RUBY

      GENERIC_CACHE[p] ||= begin
        _, found = GENERICS.find do |match, _generic|
          p.os == match.os && (!match.cpu || p.cpu == match.cpu)
        end
        found || Gem::Platform::RUBY
      end
    end
  end
end
