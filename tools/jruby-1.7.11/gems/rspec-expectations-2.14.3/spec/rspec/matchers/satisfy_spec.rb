require 'spec_helper'

describe "expect(...).to satisfy { block }" do
  it_behaves_like "an RSpec matcher", :valid_value => true, :invalid_value => false do
    let(:matcher) { satisfy { |v| v } }
  end

  it "describes itself" do
    expect(satisfy.description).to eq("satisfy block")
  end

  it "passes if block returns true" do
    expect(true).to satisfy { |val| val }
    expect(true).to satisfy do |val|
      val
    end
  end

  it "fails if block returns false" do
    expect {
      expect(false).to satisfy { |val| val }
    }.to fail_with("expected false to satisfy block")
    expect do
      expect(false).to satisfy do |val|
        val
      end
    end.to fail_with("expected false to satisfy block")
  end
end

describe "expect(...).not_to satisfy { block }" do
  it "passes if block returns false" do
    expect(false).not_to satisfy { |val| val }
    expect(false).not_to satisfy do |val|
      val
    end
  end

  it "fails if block returns true" do
    expect {
      expect(true).not_to satisfy { |val| val }
    }.to fail_with("expected true not to satisfy block")
  end
end
