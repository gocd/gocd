require "helper"
require "thor/parser"

describe Thor::Argument do

  def argument(name, options = {})
    @argument ||= Thor::Argument.new(name, options)
  end

  describe "errors" do
    it "raises an error if name is not supplied" do
      expect do
        argument(nil)
      end.to raise_error(ArgumentError, "Argument name can't be nil.")
    end

    it "raises an error if type is unknown" do
      expect do
        argument(:command, :type => :unknown)
      end.to raise_error(ArgumentError, "Type :unknown is not valid for arguments.")
    end

    it "raises an error if argument is required and has default values" do
      expect do
        argument(:command, :type => :string, :default => "bar", :required => true)
      end.to raise_error(ArgumentError, "An argument cannot be required and have default value.")
    end

    it "raises an error if enum isn't an array" do
      expect do
        argument(:command, :type => :string, :enum => "bar")
      end.to raise_error(ArgumentError, "An argument cannot have an enum other than an array.")
    end
  end

  describe "#usage" do
    it "returns usage for string types" do
      expect(argument(:foo, :type => :string).usage).to eq("FOO")
    end

    it "returns usage for numeric types" do
      expect(argument(:foo, :type => :numeric).usage).to eq("N")
    end

    it "returns usage for array types" do
      expect(argument(:foo, :type => :array).usage).to eq("one two three")
    end

    it "returns usage for hash types" do
      expect(argument(:foo, :type => :hash).usage).to eq("key:value")
    end
  end
end
