require 'spec_helper'

module RSpec
  module Matchers
    describe DifferentiateBlockMethodTypes do
      let(:differentiator) do
        DifferentiateBlockMethodTypes.new do
          def some_instance_method_1; end
          def self.some_singleton_method_1; end
          define_method(:some_instance_method_2) { }

          if RUBY_VERSION.to_f > 1.8
            define_singleton_method(:some_singleton_method_2) { }
          else
            def self.some_singleton_method_2; end
          end
        end
      end

      it 'differentiates singleton method defs from instance method defs' do
        expect(differentiator.instance_methods).to eq([:some_instance_method_1, :some_instance_method_2])
        expect(differentiator.singleton_methods).to eq([:some_singleton_method_1, :some_singleton_method_2])
      end

      it 'passes the given args through to the block' do
        expect { |b|
          DifferentiateBlockMethodTypes.new(1, 2, &b)
        }.to yield_with_args(1, 2)
      end

      it 'ignores unrecognized DSL methods called in the block' do
        expect {
          DifferentiateBlockMethodTypes.new { foo.bar; some_dsl { nested } }
        }.not_to raise_error
      end
    end
  end
end

