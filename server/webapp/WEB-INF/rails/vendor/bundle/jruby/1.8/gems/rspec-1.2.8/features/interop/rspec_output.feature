Feature: spec output

  When running in interop mode with test/unit, RSpec will output
  the RSpec summary, but not the test/unit summary.

  Scenario Outline: Interop mode with test/unit
    Given a file named "simple_spec.rb" with:
    """
    require 'spec/autorun'

    describe "Running an Example" do
      it "should not output twice" do
        true.should be_true
      end
    end
    """
    When I run "<Command> simple_spec.rb"
    Then the exit code should be 0
    And the stdout should not match /\d+ tests, \d+ assertions, \d+ failures, \d+ errors/m
    And the stdout should match "1 example, 0 failures"

  Scenarios: Run with ruby and CommandLine object
    | Command     |
    | ruby        |
    | cmdline.rb  |
