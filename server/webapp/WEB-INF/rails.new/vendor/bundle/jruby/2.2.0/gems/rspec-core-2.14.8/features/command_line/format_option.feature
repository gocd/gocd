Feature: --format option

  Use the --format option to tell RSpec how to format the output.

  RSpec ships with several formatters built in. By default, it uses the progress
  formatter, which generates output like this:

      ....F.....*.....

  A '.' represents a passing example, 'F' is failing, and '*' is pending.

  Use the documentation formatter to see the documentation strings passed to
  `describe`, `it`, and their aliases:

      $ rspec spec --format documentation

  You can also specify an output target ($stdout by default) with an --out
  option immediately following the --format option:

      $ rspec spec --format documentation --out rspec.txt

  Run `rspec --help` to see a listing of available formatters.

  Background:
    Given a file named "example_spec.rb" with:
      """ruby
      describe "something" do
        it "does something that passes" do
          5.should eq(5)
        end

        it "does something that fails" do
          5.should eq(4)
        end

        it "does something that is pending", :pending => true do
          5.should be > 3
        end
      end
      """

  Scenario: progress bar format (default)
    When I run `rspec --format progress example_spec.rb`
    Then the output should contain ".F*"

  Scenario: documentation format
    When I run `rspec example_spec.rb --format documentation`
    Then the output should contain:
      """
      something
        does something that passes
        does something that fails (FAILED - 1)
        does something that is pending (PENDING: No reason given)
      """

  Scenario: documentation format saved to a file
    When I run `rspec example_spec.rb --format documentation --out rspec.txt`
    Then the file "rspec.txt" should contain:
      """
      something
        does something that passes
        does something that fails (FAILED - 1)
        does something that is pending (PENDING: No reason given)
      """

  Scenario: multiple formats and output targets
    When I run `rspec example_spec.rb --format progress --format documentation --out rspec.txt`
    Then the output should contain ".F*"
    And the file "rspec.txt" should contain:
      """
      something
        does something that passes
        does something that fails (FAILED - 1)
        does something that is pending (PENDING: No reason given)
      """
