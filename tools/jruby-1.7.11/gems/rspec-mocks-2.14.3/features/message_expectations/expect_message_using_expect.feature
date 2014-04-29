Feature: expect message using `expect`

  Use `expect(object).to receive(:message)` to set an expectation that
  `object` should receive the message `:message` before the example is
  completed.

  Note: You can use `expect_any_instance_of` when you don't have a reference
  to the object that receives a message in your test. For more information,
  see the message_expectations/expect_any_instance_of feature.

  Scenario: expect a message
    Given a file named "spec/account_spec.rb" with:
      """ruby
      require "account"

      describe Account do
        context "when closed" do
          it "logs an account closed message" do
            logger = double("logger")
            account = Account.new logger

            expect(logger).to receive(:account_closed)

            account.close
          end
        end
      end
      """
    And a file named "lib/account.rb" with:
      """ruby
      Account = Struct.new(:logger) do
        def close
          logger.account_closed
        end
      end
      """
    When I run `rspec spec/account_spec.rb`
    Then the output should contain "1 example, 0 failures"

  Scenario: expect a message with an argument
    Given a file named "spec/account_spec.rb" with:
      """ruby
      require "account"

      describe Account do
        context "when closed" do
          it "logs an account closed message" do
            logger = double("logger")
            account = Account.new logger

            expect(logger).to receive(:account_closed).with(account)

            account.close
          end
        end
      end
      """
    And a file named "lib/account.rb" with:
      """ruby
      Account = Struct.new(:logger) do
        def close
          logger.account_closed(self)
        end
      end
      """
    When I run `rspec spec/account_spec.rb`
    Then the output should contain "1 example, 0 failures"

  Scenario: provide a return value
    Given a file named "spec/message_expectation_spec.rb" with:
      """ruby

      describe "a message expectation with a return value" do
        context "specified in a block" do
          it "returns the specified value" do
            object = double("object")
            expect(object).to receive(:message) { :return_value }
            object.message.should eq(:return_value)
          end
        end

        context "specified with and_return" do
          it "returns the specified value" do
            object = double("object")
            expect(object).to receive(:message).and_return(:return_value)
            object.message.should eq(:return_value)
          end
        end
      end
      """
    When I run `rspec spec/message_expectation_spec.rb`
    Then the output should contain "2 examples, 0 failures"

  Scenario: expect a specific number of calls
    Given a file named "spec/message_count_spec.rb" with:
      """ruby
      describe "a message expectation with a count" do
        it "passes if the expected number of calls happen" do
          string = "hi"
          expect(string).to receive(:length).exactly(3).times

          3.times { string.length }
        end
      end
      """
    When I run `rspec spec/message_count_spec.rb`
    Then the output should contain "1 example, 0 failures"
