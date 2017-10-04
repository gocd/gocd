require 'spec_helper'

describe RSpec::Core::Pending do
  describe 'PendingDeclaredInExample' do
    before { allow_deprecation }

    it 'is an alternate reference to SkipDeclaredInExample' do
      expect(::RSpec::Core::Pending::PendingDeclaredInExample).to \
        be(::RSpec::Core::Pending::SkipDeclaredInExample)
    end

    it 'prints a deprecation warning' do
      expect_deprecation_with_call_site(
        __FILE__,
        __LINE__ + 3,
        /PendingDeclaredInExample/
      )
      ::RSpec::Core::Pending::PendingDeclaredInExample
    end

    specify 'the const_missing hook raises for other undefined constants' do
      expect {
        ::RSpec::Core::Pending::SomeUndefinedConst
      }.to raise_error(NameError, /uninitialized constant/)
    end
  end
end
