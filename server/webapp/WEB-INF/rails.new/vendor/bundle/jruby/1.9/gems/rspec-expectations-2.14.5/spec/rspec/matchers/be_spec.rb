require 'spec_helper'

describe "expect(...).to be_predicate" do
  it "passes when actual returns true for :predicate?" do
    actual = double("actual", :happy? => true)
    expect(actual).to be_happy
  end

  it "passes when actual returns true for :predicates? (present tense)" do
    actual = double("actual", :exists? => true, :exist? => true)
    expect(actual).to be_exist
  end

  it "fails when actual returns false for :predicate?" do
    actual = double("actual", :happy? => false)
    expect {
      expect(actual).to be_happy
    }.to fail_with("expected happy? to return true, got false")
  end

  it "fails when actual returns false for :predicate?" do
    actual = double("actual", :happy? => nil)
    expect {
      expect(actual).to be_happy
    }.to fail_with("expected happy? to return true, got nil")
  end

  it "fails when actual does not respond to :predicate?" do
    expect {
      expect(Object.new).to be_happy
    }.to raise_error(NameError, /happy\?/)
  end

  it "fails on error other than NameError" do
    actual = double("actual")
    actual.should_receive(:foo?).and_raise("aaaah")
    expect {
      expect(actual).to be_foo
    }.to raise_error(/aaaah/)
  end

  it "fails on error other than NameError (with the present tense predicate)" do
    actual = Object.new
    actual.should_receive(:foos?).and_raise("aaaah")
    expect {
      expect(actual).to be_foo
    }.to raise_error(/aaaah/)
  end

  it "does not support operator chaining like a basic `be` matcher does" do
    matcher = be_happy
    value = double(:happy? => false)
    expect(be_happy == value).to be false
  end
end

describe "expect(...).not_to be_predicate" do
  it "passes when actual returns false for :sym?" do
    actual = double("actual", :happy? => false)
    expect(actual).not_to be_happy
  end

  it "passes when actual returns nil for :sym?" do
    actual = double("actual", :happy? => nil)
    expect(actual).not_to be_happy
  end

  it "fails when actual returns true for :sym?" do
    actual = double("actual", :happy? => true)
    expect {
      expect(actual).not_to be_happy
    }.to fail_with("expected happy? to return false, got true")
  end

  it "fails when actual does not respond to :sym?" do
    expect {
      expect(Object.new).not_to be_happy
    }.to raise_error(NameError)
  end
end

describe "expect(...).to be_predicate(*args)" do
  it "passes when actual returns true for :predicate?(*args)" do
    actual = double("actual")
    actual.should_receive(:older_than?).with(3).and_return(true)
    expect(actual).to be_older_than(3)
  end

  it "fails when actual returns false for :predicate?(*args)" do
    actual = double("actual")
    actual.should_receive(:older_than?).with(3).and_return(false)
    expect {
      expect(actual).to be_older_than(3)
    }.to fail_with("expected older_than?(3) to return true, got false")
  end

  it "fails when actual does not respond to :predicate?" do
    expect {
      expect(Object.new).to be_older_than(3)
    }.to raise_error(NameError)
  end
end

describe "expect(...).not_to be_predicate(*args)" do
  it "passes when actual returns false for :predicate?(*args)" do
    actual = double("actual")
    actual.should_receive(:older_than?).with(3).and_return(false)
    expect(actual).not_to be_older_than(3)
  end

  it "fails when actual returns true for :predicate?(*args)" do
    actual = double("actual")
    actual.should_receive(:older_than?).with(3).and_return(true)
    expect {
      expect(actual).not_to be_older_than(3)
    }.to fail_with("expected older_than?(3) to return false, got true")
  end

  it "fails when actual does not respond to :predicate?" do
    expect {
      expect(Object.new).not_to be_older_than(3)
    }.to raise_error(NameError)
  end
end

describe "expect(...).to be_predicate(&block)" do
  it "passes when actual returns true for :predicate?(&block)" do
    actual = double("actual")
    delegate = double("delegate")
    actual.should_receive(:happy?).and_yield
    delegate.should_receive(:check_happy).and_return(true)
    expect(actual).to be_happy { delegate.check_happy }
  end

  it "fails when actual returns false for :predicate?(&block)" do
    actual = double("actual")
    delegate = double("delegate")
    actual.should_receive(:happy?).and_yield
    delegate.should_receive(:check_happy).and_return(false)
    expect {
      expect(actual).to be_happy { delegate.check_happy }
    }.to fail_with("expected happy? to return true, got false")
  end

  it "fails when actual does not respond to :predicate?" do
    delegate = double("delegate", :check_happy => true)
    expect {
      expect(Object.new).to be_happy { delegate.check_happy }
    }.to raise_error(NameError)
  end
end

