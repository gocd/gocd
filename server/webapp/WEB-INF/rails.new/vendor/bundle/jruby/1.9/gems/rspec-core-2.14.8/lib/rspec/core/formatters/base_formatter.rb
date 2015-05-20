require 'rspec/core/formatters/helpers'
require 'stringio'

module RSpec
  module Core
    module Formatters
      # RSpec's built-in formatters are all subclasses of RSpec::Core::Formatters::BaseTextFormatter,
      # but the BaseTextFormatter documents all of the methods needed to be implemented by a formatter,
      # as they are called from the reporter.
      #
      # @see RSpec::Core::Formatters::BaseTextFormatter
      # @see RSpec::Core::Reporter
      class BaseFormatter
        include Helpers
        attr_accessor :example_group
        attr_reader :duration, :examples, :output
        attr_reader :example_count, :pending_count, :failure_count
        attr_reader :failed_examples, :pending_examples

        # @api public
        #
        # @param output
        def initialize(output)
          @output = output || StringIO.new
          @example_count = @pending_count = @failure_count = 0
          @examples = []
          @failed_examples = []
          @pending_examples = []
          @example_group = nil
        end

        # @api public
        #
        # This method is invoked before any examples are run, right after
        # they have all been collected. This can be useful for special
        # formatters that need to provide progress on feedback (graphical ones).
        #
        # This will only be invoked once, and the next one to be invoked
        # is {#example_group_started}.
        #
        # @param example_count
        def start(example_count)
          start_sync_output
          @example_count = example_count
        end

        # @api public
        #
        # This method is invoked at the beginning of the execution of each example group.
        #
        # @param example_group subclass of `RSpec::Core::ExampleGroup`
        #
        # The next method to be invoked after this is {#example_passed},
        # {#example_pending}, or {#example_group_finished}.
        #
        # @param example_group
        def example_group_started(example_group)
          @example_group = example_group
        end

        # @api public
        #
        # Invoked at the end of the execution of each example group.
        #
        # @param example_group subclass of `RSpec::Core::ExampleGroup`
        def example_group_finished(example_group)
        end

        # @api public
        #
        # Invoked at the beginning of the execution of each example.
        #
        # @param example instance of subclass of `RSpec::Core::ExampleGroup`
        # @return [Array]
        def example_started(example)
          examples << example
        end

        # @api public
        #
        # Invoked when an example passes.
        #
        # @param example instance of subclass of `RSpec::Core::ExampleGroup`
        def example_passed(example)
        end

        # Invoked when an example is pending.
        #
        # @param example instance of subclass of `RSpec::Core::ExampleGroup`
        # @return [Array]
        def example_pending(example)
          @pending_examples << example
        end

        # @api public
        #
        # Invoked when an example fails.
        #
        # @param example instance of subclass of `RSpec::Core::ExampleGroup`
        # @return [Array]
        def example_failed(example)
          @failed_examples << example
        end

        # @api public
        #
        # Used by the reporter to send messages to the output stream.
        #
        # @param [String] message
        def message(message)
        end

        # @api public
        #
        # Invoked after all examples have executed, before dumping post-run reports.
        #
        # @return [nil]
        def stop
        end

        # @api public
        #
        # This method is invoked after all of the examples have executed. The next method
        # to be invoked after this one is {#dump_failures}
        # (BaseTextFormatter then calls {#dump_failure} once for each failed example.)
        #
        # @return [nil]
        def start_dump
        end

        # @api public
        #
        # Dumps detailed information about each example failure.
        #
        # @return [nil]
        def dump_failures
        end

        # @api public
        #
        # This method is invoked after the dumping of examples and failures. Each parameter
        # is assigned to a corresponding attribute.
        #
        # @param duration
        # @param example_count
        # @param failure_count
        # @param pending_count
        def dump_summary(duration, example_count, failure_count, pending_count)
          @duration = duration
          @example_count = example_count
          @failure_count = failure_count
          @pending_count = pending_count
        end

        # @api public
        #
        # Outputs a report of pending examples.  This gets invoked
        # after the summary if option is set to do so.
        #
        # @return [nil]
        def dump_pending
        end

        # @private not intended for use outside RSpec.
        def seed(number)
        end

        # @api public
        #
        # Invoked at the very end, `close` allows the formatter to clean
        # up resources, e.g. open streams, etc.
        def close
          restore_sync_output
        end

        # @api public
        #
        # Formats the given backtrace based on configuration and
        # the metadata of the given example.
        def format_backtrace(backtrace, example)
          super(backtrace, example.metadata)
        end

      protected

        def configuration
          RSpec.configuration
        end

        def read_failed_line(exception, example)
          unless matching_line = find_failed_line(exception.backtrace, example.file_path)
            return "Unable to find matching line from backtrace"
          end

          file_path, line_number = matching_line.match(/(.+?):(\d+)(|:\d+)/)[1..2]

          if File.exist?(file_path)
            File.readlines(file_path)[line_number.to_i - 1] ||
              "Unable to find matching line in #{file_path}"
          else
            "Unable to find #{file_path} to read failed line"
          end
        rescue SecurityError
          "Unable to read failed line"
        end

        def find_failed_line(backtrace, path)
          path = File.expand_path(path)
          backtrace.detect { |line|
            match = line.match(/(.+?):(\d+)(|:\d+)/)
            match && match[1].downcase == path.downcase
          }
        end

        def start_sync_output
          @old_sync, output.sync = output.sync, true if output_supports_sync
        end

        def restore_sync_output
          output.sync = @old_sync if output_supports_sync and !output.closed?
        end

        def output_supports_sync
          output.respond_to?(:sync=)
        end

        def profile_examples?
          configuration.profile_examples
        end

        def fail_fast?
          configuration.fail_fast
        end

        def color_enabled?
          configuration.color_enabled?(output)
        end

        def mute_profile_output?(failure_count)
          # Don't print out profiled info if there are failures and `--fail-fast` is used, it just clutters the output
          !profile_examples? || (fail_fast? && failure_count != 0)
        end
      end
    end
  end
end
