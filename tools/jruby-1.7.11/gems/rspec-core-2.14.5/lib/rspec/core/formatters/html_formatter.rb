require 'rspec/core/formatters/base_text_formatter'
require 'rspec/core/formatters/html_printer'

module RSpec
  module Core
    module Formatters
      class HtmlFormatter < BaseTextFormatter

        def initialize(output)
          super(output)
          @example_group_number = 0
          @example_number = 0
          @header_red = nil
          @printer = HtmlPrinter.new(output)
        end

        private
        def method_missing(m, *a, &b)
          # no-op
        end

        public
        def message(message)
        end

        # The number of the currently running example_group
        def example_group_number
          @example_group_number
        end

        # The number of the currently running example (a global counter)
        def example_number
          @example_number
        end

        def start(example_count)
          super(example_count)
          @printer.print_html_start
          @printer.flush
        end

        def example_group_started(example_group)
          super(example_group)
          @example_group_red = false
          @example_group_number += 1

          unless example_group_number == 1
            @printer.print_example_group_end
          end
          @printer.print_example_group_start( example_group_number, example_group.description, example_group.parent_groups.size )
          @printer.flush
        end

        def start_dump
          @printer.print_example_group_end
          @printer.flush
        end

        def example_started(example)
          super(example)
          @example_number += 1
        end

        def example_passed(example)
          @printer.move_progress(percent_done)
          @printer.print_example_passed( example.description, example.execution_result[:run_time] )
          @printer.flush
        end

        def example_failed(example)
          super(example)

          unless @header_red
            @header_red = true
            @printer.make_header_red
          end

          unless @example_group_red
            @example_group_red = true
            @printer.make_example_group_header_red(example_group_number)
          end

          @printer.move_progress(percent_done)

          exception = example.metadata[:execution_result][:exception]
          exception_details = if exception
            {
              :message => exception.message,
              :backtrace => format_backtrace(exception.backtrace, example).join("\n")
            }
          else
            false
          end
          extra = extra_failure_content(exception)

          @printer.print_example_failed(
            exception.pending_fixed?,
            example.description,
            example.execution_result[:run_time],
            @failed_examples.size,
            exception_details,
            (extra == "") ? false : extra,
            true
          )
          @printer.flush
        end

        def example_pending(example)

          @printer.make_header_yellow unless @header_red
          @printer.make_example_group_header_yellow(example_group_number) unless @example_group_red
          @printer.move_progress(percent_done)
          @printer.print_example_pending( example.description, example.metadata[:execution_result][:pending_message] )
          @printer.flush
        end

        # Override this method if you wish to output extra HTML for a failed spec. For example, you
        # could output links to images or other files produced during the specs.
        #
        def extra_failure_content(exception)
          require 'rspec/core/formatters/snippet_extractor'
          backtrace = exception.backtrace.map {|line| backtrace_line(line)}
          backtrace.compact!
          @snippet_extractor ||= SnippetExtractor.new
          "    <pre class=\"ruby\"><code>#{@snippet_extractor.snippet(backtrace)}</code></pre>"
        end

        def percent_done
          result = 100.0
          if @example_count > 0
            result = ((example_number).to_f / @example_count.to_f * 1000).to_i / 10.0
          end
          result
        end

        def dump_failures
        end

        def dump_pending
        end

        def dump_summary(duration, example_count, failure_count, pending_count)
          @printer.print_summary(
            dry_run?,
            duration,
            example_count,
            failure_count,
            pending_count
          )
          @printer.flush
        end
      end
    end
  end
end