describe "expect(...).not_to be_predicate(&block)" do
  it "passes when actual returns false for :predicate?(&block)" do
    actual = double("actual")
    delegate = double("delegate")
    actual.should_receive(:happy?).and_yield
    delegate.should_receive(:check_happy).and_return(false)
    expect(actual).not_to be_happy { delegate.check_happy }
  end

  it "fails when actual returns true for :predicate?(&block)" do
    actual = double("actual")
    delegate = double("delegate")
    actual.should_receive(:happy?).and_yield
    delegate.should_receive(:check_happy).and_return(true)
    expect {
      expect(actual).not_to be_happy { delegate.check_happy }
    }.to fail_with("expected happy? to return false, got true")
  end

  it "fails when actual does not respond to :predicate?" do
    delegate = double("delegate", :check_happy => true)
    expect {
      expect(Object.new).not_to be_happy { delegate.check_happy }
    }.to raise_error(NameError)
  end
end

describe "expect(...).to be_predicate(*args, &block)" do
  it "passes when actual returns true for :predicate?(*args, &block)" do
    actual = double("actual")
    delegate = double("delegate")
    actual.should_receive(:older_than?).with(3).and_yield(3)
    delegate.should_receive(:check_older_than).with(3).and_return(true)
    expect(actual).to be_older_than(3) { |age| delegate.check_older_than(age) }
  end

  it "fails when actual returns false for :predicate?(*args, &block)" do
    actual = double("actual")
    delegate = double("delegate")
    actual.should_receive(:older_than?).with(3).and_yield(3)
    delegate.should_receive(:check_older_than).with(3).and_return(false)
    expect {
      expect(actual).to be_older_than(3) { |age| delegate.check_older_than(age) }
    }.to fail_with("expected older_than?(3) to return true, got false")
  end

  it "fails when actual does not respond to :predicate?" do
    delegate = double("delegate", :check_older_than => true)
    expect {
      expect(Object.new).to be_older_than(3) { |age| delegate.check_older_than(age) }
    }.to raise_error(NameError)
  end
end

describe "expect(...).not_to be_predicate(*args, &block)" do
  it "passes when actual returns false for :predicate?(*args, &block)" do
    actual = double("actual")
    delegate = double("delegate")
    actual.should_receive(:older_than?).with(3).and_yield(3)
    delegate.should_receive(:check_older_than).with(3).and_return(false)
    expect(actual).not_to be_older_than(3) { |age| delegate.check_older_than(age) }
  end

  it "fails when actual returns true for :predicate?(*args, &block)" do
    actual = double("actual")
    delegate = double("delegate")
    actual.should_receive(:older_than?).with(3).and_yield(3)
    delegate.should_receive(:check_older_than).with(3).and_return(true)
    expect {
      expect(actual).not_to be_older_than(3) { |age| delegate.check_older_than(age) }
    }.to fail_with("expected older_than?(3) to return false, got true")
  end

  it "fails when actual does not respond to :predicate?" do
    delegate = double("delegate", :check_older_than => true)
    expect {
      expect(Object.new).not_to be_older_than(3) { |age| delegate.check_older_than(age) }
    }.to raise_error(NameError)
  end
end

describe "expect(...).to be_true" do
  it "passes when actual equal?(true)" do
    expect(true).to be_true
  end

  it "passes when actual is 1" do
    expect(1).to be_true
  end

  it "fails when actual equal?(false)" do
    expect {
      expect(false).to be_true
    }.to fail_with("expected: true value\n     got: false")
  end
end

describe "expect(...).to be_false" do
  it "passes when actual equal?(false)" do
    expect(false).to be_false
  end

  it "passes when actual equal?(nil)" do
    expect(nil).to be_false
  end

  it "fails when actual equal?(true)" do
    expect {
      expect(true).to be_false
    }.to fail_with("expected: false value\n     got: true")
  end
end

describe "expect(...).to be_nil" do
  it "passes when actual is nil" do
    expect(nil).to be_nil
  end

  it "fails when actual is not nil" do
    expect {
      expect(:not_nil).to be_nil
    }.to fail_with(/^expected: nil/)
  end
end

describe "expect(...).not_to be_nil" do
  it "passes when actual is not nil" do
    expect(:not_nil).not_to be_nil
  end

  it "fails when actual is nil" do
    expect {
      expect(nil).not_to be_nil
    }.to fail_with(/^expected: not nil/)
  end
end

describe "expect(...).to be <" do
  it "passes when < operator returns true" do
    expect(3).to be < 4
  end

  it "fails when < operator returns false" do
    expect {
      expect(3).to be < 3
    }.to fail_with("expected: < 3\n     got:   3")
  end

  it "describes itself" do
    expect(be.<(4).description).to eq "be < 4"
  end
end

describe "expect(...).to be <=" do
  it "passes when <= operator returns true" do
    expect(3).to be <= 4
    expect(4).to be <= 4
  end

  it "fails when <= operator returns false" do
    expect {
      expect(3).to be <= 2
    }.to fail_with("expected: <= 2\n     got:    3")
  end
end

