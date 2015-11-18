require "helper"
require "thor/parser"

describe Thor::Arguments do
  def create(opts = {})
    arguments = opts.map do |type, default|
      options = {:required => default.nil?, :type => type, :default => default}
      Thor::Argument.new(type.to_s, options)
    end

    arguments.sort! { |a, b| b.name <=> a.name }
    @opt = Thor::Arguments.new(arguments)
  end

  def parse(*args)
    @opt.parse(args)
  end

  describe "#parse" do
    it "parses arguments in the given order" do
      create :string => nil, :numeric => nil
      expect(parse("name", "13")["string"]).to eq("name")
      expect(parse("name", "13")["numeric"]).to eq(13)
    end

    it "accepts hashes" do
      create :string => nil, :hash => nil
      expect(parse("product", "title:string", "age:integer")["string"]).to eq("product")
      expect(parse("product", "title:string", "age:integer")["hash"]).to eq("title" => "string", "age" => "integer")
      expect(parse("product", "url:http://www.amazon.com/gp/product/123")["hash"]).to eq("url" => "http://www.amazon.com/gp/product/123")
    end

    it "accepts arrays" do
      create :string => nil, :array => nil
      expect(parse("product", "title", "age")["string"]).to eq("product")
      expect(parse("product", "title", "age")["array"]).to eq(%w[title age])
    end

    describe "with no inputs" do
      it "and no arguments returns an empty hash" do
        create
        expect(parse).to eq({})
      end

      it "and required arguments raises an error" do
        create :string => nil, :numeric => nil
        expect { parse }.to raise_error(Thor::RequiredArgumentMissingError, "No value provided for required arguments 'string', 'numeric'")
      end

      it "and default arguments returns default values" do
        create :string => "name", :numeric => 13
        expect(parse).to eq("string" => "name", "numeric" => 13)
      end
    end

    it "returns the input if it's already parsed" do
      create :string => nil, :hash => nil, :array => nil, :numeric => nil
      expect(parse("", 0, {}, [])).to eq("string" => "", "numeric" => 0, "hash" => {}, "array" => [])
    end

    it "returns the default value if none is provided" do
      create :string => "foo", :numeric => 3.0
      expect(parse("bar")).to eq("string" => "bar", "numeric" => 3.0)
    end
  end
end
