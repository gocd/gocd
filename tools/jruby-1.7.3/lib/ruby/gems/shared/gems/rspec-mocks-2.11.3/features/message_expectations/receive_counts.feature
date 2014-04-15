Feature: receive counts

  Scenario: expect a message once
    Given a file named "spec/account_spec.rb" with:
      """
      class Account
        attr_accessor :logger

        def open
          logger.account_opened
        end
      end

      describe Account do
        context "when opened" do
          it "logger#account_opened was called once" do
            logger = double("logger")
            account = Account.new
            account.logger = logger

            logger.should_receive(:account_opened).once

            account.open
          end
        end
      end
      """
    When I run `rspec spec/account_spec.rb`
    Then the output should contain "1 example, 0 failures"

  Scenario: expect a message twice
    Given a file named "spec/account_spec.rb" with:
      """
      class Account
        attr_accessor :logger

        def open
          logger.account_opened
        end
      end

      describe Account do
        context "when opened" do
          it "logger#account_opened was called once" do
            logger = double("logger")
            account = Account.new
            account.logger = logger

            logger.should_receive(:account_opened).twice

            account.open
            account.open
          end
        end
      end
      """
    When I run `rspec spec/account_spec.rb`
    Then the output should contain "1 example, 0 failures"

  Scenario: expect a message 3 times
    Given a file named "spec/account_spec.rb" with:
      """
      class Account
        attr_accessor :logger

        def open
          logger.account_opened
        end
      end

      describe Account do
        context "when opened" do
          it "logger#account_opened was called once" do
            logger = double("logger")
            account = Account.new
            account.logger = logger

            logger.should_receive(:account_opened).exactly(3).times

            account.open
            account.open
            account.open
          end
        end
      end
      """
    When I run `rspec spec/account_spec.rb`
    Then the output should contain "1 example, 0 failures"

  Scenario: expect a message at least (:once)
    Given a file named "spec/account_spec.rb" with:
      """
      class Account
        attr_accessor :logger

        def open
          logger.account_opened
        end
      end

      describe Account do
        context "when opened" do
          it "logger#account_opened was called once" do
            logger = double("logger")
            account = Account.new
            account.logger = logger

            logger.should_receive(:account_opened).at_least(:once)

            account.open
          end
        end
      end
      """
    When I run `rspec spec/account_spec.rb`
    Then the output should contain "1 example, 0 failures"


  Scenario: expect a message at least (n) times
    Given a file named "spec/account_spec.rb" with:
      """
      class Account
        attr_accessor :logger

        def open
          logger.account_opened
        end
      end

      describe Account do
        context "when opened" do
          it "logger#account_opened was called once" do
            logger = double("logger")
            account = Account.new
            account.logger = logger

            logger.should_receive(:account_opened).at_least(3).times

            # Note that I am calling method under test 4 times
            # and I specified it to be called at least 3 times
            account.open
            account.open
            account.open
            account.open
          end
        end
      end
      """
    When I run `rspec spec/account_spec.rb`
    Then the output should contain "1 example, 0 failures"

  Scenario: expect a message at most (:once)
    Given a file named "spec/account_spec.rb" with:
      """
      class Account
        attr_accessor :logger

        def open
          logger.account_opened
        end
      end

      describe Account do
        context "when opened" do
          it "logger#account_opened was called once" do
            logger = double("logger")
            account = Account.new
            account.logger = logger

            logger.should_receive(:account_opened).at_most(:once)

            account.open
            account.open
          end
        end
      end
      """
    When I run `rspec spec/account_spec.rb`
    Then the output should contain "expected: 1 time"
     And the output should contain "received: 2 times"

  Scenario: expect a message at most (n) times
    Given a file named "spec/account_spec.rb" with:
      """
      class Account
        attr_accessor :logger

        def open
          logger.account_opened
        end
      end

      describe Account do
        context "when opened" do
          it "logger#account_opened was called once" do
            logger = double("logger")
            account = Account.new
            account.logger = logger

            logger.should_receive(:account_opened).at_most(2).times

            account.open
            account.open
          end
        end
      end
      """
    When I run `rspec spec/account_spec.rb`
    Then the output should contain "1 example, 0 failures"
