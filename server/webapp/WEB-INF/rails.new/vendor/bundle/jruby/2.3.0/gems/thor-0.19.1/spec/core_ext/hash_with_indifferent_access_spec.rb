require "helper"
require "thor/core_ext/hash_with_indifferent_access"

describe Thor::CoreExt::HashWithIndifferentAccess do
  before do
    @hash = Thor::CoreExt::HashWithIndifferentAccess.new :foo => "bar", "baz" => "bee", :force => true
  end

  it "has values accessible by either strings or symbols" do
    expect(@hash["foo"]).to eq("bar")
    expect(@hash[:foo]).to eq("bar")

    expect(@hash.values_at(:foo, :baz)).to eq(%w[bar bee])
    expect(@hash.delete(:foo)).to eq("bar")
  end

  it "handles magic boolean predicates" do
    expect(@hash.force?).to be true
    expect(@hash.foo?).to be true
    expect(@hash.nothing?).to be false
  end

  it "handles magic comparisons" do
    expect(@hash.foo?("bar")).to be true
    expect(@hash.foo?("bee")).to be false
  end

  it "maps methods to keys" do
    expect(@hash.foo).to eq(@hash["foo"])
  end

  it "merges keys independent if they are symbols or strings" do
    @hash.merge!("force" => false, :baz => "boom")
    expect(@hash[:force]).to eq(false)
    expect(@hash["baz"]).to eq("boom")
  end

  it "creates a new hash by merging keys independent if they are symbols or strings" do
    other = @hash.merge("force" => false, :baz => "boom")
    expect(other[:force]).to eq(false)
    expect(other["baz"]).to eq("boom")
  end

  it "converts to a traditional hash" do
    expect(@hash.to_hash.class).to eq(Hash)
    expect(@hash).to eq("foo" => "bar", "baz" => "bee", "force" => true)
  end
end
