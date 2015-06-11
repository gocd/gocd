Feature: allow a message on any instance of a class

  Use `allow_any_instance_of(Class).to receive` when you want to configure how
  instances of the given class respond to a message without setting an
  expectation that the message will be received.

  Scenario: allowing a message on any instance of a class
    Given a file named "example_spec.rb" with:
      """ruby
      describe "any_instance.should_receive" do
        before do
          allow_any_instance_of(Object).to receive(:foo).and_return(:return_value)
        end

        it "allows any instance of the class to receive the message" do
          o = Object.new
          expect(o.foo).to eq(:return_value)
        end

        it "passes even if no instances receive that message" do
          o = Object.new
        end
      end
      """
    When I run `rspec example_spec.rb`
    Then the examples should all pass
