require 'spec_helper'

module RSpec
  module Mocks
    describe "stubbing a base class class method" do
      before do
        @base_class     = Class.new
        @concrete_class = Class.new(@base_class)

        @base_class.stub(:find).and_return "stubbed_value"
      end

      it "returns the value for the stub on the base class" do
        expect(@base_class.find).to eq "stubbed_value"
      end

      it "returns the value for the descendent class" do
        expect(@concrete_class.find).to eq "stubbed_value"
      end
    end
  end
end
