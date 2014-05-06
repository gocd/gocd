Feature: Spec and test together

  As an RSpec adopter with existing Test::Unit tests
  I want to run a few specs alongside my existing Test::Unit tests
  So that I can experience a smooth, gradual migration path

  Scenario Outline: Run specs and tests together
    Given a file named "spec_and_test_together.rb" with:
    """
    require 'spec/autorun'
    require 'spec/test/unit'

    describe "An Example" do
      it "should pass with assert" do
        assert true
      end

      it "should fail with assert" do
        assert false
      end

      it "should pass with should" do
        1.should == 1
      end

      it "should fail with should" do
        1.should == 2
      end
    end

    class ATest < Test::Unit::TestCase
      def test_should_pass_with_assert
        assert true
      end

      def test_should_fail_with_assert
        assert false
      end

      def test_should_pass_with_should
        1.should == 1
      end

      def test_should_fail_with_should
        1.should == 2
      end

      def setup
        @from_setup ||= 3
        @from_setup += 1
      end

      def test_should_fail_with_setup_method_variable
        @from_setup.should == 40
      end

      before do
        @from_before = @from_setup + 1
      end

      def test_should_fail_with_before_block_variable
        @from_before.should == 50
      end
    end
    """

    When I run "<Command> spec_and_test_together.rb -fs"

    Then the exit code should be 256
    And the stdout should match "ATest"
    And the stdout should match "Test::Unit::AssertionFailedError in 'An Example should fail with assert'"
    And the stdout should match "'An Example should fail with should' FAILED"
    And the stdout should match "10 examples, 6 failures"
    And the stdout should match /expected: 40,\s*got: 4/m
    And the stdout should match /expected: 50,\s*got: 5/m

  Scenarios: run with ruby and spec
    | Command |
    | ruby    |
    | spec    |
