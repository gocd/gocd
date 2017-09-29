Feature: --init option

  Use the --init option on the command line to generate conventional
  files for an rspec project.

  Scenario: generate .rspec
    When I run `rspec --init`
    Then the following files should exist:
      | .rspec |
    And the output should contain "create   .rspec"

  Scenario: .rspec file already exists
    Given a file named ".rspec" with:
      """
      --color
      """
    When I run `rspec --init`
    Then the output should contain "exist   .rspec"
