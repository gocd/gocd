module RSpec
  module Matchers

    private

    def method_missing(method, *args, &block)
      return Matchers::BuiltIn::BePredicate.new(method, *args, &block) if method.to_s =~ /^be_/
      return Matchers::BuiltIn::Has.new(method, *args, &block) if method.to_s =~ /^have_/
      super
    end
  end
end
