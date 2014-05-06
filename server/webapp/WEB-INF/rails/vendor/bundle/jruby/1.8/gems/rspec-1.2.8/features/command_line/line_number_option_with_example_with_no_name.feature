Feature: Spec and test together

  As an RSpec user
  I want to run one example identified by the line number
  
  Background:
    Given a file named "example.rb" with:
      """
      describe 9 do

        it { should be > 8 }

        it { should be < 10 }
        
      end
      """

  Scenario: two examples - first example on declaration line
    When I run "spec example.rb:3 --format nested"
    Then the stdout should match "1 example, 0 failures"
    Then the stdout should match "should be > 8"
    But the stdout should not match "should be < 10"