describe "expect(...).to be >=" do
  it "passes when >= operator returns true" do
    expect(4).to be >= 4
    expect(5).to be >= 4
  end

  it "fails when >= operator returns false" do
    expect {
      expect(3).to be >= 4
    }.to fail_with("expected: >= 4\n     got:    3")
  end
end

describe "expect(...).to be >" do
  it "passes when > operator returns true" do
    expect(5).to be > 4
  end

  it "fails when > operator returns false" do
    expect {
      expect(3).to be > 4
    }.to fail_with("expected: > 4\n     got:   3")
  end
end

describe "expect(...).to be ==" do
  it "passes when == operator returns true" do
    expect(5).to be == 5
  end

  it "fails when == operator returns false" do
    expect {
      expect(3).to be == 4
    }.to fail_with("expected: == 4\n     got:    3")
  end

  it 'works when the target overrides `#send`' do
    klass = Struct.new(:message) do
      def send
        :message_sent
      end
    end

    msg_1 = klass.new("hello")
    msg_2 = klass.new("hello")
    expect(msg_1).to be == msg_2
  end
end

describe "expect(...).to be =~" do
  it "passes when =~ operator returns true" do
    expect("a string").to be =~ /str/
  end

  it "fails when =~ operator returns false" do
    expect {
      expect("a string").to be =~ /blah/
    }.to fail_with(%Q|expected: =~ /blah/\n     got:    "a string"|)
  end
end

describe "should be =~", :uses_should do
  it "passes when =~ operator returns true" do
    "a string".should be =~ /str/
  end

  it "fails when =~ operator returns false" do
    expect {
      "a string".should be =~ /blah/
    }.to fail_with(%Q|expected: =~ /blah/\n     got:    "a string"|)
  end
end

describe "expect(...).to be ===" do
  it "passes when === operator returns true" do
    expect(Hash).to be === Hash.new
  end

  it "fails when === operator returns false" do
    expect {
      expect(Hash).to be === "not a hash"
    }.to fail_with(%[expected: === "not a hash"\n     got:     Hash])
  end
end

describe "expect(...).not_to with operators" do
  it "coaches user to stop using operators with expect().not_to" do
    expect {
      expect(5).not_to be < 6
    }.to raise_error(/`expect\(actual\).not_to be < 6` not only FAILED,\nit is a bit confusing./m)
  end
end

describe "should_not with operators", :uses_only_should do
  it "coaches user to stop using operators with should_not" do
    lambda {
      5.should_not be < 6
    }.should raise_error(/`actual.should_not be < 6` not only FAILED,\nit is a bit confusing./m)
  end
end

describe "expect(...).to be" do
  it "passes if actual is truthy" do
    expect(true).to be
    expect(1).to be
  end

  it "fails if actual is false" do
    expect {
      expect(false).to be
    }.to fail_with("expected false to evaluate to true")
  end

  it "fails if actual is nil" do
    expect {
      expect(nil).to be
    }.to fail_with("expected nil to evaluate to true")
  end

  it "describes itself" do
    expect(be.description).to eq "be"
  end
end

describe "expect(...).not_to be" do
  it "passes if actual is falsy" do
    expect(false).not_to be
    expect(nil).not_to be
  end

  it "fails on true" do
    expect {
      expect(true).not_to be
    }.to fail_with("expected true to evaluate to false")
  end
end

describe "expect(...).to be(value)" do
  it "delegates to equal" do
    matcher = equal(5)
    self.should_receive(:equal).with(5).and_return(matcher)
    expect(5).to be(5)
  end
end

describe "expect(...).not_to be(value)" do
  it "delegates to equal" do
    matcher = equal(4)
    self.should_receive(:equal).with(4).and_return(matcher)
    expect(5).not_to be(4)
  end
end

describe "'expect(...).to be' with operator" do
  it "includes 'be' in the description" do
    expect((be > 6).description).to match(/be > 6/)
    expect((be >= 6).description).to match(/be >= 6/)
    expect((be <= 6).description).to match(/be <= 6/)
    expect((be < 6).description).to match(/be < 6/)
  end
end


describe "arbitrary predicate with DelegateClass" do
  it "accesses methods defined in the delegating class (LH[#48])" do
    require 'delegate'
    class ArrayDelegate < DelegateClass(Array)
      def initialize(array)
        @internal_array = array
        super(@internal_array)
      end

      def large?
        @internal_array.size >= 5
      end
    end

    delegate = ArrayDelegate.new([1,2,3,4,5,6])
    expect(delegate).to be_large
  end
end

describe "be_a, be_an" do
  it "passes when class matches" do
    expect("foobar").to be_a(String)
    expect([1,2,3]).to be_an(Array)
  end

  it "fails when class does not match" do
    expect("foobar").not_to be_a(Hash)
    expect([1,2,3]).not_to be_an(Integer)
  end
end

describe "be_an_instance_of" do
  it "passes when direct class matches" do
    expect(5).to be_an_instance_of(Fixnum)
  end

  it "fails when class is higher up hierarchy" do
    expect(5).not_to be_an_instance_of(Numeric)
  end
end

