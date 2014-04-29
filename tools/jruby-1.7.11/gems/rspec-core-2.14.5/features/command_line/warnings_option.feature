Feature: run with warnings enabled

  You can use the `--warnings` option to run specs with warnings enabled

  @unsupported-on-rbx
  Scenario:
    Given a file named "example_spec.rb" with:
      """ruby
      describe do
        it 'generates warning' do
          @undefined
        end
      end
      """
    When I run `rspec --warnings example_spec.rb`
    Then the output should contain "warning"

  @unsupported-on-rbx
  Scenario:
    Given a file named "example_spec.rb" with:
      """ruby
      describe do
        it 'generates warning' do
          @undefined
        end
      end
      """
    When I run `rspec example_spec.rb`
    Then the output should not contain "warning"
