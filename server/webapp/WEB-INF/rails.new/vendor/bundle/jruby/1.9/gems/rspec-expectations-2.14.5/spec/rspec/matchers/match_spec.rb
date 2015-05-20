require 'spec_helper'

describe "expect(...).to match(expected)" do
  it_behaves_like "an RSpec matcher", :valid_value => 'ab', :invalid_value => 'bc' do
    let(:matcher) { match(/a/) }
  end

  it "passes when target (String) matches expected (Regexp)" do
    expect("string").to match(/tri/)
  end

  it "passes when target (String) matches expected (String)" do
    expect("string").to match("tri")
  end

  it "fails when target (String) does not match expected (Regexp)" do
    expect {
      expect("string").to match(/rings/)
    }.to fail
  end

  it "fails when target (String) does not match expected (String)" do
    expect {
      expect("string").to match("rings")
    }.to fail
  end

  it "provides message, expected and actual on failure" do
    matcher = match(/rings/)
    matcher.matches?("string")
    expect(matcher.failure_message_for_should).to eq "expected \"string\" to match /rings/"
  end

  it "provides a diff on failure" do
    allow(RSpec::Matchers.configuration).to receive(:color?).and_return(false)

    failure_message_that_includes_diff = %r%
\s*Diff:
\s*@@ -1,2 \+1,2 @@
\s*-/bar/
\s*\+"foo"%

    expect { expect("foo").to match(/bar/) }.to fail_with(failure_message_that_includes_diff)
  end

end

describe "expect(...).not_to match(expected)" do
  it "passes when target (String) matches does not match (Regexp)" do
    expect("string").not_to match(/rings/)
  end

  it "passes when target (String) matches does not match (String)" do
    expect("string").not_to match("rings")
  end

  it "fails when target (String) matches expected (Regexp)" do
    expect {
      expect("string").not_to match(/tri/)
    }.to fail
  end

  it "fails when target (String) matches expected (String)" do
    expect {
      expect("string").not_to match("tri")
    }.to fail
  end

  it "provides message, expected and actual on failure" do
    matcher = match(/tri/)
    matcher.matches?("string")
    expect(matcher.failure_message_for_should_not).to eq "expected \"string\" not to match /tri/"
  end
end
