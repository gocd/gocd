Feature: Spec::ExampleGroup with should methods

  As an RSpec adopter accustomed to classes and methods
  I want to use should_* methods in an ExampleGroup
  So that I use RSpec with classes and methods that look more like RSpec examples

  Scenario Outline: Example Group class with should methods
    Given a file named "example_group_with_should_methods.rb" with:
    """
    require 'spec/autorun'

    class MySpec < Spec::ExampleGroup
      def should_pass_with_should
        1.should == 1
      end

      def should_fail_with_should
        1.should == 2
      end
    end
    """
    When I run "<Command> example_group_with_should_methods.rb"
    Then the exit code should be 256
    And the stdout should match "2 examples, 1 failure"

  Scenarios: Run with ruby and spec
    | Command |
    | ruby    |
    | spec    |
