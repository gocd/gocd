Feature: custom formatters

  In order to format output/reporting to my particular needs
  As an RSpec user
  I want to create my own custom output formatters

  Scenario: specdoc format
    Given a file named "custom_formatter.rb" with:
      """
      require 'spec/runner/formatter/base_formatter'
      class CustomFormatter < Spec::Runner::Formatter::BaseFormatter
        def initialize(options, output)
          @output = output
        end
        def example_started(proxy)
          @output << "example: " << proxy.description
        end
      end
      """
    And a file named "simple_example_spec.rb" with:
    """
      describe "my group" do
        specify "my example" do
        end
      end
    """

    When I run "spec simple_example_spec.rb --require custom_formatter.rb --format CustomFormatter"
    Then the exit code should be 0
    And the stdout should match "example: my example"
