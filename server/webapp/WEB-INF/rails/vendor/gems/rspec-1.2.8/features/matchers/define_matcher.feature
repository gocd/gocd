Feature: define matcher

  In order to express my domain clearly in my code examples
  As an RSpec user
  I want a shortcut to define custom matchers

  Scenario: define a matcher with default messages
    Given a file named "matcher_with_default_message_spec.rb" with:
      """
      Spec::Matchers.define :be_a_multiple_of do |expected|
        match do |actual|
          actual % expected == 0
        end
      end

      describe 9 do
        it {should be_a_multiple_of(3)}
      end

      describe 9 do
        it {should_not be_a_multiple_of(4)}
      end

      # fail intentionally to generate expected output
      describe 9 do
        it {should be_a_multiple_of(4)}
      end

      # fail intentionally to generate expected output
      describe 9 do
        it {should_not be_a_multiple_of(3)}
      end

      """
    When I run "spec matcher_with_default_message_spec.rb --format specdoc"
    Then the exit code should be 256

    And the stdout should match "should be a multiple of 3"
    And the stdout should match "should not be a multiple of 4"
    And the stdout should match "should be a multiple of 4 (FAILED - 1)"
    And the stdout should match "should not be a multiple of 3 (FAILED - 2)"

    And the stdout should match "4 examples, 2 failures"
    And the stdout should match "expected 9 to be a multiple of 4"
    And the stdout should match "expected 9 not to be a multiple of 3"

  Scenario: overriding the failure_message_for_should
    Given a file named "matcher_with_failure_message_spec.rb" with:
      """
      Spec::Matchers.define :be_a_multiple_of do |expected|
        match do |actual|
          actual % expected == 0
        end
        failure_message_for_should do |actual|
          "expected that #{actual} would be a multiple of #{expected}"
        end
      end

      # fail intentionally to generate expected output
      describe 9 do
        it {should be_a_multiple_of(4)}
      end
      """
    When I run "spec matcher_with_failure_message_spec.rb"
    Then the exit code should be 256
    And the stdout should match "1 example, 1 failure"
    And the stdout should match "expected that 9 would be a multiple of 4"

  Scenario: overriding the failure_message_for_should_not
    Given a file named "matcher_with_failure_for_message_spec.rb" with:
      """
      Spec::Matchers.define :be_a_multiple_of do |expected|
        match do |actual|
          actual % expected == 0
        end
        failure_message_for_should_not do |actual|
          "expected that #{actual} would not be a multiple of #{expected}"
        end
      end

      # fail intentionally to generate expected output
      describe 9 do
        it {should_not be_a_multiple_of(3)}
      end
      """
    When I run "spec matcher_with_failure_for_message_spec.rb"
    Then the exit code should be 256
    And the stdout should match "1 example, 1 failure"
    And the stdout should match "expected that 9 would not be a multiple of 3"

  Scenario: overriding the description
    Given a file named "matcher_overriding_description_spec.rb" with:
      """
      Spec::Matchers.define :be_a_multiple_of do |expected|
        match do |actual|
          actual % expected == 0
        end
        description do
          "be multiple of #{expected}"
        end
      end

      describe 9 do
        it {should be_a_multiple_of(3)}
      end

      describe 9 do
        it {should_not be_a_multiple_of(4)}
      end
      """
    When I run "spec matcher_overriding_description_spec.rb --format specdoc"
    Then the exit code should be 0
    And the stdout should match "2 examples, 0 failures"
    And the stdout should match "should be multiple of 3"
    And the stdout should match "should not be multiple of 4"

  Scenario: with no args
    Given a file named "matcher_with_no_args_spec.rb" with:
      """
      Spec::Matchers.define :have_7_fingers do
        match do |thing|
          thing.fingers.length == 7
        end
      end

      class Thing
        def fingers; (1..7).collect {"finger"}; end
      end

      describe Thing do
        it {should have_7_fingers}
      end
      """
    When I run "spec matcher_with_no_args_spec.rb --format specdoc"
    Then the exit code should be 0
    And the stdout should match "1 example, 0 failures"
    And the stdout should match "should have 7 fingers"

  Scenario: with multiple args
    Given a file named "matcher_with_multiple_args_spec.rb" with:
      """
      Spec::Matchers.define :be_the_sum_of do |a,b,c,d|
        match do |sum|
          a + b + c + d == sum
        end
      end

      describe 10 do
        it {should be_the_sum_of(1,2,3,4)}
      end
      """
    When I run "spec matcher_with_multiple_args_spec.rb --format specdoc"
    Then the exit code should be 0
    And the stdout should match "1 example, 0 failures"
    And the stdout should match "should be the sum of 1, 2, 3, and 4"
    
  Scenario: with helper methods
    Given a file named "matcher_with_internal_helper_spec.rb" with:
      """
      Spec::Matchers.define :have_same_elements_as do |sample|
        match do |actual|
          similar?(sample, actual)
        end
        
        def similar?(a, b)
          a.sort == b.sort
        end
      end
      
      describe "these two arrays" do
        specify "should be similar" do
          [1,2,3].should have_same_elements_as([2,3,1])
        end
      end
      """
    When I run "spec matcher_with_internal_helper_spec.rb"
    Then the exit code should be 0
    And the stdout should match "1 example, 0 failures"

