Feature: --example option

  Use the --example (or -e) option to filter examples by name.

  The argument is matched against the full description of the example,
  which is the concatenation of descriptions of the group (including
  any nested groups) and the example.

  This allows you to run a single uniquely named example, all examples with
  similar names, all the examples in a uniquely named group, etc, etc.

  You can also use the option more than once to specify multiple example matches.

  Background:
    Given a file named "first_spec.rb" with:
      """ruby
      describe "first group" do
        it "first example in first group" do; end
        it "second example in first group" do; end
      end
      """
    And a file named "second_spec.rb" with:
      """ruby
      describe "second group" do
        it "first example in second group" do; end
        it "second example in second group" do; end
      end
      """
    And a file named "third_spec.rb" with:
      """ruby
      describe "third group" do
        it "first example in third group" do; end
        context "nested group" do
          it "first example in nested group" do; end
          it "second example in nested group" do; end
        end
      end
      """
    And a file named "fourth_spec.rb" with:
      """ruby
      describe Array do
        describe "#length" do
          it "is the number of items" do
            Array.new([1,2,3]).length.should eq 3
          end
        end
      end
      """

  Scenario: no matches
    When I run `rspec . --example nothing_like_this`
    Then the process should succeed even though no examples were run

  Scenario: match on one word
    When I run `rspec . --example example`
    Then the examples should all pass

  Scenario: one match in each context
    When I run `rspec . --example 'first example'`
    Then the examples should all pass

  Scenario: one match in one file using just the example name
    When I run `rspec . --example 'first example in first group'`
    Then the examples should all pass

  Scenario: one match in one file using the example name and the group name
    When I run `rspec . --example 'first group first example in first group'`
    Then the examples should all pass

  Scenario: all examples in one group
    When I run `rspec . --example 'first group'`
    Then the examples should all pass

  Scenario: one match in one file with group name
    When I run `rspec . --example 'second group first example'`
    Then the examples should all pass

  Scenario: all examples in one group including examples in nested groups
    When I run `rspec . --example 'third group'`
    Then the examples should all pass

  Scenario: Object#method
    When I run `rspec . --example 'Array#length'`
    Then the examples should all pass

  Scenario: Multiple applications of example name option
    When I run `rspec . --example 'first group' --example 'second group' --format d`
    Then the examples should all pass
    And the output should contain all of these:
      |first example in first group|
      |second example in first group|
      |first example in second group|
      |second example in second group|
    And the output should not contain any of these:
      |first example in third group|
      |nested group first example in nested group|
      |nested group second example in nested group|
