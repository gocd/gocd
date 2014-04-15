Feature: expect error

  Expect a proc to change the state of some object.
  
  Scenario: expect error
    Given a file named "expect_error.rb" with:
      """
      describe Object, "#non_existent_message" do
        it "should raise" do
          expect{Object.non_existent_message}.to raise_error(NameError)
        end
      end

      #deliberate failure
      describe Object, "#public_instance_methods" do
        it "should raise" do
          expect{Object.public_instance_methods}.to raise_error(NameError)
        end
      end
      """
    When I run "spec expect_error.rb"
    Then the stdout should match "2 examples, 1 failure"
    Then the stdout should match "expected NameError but nothing was raised"

  Scenario: expect no error
    Given a file named "expect_no_error.rb" with:
      """
      describe Object, "#public_instance_methods" do
        it "should not raise" do
          expect{Object.public_instance_methods}.to_not raise_error(NameError)
        end
      end

      #deliberate failure
      describe Object, "#non_existent_message" do
        it "should not raise" do
          expect{Object.non_existent_message}.to_not raise_error(NameError)
        end
      end
      """
    When I run "spec expect_no_error.rb"
    Then the stdout should match "2 examples, 1 failure"
    Then the stdout should match "undefined method `non_existent_message'"

