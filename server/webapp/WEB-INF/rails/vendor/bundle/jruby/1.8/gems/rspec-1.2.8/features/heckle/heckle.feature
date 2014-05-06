Feature: heckle a class

  As an RSpec user who wants to verify that
    my specs cover what I think it covers
  I want to heckle a class

  Scenario: Heckle finds problems
    Given a file named "heckle_fail_spec.rb" with:
      """
      class Thing
        def a_or_b
          if true
            "a"
          else
            "b"
          end
        end
      end

      describe Thing do
        it "returns a for true" do
          Thing.new.a_or_b.should == "a"
        end
      end
      """
    When I run "spec heckle_fail_spec.rb --heckle Thing"
    Then the stdout should match "The following mutations didn't cause test failures:"
    But the stdout should not match "FAILED"

  Scenario: Heckle does not find a problem
    Given a file named "heckle_success_spec.rb" with:
      """
      class Thing
        def a_or_b(key)
          if key
            "a"
          else
            "b"
          end
        end
      end

      describe Thing do
        it "returns a for true" do
          Thing.new.a_or_b(true).should == "a"
        end

        it "returns b for false" do
          Thing.new.a_or_b(false).should == "b"
        end
      end
      """
    When I run "spec heckle_success_spec.rb --heckle Thing"
    Then the stdout should match "No mutants survived"
    But the stdout should not match "FAILED"

