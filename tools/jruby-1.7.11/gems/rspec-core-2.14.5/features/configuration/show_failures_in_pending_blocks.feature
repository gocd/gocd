Feature: show_failures_in_pending_blocks

  Use the show_failures_in_pending_blocks option to run the code in pending blocks while keeping the tests pending.

    RSpec.configure { |c| c.show_failures_in_pending_blocks = true }

  Background:
    Given a file named "spec/spec_helper.rb" with:
      """ruby
      RSpec.configure {|c| c.show_failures_in_pending_blocks = true}
      """

  Scenario: by default, code in pending examples is not exercised
    Given a file named "spec/example_spec.rb" with:
      """ruby
      describe "fails" do
        pending "code will not be exercised" do
          fail
        end
      end
      """
    When I run `rspec spec/example_spec.rb`
    Then the output should not contain "Failure/Error: pending { fail }"

  Scenario: by default, code in pending blocks inside examples is not exercised
    Given a file named "spec/example_spec.rb" with:
      """ruby
      describe "fails" do
        it "code will not be exercised" do
          pending { fail }
        end
      end
      """
    When I run `rspec spec/example_spec.rb`
    Then the output should not contain "Failure/Error: pending { fail }"

  Scenario: when turned on, pending code blocks inside examples are exercised
    Given a file named "spec/example_spec.rb" with:
      """ruby
      require "spec_helper"
      describe "fails" do
        it "code will be exercised" do
          pending { fail }
        end
      end
      """
    When I run `rspec spec/example_spec.rb`
    Then the output should contain "Failure/Error: pending { fail }"

  Scenario: when turned on, code inside pending examples is not exercised
    Given a file named "spec/example_spec.rb" with:
      """ruby
      require "spec_helper"
      describe "fails" do
        pending "code will not be exercised" do
          fail
        end
      end
      """
    When I run `rspec spec/example_spec.rb`
    Then the output should not contain "Failure/Error: pending { fail }"
