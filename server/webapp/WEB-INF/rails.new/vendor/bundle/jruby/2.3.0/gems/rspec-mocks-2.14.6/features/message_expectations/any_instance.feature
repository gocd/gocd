Feature: expect a message on any instance of a class

  Use `any_instance.should_receive` to set an expectation that one (and only
  one) instance of a class receives a message before the example is completed.

  The spec will fail if no instance receives a message.

  Scenario: expect a message on any instance of a class
    Given a file named "example_spec.rb" with:
      """ruby
      describe "any_instance.should_receive" do
        it "verifies that one instance of the class receives the message" do
          Object.any_instance.should_receive(:foo).and_return(:return_value)

          o = Object.new
          o.foo.should eq(:return_value)
        end
      end
      """
    When I run `rspec example_spec.rb`
    Then the examples should all pass