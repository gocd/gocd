Feature: --tag option

  Use the --tag (or -t) option to filter the examples by tags.

  The tag can be a simple name or a name:value pair. In the first case,
  examples with :name => true will be filtered. In the second case, examples
  with :name => value will be filtered, where value is always a string.  In
  both cases, name is converted to a symbol.

  Tags can also be used to exclude examples by adding a ~ before the tag.  For
  example ~tag will exclude all examples marked with :tag => true and
  ~tag:value will exclude all examples marked with :tag => value.

  To be compatible with the Cucumber syntax, tags can optionally start with
  an @ symbol, which will be ignored.

  Background:
    Given a file named "tagged_spec.rb" with:
      """ruby
      describe "group with tagged specs" do
        it "example I'm working now", :focus => true do; end
        it "special example with string", :type => 'special' do; end
        it "special example with symbol", :type => :special do; end
        it "slow example", :skip => true do; end
        it "ordinary example", :speed => 'slow' do; end
        it "untagged example" do; end
      end
      """

  Scenario: filter examples with non-existent tag
    When I run `rspec . --tag mytag`
    Then the process should succeed even though no examples were run

  Scenario: filter examples with a simple tag
    When I run `rspec . --tag focus`
    Then the output should contain "include {:focus=>true}"
    And the examples should all pass

  Scenario: filter examples with a simple tag and @
    When I run `rspec . --tag @focus`
    Then the output should contain "include {:focus=>true}"
    Then the examples should all pass

  Scenario: filter examples with a name:value tag
    When I run `rspec . --tag type:special`
    Then the output should contain:
      """
      include {:type=>"special"}
      """
    And the output should contain "2 examples"
    And the examples should all pass

  Scenario: filter examples with a name:value tag and @
    When I run `rspec . --tag @type:special`
    Then the output should contain:
      """
      include {:type=>"special"}
      """
    And the examples should all pass

  Scenario: exclude examples with a simple tag
    When I run `rspec . --tag ~skip`
    Then the output should contain "exclude {:skip=>true}"
    Then the examples should all pass

  Scenario: exclude examples with a simple tag and @
    When I run `rspec . --tag ~@skip`
    Then the output should contain "exclude {:skip=>true}"
    Then the examples should all pass

  Scenario: exclude examples with a name:value tag
    When I run `rspec . --tag ~speed:slow`
    Then the output should contain:
      """
      exclude {:speed=>"slow"}
      """
    Then the examples should all pass

  Scenario: exclude examples with a name:value tag and @
    When I run `rspec . --tag ~@speed:slow`
    Then the output should contain:
      """
      exclude {:speed=>"slow"}
      """
    Then the examples should all pass

  Scenario: filter examples with a simple tag, exclude examples with another tag
    When I run `rspec . --tag focus --tag ~skip`
    Then the output should contain "include {:focus=>true}"
    And the output should contain "exclude {:skip=>true}"
    And the examples should all pass

  Scenario: exclude examples with multiple tags
    When I run `rspec . --tag ~skip --tag ~speed:slow`
    Then the output should contain one of the following:
      | exclude {:skip=>true, :speed=>"slow"} |
      | exclude {:speed=>"slow", :skip=>true} |
    Then the examples should all pass
