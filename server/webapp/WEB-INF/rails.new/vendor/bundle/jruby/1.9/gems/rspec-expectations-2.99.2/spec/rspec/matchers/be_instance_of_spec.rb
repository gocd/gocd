require 'spec_helper'

module RSpec
  module Matchers
    [:be_an_instance_of, :be_instance_of].each do |method|
      describe "expect(actual).to #{method}(expected)" do
        it_behaves_like "an RSpec matcher", :valid_value => 5, :invalid_value => "a" do
          let(:matcher) { send(method, Fixnum) }
        end

        it "passes if actual is instance of expected class" do
          expect(5).to send(method, Fixnum)
        end

        it "fails if actual is instance of subclass of expected class" do
          expect {
            expect(5).to send(method, Numeric)
          }.to fail_with(%Q{expected 5 to be an instance of Numeric})
        end

        it "fails with failure message for should unless actual is instance of expected class" do
          expect {
            expect("foo").to send(method, Array)
          }.to fail_with(%Q{expected "foo" to be an instance of Array})
        end

        it "provides a description" do
          matcher = be_an_instance_of(Fixnum)
          matcher.matches?(Numeric)
          expect(matcher.description).to eq "be an instance of Fixnum"
        end

        context "when expected provides an expanded inspect, e.g. AR::Base" do
          let(:user_klass) do
            Class.new do
              def self.inspect
                "User(id: integer, name: string)"
              end
            end
          end

          before { stub_const("User", user_klass) }

          it "provides a description including only the class name" do
            matcher = be_an_instance_of(User)
            expect(matcher.description).to eq "be an instance of User"
          end
        end
      end

      describe "expect(actual).not_to #{method}(expected)" do

        it "fails with failure message for should_not if actual is instance of expected class" do
          expect {
            expect("foo").not_to send(method, String)
          }.to fail_with(%Q{expected "foo" not to be an instance of String})
        end

      end

    end
  end
end
