require "spec_helper"
require 'active_model'

module RSpec::CollectionMatchers
  describe "Have extensions for rails" do
    describe "error_on" do
      it "provides a description including the name of what the error is on" do
        expect(have(1).error_on(:whatever).description).to eq "have 1 error on :whatever"
      end

      it "provides a failure message including the number actually given" do
        expect {
          expect([]).to have(1).error_on(:whatever)
        }.to raise_error("expected 1 error on :whatever, got 0")
      end
    end

    describe "errors_on" do
      it "provides a description including the name of what the error is on" do
        expect(have(2).errors_on(:whatever).description).to eq "have 2 errors on :whatever"
      end

      it "provides a failure message including the number actually given" do
        expect {
          expect([1]).to have(3).errors_on(:whatever)
        }.to raise_error("expected 3 errors on :whatever, got 1")
      end

      let(:klass) do
        Class.new do
          include ActiveModel::Validations
        end
      end

      it "calls valid?" do
        model = klass.new
        expect(model).to receive(:valid?)
        model.errors_on(:foo)
      end

      it "returns the errors on that attribute" do
        model = klass.new
        allow(model).to receive(:errors) do
          { :foo => ['a', 'b'] }
        end
        expect(model.errors_on(:foo)).to eq(['a','b'])
      end

      context "ActiveModel class that takes no arguments to valid?" do
        let(:klass) {
          Class.new do
            include ActiveModel::Validations

            def self.name
              "ActiveModelValidationsFake"
            end

            def valid?
              super
            end

            attr_accessor :name
            validates_presence_of :name
          end
        }

        context "with nil name" do
          it "has one error" do
            object = klass.new
            object.name = ""

            expect(object).to have(1).error_on(:name)
          end
        end

        context "with non-blank name" do
          it "has no error" do
            object = klass.new
            object.name = "Ywen"

            expect(object).to have(:no).error_on(:name)
          end
        end
      end
    end

    describe "have something other than error_on or errors_on" do
      it "has a standard rspec failure message" do
        expect {
          expect([1,2,3]).to have(2).elements
        }.to raise_error("expected 2 elements, got 3")
      end

      it "has a standard rspec description" do
        expect(have(2).elements.description).to eq "have 2 elements"
      end
    end
  end
end
