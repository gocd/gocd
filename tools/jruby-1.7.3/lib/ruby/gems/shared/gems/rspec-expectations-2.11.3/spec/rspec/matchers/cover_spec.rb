require 'spec_helper'

if (1..2).respond_to?(:cover?)
  describe "should cover(expected)" do
    it_behaves_like "an RSpec matcher", :valid_value => (1..10), :invalid_value => (20..30) do
      let(:matcher) { cover(5) }
    end

    context "for a range target" do
      it "passes if target covers expected" do
        (1..10).should cover(5)
      end

      it "fails if target does not cover expected" do
        lambda {
          (1..10).should cover(11)
        }.should fail_with("expected 1..10 to cover 11")
      end
    end
  end

  describe "should cover(with, multiple, args)" do
    context "for a range target" do
      it "passes if target covers all items" do
        (1..10).should cover(4, 6)
      end

      it "fails if target does not cover any one of the items" do
        lambda {
          (1..10).should cover(4, 6, 11)
        }.should fail_with("expected 1..10 to cover 4, 6, and 11")
      end
    end
  end

  describe "should_not cover(expected)" do
    context "for a range target" do
      it "passes if target does not cover expected" do
        (1..10).should_not cover(11)
      end

      it "fails if target covers expected" do
        lambda {
          (1..10).should_not cover(5)
        }.should fail_with("expected 1..10 not to cover 5")
      end
    end
  end

  describe "should_not cover(with, multiple, args)" do
    context "for a range target" do
      it "passes if the target does not cover any of the expected" do
        (1..10).should_not cover(11, 12, 13)
      end

      it "fails if the target covers all of the expected" do
        expect {
          (1..10).should_not cover(4, 6)
        }.to fail_with("expected 1..10 not to cover 4 and 6")
      end

      it "fails if the target covers some (but not all) of the expected" do
        expect {
          (1..10).should_not cover(5, 11)
        }.to fail_with("expected 1..10 not to cover 5 and 11")
      end
    end
  end
end
