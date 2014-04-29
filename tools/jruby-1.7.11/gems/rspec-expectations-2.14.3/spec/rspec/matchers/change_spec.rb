#Based on patch from Wilson Bilkovich

require 'spec_helper'

class SomethingExpected
  attr_accessor :some_value
end

describe "expect { ... }.to change(actual, message)" do
  context "with a numeric value" do
    before(:each) do
      @instance = SomethingExpected.new
      @instance.some_value = 5
    end

    it "passes when actual is modified by the block" do
      expect {@instance.some_value = 6.0}.to change(@instance, :some_value)
    end

    it "fails when actual is not modified by the block" do
      expect do
        expect {}.to change(@instance, :some_value)
      end.to fail_with("some_value should have changed, but is still 5")
    end

    it "provides a #description" do
      expect(change(@instance, :some_value).description).to eq "change #some_value"
    end
  end

  it "can specify the change of a variable's class" do
    val = nil

    expect {
      val = 42
    }.to change { val.class }.from(NilClass).to(Fixnum)

    expect {
      expect {
        val = "string"
      }.to change { val.class }.from(Fixnum).to(NilClass)
    }.to fail_with(/but is now String/)
  end

  context "with boolean values" do
    before(:each) do
      @instance = SomethingExpected.new
      @instance.some_value = true
    end

    it "passes when actual is modified by the block" do
      expect {@instance.some_value = false}.to change(@instance, :some_value)
    end

    it "fails when actual is not modified by the block" do
      expect do
        expect {}.to change(@instance, :some_value)
      end.to fail_with("some_value should have changed, but is still true")
    end
  end

  context "with nil value" do
    before(:each) do
      @instance = SomethingExpected.new
      @instance.some_value = nil
    end

    it "passes when actual is modified by the block" do
      expect {@instance.some_value = false}.to change(@instance, :some_value)
    end

    it "fails when actual is not modified by the block" do
      expect do
        expect {}.to change(@instance, :some_value)
      end.to fail_with("some_value should have changed, but is still nil")
    end
  end

  context "with an array" do
    before(:each) do
      @instance = SomethingExpected.new
      @instance.some_value = []
    end

    it "passes when actual is modified by the block" do
      expect {@instance.some_value << 1}.to change(@instance, :some_value)
    end

    it "fails when a predicate on the actual fails" do
      expect do
        expect {@instance.some_value << 1}.to change { @instance.some_value }.to be_empty
      end.to fail_with(/result should have been changed to/)
    end

    it "passes when a predicate on the actual passes" do
      @instance.some_value = [1]
      expect {@instance.some_value.pop}.to change { @instance.some_value }.to be_empty
    end

    it "fails when actual is not modified by the block" do
      expect do
        expect {}.to change(@instance, :some_value)
      end.to fail_with("some_value should have changed, but is still []")
    end
  end

  context "with a hash" do
    before(:each) do
      @instance = SomethingExpected.new
      @instance.some_value = {:a => 'a'}
    end

    it "passes when actual is modified by the block" do
      expect {@instance.some_value[:a] = 'A'}.to change(@instance, :some_value)
    end

    it "fails when actual is not modified by the block" do
      expect do
        expect {}.to change(@instance, :some_value)
      end.to fail
    end
  end

  context "with a string" do
    it "passes when actual is modified by the block" do
      string = "ab"
      expect { string << "c" }.to change { string }
    end

    it 'fails when actual is not modified by the block' do
      string = "ab"
      expect {
        expect { }.to change { string }
      }.to fail_with(/should have changed/)
    end
  end

  context "with an arbitrary enumerable" do
    before(:each) do
      @instance = SomethingExpected.new
      @instance.some_value = Class.new do
        include Enumerable

        attr_reader :elements

        def initialize(*elements)
          @elements = elements.dup
        end

        def <<(element)
          elements << element
        end

        def dup
          self.class.new(*elements)
        end

        def ==(other)
          elements == other.elements
        end
      end.new
    end

    it "passes when actual is modified by the block" do
      expect {@instance.some_value << 1}.to change(@instance, :some_value)
    end

    it "fails when actual is not modified by the block" do
      expect do
        expect {}.to change(@instance, :some_value)
      end.to fail_with(/^some_value should have changed, but is still/)
    end

  end
end

