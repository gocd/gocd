Feature: implicit docstrings

  As an RSpec user
  I want examples to generate their own names
  So that I can reduce duplication between example names and example code

  Scenario Outline: run passing examples
    Given a file named "implicit_docstrings_example.rb" with:
    """
    require 'spec/autorun'
    describe "Examples with no docstrings generate their own:" do

      specify { 3.should be < 5 }

      specify { ["a"].should include("a") }

      specify { [1,2,3].should respond_to(:size) }

    end
    """

    When I run "<Command> implicit_docstrings_example.rb -fs"

    Then the stdout should match /should be < 5/
    And the stdout should match /should include "a"/
    And the stdout should match /should respond to #size/

  Scenarios: Run with ruby and spec
    | Command |
    | ruby    |
    | spec    |

  Scenario Outline: run failing examples
    Given a file named "failing_implicit_docstrings_example.rb" with:
    """
    require 'spec/autorun'
    describe "Failing examples with no descriptions" do

      # description is auto-generated as "should equal(5)" based on the last #should
      it do
        3.should equal(2)
        5.should equal(5)
      end

      it { 3.should be > 5 }

      it { ["a"].should include("b") }

      it { [1,2,3].should_not respond_to(:size) }

    end
    """

    When I run "<Command> failing_implicit_docstrings_example.rb -fs"

    Then the stdout should match /should equal 2/
    And the stdout should match /should be > 5/
    And the stdout should match /should include "b"/
    And the stdout should match /should not respond to #size/
