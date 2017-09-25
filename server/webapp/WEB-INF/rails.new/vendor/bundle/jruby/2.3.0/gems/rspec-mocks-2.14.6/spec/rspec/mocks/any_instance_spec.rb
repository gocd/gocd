require 'spec_helper'
require 'delegate'

module RSpec
  module Mocks
    describe "#any_instance" do
      class CustomErrorForAnyInstanceSpec < StandardError;end

      let(:klass) do
        Class.new do
          def existing_method; :existing_method_return_value; end
          def existing_method_with_arguments(arg_one, arg_two = nil); :existing_method_with_arguments_return_value; end
          def another_existing_method; end
          private
          def private_method; :private_method_return_value; end
        end
      end
      let(:existing_method_return_value){ :existing_method_return_value }

      context "invocation order" do
        context "#stub" do
          it "raises an error if 'stub' follows 'with'" do
            expect { klass.any_instance.with("1").stub(:foo) }.to raise_error(NoMethodError)
          end

          it "raises an error if 'with' follows 'and_return'" do
            expect { klass.any_instance.stub(:foo).and_return(1).with("1") }.to raise_error(NoMethodError)
          end

          it "raises an error if 'with' follows 'and_raise'" do
            expect { klass.any_instance.stub(:foo).and_raise(1).with("1") }.to raise_error(NoMethodError)
          end

          it "raises an error if 'with' follows 'and_yield'" do
            expect { klass.any_instance.stub(:foo).and_yield(1).with("1") }.to raise_error(NoMethodError)
          end
        end

        context "#stub_chain" do
          it "raises an error if 'stub_chain' follows 'any_instance'" do
            expect { klass.any_instance.and_return("1").stub_chain(:foo, :bar) }.to raise_error(NoMethodError)
          end
        end

        context "#should_receive" do
          it "raises an error if 'should_receive' follows 'with'" do
            expect { klass.any_instance.with("1").should_receive(:foo) }.to raise_error(NoMethodError)
          end

          it "raises an error if 'with' follows 'and_return'" do
            pending "see Github issue #42"
            expect { klass.any_instance.should_receive(:foo).and_return(1).with("1") }.to raise_error(NoMethodError)
          end

          it "raises an error if 'with' follows 'and_raise'" do
            pending "see Github issue #42"
            expect { klass.any_instance.should_receive(:foo).and_raise(1).with("1") }.to raise_error(NoMethodError)
          end
        end
      end

      context "with #stub" do
        it "does not suppress an exception when a method that doesn't exist is invoked" do
          klass.any_instance.stub(:foo)
          expect { klass.new.bar }.to raise_error(NoMethodError)
        end

        context 'multiple methods' do
          it "allows multiple methods to be stubbed in a single invocation" do
            klass.any_instance.stub(:foo => 'foo', :bar => 'bar')
            instance = klass.new
            expect(instance.foo).to eq('foo')
            expect(instance.bar).to eq('bar')
          end

          it "adheres to the contract of multiple method stubbing withou any instance" do
            expect(Object.new.stub(:foo => 'foo', :bar => 'bar')).to eq(:foo => 'foo', :bar => 'bar')
            expect(klass.any_instance.stub(:foo => 'foo', :bar => 'bar')).to eq(:foo => 'foo', :bar => 'bar')
          end

          context "allows a chain of methods to be stubbed using #stub_chain" do
            it "given symbols representing the methods" do
              klass.any_instance.stub_chain(:one, :two, :three).and_return(:four)
              expect(klass.new.one.two.three).to eq(:four)
            end

            it "given a hash as the last argument uses the value as the expected return value" do
              klass.any_instance.stub_chain(:one, :two, :three => :four)
              expect(klass.new.one.two.three).to eq(:four)
            end

            it "given a string of '.' separated method names representing the chain" do
              klass.any_instance.stub_chain('one.two.three').and_return(:four)
              expect(klass.new.one.two.three).to eq(:four)
            end
          end
        end

        context "behaves as 'every instance'" do
          it "stubs every instance in the spec" do
            klass.any_instance.stub(:foo).and_return(result = Object.new)
            expect(klass.new.foo).to eq(result)
            expect(klass.new.foo).to eq(result)
          end

          it "stubs instance created before any_instance was called" do
            instance = klass.new
            klass.any_instance.stub(:foo).and_return(result = Object.new)
            expect(instance.foo).to eq(result)
          end

          it 'handles freeze and duplication correctly' do
            String.any_instance.stub(:any_method)

            foo = 'foo'.freeze
            expect(foo.dup.concat 'bar').to eq 'foobar'
          end
        end

        context "with argument matching" do
          before do
            klass.any_instance.stub(:foo).with(:param_one, :param_two).and_return(:result_one)
            klass.any_instance.stub(:foo).with(:param_three, :param_four).and_return(:result_two)
          end

          it "returns the stubbed value when arguments match" do
            instance = klass.new
            expect(instance.foo(:param_one, :param_two)).to eq(:result_one)
            expect(instance.foo(:param_three, :param_four)).to eq(:result_two)
          end

          it "fails the spec with an expectation error when the arguments do not match" do
            expect do
              klass.new.foo(:param_one, :param_three)
            end.to(raise_error(RSpec::Mocks::MockExpectationError))
          end
        end

        context "with multiple stubs" do
          before do
            klass.any_instance.stub(:foo).and_return(1)
            klass.any_instance.stub(:bar).and_return(2)
          end

          it "stubs a method" do
            instance = klass.new
            expect(instance.foo).to eq(1)
            expect(instance.bar).to eq(2)
          end

          it "returns the same value for calls on different instances" do
            expect(klass.new.foo).to eq(klass.new.foo)
            expect(klass.new.bar).to eq(klass.new.bar)
          end
        end

        context "with #and_return" do
          it "stubs a method that doesn't exist" do
            klass.any_instance.stub(:foo).and_return(1)
            expect(klass.new.foo).to eq(1)
          end

          it "stubs a method that exists" do
            klass.any_instance.stub(:existing_method).and_return(1)
            expect(klass.new.existing_method).to eq(1)
          end

          it "returns the same object for calls on different instances" do
            return_value = Object.new
            klass.any_instance.stub(:foo).and_return(return_value)
            expect(klass.new.foo).to be(return_value)
            expect(klass.new.foo).to be(return_value)
          end
        end

        context "with #and_yield" do
          it "yields the value specified" do
            yielded_value = Object.new
            klass.any_instance.stub(:foo).and_yield(yielded_value)
            klass.new.foo{|value| expect(value).to be(yielded_value)}
          end
        end

        context 'with #and_call_original and competing #with' do
          let(:klass) { Struct.new(:a_method) }

          it 'can combine and_call_original, with, and_return' do
            allow_any_instance_of(klass).to receive(:a_method).and_call_original
            allow_any_instance_of(klass).to receive(:a_method).with(:arg).and_return('value')

            expect(klass.new('org').a_method).to eq 'org'
            expect(klass.new.a_method(:arg)).to  eq 'value'
          end

          it 'can combine and_call_original, with, and_return (old syntax)' do
            klass.any_instance.stub(:a_method).and_call_original
            klass.any_instance.stub(:a_method).with(:arg).and_return('value')

            expect(klass.new('org').a_method).to eq 'org'
            expect(klass.new.a_method(:arg)).to  eq 'value'
          end
        end

        context "with #and_raise" do
          it "stubs a method that doesn't exist" do
            klass.any_instance.stub(:foo).and_raise(CustomErrorForAnyInstanceSpec)
            expect { klass.new.foo}.to raise_error(CustomErrorForAnyInstanceSpec)
          end

          it "stubs a method that exists" do
            klass.any_instance.stub(:existing_method).and_raise(CustomErrorForAnyInstanceSpec)
            expect { klass.new.existing_method}.to raise_error(CustomErrorForAnyInstanceSpec)
          end
        end

        context "with a block" do
          it "stubs a method" do
            klass.any_instance.stub(:foo) { 1 }
            expect(klass.new.foo).to eq(1)
          end

          it "returns the same computed value for calls on different instances" do
            klass.any_instance.stub(:foo) { 1 + 2 }
            expect(klass.new.foo).to eq(klass.new.foo)
          end
        end

        context "core ruby objects" do
          it "works uniformly across *everything*" do
            Object.any_instance.stub(:foo).and_return(1)
            expect(Object.new.foo).to eq(1)
          end

          it "works with the non-standard constructor []" do
            Array.any_instance.stub(:foo).and_return(1)
            expect([].foo).to eq(1)
          end

          it "works with the non-standard constructor {}" do
            Hash.any_instance.stub(:foo).and_return(1)
            expect({}.foo).to eq(1)
          end

          it "works with the non-standard constructor \"\"" do
            String.any_instance.stub(:foo).and_return(1)
            expect("".foo).to eq(1)
          end

          it "works with the non-standard constructor \'\'" do
            String.any_instance.stub(:foo).and_return(1)
            expect(''.foo).to eq(1)
          end

          it "works with the non-standard constructor module" do
            Module.any_instance.stub(:foo).and_return(1)
            module RSpec::SampleRspecTestModule;end
            expect(RSpec::SampleRspecTestModule.foo).to eq(1)
          end

          it "works with the non-standard constructor class" do
            Class.any_instance.stub(:foo).and_return(1)
            class RSpec::SampleRspecTestClass;end
            expect(RSpec::SampleRspecTestClass.foo).to eq(1)
          end
        end
      end

      context "with #stub!" do
        it "raises with a message instructing the user to use stub instead" do
          expect do
            klass.any_instance.stub!(:foo)
          end.to raise_error(/Use stub instead/)
        end
      end

      context "with #unstub!" do
        it "raises with a message instructing the user to use unstub instead" do
          expect do
            klass.any_instance.unstub!(:foo)
          end.to raise_error(/Use unstub instead/)
        end
      end

      context "unstub implementation" do
        it "replaces the stubbed method with the original method" do
          klass.any_instance.stub(:existing_method)
          klass.any_instance.unstub(:existing_method)
          expect(klass.new.existing_method).to eq(:existing_method_return_value)
        end

        it "removes all stubs with the supplied method name" do
          klass.any_instance.stub(:existing_method).with(1)
          klass.any_instance.stub(:existing_method).with(2)
          klass.any_instance.unstub(:existing_method)
          expect(klass.new.existing_method).to eq(:existing_method_return_value)
        end

        it "removes stubs even if they have already been invoked" do
          klass.any_instance.stub(:existing_method).and_return(:any_instance_value)
          obj = klass.new
          obj.existing_method
          klass.any_instance.unstub(:existing_method)
          expect(obj.existing_method).to eq(:existing_method_return_value)
        end

        it "removes stubs from sub class after invokation when super class was originally stubbed" do
          klass.any_instance.stub(:existing_method).and_return(:any_instance_value)
          obj = Class.new(klass).new
          expect(obj.existing_method).to eq(:any_instance_value)
          klass.any_instance.unstub(:existing_method)
          expect(obj.existing_method).to eq(:existing_method_return_value)
        end

        it "does not remove any stubs set directly on an instance" do
          klass.any_instance.stub(:existing_method).and_return(:any_instance_value)
          obj = klass.new
          obj.stub(:existing_method).and_return(:local_method)
          klass.any_instance.unstub(:existing_method)
          expect(obj.existing_method).to eq(:local_method)
        end

        it "does not remove any expectations with the same method name" do
          klass.any_instance.should_receive(:existing_method_with_arguments).with(3).and_return(:three)
          klass.any_instance.stub(:existing_method_with_arguments).with(1)
          klass.any_instance.stub(:existing_method_with_arguments).with(2)
          klass.any_instance.unstub(:existing_method_with_arguments)
          expect(klass.new.existing_method_with_arguments(3)).to eq(:three)
        end

        it "raises a MockExpectationError if the method has not been stubbed" do
          expect {
            klass.any_instance.unstub(:existing_method)
          }.to raise_error(RSpec::Mocks::MockExpectationError, 'The method `existing_method` was not stubbed or was already unstubbed')
        end
      end

      context "with #should_not_receive" do
        it "fails if the method is called" do
          klass.any_instance.should_not_receive(:existing_method)
          expect { klass.new.existing_method }.to raise_error(RSpec::Mocks::MockExpectationError)
        end

        it "passes if no method is called" do
          expect { klass.any_instance.should_not_receive(:existing_method) }.to_not raise_error
        end

        it "passes if only a different method is called" do
          klass.any_instance.should_not_receive(:existing_method)
          expect { klass.new.another_existing_method }.to_not raise_error
        end

        context "with constraints" do
          it "fails if the method is called with the specified parameters" do
            klass.any_instance.should_not_receive(:existing_method_with_arguments).with(:argument_one, :argument_two)
            expect {
              klass.new.existing_method_with_arguments(:argument_one, :argument_two)
            }.to raise_error(RSpec::Mocks::MockExpectationError)
          end

          it "passes if the method is called with different parameters" do
            klass.any_instance.should_not_receive(:existing_method_with_arguments).with(:argument_one, :argument_two)
            expect { klass.new.existing_method_with_arguments(:argument_three, :argument_four) }.to_not raise_error
          end
        end

        context 'when used in combination with should_receive' do
          it 'passes if only the expected message is received' do
            klass.any_instance.should_receive(:foo)
            klass.any_instance.should_not_receive(:bar)
            klass.new.foo
            RSpec::Mocks.space.verify_all
            RSpec::Mocks.space.reset_all
          end
        end

        it "prevents confusing double-negative expressions involving `never`" do
          expect {
            klass.any_instance.should_not_receive(:not_expected).never
          }.to raise_error(/trying to negate it again/)
        end
      end

      context "with #should_receive" do
        let(:foo_expectation_error_message) { 'Exactly one instance should have received the following message(s) but didn\'t: foo' }
        let(:existing_method_expectation_error_message) { 'Exactly one instance should have received the following message(s) but didn\'t: existing_method' }

        context "with an expectation is set on a method which does not exist" do
          it "returns the expected value" do
            klass.any_instance.should_receive(:foo).and_return(1)
            expect(klass.new.foo(1)).to eq(1)
          end

          it "fails if an instance is created but no invocation occurs" do
            expect do
              begin
                klass.any_instance.should_receive(:foo)
                klass.new
                RSpec::Mocks.space.verify_all
              ensure
                RSpec::Mocks.space.reset_all
              end
            end.to raise_error(RSpec::Mocks::MockExpectationError, foo_expectation_error_message)
          end

          it "fails if no instance is created" do
            expect do
              begin
                klass.any_instance.should_receive(:foo).and_return(1)
                RSpec::Mocks.space.verify_all
              ensure
                RSpec::Mocks.space.reset_all
              end
            end.to raise_error(RSpec::Mocks::MockExpectationError, foo_expectation_error_message)
          end

          it "fails if no instance is created and there are multiple expectations" do
            expect do
              begin
                klass.any_instance.should_receive(:foo)
                klass.any_instance.should_receive(:bar)
                RSpec::Mocks.space.verify_all
              ensure
                RSpec::Mocks.space.reset_all
              end
            end.to raise_error(RSpec::Mocks::MockExpectationError, 'Exactly one instance should have received the following message(s) but didn\'t: bar, foo')
          end

          it "allows expectations on instances to take priority" do
            klass.any_instance.should_receive(:foo)
            klass.new.foo

            instance = klass.new
            instance.should_receive(:foo).and_return(result = Object.new)
            expect(instance.foo).to eq(result)
          end

          context "behaves as 'exactly one instance'" do
            it "passes if subsequent invocations do not receive that message" do
              klass.any_instance.should_receive(:foo)
              klass.new.foo
              klass.new
            end

            it "fails if the method is invoked on a second instance" do
              instance_one = klass.new
              instance_two = klass.new
              expect do
                klass.any_instance.should_receive(:foo)

                instance_one.foo
                instance_two.foo
              end.to raise_error(RSpec::Mocks::MockExpectationError, "The message 'foo' was received by #{instance_two.inspect} but has already been received by #{instance_one.inspect}")
            end
          end

          context "normal expectations on the class object" do
            it "fail when unfulfilled" do
              expect do
                klass.any_instance.should_receive(:foo)
                klass.should_receive(:woot)
                klass.new.foo
                RSpec::Mocks.space.verify_all
              end.to(raise_error(RSpec::Mocks::MockExpectationError) do |error|
                expect(error.message).not_to eq(existing_method_expectation_error_message)
              end)
            end


            it "pass when expectations are met" do
              klass.any_instance.should_receive(:foo)
              klass.should_receive(:woot).and_return(result = Object.new)
              klass.new.foo
              expect(klass.woot).to eq(result)
            end
          end
        end

        context "with an expectation is set on a method that exists" do
          it "returns the expected value" do
            klass.any_instance.should_receive(:existing_method).and_return(1)
            expect(klass.new.existing_method(1)).to eq(1)
          end

          it "fails if an instance is created but no invocation occurs" do
            expect do
              begin
                klass.any_instance.should_receive(:existing_method)
                klass.new
                RSpec::Mocks.space.verify_all
              ensure
                RSpec::Mocks.space.reset_all
              end
            end.to raise_error(RSpec::Mocks::MockExpectationError, existing_method_expectation_error_message)
          end

          it "fails if no instance is created" do
            expect do
              begin
                klass.any_instance.should_receive(:existing_method)
                RSpec::Mocks.space.verify_all
              ensure
                RSpec::Mocks.space.reset_all
              end
            end.to raise_error(RSpec::Mocks::MockExpectationError, existing_method_expectation_error_message)
          end

          it "fails if no instance is created and there are multiple expectations" do
            expect do
              begin
                klass.any_instance.should_receive(:existing_method)
                klass.any_instance.should_receive(:another_existing_method)
                RSpec::Mocks.space.verify_all
              ensure
                RSpec::Mocks.space.reset_all
              end
            end.to raise_error(RSpec::Mocks::MockExpectationError, 'Exactly one instance should have received the following message(s) but didn\'t: another_existing_method, existing_method')
          end

          context "after any one instance has received a message" do
            it "passes if subsequent invocations do not receive that message" do
              klass.any_instance.should_receive(:existing_method)
              klass.new.existing_method
              klass.new
            end

            it "fails if the method is invoked on a second instance" do
              instance_one = klass.new
              instance_two = klass.new
              expect do
                klass.any_instance.should_receive(:existing_method)

                instance_one.existing_method
                instance_two.existing_method
              end.to raise_error(RSpec::Mocks::MockExpectationError, "The message 'existing_method' was received by #{instance_two.inspect} but has already been received by #{instance_one.inspect}")
            end
          end
        end

        it 'works with a BasicObject subclass that mixes in Kernel', :if => defined?(BasicObject) do
          klass = Class.new(BasicObject) do
            include ::Kernel
            def foo; end
          end

          klass.any_instance.should_receive(:foo)
          klass.new.foo
        end

        it 'works with a SimpleDelegator subclass', :if => (RUBY_VERSION.to_f > 1.8) do
          klass = Class.new(SimpleDelegator) do
            def foo; end
          end

          klass.any_instance.should_receive(:foo)
          klass.new(Object.new).foo
        end

        context "with argument matching" do
          before do
            klass.any_instance.should_receive(:foo).with(:param_one, :param_two).and_return(:result_one)
            klass.any_instance.should_receive(:foo).with(:param_three, :param_four).and_return(:result_two)
          end

          it "returns the expected value when arguments match" do
            instance = klass.new
            expect(instance.foo(:param_one, :param_two)).to eq(:result_one)
            expect(instance.foo(:param_three, :param_four)).to eq(:result_two)
          end

          it "fails when the arguments match but different instances are used" do
            instances = Array.new(2) { klass.new }
            expect do
              expect(instances[0].foo(:param_one, :param_two)).to eq(:result_one)
              expect(instances[1].foo(:param_three, :param_four)).to eq(:result_two)
            end.to raise_error(RSpec::Mocks::MockExpectationError)

            # ignore the fact that should_receive expectations were not met
            instances.each { |instance| reset instance }
          end

          it "is not affected by the invocation of existing methods on other instances" do
            expect(klass.new.existing_method_with_arguments(:param_one, :param_two)).to eq(:existing_method_with_arguments_return_value)
            instance = klass.new
            expect(instance.foo(:param_one, :param_two)).to eq(:result_one)
            expect(instance.foo(:param_three, :param_four)).to eq(:result_two)
          end

          it "fails when arguments do not match" do
            instance = klass.new
            expect do
              instance.foo(:param_one, :param_three)
            end.to raise_error(RSpec::Mocks::MockExpectationError)

            # ignore the fact that should_receive expectations were not met
            reset instance
          end
        end

        context "message count" do
          context "the 'once' constraint" do
            it "passes for one invocation" do
              klass.any_instance.should_receive(:foo).once
              klass.new.foo
            end

            it "fails when no instances are declared" do
              expect do
                begin
                  klass.any_instance.should_receive(:foo).once
                  RSpec::Mocks.space.verify_all
                ensure
                  RSpec::Mocks.space.reset_all
                end
              end.to raise_error(RSpec::Mocks::MockExpectationError, foo_expectation_error_message)
            end

            it "fails when an instance is declared but there are no invocations" do
              expect do
                begin
                  klass.any_instance.should_receive(:foo).once
                  klass.new
                  RSpec::Mocks.space.verify_all
                ensure
                  RSpec::Mocks.space.reset_all
                end
              end.to raise_error(RSpec::Mocks::MockExpectationError, foo_expectation_error_message)
            end

            it "fails for more than one invocation" do
              expect do
                klass.any_instance.should_receive(:foo).once
                instance = klass.new
                2.times { instance.foo }
                verify instance
              end.to raise_error(RSpec::Mocks::MockExpectationError)
            end
          end

          context "the 'twice' constraint" do
            it "passes for two invocations" do
              klass.any_instance.should_receive(:foo).twice
              instance = klass.new
              2.times { instance.foo }
            end

            it "fails for more than two invocations" do
              expect do
                klass.any_instance.should_receive(:foo).twice
                instance = klass.new
                3.times { instance.foo }
                verify instance
              end.to raise_error(RSpec::Mocks::MockExpectationError)
            end
          end

          context "the 'exactly(n)' constraint" do
            it "passes for n invocations where n = 3" do
              klass.any_instance.should_receive(:foo).exactly(3).times
              instance = klass.new
              3.times { instance.foo }
            end

            it "fails for n invocations where n < 3" do
              expect do
                klass.any_instance.should_receive(:foo).exactly(3).times
                instance = klass.new
                2.times { instance.foo }
                verify instance
              end.to raise_error(RSpec::Mocks::MockExpectationError)
            end

            it "fails for n invocations where n > 3" do
              expect do
                klass.any_instance.should_receive(:foo).exactly(3).times
                instance = klass.new
                4.times { instance.foo }
                verify instance
              end.to raise_error(RSpec::Mocks::MockExpectationError)
            end
          end

          context "the 'at_least(n)' constraint" do
            it "passes for n invocations where n = 3" do
              klass.any_instance.should_receive(:foo).at_least(3).times
              instance = klass.new
              3.times { instance.foo }
            end

            it "fails for n invocations where n < 3" do
              expect do
                klass.any_instance.should_receive(:foo).at_least(3).times
                instance = klass.new
                2.times { instance.foo }
                verify instance
              end.to raise_error(RSpec::Mocks::MockExpectationError)
            end

            it "passes for n invocations where n > 3" do
              klass.any_instance.should_receive(:foo).at_least(3).times
              instance = klass.new
              4.times { instance.foo }
            end
          end

          context "the 'at_most(n)' constraint" do
            it "passes for n invocations where n = 3" do
              klass.any_instance.should_receive(:foo).at_most(3).times
              instance = klass.new
              3.times { instance.foo }
            end

            it "passes for n invocations where n < 3" do
              klass.any_instance.should_receive(:foo).at_most(3).times
              instance = klass.new
              2.times { instance.foo }
            end

            it "fails for n invocations where n > 3" do
              expect do
                klass.any_instance.should_receive(:foo).at_most(3).times
                instance = klass.new
                4.times { instance.foo }
                verify instance
              end.to raise_error(RSpec::Mocks::MockExpectationError)
            end
          end

          context "the 'never' constraint" do
            it "passes for 0 invocations" do
              klass.any_instance.should_receive(:foo).never
              RSpec::Mocks.space.verify_all
            end

            it "fails on the first invocation" do
              expect do
                klass.any_instance.should_receive(:foo).never
                klass.new.foo
              end.to raise_error(RSpec::Mocks::MockExpectationError)
            end

            context "when combined with other expectations" do
              it "passes when the other expecations are met" do
                klass.any_instance.should_receive(:foo).never
                klass.any_instance.should_receive(:existing_method).and_return(5)
                expect(klass.new.existing_method).to eq(5)
              end

              it "fails when the other expecations are not met" do
                expect do
                  begin
                    klass.any_instance.should_receive(:foo).never
                    klass.any_instance.should_receive(:existing_method).and_return(5)
                    RSpec::Mocks.space.verify_all
                  ensure
                    RSpec::Mocks.space.reset_all
                  end
                end.to raise_error(RSpec::Mocks::MockExpectationError, existing_method_expectation_error_message)
              end
            end
          end

          context "the 'any_number_of_times' constraint" do
            it "passes for 0 invocations" do
              klass.any_instance.should_receive(:foo).any_number_of_times
              verify klass.new
            end

            it "passes for a non-zero number of invocations" do
              allow(RSpec).to receive(:deprecate).with("any_number_of_times", :replacement => "stub")

              klass.any_instance.should_receive(:foo).any_number_of_times
              instance = klass.new
              instance.foo
              verify instance
            end

            it "does not interfere with other expectations" do
              klass.any_instance.should_receive(:foo).any_number_of_times
              klass.any_instance.should_receive(:existing_method).and_return(5)
              expect(klass.new.existing_method).to eq(5)
            end

            context "when combined with other expectations" do
              it "passes when the other expecations are met" do
                klass.any_instance.should_receive(:foo).any_number_of_times
                klass.any_instance.should_receive(:existing_method).and_return(5)
                expect(klass.new.existing_method).to eq(5)
              end

              it "fails when the other expecations are not met" do
                expect do
                  begin
                    klass.any_instance.should_receive(:foo).any_number_of_times
                    klass.any_instance.should_receive(:existing_method).and_return(5)
                    RSpec::Mocks.space.verify_all
                  ensure
                    RSpec::Mocks.space.reset_all
                  end
                end.to raise_error(RSpec::Mocks::MockExpectationError, existing_method_expectation_error_message)
              end
            end
          end
        end
      end

      context "when resetting post-verification" do
        let(:space) { RSpec::Mocks.space }

        context "existing method" do
          before(:each) do
            klass.any_instance # to force it to be tracked
          end

          context "with stubbing" do
            context "public methods" do
              before(:each) do
                klass.any_instance.stub(:existing_method).and_return(1)
                expect(klass.method_defined?(:__existing_method_without_any_instance__)).to be_true
              end

              it "restores the class to its original state after each example when no instance is created" do
                space.verify_all
                space.reset_all

                expect(klass.method_defined?(:__existing_method_without_any_instance__)).to be_false
                expect(klass.new.existing_method).to eq(existing_method_return_value)
              end

              it "restores the class to its original state after each example when one instance is created" do
                klass.new.existing_method

                space.verify_all
                space.reset_all

                expect(klass.method_defined?(:__existing_method_without_any_instance__)).to be_false
                expect(klass.new.existing_method).to eq(existing_method_return_value)
              end

              it "restores the class to its original state after each example when more than one instance is created" do
                klass.new.existing_method
                klass.new.existing_method

                space.verify_all
                space.reset_all

                expect(klass.method_defined?(:__existing_method_without_any_instance__)).to be_false
                expect(klass.new.existing_method).to eq(existing_method_return_value)
              end
            end

            context "private methods" do
              before :each do
                klass.any_instance.stub(:private_method).and_return(:something)
                space.verify_all
                space.reset_all
              end

              it "cleans up the backed up method" do
                expect(klass.method_defined?(:__existing_method_without_any_instance__)).to be_false
              end

              it "restores a stubbed private method after the spec is run" do
                expect(klass.private_method_defined?(:private_method)).to be_true
              end

              it "ensures that the restored method behaves as it originally did" do
                expect(klass.new.send(:private_method)).to eq(:private_method_return_value)
              end
            end
          end

          context "with expectations" do
            context "private methods" do
              before :each do
                klass.any_instance.should_receive(:private_method).and_return(:something)
                klass.new.private_method
                space.verify_all
                space.reset_all
              end

              it "cleans up the backed up method" do
                expect(klass.method_defined?(:__existing_method_without_any_instance__)).to be_false
              end

              it "restores a stubbed private method after the spec is run" do
                expect(klass.private_method_defined?(:private_method)).to be_true
              end

              it "ensures that the restored method behaves as it originally did" do
                expect(klass.new.send(:private_method)).to eq(:private_method_return_value)
              end
            end

            context "ensures that the subsequent specs do not see expectations set in previous specs" do
              context "when the instance created after the expectation is set" do
                it "first spec" do
                  klass.any_instance.should_receive(:existing_method).and_return(Object.new)
                  klass.new.existing_method
                end

                it "second spec" do
                  expect(klass.new.existing_method).to eq(existing_method_return_value)
                end
              end

              context "when the instance created before the expectation is set" do
                before :each do
                  @instance = klass.new
                end

                it "first spec" do
                  klass.any_instance.should_receive(:existing_method).and_return(Object.new)
                  @instance.existing_method
                end

                it "second spec" do
                  expect(@instance.existing_method).to eq(existing_method_return_value)
                end
              end
            end

            it "ensures that the next spec does not see that expectation" do
              klass.any_instance.should_receive(:existing_method).and_return(Object.new)
              klass.new.existing_method
              space.verify_all
              space.reset_all

              expect(klass.new.existing_method).to eq(existing_method_return_value)
            end
          end
        end

        context "with multiple calls to any_instance in the same example" do
          it "does not prevent the change from being rolled back" do
            klass.any_instance.stub(:existing_method).and_return(false)
            klass.any_instance.stub(:existing_method).and_return(true)

            RSpec::Mocks.space.verify_all
            RSpec::Mocks.space.reset_all
            expect(klass.new).to respond_to(:existing_method)
            expect(klass.new.existing_method).to eq(existing_method_return_value)
          end
        end

        it "adds an class to the current space when #any_instance is invoked" do
          expect {
            klass.any_instance
          }.to change { space.any_instance_recorders.size }.by(1)
        end

        it "adds an instance to the current space when stubbed method is invoked" do
          klass.any_instance.stub(:foo)
          instance = klass.new
          instance.foo
          expect(RSpec::Mocks.space.proxies.keys).to include(instance.object_id)
        end
      end

      context 'when used in conjunction with a `dup`' do
        it "doesn't cause an infinite loop" do
          pending "This intermittently fails on JRuby" if RUBY_PLATFORM == 'java'

          Object.any_instance.stub(:some_method)
          o = Object.new
          o.some_method
          expect { o.dup.some_method }.to_not raise_error
        end

        it "doesn't bomb if the object doesn't support `dup`" do
          klass = Class.new do
            undef_method :dup
          end
          klass.any_instance
        end

        it "doesn't fail when dup accepts parameters" do
          klass = Class.new do
            def dup(funky_option)
            end
          end

          klass.any_instance

          expect { klass.new.dup('Dup dup dup') }.to_not raise_error
        end
      end

      context "when directed at a method defined on a superclass" do
        let(:sub_klass) { Class.new(klass) }

        it "stubs the method correctly" do
          klass.any_instance.stub(:existing_method).and_return("foo")
          expect(sub_klass.new.existing_method).to eq "foo"
        end

        it "mocks the method correctly" do
          instance_one = sub_klass.new
          instance_two = sub_klass.new
          expect do
            klass.any_instance.should_receive(:existing_method)
            instance_one.existing_method
            instance_two.existing_method
          end.to raise_error(RSpec::Mocks::MockExpectationError, "The message 'existing_method' was received by #{instance_two.inspect} but has already been received by #{instance_one.inspect}")
        end
      end

      context "when a class overrides Object#method" do
        let(:http_request_class) { Struct.new(:method, :uri) }

        it "stubs the method correctly" do
          http_request_class.any_instance.stub(:existing_method).and_return("foo")
          expect(http_request_class.new.existing_method).to eq "foo"
        end

        it "mocks the method correctly" do
          http_request_class.any_instance.should_receive(:existing_method).and_return("foo")
          expect(http_request_class.new.existing_method).to eq "foo"
        end
      end

      context "when used after the test has finished" do
        it "restores the original behavior of a stubbed method" do
          klass.any_instance.stub(:existing_method).and_return(:stubbed_return_value)

          instance = klass.new
          expect(instance.existing_method).to eq :stubbed_return_value

          RSpec::Mocks.verify
          RSpec::Mocks.teardown

          expect(instance.existing_method).to eq :existing_method_return_value
        end
      end
    end
  end
end
