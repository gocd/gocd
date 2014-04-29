require 'spec_helper'

module RSpec
  module Expectations
    describe Syntax do
      context "when passing a message to an expectation" do
        let(:warner) { ::Kernel }

        let(:string_like_object) do
          Struct.new(:to_str, :to_s).new(*(["Ceci n'est pas une Chaine."]*2))
        end

        let(:insufficiently_string_like_object) do
          Struct.new(:to_s).new("Ceci n'est pas une Chaine.")
        end

        let(:callable_object) do
          Struct.new(:call).new("Ceci n'est pas une Chaine.")
        end

        describe "expect(...).to" do
          it "prints a warning when the message object isn't a String" do
            warner.should_receive(:warn).with(/ignoring.*message/)
            expect(3).to eq(3), :not_a_string
          end

          it "doesn't print a warning when message is a String" do
            warner.should_not_receive(:warn)
            expect(3).to eq(3), "a string"
          end

          it "doesn't print a warning when message responds to to_str" do
            warner.should_not_receive(:warn)
            expect(3).to eq(3), string_like_object
          end

          it "prints a warning when the message object handles to_s but not to_str" do
            warner.should_receive(:warn).with(/ignoring.*message/)
            expect(3).to eq(3), insufficiently_string_like_object
          end

          it "doesn't print a warning when message responds to call" do
            warner.should_not_receive(:warn)
            expect(3).to eq(3), callable_object
          end
        end

        describe "expect(...).not_to" do
          it "prints a warning when the message object isn't a String" do
            warner.should_receive(:warn).with(/ignoring.*message/)
            expect(3).not_to eq(4), :not_a_string
          end

          it "doesn't print a warning when message is a String" do
            warner.should_not_receive(:warn)
            expect(3).not_to eq(4), "a string"
          end

          it "doesn't print a warning when message responds to to_str" do
            warner.should_not_receive(:warn)
            expect(3).not_to eq(4), string_like_object
          end

          it "prints a warning when the message object handles to_s but not to_str" do
            warner.should_receive(:warn).with(/ignoring.*message/)
            expect(3).not_to eq(4), insufficiently_string_like_object
          end

          it "doesn't print a warning when message responds to call" do
            warner.should_not_receive(:warn)
            expect(3).not_to eq(4), callable_object
          end
        end
      end

      describe "expression generation" do
        let(:target) { "foo" }
        let(:expectation) { "eq('bar')" }
        let(:positive_expect_example) { "expect(foo).to eq('bar')" }
        let(:positive_should_example) { "foo.should eq('bar')" }
        let(:negative_expect_example) { "expect(foo).not_to eq('bar')" }
        let(:negative_should_example) { "foo.should_not eq('bar')" }

        def positive_expression
          Syntax.positive_expression(target, expectation)
        end

        def negative_expression
          Syntax.negative_expression(target, expectation)
        end

        context "when only :expect is enabled" do
          before do
            expect(Syntax.should_enabled?).to be_false
            expect(Syntax.expect_enabled?).to be_true
          end

          it 'generates a positive expression using the expect syntax' do
            expect(positive_expression).to eq(positive_expect_example)
          end

          it 'generates a negative expression using the expect syntax' do
            expect(negative_expression).to eq(negative_expect_example)
          end
        end

        context "when both :should and :expect are enabled", :uses_should do
          before do
            expect(Syntax.should_enabled?).to be_true
            expect(Syntax.expect_enabled?).to be_true
          end

          it 'generates a positive expression using the expect syntax' do
            expect(positive_expression).to eq(positive_expect_example)
          end

          it 'generates a negative expression using the expect syntax' do
            expect(negative_expression).to eq(negative_expect_example)
          end
        end

        context "when only :should is enabled", :uses_only_should do
          before do
            Syntax.should_enabled?.should be_true
            Syntax.expect_enabled?.should be_false
          end

          it 'generates a positive expression using the expect syntax' do
            positive_expression.should eq(positive_should_example)
          end

          it 'generates a negative expression using the expect syntax' do
            negative_expression.should eq(negative_should_example)
          end
        end
      end
    end
  end
end
