Feature: read command line configuration options from files

  RSpec reads command line configuration options from files in two different
  locations:

    Local: `./.rspec-local` (i.e. in the project's root directory, can be gitignored)
    Project:  `./.rspec` (i.e. in the project's root directory, usually checked into the project)
    Global: `~/.rspec` (i.e. in the user's home directory)

  Configuration options are loaded from `~/.rspec`, `.rspec`,
  `.rspec-local`, command line switches, and the `SPEC_OPTS` environment
  variable (listed in lowest to highest precedence; for example, an option
  in `~/.rspec` can be overridden by an option in `.rspec-local`).

  Scenario: color set in .rspec
    Given a file named ".rspec" with:
      """
      --color
      """
    And a file named "spec/example_spec.rb" with:
      """ruby
      describe "color_enabled" do
        context "when set with RSpec.configure" do
          before do
            # color is disabled for non-tty output, so stub the output stream
            # to say it is tty, even though we're running this with cucumber
            RSpec.configuration.output_stream.stub(:tty?) { true }
          end

          it "is true" do
            RSpec.configuration.should be_color_enabled
          end
        end
      end
      """
    When I run `rspec ./spec/example_spec.rb`
    Then the examples should all pass

  Scenario: custom options file
    Given a file named "my.options" with:
      """
      --format documentation
      """
    And a file named "spec/example_spec.rb" with:
      """ruby
      describe "formatter set in custom options file" do
        it "sets formatter" do
          RSpec.configuration.formatters.first.
            should be_a(RSpec::Core::Formatters::DocumentationFormatter)
        end
      end
      """
    When I run `rspec spec/example_spec.rb --options my.options`
    Then the examples should all pass

  Scenario: RSpec ignores ./.rspec when custom options file is used
    Given a file named "my.options" with:
      """
      --format documentation
      """
    And a file named ".rspec" with:
      """
      --color
      """
    And a file named "spec/example_spec.rb" with:
      """ruby
      describe "custom options file" do
        it "causes .rspec to be ignored" do
          RSpec.configuration.color_enabled.should be_false
        end
      end
      """
    When I run `rspec spec/example_spec.rb --options my.options`
    Then the examples should all pass

  Scenario: using ERB in .rspec
    Given a file named ".rspec" with:
      """
      --format <%= true ? 'documentation' : 'progress' %>
      """
    And a file named "spec/example_spec.rb" with:
      """ruby
      describe "formatter" do
        it "is set to documentation" do
          RSpec.configuration.formatters.first.should be_an(RSpec::Core::Formatters::DocumentationFormatter)
        end
      end
      """
    When I run `rspec ./spec/example_spec.rb`
    Then the examples should all pass
