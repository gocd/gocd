Feature: mock with flexmock

  As an RSpec user who prefers flexmock
  I want to be able to use flexmock without rspec mocks interfering

  Scenario: Mock with flexmock
    Given a file named "flexmock_example_spec.rb" with:
      """
      Spec::Runner.configure do |config|
        config.mock_with :flexmock
      end

      describe "plugging in flexmock" do
        it "allows flexmock to be used" do
          target = Object.new
          flexmock(target).should_receive(:foo).once
          target.foo
        end

        it "does not include rspec mocks" do
          Spec.const_defined?(:Mocks).should be_false
        end
      end
      """
    When I run "spec flexmock_example_spec.rb"
    Then the exit code should be 0
    And the stdout should match "2 examples, 0 failures"
