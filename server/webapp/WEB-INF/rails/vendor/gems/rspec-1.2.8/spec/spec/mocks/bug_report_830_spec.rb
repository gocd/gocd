require File.dirname(__FILE__) + '/../../spec_helper.rb'

module Spec
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
        lambda do
          Foo.foo
        end.should raise_error(MockExpectationError)
      end
    end
  end
end
