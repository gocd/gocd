Feature: failure exit code

  Use the feature_exit_code option to set a custom exit code when RSpec fails.

    RSpec.configure { |c| c.failure_exit_code = 42 }

  Background:
    Given a file named "spec/spec_helper.rb" with:
      """ruby
      RSpec.configure { |c| c.failure_exit_code = 42 }
      """

  Scenario: a failing spec with the default exit code
    Given a file named "spec/example_spec.rb" with:
      """ruby
      describe "something" do
        it "fails" do
          fail
        end
      end
      """
    When I run `rspec spec/example_spec.rb`
    Then the exit status should be 1

  Scenario: a failing spec with a custom exit code
    Given a file named "spec/example_spec.rb" with:
      """ruby
      require 'spec_helper'
      describe "something" do
        it "fails" do
          fail
        end
      end
      """
    When I run `rspec spec/example_spec.rb`
    Then the exit status should be 42
