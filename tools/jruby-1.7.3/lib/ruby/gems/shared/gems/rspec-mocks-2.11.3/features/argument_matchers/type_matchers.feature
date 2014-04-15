Feature: stub with argument constraints

  You can further specify the behavior by constraining the type,
  format and/or number of arguments with the `#with()` method 
  chained off of `#stub()`
  
  Scenario: an_instance_of argument matcher
    Given a file named "stub_an_instance_of_args_spec.rb" with:
      """
      describe "stubbed an_instance_of() args spec" do
        it "works" do
          object = Object.new
          object.stub(:foo).with(an_instance_of(Symbol)) do
            "symbol"
          end
          object.stub(:foo).with(an_instance_of(String)) do
            "string"
          end

          object.foo("bar").should eq("string")
          object.foo(:that).should eq("symbol")
        end
      end
      """
    When I run `rspec stub_an_instance_of_args_spec.rb`
    Then the output should contain "1 example, 0 failures"

