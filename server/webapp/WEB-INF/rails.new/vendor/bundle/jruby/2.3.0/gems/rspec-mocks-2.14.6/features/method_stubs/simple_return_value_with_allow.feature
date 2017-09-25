Feature: allow with a simple return value

  Use the `allow` method with the `receive` matcher on a test double or a real
  object to tell the object to return a value (or values) in response to a given
  message. Nothing happens if the message is never received.

  Scenario: stub with no return value
    Given a file named "example_spec.rb" with:
      """ruby
      describe "a stub with no return value specified" do
        let(:collaborator) { double("collaborator") }

        it "returns nil" do
          allow(collaborator).to receive(:message)
          expect(collaborator.message).to be(nil)
        end
      end
      """
    When I run `rspec example_spec.rb`
    Then the examples should all pass

  Scenario: stubs with return values
    Given a file named "example_spec.rb" with:
      """ruby
      describe "a stub with a return value" do
        context "specified in a block" do
          it "returns the specified value" do
            collaborator = double("collaborator")
            allow(collaborator).to receive(:message) { :value }
            expect(collaborator.message).to eq(:value)
          end
        end

        context "specified with #and_return" do
          it "returns the specified value" do
            collaborator = double("collaborator")
            allow(collaborator).to receive(:message).and_return(:value)
            expect(collaborator.message).to eq(:value)
          end
        end
      end
      """
    When I run `rspec example_spec.rb`
    Then the examples should all pass
