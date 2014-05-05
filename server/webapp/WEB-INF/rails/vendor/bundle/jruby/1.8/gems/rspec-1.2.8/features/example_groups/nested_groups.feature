Feature: Nested example groups

  As an RSpec user
  I want to nest examples groups
  So that I can better organize my examples

  Scenario Outline: Nested example groups
    Given a file named "nested_example_groups.rb" with:
    """
    require 'spec/autorun'

    describe "Some Object" do
      describe "with some more context" do
        it "should do this" do
          true.should be_true
        end
      end
      describe "with some other context" do
        it "should do that" do
          false.should be_false
        end
      end
    end
    """
    When I run "<Command> nested_example_groups.rb -fs"
    Then the stdout should match /Some Object with some more context/
    And the stdout should match /Some Object with some other context/

  Scenarios: Run with ruby and spec
    | Command |
    | ruby    |
    | spec    |
