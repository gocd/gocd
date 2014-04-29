Feature: stub with a simple return value

  Use the `stub` method on a test double or a real object to tell the object to
  return a value (or values) in response to a given message. Nothing happens if
  the message is never received.

  Scenario: stub with no return value
    Given a file named "example_spec.rb" with:
      """ruby
      describe "a stub with no return value specified" do
        let(:collaborator) { double("collaborator") }

        it "returns nil" do
          collaborator.stub(:message)
          collaborator.message.should be(nil)
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
            collaborator.stub(:message) { :value }
            collaborator.message.should eq(:value)
          end
        end

        context "specified with #and_return" do
          it "returns the specified value" do
            collaborator = double("collaborator")
            collaborator.stub(:message).and_return(:value)
            collaborator.message.should eq(:value)
          end
        end

        context "specified with a hash passed to #stub" do
          it "returns the specified value" do
            collaborator = double("collaborator")
            collaborator.stub(:message_1 => :value_1, :message_2 => :value_2)
            collaborator.message_1.should eq(:value_1)
            collaborator.message_2.should eq(:value_2)
          end
        end

        context "specified with a hash passed to #double" do
          it "returns the specified value" do
            collaborator = double("collaborator",
              :message_1 => :value_1,
              :message_2 => :value_2
            )
            collaborator.message_1.should eq(:value_1)
            collaborator.message_2.should eq(:value_2)
          end
        end
      end
      """
    When I run `rspec example_spec.rb`
    Then the examples should all pass
