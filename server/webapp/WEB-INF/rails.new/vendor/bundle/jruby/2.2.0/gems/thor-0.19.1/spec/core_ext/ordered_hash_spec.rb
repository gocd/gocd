require "helper"
require "thor/core_ext/ordered_hash"

describe Thor::CoreExt::OrderedHash do
  before do
    @hash = Thor::CoreExt::OrderedHash.new
  end

  describe "without any items" do
    it "returns nil for an undefined key" do
      expect(@hash["foo"]).to be nil
    end

    it "doesn't iterate through any items" do
      @hash.each { fail }
    end

    it "has an empty key and values list" do
      expect(@hash.keys).to be_empty
      expect(@hash.values).to be_empty
    end

    it "must be empty" do
      expect(@hash).to be_empty
    end
  end

  describe "with several items" do
    before do
      @hash[:foo] = "Foo!"
      @hash[:bar] = "Bar!"
      @hash[:baz] = "Baz!"
      @hash[:bop] = "Bop!"
      @hash[:bat] = "Bat!"
    end

    it "returns nil for an undefined key" do
      expect(@hash[:boom]).to be nil
    end

    it "returns the value for each key" do
      expect(@hash[:foo]).to eq("Foo!")
      expect(@hash[:bar]).to eq("Bar!")
      expect(@hash[:baz]).to eq("Baz!")
      expect(@hash[:bop]).to eq("Bop!")
      expect(@hash[:bat]).to eq("Bat!")
    end

    it "iterates through the keys and values in order of assignment" do
      arr = []
      @hash.each do |key, value|
        arr << [key, value]
      end
      expect(arr).to eq([[:foo, "Foo!"], [:bar, "Bar!"], [:baz, "Baz!"],
                         [:bop, "Bop!"], [:bat, "Bat!"]])
    end

    it "returns the keys in order of insertion" do
      expect(@hash.keys).to eq([:foo, :bar, :baz, :bop, :bat])
    end

    it "returns the values in order of insertion" do
      expect(@hash.values).to eq(["Foo!", "Bar!", "Baz!", "Bop!", "Bat!"])
    end

    it "does not move an overwritten node to the end of the ordering" do
      @hash[:baz] = "Bip!"
      expect(@hash.values).to eq(["Foo!", "Bar!", "Bip!", "Bop!", "Bat!"])

      @hash[:foo] = "Bip!"
      expect(@hash.values).to eq(["Bip!", "Bar!", "Bip!", "Bop!", "Bat!"])

      @hash[:bat] = "Bip!"
      expect(@hash.values).to eq(["Bip!", "Bar!", "Bip!", "Bop!", "Bip!"])
    end

    it "appends another ordered hash while preserving ordering" do
      other_hash = Thor::CoreExt::OrderedHash.new
      other_hash[1] = "one"
      other_hash[2] = "two"
      other_hash[3] = "three"
      expect(@hash.merge(other_hash).values).to eq(["Foo!", "Bar!", "Baz!", "Bop!", "Bat!", "one", "two", "three"])
    end

    it "overwrites hash keys with matching appended keys" do
      other_hash = Thor::CoreExt::OrderedHash.new
      other_hash[:bar] = "bar"
      expect(@hash.merge(other_hash)[:bar]).to eq("bar")
      expect(@hash[:bar]).to eq("Bar!")
    end

    it "converts to an array" do
      expect(@hash.to_a).to eq([[:foo, "Foo!"], [:bar, "Bar!"], [:baz, "Baz!"], [:bop, "Bop!"], [:bat, "Bat!"]])
    end

    it "must not be empty" do
      expect(@hash).not_to be_empty
    end

    it "deletes values from hash" do
      expect(@hash.delete(:baz)).to eq("Baz!")
      expect(@hash.values).to eq(["Foo!", "Bar!", "Bop!", "Bat!"])

      expect(@hash.delete(:foo)).to eq("Foo!")
      expect(@hash.values).to eq(["Bar!", "Bop!", "Bat!"])

      expect(@hash.delete(:bat)).to eq("Bat!")
      expect(@hash.values).to eq(["Bar!", "Bop!"])
    end

    it "returns nil if the value to be deleted can't be found" do
      expect(@hash.delete(:nothing)).to be nil
    end
  end
end
