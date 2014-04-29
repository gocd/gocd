Feature: Spy on a stubbed method on a pure mock

  You can use `have_received` to verify that a stubbed method was invoked,
  rather than setting an expectation for it to be invoked beforehand.

  Scenario: verify a stubbed method
    Given a file named "verified_spy_spec.rb" with:
      """ruby
      describe "have_received" do
        it "passes when the expectation is met" do
          invitation = double('invitation', :deliver => true)
          invitation.deliver
          invitation.should have_received(:deliver)
        end
      end
      """
    When I run `rspec verified_spy_spec.rb`
    Then the examples should all pass

  Scenario: verify a stubbed method with message expectations
    Given a file named "verified_message_expectations_spec.rb" with:
      """ruby
      describe "have_received" do
        it "passes when the expectation is met" do
          invitation = double('invitation', :deliver => true)
          2.times { invitation.deliver(:expected, :arguments) }
          invitation.should have_received(:deliver).
            with(:expected, :arguments).
            twice
        end
      end
      """
    When I run `rspec verified_message_expectations_spec.rb`
    Then the examples should all pass

  Scenario: fail to verify a stubbed method
    Given a file named "failed_spy_spec.rb" with:
      """ruby
      describe "have_received" do
        it "fails when the expectation is not met" do
          invitation = double('invitation', :deliver => true)
          invitation.should have_received(:deliver)
        end
      end
      """
    When I run `rspec failed_spy_spec.rb`
    Then the output should contain "expected: 1 time"
     And the output should contain "received: 0 times"

  Scenario: fail to verify message expectations
    Given a file named "failed_message_expectations_spec.rb" with:
      """ruby
      describe "have_received" do
        it "fails when the arguments are different" do
          invitation = double('invitation', :deliver => true)
          invitation.deliver(:unexpected)
          invitation.should have_received(:deliver).with(:expected, :arguments)
        end
      end
      """
    When I run `rspec failed_message_expectations_spec.rb`
    Then the output should contain "expected: (:expected, :arguments)"
    And the output should contain "got: (:unexpected)"

  Scenario: generate a spy message
    Given a file named "spy_message_spec.rb" with:
      """ruby
      describe "have_received" do
      subject(:invitation) { double('invitation', :deliver => true) }
        before { invitation.deliver }

        it { should have_received(:deliver) }
      end
      """
    When I run `rspec --format documentation spy_message_spec.rb`
    Then the output should contain "should have received deliver"
