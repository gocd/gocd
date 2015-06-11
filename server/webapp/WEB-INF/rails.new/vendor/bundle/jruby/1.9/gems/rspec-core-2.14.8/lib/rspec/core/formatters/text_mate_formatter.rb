require 'cgi'
require 'rspec/core/formatters/html_formatter'

module RSpec
  module Core
    module Formatters
      # Formats backtraces so they're clickable by TextMate
      class TextMateFormatter < HtmlFormatter
        class NonEscapingHtmlPrinter < RSpec::Core::Formatters::HtmlPrinter
          def print_example_failed(pending_fixed, description, run_time, failure_id, exception, extra_content, escape_backtrace = false)
            # Call implementation from superclass, but ignore `escape_backtrace` and always pass `false` instead.
            super(pending_fixed, description, run_time, failure_id, exception, extra_content, false)
          end
        end

        def initialize(output)
          super
          @printer = NonEscapingHtmlPrinter.new(output)
        end

        def backtrace_line(line, skip_textmate_conversion=false)
          if skip_textmate_conversion
            super(line)
          else
            format_backtrace_line_for_textmate(super(line))
          end
        end

        def format_backtrace_line_for_textmate(line)
          return nil unless line
          CGI.escapeHTML(line).sub(/([^:]*\.e?rb):(\d*)/) do
            "<a href=\"txmt://open?url=file://#{File.expand_path($1)}&amp;line=#{$2}\">#{$1}:#{$2}</a> "
          end
        end

        def extra_failure_content(exception)
          require 'rspec/core/formatters/snippet_extractor'
          backtrace = exception.backtrace.map {|line| backtrace_line(line, :skip_textmate_conversion)}
          backtrace.compact!
          @snippet_extractor ||= SnippetExtractor.new
          "    <pre class=\"ruby\"><code>#{@snippet_extractor.snippet(backtrace)}</code></pre>"
        end
      end
    end
  end
end
