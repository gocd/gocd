Feature: "be" matchers

  There are several related "be" matchers:

    ```ruby
    obj.should be_truthy  # passes if obj is truthy (not nil or false)
    obj.should be_falsey # passes if obj is falsy (nil or false)
    obj.should be_nil   # passes if obj is nil
    obj.should be       # passes if obj is truthy (not nil or false)
    ```

  Scenario: be_truthy matcher
    Given a file named "be_truthy_spec.rb" with:
      """ruby
      describe "be_truthy matcher" do
        specify { true.should be_truthy }
        specify { 7.should be_truthy }
        specify { "foo".should be_truthy }
        specify { nil.should_not be_truthy }
        specify { false.should_not be_truthy }

        # deliberate failures
        specify { true.should_not be_truthy }
        specify { 7.should_not be_truthy }
        specify { "foo".should_not be_truthy }
        specify { nil.should be_truthy }
        specify { false.should be_truthy }
      end
      """
    When I run `rspec be_truthy_spec.rb`
    Then the output should contain "10 examples, 5 failures"
    And the output should contain:
      """
             expected: falsey value
                  got: true
      """
    And the output should contain:
      """
             expected: falsey value
                  got: 7
      """
    And the output should contain:
      """
             expected: falsey value
                  got: "foo"
      """
    And the output should contain:
      """
             expected: truthy value
                  got: nil
      """
    And the output should contain:
      """
             expected: truthy value
                  got: false
      """

  Scenario: be_falsey matcher
    Given a file named "be_falsey_spec.rb" with:
      """ruby
      describe "be_falsey matcher" do
        specify { nil.should be_falsey }
        specify { false.should be_falsey }
        specify { true.should_not be_falsey }
        specify { 7.should_not be_falsey }
        specify { "foo".should_not be_falsey }

        # deliberate failures
        specify { nil.should_not be_falsey }
        specify { false.should_not be_falsey }
        specify { true.should be_falsey }
        specify { 7.should be_falsey }
        specify { "foo".should be_falsey }
      end
      """
    When I run `rspec be_falsey_spec.rb`
    Then the output should contain "10 examples, 5 failures"
    And the output should contain:
      """
             expected: truthy value
                  got: nil
      """
    And the output should contain:
      """
             expected: truthy value
                  got: false
      """
    And the output should contain:
      """
             expected: falsey value
                  got: true
      """
    And the output should contain:
      """
             expected: falsey value
                  got: 7
      """
    And the output should contain:
      """
             expected: falsey value
                  got: "foo"
      """

  Scenario: be_nil matcher
    Given a file named "be_nil_spec.rb" with:
      """ruby
      describe "be_nil matcher" do
        specify { nil.should be_nil }
        specify { false.should_not be_nil }
        specify { true.should_not be_nil }
        specify { 7.should_not be_nil }
        specify { "foo".should_not be_nil }

        # deliberate failures
        specify { nil.should_not be_nil }
        specify { false.should be_nil }
        specify { true.should be_nil }
        specify { 7.should be_nil }
        specify { "foo".should be_nil }
      end
      """
    When I run `rspec be_nil_spec.rb`
    Then the output should contain "10 examples, 5 failures"
    And the output should contain:
      """
             expected: not nil
                  got: nil
      """
    And the output should contain:
      """
             expected: nil
                  got: false
      """
    And the output should contain:
      """
             expected: nil
                  got: true
      """
    And the output should contain:
      """
             expected: nil
                  got: 7
      """
    And the output should contain:
      """
             expected: nil
                  got: "foo"
      """

  Scenario: be matcher
    Given a file named "be_spec.rb" with:
      """ruby
      describe "be_matcher" do
        specify { true.should be }
        specify { 7.should be }
        specify { "foo".should be }
        specify { nil.should_not be }
        specify { false.should_not be }

        # deliberate failures
        specify { true.should_not be }
        specify { 7.should_not be }
        specify { "foo".should_not be }
        specify { nil.should be }
        specify { false.should be }
      end
      """
    When I run `rspec be_spec.rb`
    Then the output should contain all of these:
      | 10 examples, 5 failures             |
      | expected true to evaluate to false  |
      | expected 7 to evaluate to false     |
      | expected "foo" to evaluate to false |
      | expected nil to evaluate to true    |
      | expected false to evaluate to true  |
