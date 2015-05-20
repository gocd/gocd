require 'spec_helper'

module RSpec
  module Mocks
    describe "A method stub" do
      before(:each) do
        @class = Class.new do
          class << self
            def existing_class_method
              existing_private_class_method
            end

            private
            def existing_private_class_method
              :original_value
            end
          end

          def existing_instance_method
            existing_private_instance_method
          end

          private
          def existing_private_instance_method
            :original_value
          end
        end
        @instance = @class.new
        @stub = Object.new
      end

      describe "using stub" do
        it "returns declared value when message is received" do
          @instance.stub(:msg).and_return(:return_value)
          expect(@instance.msg).to equal(:return_value)
          verify @instance
        end
      end

      describe "using stub!" do
        before do
          allow(RSpec).to receive(:deprecate)
        end

        it "warns of deprecation" do
          expect(RSpec).to receive(:deprecate).with("stub!", :replacement => "stub")
          @instance.stub!(:msg).and_return(:return_value)
        end

        it "returns the declared value when the message is received" do
          @instance.stub!(:msg).and_return(:return_value)
          expect(@instance.msg).to equal(:return_value)
          verify @instance
        end

        it "can be used to stub the example context itself (since `stub` returns a test dobule instead)" do
          stub!(:foo).and_return(5)
          expect(foo).to eq(5)
        end
      end

      describe 'using unstub' do
        it 'removes the message stub' do
          @instance.stub(:msg)
          @instance.unstub(:msg)
          expect { @instance.msg }.to raise_error NoMethodError
        end
      end

      describe 'using unstub!' do
        it 'removes the message stub but warns about deprecation' do
          @instance.stub(:msg)
          RSpec.should_receive(:deprecate).with("unstub!", :replacement => "unstub")
          @instance.unstub!(:msg)
          expect { @instance.msg }.to raise_error NoMethodError
        end
      end

      it "instructs an instance to respond_to the message" do
        @instance.stub(:msg)
        expect(@instance).to respond_to(:msg)
      end

      it "instructs a class object to respond_to the message" do
        @class.stub(:msg)
        expect(@class).to respond_to(:msg)
      end

      it "ignores when expected message is received with no args" do
        @instance.stub(:msg)
        @instance.msg
        expect do
          verify @instance
        end.not_to raise_error
      end

      it "ignores when message is received with args" do
        @instance.stub(:msg)
        @instance.msg(:an_arg)
        expect do
          verify @instance
        end.not_to raise_error
      end

      it "ignores when expected message is not received" do
        @instance.stub(:msg)
        expect do
          verify @instance
        end.not_to raise_error
      end

      it "handles multiple stubbed methods" do
        @instance.stub(:msg1 => 1, :msg2 => 2)
        expect(@instance.msg1).to eq(1)
        expect(@instance.msg2).to eq(2)
      end

      describe "#rspec_reset" do
        it "removes stubbed methods that didn't exist" do
          @instance.stub(:non_existent_method)
          reset @instance
          expect(@instance).not_to respond_to(:non_existent_method)
        end

        it "restores existing instance methods" do
          # See bug reports 8302 adn 7805
          @instance.stub(:existing_instance_method) { :stub_value }
          reset @instance
          expect(@instance.existing_instance_method).to eq(:original_value)
        end

        it "restores existing private instance methods" do
          # See bug reports 8302 adn 7805
          @instance.stub(:existing_private_instance_method) { :stub_value }
          reset @instance
          expect(@instance.send(:existing_private_instance_method)).to eq(:original_value)
        end

        it "restores existing class methods" do
          # See bug reports 8302 adn 7805
          @class.stub(:existing_class_method) { :stub_value }
          reset @class
          expect(@class.existing_class_method).to eq(:original_value)
        end

        it "restores existing private class methods" do
          # See bug reports 8302 adn 7805
          @class.stub(:existing_private_class_method) { :stub_value }
          reset @class
          expect(@class.send(:existing_private_class_method)).to eq(:original_value)
        end

        it "does not remove existing methods that have been stubbed twice" do
          @instance.stub(:existing_instance_method)
          @instance.stub(:existing_instance_method)

          reset @instance

          expect(@instance.existing_instance_method).to eq(:original_value)
        end

        it "correctly restores the visibility of methods whose visibility has been tweaked on the singleton class" do
          # hello is a private method when mixed in, but public on the module
          # itself
          mod = Module.new {
            extend self
            def hello; :hello; end

            private :hello
            class << self; public :hello; end;
          }

          expect(mod.hello).to eq(:hello)

          mod.stub(:hello) { :stub }
          reset mod

          expect(mod.hello).to eq(:hello)
        end

        if RUBY_VERSION >= '2.0.0'
          context "with a prepended module (ruby 2.0.0+)" do
            before do
              mod = Module.new do
                def existing_instance_method
                  "#{super}_prepended".to_sym
                end
              end

              @prepended_class = Class.new(@class) do
                prepend mod

                def non_prepended_method
                  :not_prepended
                end
              end
              @prepended_instance = @prepended_class.new
            end

            it "restores prepended instance methods" do
              allow(@prepended_instance).to receive(:existing_instance_method) { :stubbed }
              expect(@prepended_instance.existing_instance_method).to eq :stubbed

              reset @prepended_instance
              expect(@prepended_instance.existing_instance_method).to eq :original_value_prepended
            end

            it "restores non-prepended instance methods" do
              allow(@prepended_instance).to receive(:non_prepended_method) { :stubbed }
              expect(@prepended_instance.non_prepended_method).to eq :stubbed

              reset @prepended_instance
              expect(@prepended_instance.non_prepended_method).to eq :not_prepended
            end
          end
        end
      end

      it "returns values in order to consecutive calls" do
        @instance.stub(:msg).and_return("1",2,:three)
        expect(@instance.msg).to eq("1")
        expect(@instance.msg).to eq(2)
        expect(@instance.msg).to eq(:three)
      end

      it "keeps returning last value in consecutive calls" do
        @instance.stub(:msg).and_return("1",2,:three)
        expect(@instance.msg).to eq("1")
        expect(@instance.msg).to eq(2)
        expect(@instance.msg).to eq(:three)
        expect(@instance.msg).to eq(:three)
        expect(@instance.msg).to eq(:three)
      end

      it "yields a specified object" do
        @instance.stub(:method_that_yields).and_yield(:yielded_obj)
        current_value = :value_before
        @instance.method_that_yields {|val| current_value = val}
        expect(current_value).to eq :yielded_obj
        verify @instance
      end

      it "yields multiple times with multiple calls to and_yield" do
        @instance.stub(:method_that_yields_multiple_times).and_yield(:yielded_value).
                                                       and_yield(:another_value)
        current_value = []
        @instance.method_that_yields_multiple_times {|val| current_value << val}
        expect(current_value).to eq [:yielded_value, :another_value]
        verify @instance
      end

      it "yields a specified object and return another specified object" do
        yielded_obj = double("my mock")
        yielded_obj.should_receive(:foo).with(:bar)
        @instance.stub(:method_that_yields_and_returns).and_yield(yielded_obj).and_return(:baz)
        expect(@instance.method_that_yields_and_returns { |o| o.foo :bar }).to eq :baz
      end

      it "throws when told to" do
        @stub.stub(:something).and_throw(:up)
        expect { @stub.something }.to throw_symbol(:up)
      end

      it "throws with argument when told to" do
        @stub.stub(:something).and_throw(:up, 'high')
        expect { @stub.something }.to throw_symbol(:up, 'high')
      end

      it "overrides a pre-existing method" do
        @stub.stub(:existing_instance_method).and_return(:updated_stub_value)
        expect(@stub.existing_instance_method).to eq :updated_stub_value
      end

      it "overrides a pre-existing stub" do
        @stub.stub(:foo) { 'bar' }
        @stub.stub(:foo) { 'baz' }
        expect(@stub.foo).to eq 'baz'
      end

      it "allows a stub and an expectation" do
        @stub.stub(:foo).with("bar")
        @stub.should_receive(:foo).with("baz")
        @stub.foo("bar")
        @stub.foo("baz")
      end

      it "calculates return value by executing block passed to #and_return" do
        @stub.stub(:something).with("a","b","c").and_return { |a,b,c| c+b+a }
        expect(@stub.something("a","b","c")).to eq "cba"
        verify @stub
      end
    end

    describe "A method stub with args" do
      before(:each) do
        @stub = Object.new
        @stub.stub(:foo).with("bar")
      end

      it "does not complain if not called" do
      end

      it "does not complain if called with arg" do
        @stub.foo("bar")
      end

      it "complains if called with no arg" do
        expect {
          @stub.foo
        }.to raise_error(/received :foo with unexpected arguments/)
      end

      it "complains if called with other arg", :github_issue => [123,147] do
        expect {
          @stub.foo("other")
        }.to raise_error(/received :foo with unexpected arguments.*Please stub a default value/m)
      end

      it "does not complain if also mocked w/ different args" do
        @stub.should_receive(:foo).with("baz")
        @stub.foo("bar")
        @stub.foo("baz")
      end

      it "complains if also mocked w/ different args AND called w/ a 3rd set of args" do
        @stub.should_receive(:foo).with("baz")
        @stub.foo("bar")
        @stub.foo("baz")
        expect {
          @stub.foo("other")
        }.to raise_error
      end

      it "supports options" do
        @stub.stub(:foo, :expected_from => "bar")
      end

      it 'uses the correct stubbed response when responding to a mock expectation' do
        @stub.stub(:bar) { 15 }
        @stub.stub(:bar).with(:eighteen) { 18 }
        @stub.stub(:bar).with(:thirteen) { 13 }

        @stub.should_receive(:bar).exactly(4).times

        expect(@stub.bar(:blah)).to eq(15)
        expect(@stub.bar(:thirteen)).to eq(13)
        expect(@stub.bar(:eighteen)).to eq(18)
        expect(@stub.bar).to eq(15)
      end
    end

  end
end