describe "expect { ... }.not_to change(actual, message)" do
  before(:each) do
    @instance = SomethingExpected.new
    @instance.some_value = 5
  end

  it "passes when actual is not modified by the block" do
    expect { }.not_to change(@instance, :some_value)
  end

  it "fails when actual is not modified by the block" do
    expect do
      expect {@instance.some_value = 6}.not_to change(@instance, :some_value)
    end.to fail_with("some_value should not have changed, but did change from 5 to 6")
  end
end

describe "expect { ... }.to change { block }" do
  o = SomethingExpected.new
  it_behaves_like "an RSpec matcher", :valid_value => lambda { o.some_value = 5 },
                                      :invalid_value => lambda { } do
    let(:matcher) { change { o.some_value } }
  end

  before(:each) do
    @instance = SomethingExpected.new
    @instance.some_value = 5
  end

  it "passes when actual is modified by the block" do
    expect {@instance.some_value = 6}.to change { @instance.some_value }
  end

  it "fails when actual is not modified by the block" do
    expect do
      expect {}.to change{ @instance.some_value }
    end.to fail_with("result should have changed, but is still 5")
  end

  it "warns if passed a block using do/end instead of {}" do
    expect do
      expect {}.to change do; end
    end.to raise_error(SyntaxError, /block passed to should or should_not/)
  end

  it "provides a #description" do
    expect(change { @instance.some_value }.description).to eq "change #result"
  end
end

describe "expect { ... }.not_to change { block }" do
  before(:each) do
    @instance = SomethingExpected.new
    @instance.some_value = 5
  end

  it "passes when actual is modified by the block" do
    expect {}.not_to change{ @instance.some_value }
  end

  it "fails when actual is not modified by the block" do
    expect do
      expect {@instance.some_value = 6}.not_to change { @instance.some_value }
    end.to fail_with("result should not have changed, but did change from 5 to 6")
  end

  it "warns if passed a block using do/end instead of {}" do
    expect do
      expect {}.not_to change do; end
    end.to raise_error(SyntaxError, /block passed to should or should_not/)
  end
end

describe "expect { ... }.to change(actual, message).by(expected)" do
  before(:each) do
    @instance = SomethingExpected.new
    @instance.some_value = 5
  end

  it "passes when attribute is changed by expected amount" do
    expect { @instance.some_value += 1 }.to change(@instance, :some_value).by(1)
  end

  it "passes when attribute is not changed and expected amount is 0" do
    expect { @instance.some_value += 0 }.to change(@instance, :some_value).by(0)
  end

  it "fails when the attribute is changed by unexpected amount" do
    expect do
      expect { @instance.some_value += 2 }.to change(@instance, :some_value).by(1)
    end.to fail_with("some_value should have been changed by 1, but was changed by 2")
  end

  it "fails when the attribute is changed by unexpected amount in the opposite direction" do
    expect do
      expect { @instance.some_value -= 1 }.to change(@instance, :some_value).by(1)
    end.to fail_with("some_value should have been changed by 1, but was changed by -1")
  end
end

describe "expect { ... }.to change { block }.by(expected)" do
  before(:each) do
    @instance = SomethingExpected.new
    @instance.some_value = 5
  end

  it "passes when attribute is changed by expected amount" do
    expect { @instance.some_value += 1 }.to change{@instance.some_value}.by(1)
  end

  it "fails when the attribute is changed by unexpected amount" do
    expect do
      expect { @instance.some_value += 2 }.to change{@instance.some_value}.by(1)
    end.to fail_with("result should have been changed by 1, but was changed by 2")
  end

  it "fails when the attribute is changed by unexpected amount in the opposite direction" do
    expect do
      expect { @instance.some_value -= 1 }.to change{@instance.some_value}.by(1)
    end.to fail_with("result should have been changed by 1, but was changed by -1")
  end
end

describe "expect { ... }.to change(actual, message).by_at_least(expected)" do
  before(:each) do
    @instance = SomethingExpected.new
    @instance.some_value = 5
  end

  it "passes when attribute is changed by greater than the expected amount" do
    expect { @instance.some_value += 2 }.to change(@instance, :some_value).by_at_least(1)
  end

  it "passes when attribute is changed by the expected amount" do
    expect { @instance.some_value += 2 }.to change(@instance, :some_value).by_at_least(2)
  end

  it "fails when the attribute is changed by less than the expected amount" do
    expect do
      expect { @instance.some_value += 1 }.to change(@instance, :some_value).by_at_least(2)
    end.to fail_with("some_value should have been changed by at least 2, but was changed by 1")
  end

