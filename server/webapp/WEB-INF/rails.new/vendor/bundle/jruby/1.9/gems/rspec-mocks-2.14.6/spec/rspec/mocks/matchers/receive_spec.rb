require 'spec_helper'

module RSpec
  module Mocks
    describe Matchers::Receive do
      include_context "with syntax", :expect

      def verify_all
        ::RSpec::Mocks.space.verify_all
      end

      def reset_all
        ::RSpec::Mocks.space.reset_all
      end

      shared_examples_for "a receive matcher" do |*options|
        it 'allows the caller to configure how the subject responds' do
          wrapped.to receive(:foo).and_return(5)
          expect(receiver.foo).to eq(5)
        end

        it 'allows the caller to constrain the received arguments' do
          wrapped.to receive(:foo).with(:a)
          receiver.foo(:a)

          expect {
            receiver.foo(:b)
          }.to raise_error(/received :foo with unexpected arguments/)
        end

        it 'allows a `do...end` block implementation to be provided' do
          wrapped.to receive(:foo) do
            4
          end

          expect(receiver.foo).to eq(4)
        end

        it 'allows chaining off a `do...end` block implementation to be provided' do
          wrapped.to receive(:foo) do
            4
          end.and_return(6)

          expect(receiver.foo).to eq(6)
        end

        it 'allows a `{ ... }` block implementation to be provided' do
          wrapped.to receive(:foo) { 5 }
          expect(receiver.foo).to eq(5)
        end

        it 'gives precedence to a `{ ... }` block when both forms are provided ' +
           'since that form actually binds to `receive`' do
          wrapped.to receive(:foo) { :curly } do
            :do_end
          end

          expect(receiver.foo).to eq(:curly)
        end

        it 'does not support other matchers', :unless => options.include?(:allow_other_matchers) do
          expect {
            wrapped.to eq(3)
          }.to raise_error(UnsupportedMatcherError)
        end
      end

      shared_examples_for "an expect syntax allowance" do |*options|
        include_examples "a receive matcher", *options

        it 'does not expect the message to be received' do
          wrapped.to receive(:foo)
          expect { verify_all }.not_to raise_error
        end
      end

      shared_examples_for "an expect syntax negative allowance" do
        it 'is disabled since this expression is confusing' do
          expect {
            wrapped.not_to receive(:foo)
          }.to raise_error(/not_to receive` is not supported/)

          expect {
            wrapped.to_not receive(:foo)
          }.to raise_error(/to_not receive` is not supported/)
        end
      end

      shared_examples_for "an expect syntax expectation" do |*options|
        include_examples "a receive matcher", *options

        it 'sets up a message expectation that passes if the message is received' do
          wrapped.to receive(:foo)
          receiver.foo
          verify_all
        end

        it 'sets up a message expectation that fails if the message is not received' do
          wrapped.to receive(:foo)

          expect {
            verify_all
          }.to raise_error(RSpec::Mocks::MockExpectationError)

          reset_all
        end

        it "reports the line number of expectation of unreceived message", :pending => options.include?(:does_not_report_line_num) do
          expected_error_line = __LINE__; wrapped.to receive(:foo)

          expect {
            verify_all
          }.to raise_error { |e|
            expect(e.backtrace.first).to match(/#{File.basename(__FILE__)}:#{expected_error_line}/)
          }
        end
      end

      shared_examples_for "an expect syntax negative expectation" do
        it 'sets up a negaive message expectation that passes if the message is not received' do
          wrapped.not_to receive(:foo)
          verify_all
        end

        it 'sets up a negative message expectation that fails if the message is received' do
          wrapped.not_to receive(:foo)

          expect {
            receiver.foo
          }.to raise_error(/expected: 0 times.*received: 1 time/m)
        end

        it 'supports `to_not` as an alias for `not_to`' do
          wrapped.to_not receive(:foo)

          expect {
            receiver.foo
          }.to raise_error(/expected: 0 times.*received: 1 time/m)
        end

        it 'allows the caller to constrain the received arguments' do
          wrapped.not_to receive(:foo).with(:a)
          def receiver.method_missing(*a); end # a poor man's stub...

          expect {
            receiver.foo(:b)
          }.not_to raise_error

          expect {
            receiver.foo(:a)
          }.to raise_error(/expected: 0 times.*received: 1 time/m)
        end

        it 'prevents confusing double-negative expressions involving `never`' do
          expect {
            wrapped.not_to receive(:foo).never
          }.to raise_error(/trying to negate it again/)

          expect {
            wrapped.to_not receive(:foo).never
          }.to raise_error(/trying to negate it again/)
        end
      end

      describe "allow(...).to receive" do
        include_examples "an expect syntax allowance" do
          let(:receiver) { double }
          let(:wrapped)  { allow(receiver) }
        end
      end

      describe "allow(...).not_to receive" do
        include_examples "an expect syntax negative allowance" do
          let(:wrapped) { allow(double) }
        end
      end

      describe "allow_any_instance_of(...).to receive" do
        include_examples "an expect syntax allowance" do
          let(:klass)    { Class.new }
          let(:wrapped)  { allow_any_instance_of(klass) }
          let(:receiver) { klass.new }
        end
      end

      describe "allow_any_instance_of(...).not_to receive" do
        include_examples "an expect syntax negative allowance" do
          let(:wrapped) { allow_any_instance_of(Class.new) }
        end
      end

      describe "expect(...).to receive" do
        include_examples "an expect syntax expectation", :allow_other_matchers do
          let(:receiver) { double }
          let(:wrapped)  { expect(receiver) }
        end
      end

      describe "expect_any_instance_of(...).to receive" do
        include_examples "an expect syntax expectation", :does_not_report_line_num do
          let(:klass)    { Class.new }
          let(:wrapped)  { expect_any_instance_of(klass) }
          let(:receiver) { klass.new }
        end
      end

      describe "expect(...).not_to receive" do
        include_examples "an expect syntax negative expectation" do
          let(:receiver) { double }
          let(:wrapped)  { expect(receiver) }
        end
      end

      describe "expect_any_instance_of(...).not_to receive" do
        include_examples "an expect syntax negative expectation" do
          let(:klass)    { Class.new }
          let(:wrapped)  { expect_any_instance_of(klass) }
          let(:receiver) { klass.new }
        end
      end

      shared_examples "using rspec-mocks in another test framework" do
        it 'can use the `expect` syntax' do
          dbl = double

          framework.new.instance_eval do
            expect(dbl).to receive(:foo).and_return(3)
          end

          expect(dbl.foo).to eq(3)
        end

        it 'expects the method to be called when `expect` is used' do
          dbl = double

          framework.new.instance_eval do
            expect(dbl).to receive(:foo)
          end

          expect { verify dbl }.to raise_error(RSpec::Mocks::MockExpectationError)
        end

        it 'supports `expect(...).not_to receive`' do
          dbl = double

          framework.new.instance_eval do
            expect(dbl).not_to receive(:foo)
          end

          expect { dbl.foo }.to raise_error(RSpec::Mocks::MockExpectationError)
        end

        it 'supports `expect(...).to_not receive`' do
          dbl = double

          framework.new.instance_eval do
            expect(dbl).to_not receive(:foo)
          end

          expect { dbl.foo }.to raise_error(RSpec::Mocks::MockExpectationError)
        end
      end

      context "when used in a test framework without rspec-expectations" do
        let(:framework) do
          Class.new do
            include RSpec::Mocks::ExampleMethods

            def eq(value)
              double("MyMatcher")
            end
          end
        end

        include_examples "using rspec-mocks in another test framework"

        it 'cannot use `expect` with another matcher' do
          expect {
            framework.new.instance_eval do
              expect(3).to eq(3)
            end
          }.to raise_error(/only the `receive` matcher is supported/)
        end

        it 'can toggle the available syntax' do
          expect(framework.new).to respond_to(:expect)
          RSpec::Mocks.configuration.syntax = :should
          expect(framework.new).not_to respond_to(:expect)
          RSpec::Mocks.configuration.syntax = :expect
          expect(framework.new).to respond_to(:expect)
        end

        after { RSpec::Mocks.configuration.syntax = :expect }
      end

      context "when rspec-expectations is included in the test framework first" do
        before do
          # the examples here assume `expect` is define in RSpec::Matchers...
          expect(RSpec::Matchers.method_defined?(:expect)).to be_true
        end

        let(:framework) do
          Class.new do
            include RSpec::Matchers
            include RSpec::Mocks::ExampleMethods
          end
        end

        include_examples "using rspec-mocks in another test framework"

        it 'can use `expect` with any matcher' do
          framework.new.instance_eval do
            expect(3).to eq(3)
          end
        end
      end

      context "when rspec-expectations is included in the test framework last" do
        before do
          # the examples here assume `expect` is define in RSpec::Matchers...
          expect(RSpec::Matchers.method_defined?(:expect)).to be_true
        end

        let(:framework) do
          Class.new do
            include RSpec::Mocks::ExampleMethods
            include RSpec::Matchers
          end
        end

        include_examples "using rspec-mocks in another test framework"

        it 'can use `expect` with any matcher' do
          framework.new.instance_eval do
            expect(3).to eq(3)
          end
        end
      end
    end
  end
end

