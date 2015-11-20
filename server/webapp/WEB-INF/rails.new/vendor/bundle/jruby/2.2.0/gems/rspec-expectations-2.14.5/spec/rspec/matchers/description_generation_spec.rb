require 'spec_helper'

describe "Matchers should be able to generate their own descriptions" do
  after(:each) do
    RSpec::Matchers.clear_generated_description
  end

  it "expect(...).to eq expected" do
    expect("this").to eq "this"
    expect(RSpec::Matchers.generated_description).to eq "should eq \"this\""
  end

  it "expect(...).to not eq expected" do
    expect("this").not_to eq "that"
    expect(RSpec::Matchers.generated_description).to eq "should not eq \"that\""
  end

  it "expect(...).to be empty (arbitrary predicate)" do
    expect([]).to be_empty
    expect(RSpec::Matchers.generated_description).to eq "should be empty"
  end

  it "expect(...).to not be empty (arbitrary predicate)" do
    expect([1]).not_to be_empty
    expect(RSpec::Matchers.generated_description).to eq "should not be empty"
  end

  it "expect(...).to be true" do
    expect(true).to be_true
    expect(RSpec::Matchers.generated_description).to eq "should be true"
  end

  it "expect(...).to be false" do
    expect(false).to be_false
    expect(RSpec::Matchers.generated_description).to eq "should be false"
  end

  it "expect(...).to be nil" do
    expect(nil).to be_nil
    expect(RSpec::Matchers.generated_description).to eq "should be nil"
  end

  it "expect(...).to be > n" do
    expect(5).to be > 3
    expect(RSpec::Matchers.generated_description).to eq "should be > 3"
  end

  it "expect(...).to be predicate arg1, arg2 and arg3" do
    expect(5.0).to be_between(0,10)
    expect(RSpec::Matchers.generated_description).to eq "should be between 0 and 10"
  end

  it "expect(...).to equal" do
    expected = "expected"
    expect(expected).to equal(expected)
    expect(RSpec::Matchers.generated_description).to eq "should equal \"expected\""
  end

  it "expect(...).not_to equal" do
    expect(5).not_to equal(37)
    expect(RSpec::Matchers.generated_description).to eq "should not equal 37"
  end

  it "expect(...).to eql" do
    expect("string").to eql("string")
    expect(RSpec::Matchers.generated_description).to eq "should eql \"string\""
  end

  it "expect(...).not_to eql" do
    expect("a").not_to eql(:a)
    expect(RSpec::Matchers.generated_description).to eq "should not eql :a"
  end

  it "expect(...).to have_key" do
    expect({:a => "a"}).to have_key(:a)
    expect(RSpec::Matchers.generated_description).to eq "should have key :a"
  end

  it "expect(...).to have_some_method" do
    object = Object.new
    def object.has_eyes_closed?; true; end

    expect(object).to have_eyes_closed
    expect(RSpec::Matchers.generated_description).to eq 'should have eyes closed'
  end

  it "expect(...).to have_some_method(args*)" do
    object = Object.new
    def object.has_taste_for?(*args); true; end

    expect(object).to have_taste_for("wine", "cheese")
    expect(RSpec::Matchers.generated_description).to eq 'should have taste for "wine", "cheese"'
  end

  it "expect(...).to have n items" do
    expect(team).to have(3).players
    expect(RSpec::Matchers.generated_description).to eq "should have 3 players"
  end

  it "expect(...).to have at least n items" do
    expect(team).to have_at_least(2).players
    expect(RSpec::Matchers.generated_description).to eq "should have at least 2 players"
  end

  it "expect(...).to have at most n items" do
    expect(team).to have_at_most(4).players
    expect(RSpec::Matchers.generated_description).to eq "should have at most 4 players"
  end

  it "expect(...).to include(x)" do
    expect([1,2,3]).to include(3)
    expect(RSpec::Matchers.generated_description).to eq "should include 3"
  end

  it "expect(...).to include(x) when x responds to description but is not a matcher" do
    obj = double(:description => "description", :inspect => "inspect")
    expect([obj]).to include(obj)
    expect(RSpec::Matchers.generated_description).to eq "should include inspect"
  end

  it "expect(...).to include(x) when x responds to description and is a matcher" do
    matcher = double(:description                => "description",
                     :matches?                   => true,
                     :failure_message_for_should => "")
    expect([matcher]).to include(matcher)
    expect(RSpec::Matchers.generated_description).to eq "should include description"
  end

  it "expect(array).not_to match_array [1,2,3]" do
    expect([1,2,3]).to match_array [1,2,3]
    expect(RSpec::Matchers.generated_description).to eq "should contain exactly 1, 2 and 3"
  end

  it "expect(...).to match" do
    expect("this string").to match(/this string/)
    expect(RSpec::Matchers.generated_description).to eq "should match /this string/"
  end

  it "expect(...).to raise_error" do
    expect { raise }.to raise_error
    expect(RSpec::Matchers.generated_description).to eq "should raise Exception"
  end

  it "expect(...).to raise_error with type" do
    expect { raise }.to raise_error(RuntimeError)
    expect(RSpec::Matchers.generated_description).to eq "should raise RuntimeError"
  end

  it "expect(...).to raise_error with type and message" do
    expect { raise "there was an error" }.to raise_error(RuntimeError, "there was an error")
    expect(RSpec::Matchers.generated_description).to eq "should raise RuntimeError with \"there was an error\""
  end

  it "expect(...).to respond_to" do
    expect([]).to respond_to(:insert)
    expect(RSpec::Matchers.generated_description).to eq "should respond to #insert"
  end

  it "expect(...).to throw symbol" do
    expect { throw :what_a_mess }.to throw_symbol
    expect(RSpec::Matchers.generated_description).to eq "should throw a Symbol"
  end

  it "expect(...).to throw symbol (with named symbol)" do
    expect { throw :what_a_mess }.to throw_symbol(:what_a_mess)
    expect(RSpec::Matchers.generated_description).to eq "should throw :what_a_mess"
  end

  def team
    Class.new do
      def players
        [1,2,3]
      end
    end.new
  end
end

describe "a Matcher with no description" do
  def matcher
     Class.new do
       def matches?(ignore); true; end
       def failure_message; ""; end
     end.new
  end

  it "provides a helpful message when used in a string-less example block" do
    expect(5).to matcher
    expect(RSpec::Matchers.generated_description).to match(/When you call.*description method/m)
  end
end