end

describe "expect { ... }.to change { block }.by_at_least(expected)" do
  before(:each) do
    @instance = SomethingExpected.new
    @instance.some_value = 5
  end

  it "passes when attribute is changed by greater than expected amount" do
    expect { @instance.some_value += 2 }.to change{@instance.some_value}.by_at_least(1)
  end

  it "passes when attribute is changed by the expected amount" do
    expect { @instance.some_value += 2 }.to change{@instance.some_value}.by_at_least(2)
  end

  it "fails when the attribute is changed by less than the unexpected amount" do
    expect do
      expect { @instance.some_value += 1 }.to change{@instance.some_value}.by_at_least(2)
    end.to fail_with("result should have been changed by at least 2, but was changed by 1")
  end
end


describe "expect { ... }.to change(actual, message).by_at_most(expected)" do
  before(:each) do
    @instance = SomethingExpected.new
    @instance.some_value = 5
  end

  it "passes when attribute is changed by less than the expected amount" do
    expect { @instance.some_value += 2 }.to change(@instance, :some_value).by_at_most(3)
  end

  it "passes when attribute is changed by the expected amount" do
    expect { @instance.some_value += 2 }.to change(@instance, :some_value).by_at_most(2)
  end

  it "fails when the attribute is changed by greater than the expected amount" do
    expect do
      expect { @instance.some_value += 2 }.to change(@instance, :some_value).by_at_most(1)
    end.to fail_with("some_value should have been changed by at most 1, but was changed by 2")
  end

end

describe "expect { ... }.to change { block }.by_at_most(expected)" do
  before(:each) do
    @instance = SomethingExpected.new
    @instance.some_value = 5
  end

  it "passes when attribute is changed by less than expected amount" do
    expect { @instance.some_value += 2 }.to change{@instance.some_value}.by_at_most(3)
  end

  it "passes when attribute is changed by the expected amount" do
    expect { @instance.some_value += 2 }.to change{@instance.some_value}.by_at_most(2)
  end

  it "fails when the attribute is changed by greater than the unexpected amount" do
    expect do
      expect { @instance.some_value += 2 }.to change{@instance.some_value}.by_at_most(1)
    end.to fail_with("result should have been changed by at most 1, but was changed by 2")
  end
end

describe "expect { ... }.to change(actual, message).from(old)" do
  context "with boolean values" do
    before(:each) do
      @instance = SomethingExpected.new
      @instance.some_value = true
    end

    it "passes when attribute is == to expected value before executing block" do
      expect { @instance.some_value = false }.to change(@instance, :some_value).from(true)
    end

    it "fails when attribute is not == to expected value before executing block" do
      expect do
        expect { @instance.some_value = 'foo' }.to change(@instance, :some_value).from(false)
      end.to fail_with("some_value should have initially been false, but was true")
    end
  end
  context "with non-boolean values" do
    before(:each) do
      @instance = SomethingExpected.new
      @instance.some_value = 'string'
    end

    it "passes when attribute is === to expected value before executing block" do
      expect { @instance.some_value = "astring" }.to change(@instance, :some_value).from("string")
    end

    it "compares the expected and actual values with ===" do
      expected = "string"
      expected.should_receive(:===).and_return true
      expect { @instance.some_value = "astring" }.to change(@instance, :some_value).from(expected)
    end

    it "fails when attribute is not === to expected value before executing block" do
      expect do
        expect { @instance.some_value = "knot" }.to change(@instance, :some_value).from("cat")
      end.to fail_with("some_value should have initially been \"cat\", but was \"string\"")
    end
  end
end

describe "expect { ... }.to change { block }.from(old)" do
  before(:each) do
    @instance = SomethingExpected.new
    @instance.some_value = 'string'
  end

  it "passes when attribute is === to expected value before executing block" do
    expect { @instance.some_value = "astring" }.to change{@instance.some_value}.from("string")
  end

  it "compares the expected and actual values with ===" do
    expected = "string"
    expected.should_receive(:===).and_return true
    expect { @instance.some_value = "astring" }.to change{@instance.some_value}.from(expected)
  end

  it "fails when attribute is not === to expected value before executing block" do
    expect do
      expect { @instance.some_value = "knot" }.to change{@instance.some_value}.from("cat")
    end.to fail_with("result should have initially been \"cat\", but was \"string\"")
  end
