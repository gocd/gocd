module Marshal
  class << self
    # Duplicates any mock objects before serialization. Otherwise,
    # serialization will fail because methods exist on the singleton class.
    def dump_with_mocks(object, *rest)
      if ::RSpec::Mocks.space.nil? || !::RSpec::Mocks.space.registered?(object) || NilClass === object
        dump_without_mocks(object, *rest)
      else
        unless ::RSpec::Mocks.configuration.marshal_patched?
          RSpec.warn_deprecation(<<-EOS.gsub(/^\s+\|/, ''))
            |Using Marshal.dump on stubbed objects relies on a monkey-patch
            |that is being made opt-in in RSpec 3. To silence this warning
            |please explicitly enable it:
            |
            |RSpec.configure do |rspec|
            |  rspec.mock_with :rspec do |mocks|
            |    mocks.patch_marshal_to_support_partial_doubles = true
            |  end
            |end
            |
            |Called from #{RSpec::CallerFilter.first_non_rspec_line}."
          EOS
        end
        dump_without_mocks(object.dup, *rest)
      end
    end

    alias_method :dump_without_mocks, :dump
    undef_method :dump
    alias_method :dump, :dump_with_mocks
  end
end
