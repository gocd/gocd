Feature: Syntax Configuration

  In addition to the long-supported `should` syntax, rspec-expectations
  supports an alternate `expect` syntax. If you want your project to
  only use one syntax, you can configure the available syntaxes.

  Background:
    Given a file named "syntaxes_spec.rb" with:
      """ruby
      describe "using the should syntax" do
        specify { 3.should eq(3) }
        specify { 3.should_not eq(4) }
        specify { lambda { raise "boom" }.should raise_error("boom") }
        specify { lambda { }.should_not raise_error }
      end

      describe "using the expect syntax" do
        specify { expect(3).to eq(3) }
        specify { expect(3).not_to eq(4) }
        specify { expect { raise "boom" }.to raise_error("boom") }
        specify { expect { }.not_to raise_error }
      end
      """

  Scenario: Both syntaxes are available by default
    When I run `rspec syntaxes_spec.rb`
    Then the examples should all pass

  Scenario: Disable should syntax
    Given a file named "disable_should_syntax.rb" with:
      """ruby
      RSpec.configure do |config|
        config.expect_with :rspec do |c|
          c.syntax = :expect
        end
      end
      """
    When I run `rspec disable_should_syntax.rb syntaxes_spec.rb`
    Then the output should contain all of these:
      | 8 examples, 4 failures    |
      | undefined method `should' |

  Scenario: Disable expect syntax
    Given a file named "disable_expect_syntax.rb" with:
      """ruby
      RSpec.configure do |config|
        config.expect_with :rspec do |c|
          c.syntax = :should
        end
      end
      """
    When I run `rspec disable_expect_syntax.rb syntaxes_spec.rb`
    Then the output should contain all of these:
      | 8 examples, 4 failures    |
      | undefined method `expect' |

  Scenario: Explicitly enable both syntaxes
    Given a file named "enable_both_syntaxes.rb" with:
      """ruby
      RSpec.configure do |config|
        config.expect_with :rspec do |c|
          c.syntax = [:should, :expect]
        end
      end
      """
    When I run `rspec enable_both_syntaxes.rb syntaxes_spec.rb`
    Then the examples should all pass

