module RSpec::Matchers::BuiltIn
  describe BaseMatcher do
    describe "#match_unless_raises" do
      let(:matcher) do
        Class.new(BaseMatcher).new
      end

      it "returns true if there are no errors" do
        matcher.match_unless_raises {}.should be_true
      end

      it "returns false if there is an error" do
        matcher.match_unless_raises { raise }.should be_false
      end

      it "returns false if the only submitted error is raised" do
        matcher.match_unless_raises(RuntimeError){ raise "foo" }.should be_false
      end

      it "returns false if any of several errors submitted is raised" do
        matcher.match_unless_raises(RuntimeError, ArgumentError, NameError) { raise "foo" }.should be_false
        matcher.match_unless_raises(RuntimeError, ArgumentError, NameError) { raise ArgumentError.new('') }.should be_false
        matcher.match_unless_raises(RuntimeError, ArgumentError, NameError) { raise NameError.new('') }.should be_false
      end

      it "re-raises any error other than one of those specified" do
        expect do
          matcher.match_unless_raises(ArgumentError){ raise "foo" }
        end.to raise_error
      end

      it "stores the rescued exception for use in messages" do
        matcher.match_unless_raises(RuntimeError){ raise "foo" }
        matcher.rescued_exception.should be_a(RuntimeError)
        matcher.rescued_exception.message.should eq("foo")
      end

    end

    describe "#==" do
      it "responds the same way as matches?" do
        matcher = Class.new(BaseMatcher) do
          def initialize(expected)
            @expected = expected
          end

          def matches?(actual)
            (@actual = actual) == @expected
          end
        end

        matcher.new(3).matches?(3).should be_true
        matcher.new(3).should eq(3)

        matcher.new(3).matches?(4).should be_false
        matcher.new(3).should_not eq(4)
      end
    end
  end
end
