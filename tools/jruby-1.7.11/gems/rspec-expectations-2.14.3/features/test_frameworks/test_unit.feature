Feature: Test::Unit integration

  RSpec-expectations is a stand-alone gem that can be used without the rest of
  RSpec. If you like the way Test::Unit (or MiniTest) organizes tests, but
  prefer RSpec's approach to expressing expectations, you can have both.

  The one downside is that failures are reported as errors with MiniTest.

  Scenario: use rspec/expectations with Test::Unit
    Given a file named "rspec_expectations_test.rb" with:
      """ruby
      require 'test/unit'
      require 'rspec/expectations'

      class RSpecExpectationsTest < Test::Unit::TestCase
        RSpec::Matchers.define :be_an_integer do
          match { |actual| Integer === actual }
        end

        def be_an_int
          # This is actually an internal rspec-expectations API, but is used
          # here to demonstrate that deprecation warnings from within
          # rspec-expectations work correcty without depending on rspec-core
          RSpec.deprecate(:be_an_int, :replacement => :be_an_integer)
          be_an_integer
        end

        def test_passing_expectation
          expect(1 + 3).to eq 4
        end

        def test_failing_expectation
          expect([1,2]).to be_empty
        end

        def test_custom_matcher_with_deprecation_warning
          expect(1).to be_an_int
        end
      end
      """
     When I run `ruby rspec_expectations_test.rb`
     Then the output should contain "3 tests, 0 assertions, 0 failures, 1 errors" or "3 tests, 0 assertions, 1 failures, 0 errors"
      And the output should contain "expected empty? to return true, got false"
      And the output should contain "be_an_int is deprecated"
