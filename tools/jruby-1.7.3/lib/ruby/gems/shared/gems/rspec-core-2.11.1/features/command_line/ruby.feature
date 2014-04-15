Feature: run with ruby command

  You can use the `ruby` command to run specs. You just need to require
  `rspec/autorun`. 
  
  Generally speaking, you're better off using the `rspec` command, which
  requires `rspec/autorun` for you, but some tools only work with the `ruby`
  command.

  Scenario:
    Given a file named "example_spec.rb" with:
      """
      require 'rspec/autorun'

      describe 1 do
        it "is < 2" do
          1.should be < 2
        end
      end
      """
    When I run `ruby example_spec.rb`
    Then the output should contain "1 example, 0 failures"
