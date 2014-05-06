Feature: expect change

  Expect some code (wrapped in a proc) to change the state of some object.
  
  Scenario: expecting change
    Given a file named "expect_change.rb" with:
      """
      class Counter
        class << self
          def increment
            @count ||= 0
            @count += 1
          end
          
          def count
            @count ||= 0
          end
        end
      end
      
      describe Counter, "#increment" do
        it "should increment the count" do
          expect{Counter.increment}.to change{Counter.count}.from(0).to(1)
        end

        # deliberate failure
        it "should increment the count by 2" do
          expect{Counter.increment}.to change{Counter.count}.by(2)
        end
      end
      """
    When I run "spec expect_change.rb"
    Then the stdout should match "2 examples, 1 failure"
    Then the stdout should match "should have been changed by 2, but was changed by 1"

  Scenario: expecting no change
    Given a file named "expect_no_change.rb" with:
      """
      class Counter
        class << self
          def increment
            @count ||= 0
            @count += 1
          end
          
          def count
            @count ||= 0
          end
        end
      end

      describe Counter, "#increment" do
        it "should not increment the count by 2" do
          expect{Counter.increment}.to_not change{Counter.count}.from(0).to(2)
        end

        # deliberate failure
        it "should not increment the count by 1" do
          expect{Counter.increment}.to_not change{Counter.count}.by(1)
        end
      end
      """
    When I run "spec expect_no_change.rb"
    Then the stdout should match "2 examples, 1 failure"
    Then the stdout should match "should not have changed, but did change from 1 to 2"
