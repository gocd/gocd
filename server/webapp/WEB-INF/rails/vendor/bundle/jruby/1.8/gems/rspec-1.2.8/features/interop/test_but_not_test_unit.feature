Story: Test is defined, but not Test::Unit

  As an RSpec user who has my own library named Test (but not Test::Unit)
  I want to run examples without getting Test::Unit NameErrors

  Scenario Outline: Spec including Test const but not Test::Unit
    Given a file named "spec_including_test_but_not_unit.rb" with:
    """
    require 'spec/autorun'

    module Test
    end

    describe "description" do
      it "should description" do
        1.should == 1
      end
    end
    """
    When I run "<Command> spec_including_test_but_not_unit.rb"
    Then the stderr should not match "Test::Unit"

  Scenarios: Run with ruby and spec
    | Command |
    | ruby    |
    | spec    |
