module Marshal
  class << self
    # Duplicates any mock objects before serialization. Otherwise,
    # serialization will fail because methods exist on the singleton class.
    def dump_with_mocks(object, *rest)
      if ::RSpec::Mocks.space.nil? || !::RSpec::Mocks.space.registered?(object) || NilClass === object
        dump_without_mocks(object, *rest)
      else
        dump_without_mocks(object.dup, *rest)
      end
    end

    alias_method :dump_without_mocks, :dump
    undef_method :dump
    alias_method :dump, :dump_with_mocks
  end
end
