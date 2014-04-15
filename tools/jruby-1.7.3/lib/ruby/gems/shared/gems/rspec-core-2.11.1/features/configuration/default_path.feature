Feature: default_path

  As of rspec-2.7, you can just type `rspec` to run all specs that live
  in the `spec` directory.

  This is supported by a `--default_path` option, which is set to `spec` by
  default. If you prefer to keep your specs in a different directory, or assign
  an individual file to `--default_path`, you can do so on the command line or
  in a configuration file (`.rspec`, `~/.rspec`, or a custom file).

  NOTE: this option is not supported on `RSpec.configuration`, as it needs to
  be set before spec files are loaded.

  Scenario: run `rspec` with default default_path (`spec` directory)
    Given a file named "spec/example_spec.rb" with:
      """
      describe "an example" do
        it "passes" do
        end
      end
      """
    When I run `rspec`
    Then the output should contain "1 example, 0 failures"
    
  Scenario: run `rspec` with customized default_path
    Given a file named ".rspec" with:
      """
      --default_path behavior
      """
    Given a file named "behavior/example_spec.rb" with:
      """      
      describe "an example" do
        it "passes" do
        end
      end
      """
    When I run `rspec`
    Then the output should contain "1 example, 0 failures"
