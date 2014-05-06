Feature: run specific examples by line number

  In order to run a single example from command line
  RSpec allows you to specify the line number of the example(s) to run

  Scenario: --line syntax on single example
    Given a file named "example_spec.rb" with:
      """
      describe "an example" do
        it "has not yet been implemented"
        it "has been implemented" do
          true
        end
      end
      """
    When I run "spec example_spec.rb --line 2"
    Then the stdout should match "1 example, 0 failures, 1 pending"
    And the stdout should match "example_spec.rb:2"

  Scenario: colon line syntax on single example
    Given a file named "example_spec.rb" with:
      """
      describe "an example" do
        it "has not yet been implemented"
        it "has been implemented" do
          true
        end
      end
      """
    When I run "spec example_spec.rb:2"
    Then the stdout should match "1 example, 0 failures, 1 pending"
    And the stdout should match "example_spec.rb:2"
