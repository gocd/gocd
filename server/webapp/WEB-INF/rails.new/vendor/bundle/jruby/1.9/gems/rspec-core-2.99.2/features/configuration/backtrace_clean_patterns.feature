Feature: Backtrace cleaning

  To aid in diagnozing spec failures, RSpec cleans matching lines from backtraces. The default patterns cleaned are:

    /\/lib\d*\/ruby\//,
    /org\/jruby\//,
    /bin\//,
    /gems/,
    /spec\/spec_helper\.rb/,
    /lib\/rspec\/(core|expectations|matchers|mocks)/

  This list can be modified or replaced with the `backtrace_clean_patterns` option. Additionally, rspec can be run with the `--backtrace` option to skip backtrace cleaning entirely.

  Scenario: default configuration
    Given a file named "spec/failing_spec.rb" with:
    """ruby
    describe "2 + 2" do
      it "is 5" do
        (2+2).should eq(5)
      end
    end
    """
    When I run `rspec`
    Then the output should contain "1 example, 1 failure"
    And the output should not contain "lib/rspec/expectations"

  Scenario: With a custom setting for backtrace_clean_patterns
    Given a file named "spec/spec_helper.rb" with:
    """ruby
    RSpec.configure do |config|
      config.backtrace_clean_patterns = [
        /spec_helper/
      ]
    end

    def foo
      "bar"
    end
    """
    And a file named "spec/example_spec.rb" with:
    """ruby
    require 'spec_helper'
    describe "foo" do
      it "returns baz" do
        foo.should eq("baz")
      end
    end
    """
    When I run `rspec`
    Then the output should contain "1 example, 1 failure"
    And the output should contain "lib/rspec/expectations"

  Scenario: Adding a pattern
    Given a file named "spec/matchers/be_baz_matcher.rb" with:
    """ruby
    RSpec::Matchers.define :be_baz do |_|
      match do |actual|
        actual == "baz"
      end
    end
    """
    And a file named "spec/example_spec.rb" with:
    """ruby
    RSpec.configure do |config|
      config.backtrace_clean_patterns << /be_baz_matcher/
    end

    describe "bar" do
      it "is baz" do
        "bar".should be_baz
      end
    end
    """
    When I run `rspec`
    Then the output should contain "1 example, 1 failure"
    But the output should not contain "be_baz_matcher"
    And the output should not contain "lib/rspec/expectations"

  Scenario: Running with the --backtrace option
    Given a file named "spec/matchers/be_baz_matcher.rb" with:
    """ruby
    RSpec::Matchers.define :be_baz do |_|
      match do |actual|
        actual == "baz"
      end
    end
    """
    And a file named "spec/example_spec.rb" with:
    """ruby
    RSpec.configure do |config|
      config.backtrace_clean_patterns << /be_baz_matcher/
    end

    describe "bar" do
      it "is baz" do
        "bar".should be_baz
      end
    end
    """
    When I run `rspec --backtrace`
    Then the output should contain "1 example, 1 failure"
    And the output should not contain "be_baz_matcher"
