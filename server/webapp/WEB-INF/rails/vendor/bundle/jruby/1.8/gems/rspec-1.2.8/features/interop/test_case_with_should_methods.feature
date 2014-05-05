Story: Test::Unit::TestCase extended by rspec with should methods

  As an RSpec adopter with existing Test::Unit tests
  I want to use should_* methods in a Test::Unit::TestCase
  So that I use RSpec with classes and methods that look more like RSpec examples

  Scenario Outline: TestCase with should methods
    Given a file named "test_case_with_should_methods.rb" with:
    """
    require 'spec/autorun'
    require 'spec/test/unit'

    class MyTest < Test::Unit::TestCase
      def should_pass_with_should
        1.should == 1
      end

      def should_fail_with_should
        1.should == 2
      end

      def should_pass_with_assert
        assert true
      end

      def should_fail_with_assert
        assert false
      end

      def test
        raise "This is not a real test"
      end

      def test_ify
        raise "This is a real test"
      end
    end
    """
    When I run "<Command> test_case_with_should_methods.rb"
    Then the exit code should be 256
    And the stdout should match "5 examples, 3 failures"

  Scenarios: Run with ruby and spec
    | Command |
    | ruby    |
    | spec    |
