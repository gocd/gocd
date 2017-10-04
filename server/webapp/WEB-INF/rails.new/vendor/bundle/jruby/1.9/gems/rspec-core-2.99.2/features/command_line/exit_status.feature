Feature: exit status

  The rspec command exits with an exit status of 0 if all examples pass,
  and 1 if any examples fail. The failure exit code can be overridden
  using the --failure-exit-code option.

  Scenario: exit with 0 when all examples pass
    Given a file named "ok_spec.rb" with:
      """ruby
      describe "ok" do
        it "passes" do
        end
      end
      """
    When I run `rspec ok_spec.rb`
    Then the exit status should be 0
    And the examples should all pass

  Scenario: exit with 1 when one example fails
    Given a file named "ko_spec.rb" with:
      """ruby
      describe "KO" do
        it "fails" do
          raise "KO"
        end
      end
      """
    When I run `rspec ko_spec.rb`
    Then the exit status should be 1
    And the output should contain "1 example, 1 failure"

  Scenario: exit with 1 when a nested examples fails
    Given a file named "nested_ko_spec.rb" with:
      """ruby
      describe "KO" do
        describe "nested" do
          it "fails" do
            raise "KO"
          end
        end
      end
      """
    When I run `rspec nested_ko_spec.rb`
    Then the exit status should be 1
    And the output should contain "1 example, 1 failure"

  Scenario: exit with 0 when no examples are run
    Given a file named "a_no_examples_spec.rb" with:
      """ruby
      """
    When I run `rspec a_no_examples_spec.rb`
    Then the exit status should be 0
    And the output should contain "0 examples"

  Scenario: exit with 2 when one example fails and --failure-exit-code is 2
    Given a file named "ko_spec.rb" with:
      """ruby
      describe "KO" do
        it "fails" do
          raise "KO"
        end
      end
      """
    When I run `rspec --failure-exit-code 2 ko_spec.rb`
    Then the exit status should be 2
    And the output should contain "1 example, 1 failure"

  Scenario: exit with rspec's exit code when an at_exit hook is added upstream
    Given a file named "exit_at_spec.rb" with:
      """ruby
      require 'rspec/autorun'
      at_exit { exit(0) }

      describe "exit 0 at_exit" do
        it "does not interfere with rspec's exit code" do
          fail
        end
      end
      """
    When I run `ruby exit_at_spec.rb`
    Then the exit status should be 1
    And the output should contain "1 example, 1 failure"
