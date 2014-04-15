Feature: explicit arguments

  Allows you to explicitly specify the argument values

  Scenario: explicit arguments
    Given a file named "stub_explicit_args_spec.rb" with:
      """
      describe "stubbed explicit arguments" do
        it "works on stubs" do
          object = Object.new
          object.stub(:foo).with(:this) do |arg|
            "got this"
          end
          object.stub(:foo).with(:that) do |arg|
            "got that"
          end

          object.foo(:this).should eq("got this")
          object.foo(:that).should eq("got that")
        end

        it "works on doubles and expectations" do
          object = double('foo')
          object.should_receive(:bar).with(:foo)

          object.bar(:foo)
        end
      end
      """
    When I run `rspec stub_explicit_args_spec.rb`
    Then the output should contain "2 examples, 0 failures"

  Scenario: explicit arguments with multiple arities
    Given a file named "stub_multiple_explicit_args_spec.rb" with:
      """
      describe "stubbed multiple explicit arguments" do
        it "works on stubs" do
          object = Object.new
          object.stub(:foo).with(:this) do |arg|
            "got this"
          end
          object.stub(:foo).with(:this, :that) do |arg1, arg2|
            "got this and that"
          end

          object.foo(:this).should eq("got this")
          object.foo(:this, :that).should eq("got this and that")
        end
        
        it "works on mocks" do
          object = double('foo')
          object.should_receive(:foo).with(:this, :that)

          object.foo(:this, :that)
        end
      end
      """
    When I run `rspec stub_multiple_explicit_args_spec.rb`
    Then the output should contain "2 examples, 0 failures"

