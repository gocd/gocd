Feature: end_with matcher

  Use the `end_with` matcher to specify that a string or array ends with the
  expected characters or elements.

    ```ruby
    "this string".should end_with "string"
    "this string".should_not end_with "stringy"
    [0, 1, 2].should end_with 1, 2
    ```

  Scenario: string usage
    Given a file named "example_spec.rb" with:
      """ruby
      describe "this string" do
        it { should end_with "string" }
        it { should_not end_with "stringy" }

        # deliberate failures
        it { should_not end_with "string" }
        it { should end_with "stringy" }
      end
      """
    When I run `rspec example_spec.rb`
    Then the output should contain all of these:
      | 4 examples, 2 failures                          |
      | expected "this string" not to end with "string" |
      | expected "this string" to end with "stringy"    |

  Scenario: array usage
    Given a file named "example_spec.rb" with:
      """ruby
      describe [0, 1, 2, 3, 4] do
        it { should end_with 4 }
        it { should end_with 3, 4 }
        it { should_not end_with 3 }
        it { should_not end_with 0, 1, 2, 3, 4, 5 }

        # deliberate failures
        it { should_not end_with 4 }
        it { should end_with 3 }
      end
      """
    When I run `rspec example_spec.rb`
    Then the output should contain all of these:
      | 6 examples, 2 failures                     |
      | expected [0, 1, 2, 3, 4] not to end with 4 |
      | expected [0, 1, 2, 3, 4] to end with 3     |
