Feature: double handling to_ary

  Ruby implicitly sends `to_ary` to all of the objects in an `Array` when the
  array receives `flatten`:

      [obj].flatten # => obj.to_ary

  If `to_ary` raises a `NoMethodError`, Ruby sees that the object can not be coerced
  into an array and moves on to the next object.

  To support this, an RSpec double will raise a NoMethodError when it receives
  `to_ary` _even if it is set `as_null_object`_, unless `to_ary` is explicitly
  stubbed.

  Scenario: double receiving to_ary
    Given a file named "example.rb" with:
      """ruby
      describe "#to_ary" do
        shared_examples "to_ary" do
          it "can be overridden with a stub" do
            obj.stub(:to_ary) { :non_nil_value }
            obj.to_ary.should be(:non_nil_value)
          end

          it "supports Array#flatten" do
            obj = double('foo')
            [obj].flatten.should eq([obj])
          end
        end

        context "sent to a double as_null_object" do
          let(:obj) { double('obj').as_null_object }
          include_examples "to_ary"

          it "returns nil" do
            expect( obj.to_ary ).to eq nil
          end
        end

        context "sent to a double without as_null_object" do
          let(:obj) { double('obj') }
          include_examples "to_ary"

          it "raises a NoMethodError" do
            expect { obj.to_ary }.to raise_error(NoMethodError)
          end
        end
      end
     """
    When I run `rspec example.rb`
    Then the examples should all pass
