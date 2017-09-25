require 'spec_helper'

class UnsortableObject
  def initialize(id)
    @id = id
  end

  def inspect
    @id.to_s
  end

  def ==(other)
    false
  end
end

describe "should =~ array", :uses_should do
  it "passes a valid positive expectation" do
    [1, 2].should =~ [2, 1]
  end

  it "fails an invalid positive expectation" do
    expect {
      [1, 2, 3].should =~ [2, 1]
    }.to fail_with(/expected collection contained/)
  end

  context "when the array defines a `=~` method" do
    it 'delegates to that method rather than using the match_array matcher' do
      array = []
      def array.=~(other)
        other == :foo
      end

      array.should =~ :foo
      expect {
        array.should =~ :bar
      }.to fail_with(/expected: :bar/)
    end
  end

  context 'when the array defines a `send` method' do
    it 'still works' do
      array = [1, 2]
      def array.send; :sent; end

      array.should =~ array
    end
  end
end

describe "should_not =~ [:with, :multiple, :args]", :uses_should do
  it "is not supported" do
    expect {
      [1,2,3].should_not =~ [1,2,3]
    }.to fail_with(/Matcher does not support should_not/)
  end
end

describe "using match_array with expect" do
  it "passes a valid positive expectation" do
    expect([1, 2]).to match_array [2, 1]
  end

  it "fails an invalid positive expectation" do
    expect {
      expect([1, 2, 3]).to match_array [2, 1]
    }.to fail_with(/expected collection contained/)
  end
end

describe "expect(array).to match_array other_array" do
  it_behaves_like "an RSpec matcher", :valid_value => [1, 2], :invalid_value => [1] do
    let(:matcher) { match_array([2, 1]) }
  end

  it "passes if target contains all items" do
    expect([1,2,3]).to match_array [1,2,3]
  end

  it "passes if target contains all items out of order" do
    expect([1,3,2]).to match_array [1,2,3]
  end

  it "fails if target includes extra items" do
    expect {
      expect([1,2,3,4]).to match_array [1,2,3]
    }.to fail_with(<<-MESSAGE)
expected collection contained:  [1, 2, 3]
actual collection contained:    [1, 2, 3, 4]
the extra elements were:        [4]
MESSAGE
  end

  it "fails if target is missing items" do
    expect {
      expect([1,2]).to match_array [1,2,3]
    }.to fail_with(<<-MESSAGE)
expected collection contained:  [1, 2, 3]
actual collection contained:    [1, 2]
the missing elements were:      [3]
MESSAGE
  end

  it "fails if target is missing items and has extra items" do
    expect {
      expect([1,2,4]).to match_array [1,2,3]
    }.to fail_with(<<-MESSAGE)
expected collection contained:  [1, 2, 3]
actual collection contained:    [1, 2, 4]
the missing elements were:      [3]
the extra elements were:        [4]
MESSAGE
  end

  it "sorts items in the error message if they all respond to <=>" do
    expect {
      expect([6,2,1,5]).to match_array [4,1,2,3]
    }.to fail_with(<<-MESSAGE)
expected collection contained:  [1, 2, 3, 4]
actual collection contained:    [1, 2, 5, 6]
the missing elements were:      [3, 4]
the extra elements were:        [5, 6]
MESSAGE
  end

    it "does not sort items in the error message if they don't all respond to <=>" do
      expect {
        expect([UnsortableObject.new(2), UnsortableObject.new(1)]).to match_array [UnsortableObject.new(4), UnsortableObject.new(3)]
      }.to fail_with(<<-MESSAGE)
expected collection contained:  [4, 3]
actual collection contained:    [2, 1]
the missing elements were:      [4, 3]
the extra elements were:        [2, 1]
MESSAGE
    end

  it "accurately reports extra elements when there are duplicates" do
    expect {
      expect([1,1,1,5]).to match_array [1,5]
    }.to fail_with(<<-MESSAGE)
expected collection contained:  [1, 5]
actual collection contained:    [1, 1, 1, 5]
the extra elements were:        [1, 1]
MESSAGE
  end

  it "accurately reports missing elements when there are duplicates" do
    expect {
      expect([1,5]).to match_array [1,1,5]
    }.to fail_with(<<-MESSAGE)
expected collection contained:  [1, 1, 5]
actual collection contained:    [1, 5]
the missing elements were:      [1]
MESSAGE
  end
end

describe "expect(...).not_to match_array [:with, :multiple, :args]" do
  it "is not supported" do
    expect {
      expect([1,2,3]).not_to match_array [1,2,3]
    }.to fail_with(/Matcher does not support should_not/)
  end
end

describe "matching against things that aren't arrays" do
  it "fails with nil and the expected error message is given" do
    expect {
      expect(nil).to match_array([1,2,3])
    }.to fail_with(/expected an array/)
  end

  it "fails with a float and the expected error message is given" do
    expect {
      expect((3.7)).to match_array([1,2,3])
    }.to fail_with(/expected an array/)
  end

  it "fails with a string and the expected error message is given" do
    expect {
      expect("I like turtles").to match_array([1,2,3])
    }.to fail_with(/expected an array/)
  end

  context "when using the `should =~` syntax", :uses_should do
    it 'fails with a clear message when given a hash' do
      expect {
        {}.should =~ {}
      }.to fail_with(/expected an array/)
    end
  end
end

