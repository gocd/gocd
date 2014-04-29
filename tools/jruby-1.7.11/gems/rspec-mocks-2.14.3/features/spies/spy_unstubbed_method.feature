Feature: Spy on an unstubbed method

  Using have_received on an unstubbed method will never pass, so rspec-mocks
  issues a helpful error message.

  Scenario: fail to verify a stubbed method
    Given a file named "failed_spy_spec.rb" with:
      """ruby
      describe "have_received" do
        it "raises a helpful error for unstubbed methods" do
          object = Object.new
          object.object_id
          object.should have_received(:object_id)
        end
      end
      """
    When I run `rspec failed_spy_spec.rb`
    Then the output should contain "that method has not been stubbed"
