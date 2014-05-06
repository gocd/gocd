require 'spec/runner/formatter/base_formatter'
require 'fileutils'

module Spec
  module Runner
    module Formatter
      # Baseclass for text-based formatters. Can in fact be used for
      # non-text based ones too - just ignore the +output+ constructor
      # argument.
      class BaseTextFormatter < BaseFormatter
        attr_reader :output, :example_group
        # Creates a new instance that will write to +output+. If +output+ is a
        # String, output will be written to the File with that name, otherwise
        # +output+ is exected to be an IO (or an object that responds to #puts
        # and #write).
        def initialize(options, output)
          @options = options
          if String === output
            FileUtils.mkdir_p(File.dirname(output))
            @output = File.open(output, 'w')
          else
            @output = output
          end
          @pending_examples = []
        end

        def example_group_started(example_group_proxy)
          @example_group = example_group_proxy
        end
        
        def example_pending(example, message, deprecated_pending_location=nil)
          @pending_examples << ["#{@example_group.description} #{example.description}", message, example.location]
        end
        
        def dump_failure(counter, failure)
          @output.puts
          @output.puts "#{counter.to_s})"
          @output.puts colorize_failure("#{failure.header}\n#{failure.exception.message}", failure)
          @output.puts format_backtrace(failure.exception.backtrace)
          @output.flush
        end
        
        def colorize_failure(message, failure)
          failure.pending_fixed? ? blue(message) : red(message)
        end
        
        def colourise(message, failure)
          Spec::deprecate("BaseTextFormatter#colourise", "colorize_failure")
          colorize_failure(message, failure)
        end
        
        def dump_summary(duration, example_count, failure_count, pending_count)
          return if dry_run?
          @output.puts
          @output.puts "Finished in #{duration} seconds"
          @output.puts

          summary = "#{example_count} example#{'s' unless example_count == 1}, #{failure_count} failure#{'s' unless failure_count == 1}"
          summary << ", #{pending_count} pending" if pending_count > 0  

          if failure_count == 0
            if pending_count > 0
              @output.puts yellow(summary)
            else
              @output.puts green(summary)
            end
          else
            @output.puts red(summary)
          end
          @output.flush
        end

        def dump_pending
          unless @pending_examples.empty?
            @output.puts
            @output.puts "Pending:"
            @pending_examples.each do |pending_example|
              @output.puts "\n#{pending_example[0]} (#{pending_example[1]})"
              @output.puts "#{pending_example[2]}\n"
            end
          end
          @output.flush
        end
        
        def close
          @output.close  if (IO === @output) & (@output != $stdout)
        end
        
        def format_backtrace(backtrace)
          return "" if backtrace.nil?
          backtrace.map { |line| backtrace_line(line) }.join("\n")
        end
      
      protected

        def colour?
          !!@options.colour
        end

        def dry_run?
          !!@options.dry_run
        end
        
        def autospec?
          !!@options.autospec
        end
        
        def backtrace_line(line)
          line.sub(/\A([^:]+:\d+)$/, '\\1:')
        end

        def colour(text, colour_code)
          return text unless ENV['RSPEC_COLOR'] || (colour? & (autospec? || output_to_tty?))
          "#{colour_code}#{text}\e[0m"
        end

        def output_to_tty?
          begin
            @output.tty? || ENV.has_key?("AUTOTEST")
          rescue NoMethodError
            false
          end
        end
        
        def green(text); colour(text, "\e[32m"); end
        def red(text); colour(text, "\e[31m"); end
        def yellow(text); colour(text, "\e[33m"); end
        def blue(text); colour(text, "\e[34m"); end
        
        def magenta(text)
          Spec::deprecate("BaseTextFormatter#magenta")
          red(text)
        end
      end
    end
  end
end
