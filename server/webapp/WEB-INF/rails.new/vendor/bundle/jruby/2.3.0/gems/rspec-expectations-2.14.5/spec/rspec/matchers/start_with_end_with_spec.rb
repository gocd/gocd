require "spec_helper"

describe "expect(...).to start_with" do
  it_behaves_like "an RSpec matcher", :valid_value => "ab", :invalid_value => "bc" do
    let(:matcher) { start_with("a") }
  end

  context "with a string" do
    it "passes if it matches the start of the actual string" do
      expect("this string").to start_with "this str"
    end

    it "fails if it does not match the start of the actual string" do
      expect {
        expect("this string").to start_with "that str"
      }.to fail_with("expected \"this string\" to start with \"that str\"")
    end
  end

  context "with an array" do
    it "passes if it is the first element of the array" do
      expect([0, 1, 2]).to start_with 0
    end

    it "passes if the first elements of the array match" do
      expect([0, 1, 2]).to start_with 0, 1
    end

    it "fails if it does not match the first element of the array" do
      expect {
        expect([0, 1, 2]).to start_with 2
      }.to fail_with("expected [0, 1, 2] to start with 2")
    end

    it "fails if it the first elements of the array do not match" do
      expect {
        expect([0, 1, 2]).to start_with 1, 2
      }.to fail_with("expected [0, 1, 2] to start with [1, 2]")
    end
  end

  context "with an object that does not respond to :[]" do
    it "raises an ArgumentError" do
      expect {
        expect(Object.new).to start_with 0
      }.to raise_error(ArgumentError, /does not respond to :\[\]/)
    end
  end

  context "with a hash" do
    it "raises an ArgumentError if trying to match more than one element" do
      expect{
        expect({:a => 'b', :b => 'b', :c => 'c'}).to start_with({:a => 'b', :b => 'b'})
      }.to raise_error(ArgumentError, /does not have ordered elements/)
    end
  end
end

describe "expect(...).not_to start_with" do
  context "with a string" do
    it "passes if it does not match the start of the actual string" do
      expect("this string").not_to start_with "that str"
    end

    it "fails if it does match the start of the actual string" do
      expect {
        expect("this string").not_to start_with "this str"
      }.to fail_with("expected \"this string\" not to start with \"this str\"")
    end
  end

  context "with an array" do
    it "passes if it is not the first element of the array" do
      expect([0, 1, 2]).not_to start_with 2
    end

    it "passes if the first elements of the array do not match" do
      expect([0, 1, 2]).not_to start_with 1, 2
    end

    it "fails if it matches the first element of the array" do
      expect {
        expect([0, 1, 2]).not_to start_with 0
      }.to fail_with("expected [0, 1, 2] not to start with 0")
    end

    it "fails if it the first elements of the array match" do
      expect {
        expect([0, 1, 2]).not_to start_with 0, 1
      }.to fail_with("expected [0, 1, 2] not to start with [0, 1]")
    end
  end
end

describe "expect(...).to end_with" do
  it_behaves_like "an RSpec matcher", :valid_value => "ab", :invalid_value => "bc" do
    let(:matcher) { end_with("b") }
  end

  context "with a string" do
    it "passes if it matches the end of the actual string" do
      expect("this string").to end_with "is string"
    end

    it "fails if it does not match the end of the actual string" do
      expect {
        expect("this string").to end_with "is stringy"
      }.to fail_with("expected \"this string\" to end with \"is stringy\"")
    end
  end

  context "with an array" do
    it "passes if it is the last element of the array" do
      expect([0, 1, 2]).to end_with 2
    end

    it "passes if the last elements of the array match" do
      expect([0, 1, 2]).to end_with [1, 2]
    end

    it "fails if it does not match the last element of the array" do
      expect {
        expect([0, 1, 2]).to end_with 1
      }.to fail_with("expected [0, 1, 2] to end with 1")
    end

    it "fails if it the last elements of the array do not match" do
      expect {
        expect([0, 1, 2]).to end_with [0, 1]
      }.to fail_with("expected [0, 1, 2] to end with [0, 1]")
    end
  end

  context "with an object that does not respond to :[]" do
    it "raises an error if expected value can't be indexed'" do
      expect {
        expect(Object.new).to end_with 0
      }.to raise_error(ArgumentError, /does not respond to :\[\]/)
    end
  end

  context "with a hash" do
    it "raises an ArgumentError if trying to match more than one element" do
      expect{
        expect({:a => 'b', :b => 'b', :c => 'c'}).to end_with({:a => 'b', :b =>'b'})
      }.to raise_error(ArgumentError, /does not have ordered elements/)
    end
  end

end

describe "expect(...).not_to end_with" do
  context "with a sting" do
    it "passes if it does not match the end of the actual string" do
      expect("this string").not_to end_with "stringy"
    end

    it "fails if it matches the end of the actual string" do
      expect {
        expect("this string").not_to end_with "string"
      }.to fail_with("expected \"this string\" not to end with \"string\"")
    end
  end

  context "an array" do
    it "passes if it is not the last element of the array" do
      expect([0, 1, 2]).not_to end_with 1
    end

    it "passes if the last elements of the array do not match" do
      expect([0, 1, 2]).not_to end_with [0, 1]
    end

    it "fails if it matches the last element of the array" do
      expect {
        expect([0, 1, 2]).not_to end_with 2
      }.to fail_with("expected [0, 1, 2] not to end with 2")
    end

    it "fails if it the last elements of the array match" do
      expect {
        expect([0, 1, 2]).not_to end_with [1, 2]
      }.to fail_with("expected [0, 1, 2] not to end with [1, 2]")
    end
  end
end
