module RSpec
  module Core
    module Formatters
      # @api private
      #
      # Extracts code snippets by looking at the backtrace of the passed error and applies synax highlighting and line numbers using html.
      class SnippetExtractor
        class NullConverter; def convert(code, pre); code; end; end

        begin
          require 'syntax/convertors/html'
          @@converter = Syntax::Convertors::HTML.for_syntax "ruby"
        rescue LoadError
          @@converter = NullConverter.new
        end

        # @api private
        #
        # Extract lines of code corresponding to  a backtrace.
        #
        # @param [String] backtrace the backtrace from a test failure
        # @return [String] highlighted code snippet indicating where the test failure occured
        #
        # @see #post_process
        def snippet(backtrace)
          raw_code, line = snippet_for(backtrace[0])
          highlighted = @@converter.convert(raw_code, false)
          highlighted << "\n<span class=\"comment\"># gem install syntax to get syntax highlighting</span>" if @@converter.is_a?(NullConverter)
          post_process(highlighted, line)
        end

        # @api private
        #
        # Create a snippet from a line of code.
        #
        # @param [String] error_line file name with line number (i.e. 'foo_spec.rb:12')
        # @return [String] lines around the target line within the file
        #
        # @see #lines_around
        def snippet_for(error_line)
          if error_line =~ /(.*):(\d+)/
            file = $1
            line = $2.to_i
            [lines_around(file, line), line]
          else
            ["# Couldn't get snippet for #{error_line}", 1]
          end
        end

        # @api private
        #
        # Extract lines of code centered around a particular line within a source file.
        #
        # @param [String] file filename
        # @param [Fixnum] line line number
        # @return [String] lines around the target line within the file (2 above and 1 below).
        def lines_around(file, line)
          if File.file?(file)
            lines = File.read(file).split("\n")
            min = [0, line-3].max
            max = [line+1, lines.length-1].min
            selected_lines = []
            selected_lines.join("\n")
            lines[min..max].join("\n")
          else
            "# Couldn't get snippet for #{file}"
          end
        rescue SecurityError
          "# Couldn't get snippet for #{file}"
        end

        # @api private
        #
        # Adds line numbers to all lines and highlights the line where the failure occurred using html `span` tags.
        #
        # @param [String] highlighted syntax-highlighted snippet surrounding the offending line of code
        # @param [Fixnum] offending_line line where failure occured
        # @return [String] completed snippet
        def post_process(highlighted, offending_line)
          new_lines = []
          highlighted.split("\n").each_with_index do |line, i|
            new_line = "<span class=\"linenum\">#{offending_line+i-2}</span>#{line}"
            new_line = "<span class=\"offending\">#{new_line}</span>" if i == 2
            new_lines << new_line
          end
          new_lines.join("\n")
        end

      end
    end
  end
end
