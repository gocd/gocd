Feature: Stub Undefined Constant

  Use `stub_const` to stub constants. When the constant is not already defined,
  all the necessary intermediary modules will be dynamically created. When the
  example completes, the intermediary module constants will be removed to return
  the constant state to how it started.

  Scenario: Stub top-level constant
    Given a file named "stub_const_spec.rb" with:
      """ruby
      describe "stubbing FOO" do
        it "can stub undefined constant FOO" do
          stub_const("FOO", 5)
          FOO.should eq(5)
        end

        it "undefines the constant when the example completes" do
          expect { FOO }.to raise_error(NameError)
        end
      end
      """
    When I run `rspec stub_const_spec.rb`
    Then the examples should all pass

  Scenario: Stub nested constant
    Given a file named "stub_const_spec.rb" with:
      """ruby
      module MyGem
        class SomeClass
        end
      end

      module MyGem
        describe SomeClass do
          it "can stub an arbitrarily deep constant that is undefined" do
            defined?(SomeClass::A).should be_false
            stub_const("MyGem::SomeClass::A::B::C", 3)
            SomeClass::A::B::C.should eq(3)
            SomeClass::A.should be_a(Module)
          end

          it 'undefines the intermediary constants that were dynamically created' do
            defined?(SomeClass).should be_true
            defined?(SomeClass::A).should be_false
          end
        end
      end
      """
    When I run `rspec stub_const_spec.rb`
    Then the examples should all pass
