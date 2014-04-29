Feature: start_with matcher

  Use the `start_with` matcher to specify that a string or array starts with
  the expected characters or elements.

    ```ruby
    "this string".should start_with("this")
    "this string".should_not start_with("that")
    [0,1,2].should start_with(0, 1)
    ```

  Scenario: with a string
    Given a file named "example_spec.rb" with:
      """ruby
      describe "this string" do
        it { should start_with "this" }
        it { should_not start_with "that" }

        # deliberate failures
        it { should_not start_with "this" }
        it { should start_with "that" }
      end
      """
    When I run `rspec example_spec.rb`
    Then the output should contain all of these:
      | 4 examples, 2 failures                          |
      | expected "this string" not to start with "this" |
      | expected "this string" to start with "that"     |

  Scenario: with an array
    Given a file named "example_spec.rb" with:
      """ruby
      describe [0, 1, 2, 3, 4] do
        it { should start_with 0 }
        it { should start_with(0, 1)}
        it { should_not start_with(2) }
        it { should_not start_with(0, 1, 2, 3, 4, 5) }

        # deliberate failures
        it { should_not start_with 0 }
        it { should start_with 3 }
      end
      """
    When I run `rspec example_spec.rb`
    Then the output should contain all of these:
      | 6 examples, 2 failures                       |
      | expected [0, 1, 2, 3, 4] not to start with 0 |
      | expected [0, 1, 2, 3, 4] to start with 3     |
