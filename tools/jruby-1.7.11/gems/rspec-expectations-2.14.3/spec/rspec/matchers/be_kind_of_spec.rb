require 'spec_helper'

module RSpec
  module Matchers
    [:be_a_kind_of, :be_kind_of].each do |method|
      describe "expect(actual).to #{method}(expected)" do
        it_behaves_like "an RSpec matcher", :valid_value => 5, :invalid_value => "a" do
          let(:matcher) { send(method, Fixnum) }
        end

        it "passes if actual is instance of expected class" do
          expect(5).to send(method, Fixnum)
        end

        it "passes if actual is instance of subclass of expected class" do
          expect(5).to send(method, Numeric)
        end

        it "fails with failure message for should unless actual is kind of expected class" do
          expect {
            expect("foo").to send(method, Array)
          }.to fail_with(%Q{expected "foo" to be a kind of Array})
        end

        it "provides a description" do
          matcher = be_a_kind_of(String)
          matcher.matches?("this")
          expect(matcher.description).to eq "be a kind of String"
        end
      end

      describe "expect(actual).not_to #{method}(expected)" do
        it "fails with failure message for should_not if actual is kind of expected class" do
          expect {
            expect("foo").not_to send(method, String)
          }.to fail_with(%Q{expected "foo" not to be a kind of String})
        end
      end
    end
  end
end
