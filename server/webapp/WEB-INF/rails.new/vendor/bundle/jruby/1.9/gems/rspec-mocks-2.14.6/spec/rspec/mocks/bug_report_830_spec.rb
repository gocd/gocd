require 'spec_helper'

module RSpec
  module Mocks
    describe 'Calling a method that catches StandardError' do
      class Foo
        def self.foo
          bar
        rescue StandardError
        end
      end

      it 'still reports mock failures' do
        Foo.should_not_receive :bar
        expect {
          Foo.foo
        }.to raise_error(RSpec::Mocks::MockExpectationError)
      end
    end
  end
end
