Feature: Spec and test together

  As an RSpec user
  I want to run one example identified by the line number
  
  Background:
    Given a file named "example.rb" with:
      """
      describe "a group" do

        it "has a first example" do

        end
        
        it "has a second example" do

        end
        
      end
      """

  Scenario: two examples - first example on declaration line
    When I run "spec example.rb:3 --format nested"
    Then the stdout should match "1 example, 0 failures"
    And the stdout should match "has a first example"
    But the stdout should not match "has a second example"

  Scenario: two examples - first example from line inside declaration
    When I run "spec example.rb:4 --format nested"
    Then the stdout should match "1 example, 0 failures"
    And the stdout should match "has a first example"
    But the stdout should not match "has a second example"

  Scenario: two examples - first example from line below declaration
    When I run "spec example.rb:6 --format nested"
    Then the stdout should match "1 example, 0 failures"
    And the stdout should match "has a first example"
    But the stdout should not match "has a second example"

  Scenario: two examples - second example from line below declaration
    When I run "spec example.rb:7 --format nested"
    Then the stdout should match "1 example, 0 failures"
    And the stdout should match "has a second example"
    But the stdout should not match "has a first example"

  Scenario: two examples - both examples from the group declaration
    When I run "spec example.rb:1 --format nested"
    Then the stdout should match "2 examples, 0 failures"
    And the stdout should match "has a second example"
    And the stdout should match "has a first example"

  Scenario: two examples - both examples from above the first example declaration
    When I run "spec example.rb:2 --format nested"
    Then the stdout should match "2 examples, 0 failures"
    And the stdout should match "has a second example"
    And the stdout should match "has a first example"
