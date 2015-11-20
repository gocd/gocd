require 'spec_helper'
module RSpec
  module Matchers
    describe "equal" do
      it_behaves_like "an RSpec matcher", :valid_value => :a, :invalid_value => :b do
        let(:matcher) { equal(:a) }
      end

      def inspect_object(o)
        "#<#{o.class}:#{o.object_id}> => #{o.inspect}"
      end

      it "matches when actual.equal?(expected)" do
        expect(1).to equal(1)
      end

      it "does not match when !actual.equal?(expected)" do
        expect("1").not_to equal("1")
      end

      it "describes itself" do
        matcher = equal(1)
        matcher.matches?(1)
        expect(matcher.description).to eq "equal 1"
      end

      context "when the expected object is falsey in conditinal semantics" do
        it "describes itself with the expected object" do
          matcher = equal(nil)
          matcher.matches?(nil)
          expect(matcher.description).to eq "equal nil"
        end
      end

      context "when the expected object's #equal? always returns true" do
        let(:strange_string) do
          string = "foo"

          def string.equal?(other)
            true
          end

          string
        end

        it "describes itself with the expected object" do
          matcher = equal(strange_string)
          matcher.matches?(strange_string)
          expect(matcher.description).to eq 'equal "foo"'
        end
      end

      it "suggests the `eq` matcher on failure" do
        expected, actual = "1", "1"
        expect {
          expect(actual).to equal(expected)
        }.to fail_with <<-MESSAGE

expected #{inspect_object(expected)}
     got #{inspect_object(actual)}

Compared using equal?, which compares object identity,
but expected and actual are not the same object. Use
`expect(actual).to eq(expected)` if you don't care about
object identity in this example.

MESSAGE
      end

      context "when using only `should`", :uses_only_should do
        it "suggests the `eq` matcher on failure" do
          expected, actual = "1", "1"
          lambda {
            actual.should equal(expected)
          }.should fail_with <<-MESSAGE

expected #{inspect_object(expected)}
     got #{inspect_object(actual)}

Compared using equal?, which compares object identity,
but expected and actual are not the same object. Use
`actual.should eq(expected)` if you don't care about
object identity in this example.

MESSAGE
        end
      end

      it "provides message on #negative_failure_message" do
        expected = actual = "1"
        matcher = equal(expected)
        matcher.matches?(actual)
        expect(matcher.failure_message_for_should_not).to eq <<-MESSAGE

expected not #{inspect_object(expected)}
         got #{inspect_object(actual)}

Compared using equal?, which compares object identity.

MESSAGE
      end
    end
  end
end
