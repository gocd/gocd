Feature: configure expectation framework

  By default, RSpec is configured to include rspec-expectations for expressing
  desired outcomes. You can also configure RSpec to use:

  * rspec/expectations (explicitly)
  * stdlib assertions **DEPRECATED: Use test/unit or minitest explicitly**
  * test/unit assertions
  * minitest assertions
  * any combination of the above libraries

  Note that when you do not use rspec-expectations, you must explicitly provide
  a description to every example.  You cannot rely on the generated descriptions
  provided by rspec-expectations.

  Scenario: Default configuration uses rspec-expectations
    Given a file named "example_spec.rb" with:
      """ruby
      RSpec::Matchers.define :be_a_multiple_of do |factor|
        match do |actual|
          actual % factor == 0
        end
      end

      describe 6 do
        it { is_expected.to be_a_multiple_of 3 }
      end
      """
    When I run `rspec example_spec.rb`
    Then the examples should all pass

  Scenario: Configure rspec-expectations (explicitly)
    Given a file named "example_spec.rb" with:
      """ruby
      RSpec.configure do |config|
        config.expect_with :rspec
      end

      describe 5 do
        it "is greater than 4" do
          5.should be > 4
        end
      end
      """
    When I run `rspec example_spec.rb`
    Then the examples should all pass

  @deprecated
  Scenario: Configure 'stdlib' assertions
    Given a file named "example_spec.rb" with:
      """ruby
      RSpec.configure do |config|
        config.expect_with :stdlib
      end

      describe 5 do
        it "is greater than 4" do
          assert 5 > 4, "expected 5 to be greater than 4"
        end

        specify { assert 5 < 6 }

        it "is greater than 6 (no it isn't!)" do
          assert 5 > 6, "errantly expected 5 to be greater than 5"
        end
      end
      """
    When I run `rspec example_spec.rb`
    Then the output should match:
      """
           (Test::Unit::AssertionFailedError|Mini(T|t)est::Assertion):
             errantly expected 5 to be greater than 5
      """
    And  the output should contain "3 examples, 1 failure"
    And  the output should contain ":stdlib is deprecated. Use :test_unit or :minitest instead"

  Scenario: Configure test/unit assertions
    Given a file named "example_spec.rb" with:
      """ruby
      RSpec.configure do |config|
        config.expect_with :test_unit
      end

      RSpec.describe [1] do
        it "is equal to [1]" do
          assert_equal [1], [1], "expected [1] to equal [1]"
        end

        specify { assert_not_equal [1], [] }

        it "is equal to [2] (intentional failure)" do
          assert [1] == [2], "errantly expected [2] to equal [1]"
        end
      end
      """
    When I run `rspec example_spec.rb`
    Then the output should match:
      """
           (Test::Unit::AssertionFailedError|Mini(T|t)est::Assertion):
             errantly expected \[2\] to equal \[1\]
      """
    And  the output should contain "3 examples, 1 failure"

  Scenario: Configure minitest assertions
    Given a file named "example_spec.rb" with:
      """ruby
      RSpec.configure do |config|
        config.expect_with :minitest
      end

      RSpec.describe "Object identity" do
        it "the an object is the same as itself" do
          x = [1]
          assert_same x, x, "expected x to be the same x"
        end

        specify { refute_same [1], [1] }

        it "is empty (intentional failure)" do
          assert_empty [1], "errantly expected [1] to be empty"
        end
      end
      """
    When I run `rspec -b example_spec.rb`
    Then the output should match:
      """
           MiniT|test::Assertion:
             errantly expected \[1\] to be empty
      """
    And  the output should contain "3 examples, 1 failure"
    And  the output should not contain "Warning: you should require 'minitest/autorun' instead."

  Scenario: Configure rspec/expectations AND test/unit assertions
    Given a file named "example_spec.rb" with:
      """ruby
      RSpec.configure do |config|
        config.expect_with :rspec, :test_unit
      end

      RSpec.describe [1] do
        it "is equal to [1]" do
          assert_equal [1], [1], "expected [1] to equal [1]"
        end

        it "matches array [1]" do
          is_expected.to match_array([1])
        end
      end
      """
    When I run `rspec example_spec.rb`
    Then the examples should all pass

  Scenario: Configure rspec/expecations AND minitest assertions
    Given a file named "example_spec.rb" with:
      """ruby
      RSpec.configure do |config|
        config.expect_with :rspec, :minitest
      end

      RSpec.describe "Object identity" do
        it "two arrays are not the same object" do
          refute_same [1], [1]
        end

        it "an array is itself" do
          array = [1]
          expect(array).to be array
        end
      end
      """
    When I run `rspec example_spec.rb`
    Then the examples should all pass

  Scenario: Configure test/unit and minitest assertions
    Given a file named "example_spec.rb" with:
      """ruby
      RSpec.configure do |config|
        config.expect_with :test_unit, :minitest
      end

      RSpec.describe [1] do
        it "is equal to [1]" do
          assert_equal [1], [1], "expected [1] to equal [1]"
        end

        specify { assert_not_equal [1], [] }

        it "the an object is the same as itself" do
          x = [1]
          assert_same x, x, "expected x to be the same x"
        end

        specify { refute_same [1], [1] }
      end
      """
    When I run `rspec example_spec.rb`
    Then the examples should all pass
