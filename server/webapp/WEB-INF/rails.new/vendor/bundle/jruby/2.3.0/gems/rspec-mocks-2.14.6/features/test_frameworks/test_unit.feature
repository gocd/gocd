Feature: Test::Unit integration

  rspec-mocks is a stand-alone gem that can be used without the rest of
  RSpec. If you like the way Test::Unit (or MiniTest) organizes tests, but
  prefer RSpec's approach to mocking/stubbing/doubles etc, you can have both.

  The one downside is that failures are reported as errors with MiniTest.

  Scenario: use rspec/mocks with Test::Unit
    Given a file named "rspec_mocks_test.rb" with:
      """ruby
      require 'test/unit'
      require 'rspec/mocks'

      class RSpecMocksTest < Test::Unit::TestCase
        def setup
          RSpec::Mocks.setup(Object)
          RSpec::Mocks.setup(self)
        end

        def test_passing_expectation
          obj = Object.new
          expect(obj).to receive(:message)
          obj.message
        end

        def test_failing_expectation
          obj = Object.new
          expect(obj).to_not receive(:message)
          obj.message
        end

        def test_with_deprecation_warning
          obj = Object.new
          obj.stub(:old_message) { RSpec.deprecate(:old_message, :replacement => :message) }
          obj.old_message
        end
      end
      """
     When I run `ruby rspec_mocks_test.rb`
     Then the output should contain "3 tests, 0 assertions, 0 failures, 1 errors" or "3 tests, 0 assertions, 1 failures, 0 errors"
     And the output should contain "expected: 0 times with any arguments"
     And the output should contain "old_message is deprecated. Use message instead."
