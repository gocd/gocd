# frozen_string_literal: true

module SCSSLint
  # Checks for unnecessary units on zero values.
  class Linter::ZeroUnit < Linter
    include LinterRegistry

    def visit_script_string(node)
      return unless node.type == :identifier

      node.value.scan(ZERO_UNIT_REGEX) do |match|
        next unless zero_with_length_units?(match.first)
        add_lint(node, MESSAGE_FORMAT % match.first)
      end
    end

    def visit_script_number(node)
      length = source_from_range(node.source_range)[ZERO_UNIT_REGEX, 1]
      return unless zero_with_length_units?(length)

      add_lint(node, MESSAGE_FORMAT % length)
    end

  private

    ZERO_UNIT_REGEX = /
      \b
      (?<!\.|\#)    # Ignore zeroes following `#` (colors) or `.` (decimals)
      (0[a-z]+)     # Zero followed by letters indicating some sort of unit
      \b
    /ix

    MESSAGE_FORMAT = '`%s` should be written without units as `0`'.freeze

    LENGTH_UNITS = %w[em ex ch rem vw vh vmin vmax cm mm in pt pc px].to_set

    def zero_with_length_units?(string)
      string =~ /^0([a-z]+)/ && LENGTH_UNITS.include?(Regexp.last_match(1))
    end
  end
end
