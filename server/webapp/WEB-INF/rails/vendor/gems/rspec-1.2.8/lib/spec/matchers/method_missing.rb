module Spec
  module Matchers
    def method_missing(sym, *args, &block) # :nodoc:
      return Matchers::Be.new(sym, *args) if sym.to_s =~ /^be_/
      return Matchers::Has.new(sym, *args) if sym.to_s =~ /^have_/
      super
    end
  end
end
