Feature: mock with mocha

  As an RSpec user who prefers mocha
  I want to be able to use mocha without rspec mocks interfering

  Scenario: Mock with mocha
    Given a file named "mocha_example_spec.rb" with:
      """
      Spec::Runner.configure do |config|
        config.mock_with :mocha
      end

      describe "plugging in mocha" do
        it "allows mocha to be used" do
          target = Object.new
          target.expects(:foo).once
          target.foo
        end

        it "does not include rspec mocks" do
          Spec.const_defined?(:Mocks).should be_false
        end
      end
      """
    When I run "spec mocha_example_spec.rb"
    Then the exit code should be 0
    And the stdout should match "2 examples, 0 failures"