end

describe "expect { ... }.to change(actual, message).to(new)" do
  context "with boolean values" do
    before(:each) do
      @instance = SomethingExpected.new
      @instance.some_value = true
    end

    it "passes when attribute is == to expected value after executing block" do
      expect { @instance.some_value = false }.to change(@instance, :some_value).to(false)
    end

    it "fails when attribute is not == to expected value after executing block" do
      expect do
        expect { @instance.some_value = 1 }.to change(@instance, :some_value).from(true).to(false)
      end.to fail_with("some_value should have been changed to false, but is now 1")
    end
  end
  context "with non-boolean values" do
    before(:each) do
      @instance = SomethingExpected.new
      @instance.some_value = 'string'
    end

    it "passes when attribute is === to expected value after executing block" do
      expect { @instance.some_value = "cat" }.to change(@instance, :some_value).to("cat")
    end

    it "compares the expected and actual values with ===" do
      expected = "cat"
      expected.should_receive(:===).and_return true
      expect { @instance.some_value = "cat" }.to change(@instance, :some_value).to(expected)
    end

    it "fails when attribute is not === to expected value after executing block" do
      expect do
        expect { @instance.some_value = "cat" }.to change(@instance, :some_value).from("string").to("dog")
      end.to fail_with("some_value should have been changed to \"dog\", but is now \"cat\"")
    end
  end
end

describe "expect { ... }.to change { block }.to(new)" do
  before(:each) do
    @instance = SomethingExpected.new
    @instance.some_value = 'string'
  end

  it "passes when attribute is === to expected value after executing block" do
    expect { @instance.some_value = "cat" }.to change{@instance.some_value}.to("cat")
  end

  it "compares the expected and actual values with ===" do
    expected = "cat"
    expected.should_receive(:===).and_return true
    expect { @instance.some_value = "cat" }.to change{@instance.some_value}.to(expected)
  end

  it "fails when attribute is not === to expected value after executing block" do
    expect do
      expect { @instance.some_value = "cat" }.to change{@instance.some_value}.from("string").to("dog")
    end.to fail_with("result should have been changed to \"dog\", but is now \"cat\"")
  end
end

describe "expect { ... }.to change(actual, message).from(old).to(new)" do
  before(:each) do
    @instance = SomethingExpected.new
    @instance.some_value = 'string'
  end

  it "passes when #to comes before #from" do
    expect { @instance.some_value = "cat" }.to change(@instance, :some_value).to("cat").from("string")
  end

  it "passes when #from comes before #to" do
    expect { @instance.some_value = "cat" }.to change(@instance, :some_value).from("string").to("cat")
  end

  it "shows the correct messaging when #after and #to are different" do
    expect do
      expect { @instance.some_value = "cat" }.to change(@instance, :some_value).from("string").to("dog")
    end.to fail_with("some_value should have been changed to \"dog\", but is now \"cat\"")
  end

  it "shows the correct messaging when #before and #from are different" do
    expect do
      expect { @instance.some_value = "cat" }.to change(@instance, :some_value).from("not_string").to("cat")
    end.to fail_with("some_value should have initially been \"not_string\", but was \"string\"")
  end
end

describe "expect { ... }.to change { block }.from(old).to(new)" do
  before(:each) do
    @instance = SomethingExpected.new
    @instance.some_value = 'string'
  end

  it "passes when #to comes before #from" do
    expect { @instance.some_value = "cat" }.to change{@instance.some_value}.to("cat").from("string")
  end

  it "passes when #from comes before #to" do
    expect { @instance.some_value = "cat" }.to change{@instance.some_value}.from("string").to("cat")
  end
end

describe RSpec::Matchers::BuiltIn::Change do
  it "works when the receiver has implemented #send" do
    @instance = SomethingExpected.new
    @instance.some_value = "string"
    def @instance.send(*args); raise "DOH! Library developers shouldn't use #send!" end

    expect {
      expect { @instance.some_value = "cat" }.to change(@instance, :some_value)
    }.not_to raise_error
  end
end
