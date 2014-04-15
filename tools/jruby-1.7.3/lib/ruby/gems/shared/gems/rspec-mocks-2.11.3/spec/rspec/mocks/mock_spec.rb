require 'spec_helper'

module RSpec
  module Mocks
    describe Mock do
      before(:each) { @double = double("test double") }
      after(:each)  { @double.rspec_reset }

      it "has method_missing as private" do
        RSpec::Mocks::Mock.private_instance_methods.should include_method(:method_missing)
      end

      it "does not respond_to? method_missing (because it's private)" do
        RSpec::Mocks::Mock.new.should_not respond_to(:method_missing)
      end

      it "reports line number of expectation of unreceived message" do
        expected_error_line = __LINE__; @double.should_receive(:wont_happen).with("x", 3)
        begin
          @double.rspec_verify
          violated
        rescue RSpec::Mocks::MockExpectationError => e
          # NOTE - this regexp ended w/ $, but jruby adds extra info at the end of the line
          e.backtrace[0].should match(/#{File.basename(__FILE__)}:#{expected_error_line}/)
        end
      end

      it "reports line number of expectation of unreceived message after #should_receive after similar stub" do
        @double.stub(:wont_happen)
        expected_error_line = __LINE__; @double.should_receive(:wont_happen).with("x", 3)
        begin
          @double.rspec_verify
          violated
        rescue RSpec::Mocks::MockExpectationError => e
          # NOTE - this regexp ended w/ $, but jruby adds extra info at the end of the line
          e.backtrace[0].should match(/#{File.basename(__FILE__)}:#{expected_error_line}/)
        end
      end

      it "passes when not receiving message specified as not to be received" do
        @double.should_not_receive(:not_expected)
        @double.rspec_verify
      end

      it "passes when not receiving message specified as not to be received with and_return" do
        # NOTE (DC 2012-05-05) calling `and_return` after `should_not_receive` makes no sense
        # and should probably be disallowed.
        @double.should_not_receive(:not_expected).and_return nil
        @double.rspec_verify
      end

      it "passes when receiving message specified as not to be received with different args" do
        @double.should_not_receive(:message).with("unwanted text")
        @double.should_receive(:message).with("other text")
        @double.message "other text"
        @double.rspec_verify
      end

      it "fails when receiving message specified as not to be received" do
        @double.should_not_receive(:not_expected)
        expect {
          @double.not_expected
          violated
        }.to raise_error(
          RSpec::Mocks::MockExpectationError,
          %Q|(Double "test double").not_expected(no args)\n    expected: 0 times\n    received: 1 time|
        )
      end

      it "fails when receiving message specified as not to be received with args" do
        @double.should_not_receive(:not_expected).with("unexpected text")
        expect {
          @double.not_expected("unexpected text")
          violated
        }.to raise_error(
          RSpec::Mocks::MockExpectationError,
          %Q|(Double "test double").not_expected("unexpected text")\n    expected: 0 times\n    received: 1 time|
        )
      end

      it "passes when receiving message specified as not to be received with wrong args" do
        @double.should_not_receive(:not_expected).with("unexpected text")
        @double.not_expected "really unexpected text"
        @double.rspec_verify
      end

      it "allows block to calculate return values" do
        @double.should_receive(:something).with("a","b","c").and_return { |a,b,c| c+b+a }
        @double.something("a","b","c").should eq "cba"
        @double.rspec_verify
      end

      it "allows parameter as return value" do
        @double.should_receive(:something).with("a","b","c").and_return("booh")
        @double.something("a","b","c").should eq "booh"
        @double.rspec_verify
      end

      it "returns the previously stubbed value if no return value is set" do
        @double.stub(:something).with("a","b","c").and_return(:stubbed_value)
        @double.should_receive(:something).with("a","b","c")
        @double.something("a","b","c").should eq :stubbed_value
        @double.rspec_verify
      end

      it "returns nil if no return value is set and there is no previously stubbed value" do
        @double.should_receive(:something).with("a","b","c")
        @double.something("a","b","c").should be_nil
        @double.rspec_verify
      end

      it "raises exception if args don't match when method called" do
        @double.should_receive(:something).with("a","b","c").and_return("booh")
        lambda {
          @double.something("a","d","c")
          violated
        }.should raise_error(RSpec::Mocks::MockExpectationError, "Double \"test double\" received :something with unexpected arguments\n  expected: (\"a\", \"b\", \"c\")\n       got: (\"a\", \"d\", \"c\")")
      end

      describe "even when a similar expectation with different arguments exist" do
        it "raises exception if args don't match when method called, correctly reporting the offending arguments" do
          @double.should_receive(:something).with("a","b","c").once
          @double.should_receive(:something).with("z","x","c").once
          lambda {
            @double.something("a","b","c")
            @double.something("z","x","g")
          }.should raise_error(RSpec::Mocks::MockExpectationError, "Double \"test double\" received :something with unexpected arguments\n  expected: (\"z\", \"x\", \"c\")\n       got: (\"z\", \"x\", \"g\")")
        end
      end

      it "raises exception if args don't match when method called even when the method is stubbed" do
        @double.stub(:something)
        @double.should_receive(:something).with("a","b","c")
        lambda {
          @double.something("a","d","c")
          @double.rspec_verify
        }.should raise_error(RSpec::Mocks::MockExpectationError, "Double \"test double\" received :something with unexpected arguments\n  expected: (\"a\", \"b\", \"c\")\n       got: (\"a\", \"d\", \"c\")")
      end

      it "raises exception if args don't match when method called even when using null_object" do
        @double = double("test double").as_null_object
        @double.should_receive(:something).with("a","b","c")
        lambda {
          @double.something("a","d","c")
          @double.rspec_verify
        }.should raise_error(RSpec::Mocks::MockExpectationError, "Double \"test double\" received :something with unexpected arguments\n  expected: (\"a\", \"b\", \"c\")\n       got: (\"a\", \"d\", \"c\")")
      end

      describe 'with a method that has a default argument' do
        it "raises an exception if the arguments don't match when the method is called, correctly reporting the offending arguments" do
          def @double.method_with_default_argument(arg={}); end
          @double.should_receive(:method_with_default_argument).with({})

          expect {
            @double.method_with_default_argument(nil)
            @double.rspec_verify
          }.to raise_error(RSpec::Mocks::MockExpectationError, "Double \"test double\" received :method_with_default_argument with unexpected arguments\n  expected: ({})\n       got: (nil)")
        end
      end

      it "fails if unexpected method called" do
        lambda {
          @double.something("a","b","c")
          violated
        }.should raise_error(RSpec::Mocks::MockExpectationError, "Double \"test double\" received unexpected message :something with (\"a\", \"b\", \"c\")")
      end

      it "uses block for expectation if provided" do
        @double.should_receive(:something) do | a, b |
          a.should eq "a"
          b.should eq "b"
          "booh"
        end
        @double.something("a", "b").should eq "booh"
        @double.rspec_verify
      end

      it "fails if expectation block fails" do
        @double.should_receive(:something) {| bool | bool.should be_true}
        expect {
          @double.something false
        }.to raise_error(RSpec::Expectations::ExpectationNotMetError)
      end

      context "with Ruby > 1.8.6", :unless => RUBY_VERSION.to_s == '1.8.6' do
        it "passes proc to expectation block without an argument" do
          # We eval this because Ruby 1.8.6's syntax parser barfs on { |&block| ... }
          # and prevents the entire spec suite from running.
          eval("@double.should_receive(:foo) {|&block| block.call.should eq(:bar)}")
          @double.foo { :bar }
        end

        it "passes proc to expectation block with an argument" do
          eval("@double.should_receive(:foo) {|arg, &block| block.call.should eq(:bar)}")
          @double.foo(:arg) { :bar }
        end

        it "passes proc to stub block without an argurment" do
          eval("@double.stub(:foo) {|&block| block.call.should eq(:bar)}")
          @double.foo { :bar }
        end

        it "passes proc to stub block with an argument" do
          eval("@double.stub(:foo) {|arg, &block| block.call.should eq(:bar)}")
          @double.foo(:arg) { :bar }
        end
      end

      it "fails right away when method defined as never is received" do
        @double.should_receive(:not_expected).never
        expect { @double.not_expected }.
          to raise_error(RSpec::Mocks::MockExpectationError,
                         %Q|(Double "test double").not_expected(no args)\n    expected: 0 times\n    received: 1 time|
        )
      end

      it "raises when told to" do
        @double.should_receive(:something).and_raise(StandardError)
        expect { @double.something }.to raise_error(StandardError)
      end

      it "raises RuntimeError by default" do
        @double.should_receive(:something).and_raise
        expect { @double.something }.to raise_error(RuntimeError)
      end

      it "raises instance of submitted Exception" do
        error = RuntimeError.new("error message")
        @double.should_receive(:something).and_raise(error)
        lambda {
          @double.something
        }.should raise_error(RuntimeError, "error message")
      end

      it "raises instance of submitted ArgumentError" do
        error = ArgumentError.new("error message")
        @double.should_receive(:something).and_raise(error)
        lambda {
          @double.something
        }.should raise_error(ArgumentError, "error message")
      end

      it "fails with helpful message if submitted Exception requires constructor arguments" do
        class ErrorWithNonZeroArgConstructor < RuntimeError
          def initialize(i_take_an_argument)
          end
        end

        @double.stub(:something).and_raise(ErrorWithNonZeroArgConstructor)
        lambda {
          @double.something
        }.should raise_error(ArgumentError, /^'and_raise' can only accept an Exception class if an instance/)
      end

      it "raises RuntimeError with submitted message" do
        @double.should_receive(:something).and_raise("error message")
        lambda {
          @double.something
        }.should raise_error(RuntimeError, "error message")
      end

      it "does not raise when told to if args dont match" do
        @double.should_receive(:something).with(2).and_raise(RuntimeError)
        lambda {
          @double.something 1
        }.should raise_error(RSpec::Mocks::MockExpectationError)
      end

      it "throws when told to" do
        @double.should_receive(:something).and_throw(:blech)
        lambda {
          @double.something
        }.should throw_symbol(:blech)
      end

      it "ignores args on any args" do
        @double.should_receive(:something).at_least(:once).with(any_args)
        @double.something
        @double.something 1
        @double.something "a", 2
        @double.something [], {}, "joe", 7
        @double.rspec_verify
      end

      it "fails on no args if any args received" do
        @double.should_receive(:something).with(no_args())
        lambda {
          @double.something 1
        }.should raise_error(RSpec::Mocks::MockExpectationError, "Double \"test double\" received :something with unexpected arguments\n  expected: (no args)\n       got: (1)")
      end

      it "fails when args are expected but none are received" do
        @double.should_receive(:something).with(1)
        lambda {
          @double.something
        }.should raise_error(RSpec::Mocks::MockExpectationError, "Double \"test double\" received :something with unexpected arguments\n  expected: (1)\n       got: (no args)")
      end

      it "returns value from block by default" do
        @double.stub(:method_that_yields).and_yield
        @double.method_that_yields { :returned_obj }.should eq :returned_obj
        @double.rspec_verify
      end

      it "yields 0 args to blocks that take a variable number of arguments" do
        @double.should_receive(:yield_back).with(no_args()).once.and_yield
        a = nil
        @double.yield_back {|*x| a = x}
        a.should eq []
        @double.rspec_verify
      end

      it "yields 0 args multiple times to blocks that take a variable number of arguments" do
        @double.should_receive(:yield_back).once.with(no_args()).once.and_yield.
                                                                    and_yield
        b = []
        @double.yield_back {|*a| b << a}
        b.should eq [ [], [] ]
        @double.rspec_verify
      end

      it "yields one arg to blocks that take a variable number of arguments" do
        @double.should_receive(:yield_back).with(no_args()).once.and_yield(99)
        a = nil
        @double.yield_back {|*x| a = x}
        a.should eq [99]
        @double.rspec_verify
      end

      it "yields one arg 3 times consecutively to blocks that take a variable number of arguments" do
        @double.should_receive(:yield_back).once.with(no_args()).once.and_yield(99).
                                                                    and_yield(43).
                                                                    and_yield("something fruity")
        b = []
        @double.yield_back {|*a| b << a}
        b.should eq [[99], [43], ["something fruity"]]
        @double.rspec_verify
      end

      it "yields many args to blocks that take a variable number of arguments" do
        @double.should_receive(:yield_back).with(no_args()).once.and_yield(99, 27, "go")
        a = nil
        @double.yield_back {|*x| a = x}
        a.should eq [99, 27, "go"]
        @double.rspec_verify
      end

      it "yields many args 3 times consecutively to blocks that take a variable number of arguments" do
        @double.should_receive(:yield_back).once.with(no_args()).once.and_yield(99, :green, "go").
                                                                    and_yield("wait", :amber).
                                                                    and_yield("stop", 12, :red)
        b = []
        @double.yield_back {|*a| b << a}
        b.should eq [[99, :green, "go"], ["wait", :amber], ["stop", 12, :red]]
        @double.rspec_verify
      end

      it "yields single value" do
        @double.should_receive(:yield_back).with(no_args()).once.and_yield(99)
        a = nil
        @double.yield_back {|x| a = x}
        a.should eq 99
        @double.rspec_verify
      end

      it "yields single value 3 times consecutively" do
        @double.should_receive(:yield_back).once.with(no_args()).once.and_yield(99).
                                                                    and_yield(43).
                                                                    and_yield("something fruity")
        b = []
        @double.yield_back {|a| b << a}
        b.should eq [99, 43, "something fruity"]
        @double.rspec_verify
      end

      it "yields two values" do
        @double.should_receive(:yield_back).with(no_args()).once.and_yield('wha', 'zup')
        a, b = nil
        @double.yield_back {|x,y| a=x; b=y}
        a.should eq 'wha'
        b.should eq 'zup'
        @double.rspec_verify
      end

      it "yields two values 3 times consecutively" do
        @double.should_receive(:yield_back).once.with(no_args()).once.and_yield('wha', 'zup').
                                                                    and_yield('not', 'down').
                                                                    and_yield(14, 65)
        c = []
        @double.yield_back {|a,b| c << [a, b]}
        c.should eq [['wha', 'zup'], ['not', 'down'], [14, 65]]
        @double.rspec_verify
      end

      it "fails when calling yielding method with wrong arity" do
        @double.should_receive(:yield_back).with(no_args()).once.and_yield('wha', 'zup')
        lambda {
          @double.yield_back {|a|}
        }.should raise_error(RSpec::Mocks::MockExpectationError, "Double \"test double\" yielded |\"wha\", \"zup\"| to block with arity of 1")
      end

      it "fails when calling yielding method consecutively with wrong arity" do
        @double.should_receive(:yield_back).once.with(no_args()).once.and_yield('wha', 'zup').
                                                                    and_yield('down').
                                                                    and_yield(14, 65)
        lambda {
          c = []
          @double.yield_back {|a,b| c << [a, b]}
        }.should raise_error(RSpec::Mocks::MockExpectationError, "Double \"test double\" yielded |\"down\"| to block with arity of 2")
      end

      it "fails when calling yielding method without block" do
        @double.should_receive(:yield_back).with(no_args()).once.and_yield('wha', 'zup')
        lambda {
          @double.yield_back
        }.should raise_error(RSpec::Mocks::MockExpectationError, "Double \"test double\" asked to yield |[\"wha\", \"zup\"]| but no block was passed")
      end

      it "is able to double send" do
        @double.should_receive(:send).with(any_args)
        @double.send 'hi'
        @double.rspec_verify
      end

      it "is able to raise from method calling yielding double" do
        @double.should_receive(:yield_me).and_yield 44

        lambda {
          @double.yield_me do |x|
            raise "Bang"
          end
        }.should raise_error(StandardError, "Bang")

        @double.rspec_verify
      end

      it "clears expectations after verify" do
        @double.should_receive(:foobar)
        @double.foobar
        @double.rspec_verify
        lambda {
          @double.foobar
        }.should raise_error(RSpec::Mocks::MockExpectationError, %q|Double "test double" received unexpected message :foobar with (no args)|)
      end

      it "restores objects to their original state on rspec_reset" do
        double = double("this is a double")
        double.should_receive(:blah)
        double.rspec_reset
        double.rspec_verify #should throw if reset didn't work
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
        @double.rspec_verify

        lambda { @double.foobar }.should_not raise_error(NameError)
        lambda { @double.foobar }.should raise_error(RSpec::Mocks::MockExpectationError)
      end

      it "temporarily replaces a method stub on a double" do
        @double.stub(:msg).and_return(:stub_value)
        @double.should_receive(:msg).with(:arg).and_return(:double_value)
        @double.msg(:arg).should equal(:double_value)
        @double.msg.should equal(:stub_value)
        @double.msg.should equal(:stub_value)
        @double.rspec_verify
      end

      it "does not require a different signature to replace a method stub" do
        @double.stub(:msg).and_return(:stub_value)
        @double.should_receive(:msg).and_return(:double_value)
        @double.msg(:arg).should equal(:double_value)
        @double.msg.should equal(:stub_value)
        @double.msg.should equal(:stub_value)
        @double.rspec_verify
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
        non_double.msg(:arg).should equal(:double_value)
        non_double.msg.should equal(:stub_value)
        non_double.msg.should equal(:stub_value)
        non_double.rspec_verify
      end

      it "returns the stubbed value when no new value specified" do
        @double.stub(:msg).and_return(:stub_value)
        @double.should_receive(:msg)
        @double.msg.should equal(:stub_value)
        @double.rspec_verify
      end

      it "returns the stubbed value when stubbed with args and no new value specified" do
        @double.stub(:msg).with(:arg).and_return(:stub_value)
        @double.should_receive(:msg).with(:arg)
        @double.msg(:arg).should equal(:stub_value)
        @double.rspec_verify
      end

      it "does not mess with the stub's yielded values when also doubleed" do
        @double.stub(:yield_back).and_yield(:stub_value)
        @double.should_receive(:yield_back).and_yield(:double_value)
        @double.yield_back{|v| v.should eq :double_value }
        @double.yield_back{|v| v.should eq :stub_value }
        @double.rspec_verify
      end

      it "yields multiple values after a similar stub" do
        File.stub(:open).and_yield(:stub_value)
        File.should_receive(:open).and_yield(:first_call).and_yield(:second_call)
        yielded_args = []
        File.open {|v| yielded_args << v }
        yielded_args.should eq [:first_call, :second_call]
        File.open {|v| v.should eq :stub_value }
        File.rspec_verify
      end

      it "assigns stub return values" do
        double = RSpec::Mocks::Mock.new('name', :message => :response)
        double.message.should eq :response
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

        @calls.should eq 1
      end

      it "calls the block after #should_receive after a similar stub" do
        @double.stub(:foo).and_return(:bar)
        @double.should_receive(:foo) { add_call }

        @double.foo

        @calls.should eq 1
      end

      it "calls the block after #once" do
        @double.should_receive(:foo).once { add_call }

        @double.foo

        @calls.should eq 1
      end

      it "calls the block after #twice" do
        @double.should_receive(:foo).twice { add_call }

        @double.foo
        @double.foo

        @calls.should eq 2
      end

      it "calls the block after #times" do
        @double.should_receive(:foo).exactly(10).times { add_call }

        (1..10).each { @double.foo }

        @calls.should eq 10
      end

      it "calls the block after #any_number_of_times" do
        @double.should_receive(:foo).any_number_of_times { add_call }

        (1..7).each { @double.foo }

        @calls.should eq 7
      end

      it "calls the block after #ordered" do
        @double.should_receive(:foo).ordered { add_call }
        @double.should_receive(:bar).ordered { add_call }

        @double.foo
        @double.bar

        @calls.should eq 2
      end
    end

    describe 'string representation generated by #to_s' do
      it 'does not contain < because that might lead to invalid HTML in some situations' do
        double = double("Dog")
        valid_html_str = "#{double}"
        valid_html_str.should_not include('<')
      end
    end

    describe "string representation generated by #to_str" do
      it "looks the same as #to_s" do
        double = double("Foo")
        double.to_str.should eq double.to_s
      end
    end

    describe "double created with no name" do
      it "does not use a name in a failure message" do
        double = double()
        expect {double.foo}.to raise_error(/Double received/)
      end

      it "does respond to initially stubbed methods" do
        double = double(:foo => "woo", :bar => "car")
        double.foo.should eq "woo"
        double.bar.should eq "car"
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
            @double.rspec_reset
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
            @double.rspec_reset
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
            @double.rspec_reset
          end
        end
      end

      context "with a block" do
        context "with matching args" do
          it "returns the result of the block" do
            @double.should_receive(:foo).with('bar') { 'baz' }
            @double.foo('bar').should eq('baz')
          end
        end

        context "with non-matching args" do
          it "fails" do
            @double.should_receive(:foo).with('bar') { 'baz' }
            expect do
              @double.foo('wrong').should eq('baz')
            end.to raise_error(/received :foo with unexpected arguments/)
            @double.rspec_reset
          end
        end
      end
    end

  end
end
