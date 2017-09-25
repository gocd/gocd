require 'spec_helper'

module RSpec
  module Mocks
    describe Mock do
      before(:each) { @double = double("test double") }
      after(:each)  { reset @double }

      it "has method_missing as private" do
        expect(RSpec::Mocks::Mock.private_instance_methods).to include_method(:method_missing)
      end

      it "does not respond_to? method_missing (because it's private)" do
        expect(RSpec::Mocks::Mock.new).not_to respond_to(:method_missing)
      end

      it "reports line number of expectation of unreceived message" do
        expected_error_line = __LINE__; @double.should_receive(:wont_happen).with("x", 3)
        begin
          verify @double
          violated
        rescue RSpec::Mocks::MockExpectationError => e
          # NOTE - this regexp ended w/ $, but jruby adds extra info at the end of the line
          expect(e.backtrace[0]).to match(/#{File.basename(__FILE__)}:#{expected_error_line}/)
        end
      end

      it "reports line number of expectation of unreceived message after #should_receive after similar stub" do
        @double.stub(:wont_happen)
        expected_error_line = __LINE__; @double.should_receive(:wont_happen).with("x", 3)
        begin
          verify @double
          violated
        rescue RSpec::Mocks::MockExpectationError => e
          # NOTE - this regexp ended w/ $, but jruby adds extra info at the end of the line
          expect(e.backtrace[0]).to match(/#{File.basename(__FILE__)}:#{expected_error_line}/)
        end
      end

      it "passes when not receiving message specified as not to be received" do
        @double.should_not_receive(:not_expected)
        verify @double
      end

      it "prevents confusing double-negative expressions involving `never`" do
        expect {
          @double.should_not_receive(:not_expected).never
        }.to raise_error(/trying to negate it again/)
      end

      def expect_and_return_warning
        expect(RSpec).to receive(:deprecate).with(/`and_return` on a negative message expectation/)
      end

      it "warns when `should_not_receive().and_return` is used" do
        expect_and_return_warning
        @double.should_not_receive(:foo).and_return(1)
      end

      it "warns when `should_receive().never.and_return` is used" do
        expect_and_return_warning
        @double.should_receive(:foo).never.and_return(1)
      end

      it "warns when `expect().not_to receive().and_return` is used" do
        expect_and_return_warning
        expect(@double).not_to receive(:foo).and_return(1)
      end

      it "warns when `expect().to receive().never.and_return` is used" do
        expect_and_return_warning
        expect(@double).to receive(:foo).never.and_return(1)
      end

      it "passes when receiving message specified as not to be received with different args" do
        @double.should_not_receive(:message).with("unwanted text")
        @double.should_receive(:message).with("other text")
        @double.message "other text"
        verify @double
      end

      it "fails when receiving message specified as not to be received" do
        @double.should_not_receive(:not_expected)
        expect {
          @double.not_expected
          violated
        }.to raise_error(
          RSpec::Mocks::MockExpectationError,
          %Q|(Double "test double").not_expected(no args)\n    expected: 0 times with any arguments\n    received: 1 time|
        )
      end

      it "fails when receiving message specified as not to be received with args" do
        @double.should_not_receive(:not_expected).with("unexpected text")
        expect {
          @double.not_expected("unexpected text")
          violated
        }.to raise_error(
          RSpec::Mocks::MockExpectationError,
          %Q|(Double "test double").not_expected("unexpected text")\n    expected: 0 times with arguments: ("unexpected text")\n    received: 1 time with arguments: ("unexpected text")|
        )
      end

      it "fails when array arguments do not match" do
        @double.should_not_receive(:not_expected).with(["do not want"])
        expect {
          @double.not_expected(["do not want"])
          violated
        }.to raise_error(
          RSpec::Mocks::MockExpectationError,
          %Q|(Double "test double").not_expected(["do not want"])\n    expected: 0 times with arguments: (["do not want"])\n    received: 1 time with arguments: (["do not want"])|
        )
      end

      it "passes when receiving message specified as not to be received with wrong args" do
        @double.should_not_receive(:not_expected).with("unexpected text")
        @double.not_expected "really unexpected text"
        verify @double

        @double.should_receive(:not_expected).with("unexpected text").never
        @double.not_expected "really unexpected text"
        verify @double
      end

      it "allows block to calculate return values" do
        @double.should_receive(:something).with("a","b","c").and_return { |a,b,c| c+b+a }
        expect(@double.something("a","b","c")).to eq "cba"
        verify @double
      end

      it "allows parameter as return value" do
        @double.should_receive(:something).with("a","b","c").and_return("booh")
        expect(@double.something("a","b","c")).to eq "booh"
        verify @double
      end

      it "returns the previously stubbed value if no return value is set" do
        @double.stub(:something).with("a","b","c").and_return(:stubbed_value)
        @double.should_receive(:something).with("a","b","c")
        expect(@double.something("a","b","c")).to eq :stubbed_value
        verify @double
      end

      it "returns nil if no return value is set and there is no previously stubbed value" do
        @double.should_receive(:something).with("a","b","c")
        expect(@double.something("a","b","c")).to be_nil
        verify @double
      end

      it "raises exception if args don't match when method called" do
        @double.should_receive(:something).with("a","b","c").and_return("booh")
        expect {
          @double.something("a","d","c")
          violated
        }.to raise_error(RSpec::Mocks::MockExpectationError, "Double \"test double\" received :something with unexpected arguments\n  expected: (\"a\", \"b\", \"c\")\n       got: (\"a\", \"d\", \"c\")")
      end

      describe "even when a similar expectation with different arguments exist" do
        it "raises exception if args don't match when method called, correctly reporting the offending arguments" do
          @double.should_receive(:something).with("a","b","c").once
          @double.should_receive(:something).with("z","x","c").once
          expect {
            @double.something("a","b","c")
            @double.something("z","x","g")
          }.to raise_error(RSpec::Mocks::MockExpectationError, "Double \"test double\" received :something with unexpected arguments\n  expected: (\"z\", \"x\", \"c\")\n       got: (\"z\", \"x\", \"g\")")
        end
      end

      it "raises exception if args don't match when method called even when the method is stubbed" do
        @double.stub(:something)
        @double.should_receive(:something).with("a","b","c")
        expect {
          @double.something("a","d","c")
          verify @double
        }.to raise_error(RSpec::Mocks::MockExpectationError, "Double \"test double\" received :something with unexpected arguments\n  expected: (\"a\", \"b\", \"c\")\n       got: (\"a\", \"d\", \"c\")")
      end

      it "raises exception if args don't match when method called even when using null_object" do
        @double = double("test double").as_null_object
        @double.should_receive(:something).with("a","b","c")
        expect {
          @double.something("a","d","c")
          verify @double
        }.to raise_error(RSpec::Mocks::MockExpectationError, "Double \"test double\" received :something with unexpected arguments\n  expected: (\"a\", \"b\", \"c\")\n       got: (\"a\", \"d\", \"c\")")
      end

      describe 'with a method that has a default argument' do
        it "raises an exception if the arguments don't match when the method is called, correctly reporting the offending arguments" do
          def @double.method_with_default_argument(arg={}); end
          @double.should_receive(:method_with_default_argument).with({})

          expect {
            @double.method_with_default_argument(nil)
            verify @double
          }.to raise_error(RSpec::Mocks::MockExpectationError, "Double \"test double\" received :method_with_default_argument with unexpected arguments\n  expected: ({})\n       got: (nil)")
        end
      end

      it "fails if unexpected method called" do
        expect {
          @double.something("a","b","c")
          violated
        }.to raise_error(RSpec::Mocks::MockExpectationError, "Double \"test double\" received unexpected message :something with (\"a\", \"b\", \"c\")")
      end

      it "uses block for expectation if provided" do
        @double.should_receive(:something) do | a, b |
          expect(a).to eq "a"
          expect(b).to eq "b"
          "booh"
        end
        expect(@double.something("a", "b")).to eq "booh"
        verify @double
      end

      it "fails if expectation block fails" do
        @double.should_receive(:something) do |bool|
          expect(bool).to be_true
        end

        expect {
          @double.something false
        }.to raise_error(RSpec::Expectations::ExpectationNotMetError)
      end

      it "is wrappable in an array" do
        expect( Array(@double) ).to eq([@double])
      end

      it "is wrappable in an array when a null object" do
        expect( Array(@double.as_null_object) ).to eq [@double]
      end

      it "responds to to_ary as a null object" do
        expect(@double.as_null_object.to_ary).to eq nil
      end

      it "responds to to_a as a null object" do
        if RUBY_VERSION.to_f > 1.8
          expect(@double.as_null_object.to_a).to eq nil
        else
          expect(@double.as_null_object.to_a).to eq [@double]
        end
      end

      it "passes proc to expectation block without an argument" do
        @double.should_receive(:foo) { |&block| expect(block.call).to eq(:bar) }
        @double.foo { :bar }
      end

      context "with Ruby > 1.8.6", :unless => RUBY_VERSION.to_s == '1.8.6' do
        it "passes proc to expectation block without an argument" do
          # We eval this because Ruby 1.8.6's syntax parser barfs on { |&block| ... }
          # and prevents the entire spec suite from running.
          eval("@double.should_receive(:foo) {|&block| expect(block.call).to eq(:bar)}")
          @double.foo { :bar }
        end

        it "passes proc to expectation block with an argument" do
          eval("@double.should_receive(:foo) {|arg, &block| expect(block.call).to eq(:bar)}")
          @double.foo(:arg) { :bar }
        end

        it "passes proc to stub block without an argurment" do
          eval("@double.stub(:foo) {|&block| expect(block.call).to eq(:bar)}")
          @double.foo { :bar }
        end

        it "passes proc to stub block with an argument" do
          eval("@double.stub(:foo) {|arg, &block| expect(block.call).to eq(:bar)}")
          @double.foo(:arg) { :bar }
        end
      end

      it "fails right away when method defined as never is received" do
        @double.should_receive(:not_expected).never
        expect { @double.not_expected }.
          to raise_error(RSpec::Mocks::MockExpectationError,
                         %Q|(Double "test double").not_expected(no args)\n    expected: 0 times with any arguments\n    received: 1 time|
        )
      end

      it "raises RuntimeError by default" do
        @double.should_receive(:something).and_raise
        expect { @double.something }.to raise_error(RuntimeError)
      end

      it "raises RuntimeError with a message by default" do
        @double.should_receive(:something).and_raise("error message")
        expect { @double.something }.to raise_error(RuntimeError, "error message")
      end

      it "raises an exception of a given type without an error message" do
        @double.should_receive(:something).and_raise(StandardError)
        expect { @double.something }.to raise_error(StandardError)
      end

      it "raises an exception of a given type with a message" do
        @double.should_receive(:something).and_raise(RuntimeError, "error message")
        expect { @double.something }.to raise_error(RuntimeError, "error message")
      end

      it "raises a given instance of an exception" do
        @double.should_receive(:something).and_raise(RuntimeError.new("error message"))
        expect { @double.something }.to raise_error(RuntimeError, "error message")
      end

      class OutOfGas < StandardError
        attr_reader :amount, :units
        def initialize(amount, units)
          @amount = amount
          @units  = units
        end
      end

      it "raises a given instance of an exception with arguments other than the standard 'message'" do
        @double.should_receive(:something).and_raise(OutOfGas.new(2, :oz))

        begin
          @double.something
          fail "OutOfGas was not raised"
        rescue OutOfGas => e
          expect(e.amount).to eq 2
          expect(e.units).to eq :oz
        end
      end

      it "does not raise when told to if args dont match" do
        @double.should_receive(:something).with(2).and_raise(RuntimeError)
        expect {
          @double.something 1
        }.to raise_error(RSpec::Mocks::MockExpectationError)
      end

      it "throws when told to" do
        @double.should_receive(:something).and_throw(:blech)
        expect {
          @double.something
        }.to throw_symbol(:blech)
      end

      it "ignores args on any args" do
        @double.should_receive(:something).at_least(:once).with(any_args)
        @double.something
        @double.something 1
        @double.something "a", 2
        @double.something [], {}, "joe", 7
        verify @double
      end

      it "fails on no args if any args received" do
        @double.should_receive(:something).with(no_args())
        expect {
          @double.something 1
        }.to raise_error(RSpec::Mocks::MockExpectationError, "Double \"test double\" received :something with unexpected arguments\n  expected: (no args)\n       got: (1)")
      end

      it "fails when args are expected but none are received" do
        @double.should_receive(:something).with(1)
        expect {
          @double.something
        }.to raise_error(RSpec::Mocks::MockExpectationError, "Double \"test double\" received :something with unexpected arguments\n  expected: (1)\n       got: (no args)")
      end

      it "returns value from block by default" do
        @double.stub(:method_that_yields).and_yield
        value = @double.method_that_yields { :returned_obj }
        expect(value).to eq :returned_obj
        verify @double
      end

      it "yields 0 args to blocks that take a variable number of arguments" do
        @double.should_receive(:yield_back).with(no_args()).once.and_yield
        a = nil
        @double.yield_back {|*x| a = x}
        expect(a).to eq []
        verify @double
      end

      it "yields 0 args multiple times to blocks that take a variable number of arguments" do
        @double.should_receive(:yield_back).once.with(no_args()).once.and_yield.
                                                                    and_yield
        b = []
        @double.yield_back {|*a| b << a}
        expect(b).to eq [ [], [] ]
        verify @double
      end

      it "yields one arg to blocks that take a variable number of arguments" do
        @double.should_receive(:yield_back).with(no_args()).once.and_yield(99)
        a = nil
        @double.yield_back {|*x| a = x}
        expect(a).to eq [99]
        verify @double
      end

      it "yields one arg 3 times consecutively to blocks that take a variable number of arguments" do
        @double.should_receive(:yield_back).once.with(no_args()).once.and_yield(99).
                                                                    and_yield(43).
                                                                    and_yield("something fruity")
        b = []
        @double.yield_back {|*a| b << a}
        expect(b).to eq [[99], [43], ["something fruity"]]
        verify @double
      end

      it "yields many args to blocks that take a variable number of arguments" do
        @double.should_receive(:yield_back).with(no_args()).once.and_yield(99, 27, "go")
        a = nil
        @double.yield_back {|*x| a = x}
        expect(a).to eq [99, 27, "go"]
        verify @double
      end

      it "yields many args 3 times consecutively to blocks that take a variable number of arguments" do
        @double.should_receive(:yield_back).once.with(no_args()).once.and_yield(99, :green, "go").
                                                                    and_yield("wait", :amber).
                                                                    and_yield("stop", 12, :red)
        b = []
        @double.yield_back {|*a| b << a}
        expect(b).to eq [[99, :green, "go"], ["wait", :amber], ["stop", 12, :red]]
        verify @double
      end

      it "yields single value" do
        @double.should_receive(:yield_back).with(no_args()).once.and_yield(99)
        a = nil
        @double.yield_back {|x| a = x}
        expect(a).to eq 99
        verify @double
      end

      it "yields single value 3 times consecutively" do
        @double.should_receive(:yield_back).once.with(no_args()).once.and_yield(99).
                                                                    and_yield(43).
                                                                    and_yield("something fruity")
        b = []
        @double.yield_back {|a| b << a}
        expect(b).to eq [99, 43, "something fruity"]
        verify @double
      end

      it "yields two values" do
        @double.should_receive(:yield_back).with(no_args()).once.and_yield('wha', 'zup')
        a, b = nil
        @double.yield_back {|x,y| a=x; b=y}
        expect(a).to eq 'wha'
        expect(b).to eq 'zup'
        verify @double
      end

      it "yields two values 3 times consecutively" do
        @double.should_receive(:yield_back).once.with(no_args()).once.and_yield('wha', 'zup').
                                                                    and_yield('not', 'down').
                                                                    and_yield(14, 65)
        c = []
        @double.yield_back {|a,b| c << [a, b]}
        expect(c).to eq [['wha', 'zup'], ['not', 'down'], [14, 65]]
        verify @double
      end

      it "fails when calling yielding method with wrong arity" do
        @double.should_receive(:yield_back).with(no_args()).once.and_yield('wha', 'zup')
        expect {
          @double.yield_back {|a|}
        }.to raise_error(RSpec::Mocks::MockExpectationError, "Double \"test double\" yielded |\"wha\", \"zup\"| to block with arity of 1")
      end

      it "fails when calling yielding method consecutively with wrong arity" do
        @double.should_receive(:yield_back).once.with(no_args()).once.and_yield('wha', 'zup').
                                                                    and_yield('down').
                                                                    and_yield(14, 65)
        expect {
          c = []
          @double.yield_back {|a,b| c << [a, b]}
        }.to raise_error(RSpec::Mocks::MockExpectationError, "Double \"test double\" yielded |\"down\"| to block with arity of 2")
      end

      it "fails when calling yielding method without block" do
        @double.should_receive(:yield_back).with(no_args()).once.and_yield('wha', 'zup')
        expect {
          @double.yield_back
        }.to raise_error(RSpec::Mocks::MockExpectationError, "Double \"test double\" asked to yield |[\"wha\", \"zup\"]| but no block was passed")
      end

      it "is able to double send" do
        @double.should_receive(:send).with(any_args)
        @double.send 'hi'
        verify @double
      end

      it "is able to raise from method calling yielding double" do
        @double.should_receive(:yield_me).and_yield 44

        expect {
          @double.yield_me do |x|
            raise "Bang"
          end
        }.to raise_error(StandardError, "Bang")

        verify @double
      end

      it "clears expectations after verify" do
        @double.should_receive(:foobar)
        @double.foobar
        verify @double
        expect {
          @double.foobar
        }.to raise_error(RSpec::Mocks::MockExpectationError, %q|Double "test double" received unexpected message :foobar with (no args)|)
      end

      it "restores objects to their original state on rspec_reset" do
        double = double("this is a double")
        double.should_receive(:blah)
        reset double
        verify double #should throw if reset didn't work
      end

      it "works even after method_missing starts raising NameErrors instead of NoMethodErrors" do
        # Object#method_missing throws either NameErrors or NoMethodErrors.
        #
        # On a fresh ruby program Object#method_missing:
        #  * raises a NoMethodError when called directly
        #  * raises a NameError when called indirectly
        #
        # Once Object#method_missing has been called at least once (on any object)
        # it starts behaving differently:
        #  * raises a NameError when called directly
        #  * raises a NameError when called indirectly
        #
        # There was a bug in Mock#method_missing that relied on the fact
        # that calling Object#method_missing directly raises a NoMethodError.
        # This example tests that the bug doesn't exist anymore.


        # Ensures that method_missing always raises NameErrors.
        a_method_that_doesnt_exist rescue


        @double.should_receive(:foobar)
        @double.foobar
        verify @double

        expect { @double.foobar }.to raise_error(RSpec::Mocks::MockExpectationError)
      end

      it "temporarily replaces a method stub on a double" do
        @double.stub(:msg).and_return(:stub_value)
        @double.should_receive(:msg).with(:arg).and_return(:double_value)
        expect(@double.msg(:arg)).to equal(:double_value)
        expect(@double.msg).to equal(:stub_value)
        expect(@double.msg).to equal(:stub_value)
        verify @double
      end

      it "does not require a different signature to replace a method stub" do
        @double.stub(:msg).and_return(:stub_value)
        @double.should_receive(:msg).and_return(:double_value)
        expect(@double.msg(:arg)).to equal(:double_value)
        expect(@double.msg).to equal(:stub_value)
        expect(@double.msg).to equal(:stub_value)
        verify @double
      end

      it "raises an error when a previously stubbed method has a negative expectation" do
        @double.stub(:msg).and_return(:stub_value)
        @double.should_not_receive(:msg)
        expect { @double.msg(:arg) }.to raise_error(RSpec::Mocks::MockExpectationError)
      end

      it "temporarily replaces a method stub on a non-double" do
        non_double = Object.new
        non_double.stub(:msg).and_return(:stub_value)
        non_double.should_receive(:msg).with(:arg).and_return(:double_value)
        expect(non_double.msg(:arg)).to equal(:double_value)
        expect(non_double.msg).to equal(:stub_value)
        expect(non_double.msg).to equal(:stub_value)
        verify non_double
      end

      it "returns the stubbed value when no new value specified" do
        @double.stub(:msg).and_return(:stub_value)
        @double.should_receive(:msg)
        expect(@double.msg).to equal(:stub_value)
        verify @double
      end

      it "returns the stubbed value when stubbed with args and no new value specified" do
        @double.stub(:msg).with(:arg).and_return(:stub_value)
        @double.should_receive(:msg).with(:arg)
        expect(@double.msg(:arg)).to equal(:stub_value)
        verify @double
      end

      it "does not mess with the stub's yielded values when also doubleed" do
        @double.stub(:yield_back).and_yield(:stub_value)
        @double.should_receive(:yield_back).and_yield(:double_value)
        @double.yield_back{|v| expect(v).to eq :double_value }
        @double.yield_back{|v| expect(v).to eq :stub_value }
        verify @double
      end

      it "can yield multiple times when told to do so" do
        @double.stub(:foo).and_yield(:stub_value)
        @double.should_receive(:foo).and_yield(:first_yield).and_yield(:second_yield)

        expect { |b| @double.foo(&b) }.to yield_successive_args(:first_yield, :second_yield)
        expect { |b| @double.foo(&b) }.to yield_with_args(:stub_value)

        verify @double
      end

      it "assigns stub return values" do
        double = RSpec::Mocks::Mock.new('name', :message => :response)
        expect(double.message).to eq :response
      end

    end

    describe "a double message receiving a block" do
      before(:each) do
        @double = double("double")
        @calls = 0
      end

      def add_call
        @calls = @calls + 1
      end

      it "calls the block after #should_receive" do
        @double.should_receive(:foo) { add_call }

        @double.foo

        expect(@calls).to eq 1
      end

      it "calls the block after #should_receive after a similar stub" do
        @double.stub(:foo).and_return(:bar)
        @double.should_receive(:foo) { add_call }

        @double.foo

        expect(@calls).to eq 1
      end

      it "calls the block after #once" do
        @double.should_receive(:foo).once { add_call }

        @double.foo

        expect(@calls).to eq 1
      end

      it "calls the block after #twice" do
        @double.should_receive(:foo).twice { add_call }

        @double.foo
        @double.foo

        expect(@calls).to eq 2
      end

      it "calls the block after #times" do
        @double.should_receive(:foo).exactly(10).times { add_call }

        (1..10).each { @double.foo }

        expect(@calls).to eq 10
      end

      it "calls the block after #any_number_of_times" do
        expect(RSpec).to receive(:deprecate).with("any_number_of_times", :replacement => "stub")

        @double.should_receive(:foo).any_number_of_times { add_call }

        (1..7).each { @double.foo }

        expect(@calls).to eq 7
      end

      it "calls the block after #ordered" do
        @double.should_receive(:foo).ordered { add_call }
        @double.should_receive(:bar).ordered { add_call }

        @double.foo
        @double.bar

        expect(@calls).to eq 2
      end
    end

    describe 'string representation generated by #to_s' do
      it 'does not contain < because that might lead to invalid HTML in some situations' do
        double = double("Dog")
        valid_html_str = "#{double}"
        expect(valid_html_str).not_to include('<')
      end
    end

    describe "string representation generated by #to_str" do
      it "looks the same as #to_s" do
        double = double("Foo")
        expect(double.to_str).to eq double.to_s
      end
    end

    describe "double created with no name" do
      it "does not use a name in a failure message" do
        double = double()
        expect {double.foo}.to raise_error(/Double received/)
      end

      it "does respond to initially stubbed methods" do
        double = double(:foo => "woo", :bar => "car")
        expect(double.foo).to eq "woo"
        expect(double.bar).to eq "car"
      end
    end

    describe "==" do
      it "sends '== self' to the comparison object" do
        first = double('first')
        second = double('second')

        first.should_receive(:==).with(second)
        second == first
      end
    end

    describe "with" do
      before { @double = double('double') }
      context "with args" do
        context "with matching args" do
          it "passes" do
            @double.should_receive(:foo).with('bar')
            @double.foo('bar')
          end
        end

        context "with non-matching args" do
          it "fails" do
            @double.should_receive(:foo).with('bar')
            expect do
              @double.foo('baz')
            end.to raise_error
            reset @double
          end
        end

        context "with non-matching doubles" do
          it "fails" do
            d1 = double('1')
            d2 = double('2')
            @double.should_receive(:foo).with(d1)
            expect do
              @double.foo(d2)
            end.to raise_error
            reset @double
          end
        end

        context "with non-matching doubles as_null_object" do
          it "fails" do
            d1 = double('1').as_null_object
            d2 = double('2').as_null_object
            @double.should_receive(:foo).with(d1)
            expect do
              @double.foo(d2)
            end.to raise_error
            reset @double
          end
        end
      end

      context "with a block" do
        context "with matching args" do
          it "returns the result of the block" do
            @double.should_receive(:foo).with('bar') { 'baz' }
            expect(@double.foo('bar')).to eq('baz')
          end
        end

        context "with non-matching args" do
          it "fails" do
            @double.should_receive(:foo).with('bar') { 'baz' }
            expect do
              expect(@double.foo('wrong')).to eq('baz')
            end.to raise_error(/received :foo with unexpected arguments/)
            reset @double
          end
        end
      end
    end
  end
end

