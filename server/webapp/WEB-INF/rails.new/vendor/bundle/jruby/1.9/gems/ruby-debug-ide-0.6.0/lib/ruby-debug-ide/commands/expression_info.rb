require 'stringio'
require 'irb/ruby-lex'

module Debugger

  class ExpressionInfoCommand < Command
    def regexp
      /^\s*ex(?:pression_info)?\s+/
    end

    def execute
      string_to_parse = Command.unescape_incoming(@match.post_match) + "\n\n\n"
      total_lines = string_to_parse.count("\n") + 1

      lexer = RubyLex.new
      lexer.set_input(create_io_reader(string_to_parse))

      last_statement = ''
      last_prompt = ''
      last_indent = 0
      lexer.set_prompt do |ltype, indent, continue, lineno|
        next if (lineno >= total_lines)

        last_prompt = ltype || ''
        last_indent = indent
      end

      lexer.each_top_level_statement do |line, line_no|
        last_statement = line
      end

      incomplete = true
      if /\A\s*\Z/m =~ last_statement
        incomplete = false
      end

      @printer.print_expression_info(incomplete, last_prompt, last_indent)
    end

    def create_io_reader(string_to_parse)
      io = StringIO.new(string_to_parse)

      if string_to_parse.respond_to?(:encoding)
        io.instance_exec(string_to_parse.encoding) do |string_encoding|
          @my_encoding = string_encoding
          def self.encoding
            @my_encoding
          end
        end
      end

      io
    end

    class << self
      def help_command
        "expression_info"
      end

      def help(cmd)
        %{
          ex[pression_info] <expression>\t
          returns parser-related information for the expression given\t\t
          'incomplete'=true|false\tindicates whether expression is a complete ruby
          expression and can be evaluated without getting syntax errors
        }
      end
    end
  end

end
