Feature: treat symbols as metadata keys with true values

  Use the treat_symbols_as_metadata_keys_with_true_values option to tell RSpec that :key is shorthand for :key => true.

    RSpec.configure { |c| c.treat_symbols_as_metadata_keys_with_true_values = true }

  Background:
    Given a file named "spec/spec_helper.rb" with:
      """ruby
      RSpec.configure { |c| c.treat_symbols_as_metadata_keys_with_true_values = true }
      """

  Scenario: by default, symbols without values are ignored and the specs are filtered out
    Given a file named "spec/example_spec.rb" with:
      """ruby
      describe "failed filtering" do
        it "this will be filted out", :some_tag do
          true
        end

        it "so will this" do
          false
        end
      end
      """
      When I run `rspec spec/example_spec.rb --tag some_tag`
      Then the output should contain "0 examples, 0 failures"
      And the output should contain "All examples were filtered out"

  Scenario: when treat_symbols_as_metadata_keys_with_true_values is true, specs can be tagged with only a symbol
    Given a file named "spec/example_spec.rb" with:
      """ruby
      require "spec_helper"
      describe "run me", :some_tag do
        it "runs" do
          true
        end
      end

      describe "run one of these" do
        it "run this one", :some_tag do
          true
        end

        it "but not me" do
          false
        end
      end
      """
      When I run `rspec spec/example_spec.rb --tag some_tag`
      Then the output should contain "2 examples, 0 failures"
      And the output should contain "Run options: include {:some_tag=>true}"
