Feature: --line_number option

  To run a examples or groups by line numbers, one can use the --line_number option:

      rspec path/to/example_spec.rb --line_number 37

  This option can be specified multiple times.

  Scenario: standard examples
    Given a file named "example_spec.rb" with:
      """ruby
      require "rspec/expectations"

      describe 9 do

        it "should be > 8" do
          9.should be > 8
        end

        it "should be < 10" do
          9.should be < 10
        end

        it "should be 3 squared" do
          9.should be 3*3
        end

      end
      """
    When I run `rspec example_spec.rb --line_number 5`
    Then the examples should all pass
    And the output should contain "1 example"

    When I run `rspec example_spec.rb --line_number 5 --line_number 9`
    Then the examples should all pass
    And the output should contain "2 examples"

  Scenario: one liner
    Given a file named "example_spec.rb" with:
      """ruby
      require "rspec/expectations"

      describe 9 do

        it { is_expected.to be > 8 }

        it { is_expected.not_to be < 10 }

      end
      """
    When I run `rspec example_spec.rb --line_number 5`
    Then the examples should all pass
    And the output should contain "1 example"
