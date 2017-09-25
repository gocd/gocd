Feature: Hide Defined Constant

  Use `hide_const` to remove a constant for the duration of a test.

  Scenario: Hide top-level constant
    Given a file named "hide_const_spec.rb" with:
      """ruby
      FOO = 7

      describe "hiding FOO" do
        it "can hide FOO" do
          hide_const("FOO")
          expect { FOO }.to raise_error(NameError)
        end

        it "restores the hidden constant when the example completes" do
          FOO.should eq(7)
        end
      end
      """
    When I run `rspec hide_const_spec.rb`
    Then the examples should all pass

  Scenario: Hide nested constant
    Given a file named "hide_const_spec.rb" with:
      """ruby
      module MyGem
        class SomeClass
          FOO = 7
        end
      end

      module MyGem
        describe SomeClass do
          it "hides the nested constant when it is fully qualified" do
            hide_const("MyGem::SomeClass::FOO")
            expect { SomeClass::FOO }.to raise_error(NameError)
          end

          it "restores the hidden constant when the example completes" do
            MyGem::SomeClass::FOO.should eq(7)
          end
        end
      end
      """
    When I run `rspec hide_const_spec.rb`
    Then the examples should all pass

  Scenario: Hiding undefined constant
    Given a file named "hide_const_spec.rb" with:
      """
      describe "hiding UNDEFINED_CONSTANT" do
        it "has no effect" do
          hide_const("UNDEFINED_CONSTANT")
          expect { UNDEFINED_CONSTANT }.to raise_error(NameError)
        end

        it "is still undefined after the example completes" do
          expect { UNDEFINED_CONSTANT }.to raise_error(NameError)
        end
      end
      """
    When I run `rspec hide_const_spec.rb`
    Then the examples should all pass
