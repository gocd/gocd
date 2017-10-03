require 'rspec/core/formatters/helpers'
require 'set'

module RSpec
  module Core
    module Formatters
      class DeprecationFormatter
        attr_reader :count, :deprecation_stream, :summary_stream

        def initialize(deprecation_stream, summary_stream)
          @deprecation_stream = deprecation_stream
          @summary_stream = summary_stream
          @seen_deprecations = Set.new
          @count = 0
        end

        def printer
          @printer ||= case deprecation_stream
                       when File
                         ImmediatePrinter.new(FileStream.new(deprecation_stream), summary_stream, self)
                       when RaiseErrorStream
                         ImmediatePrinter.new(deprecation_stream, summary_stream, self)
                       else
                         DelayedPrinter.new(deprecation_stream, summary_stream, self)
                       end
        end

        def deprecation(data)
          return if @seen_deprecations.include?(data)

          @count += 1
          printer.print_deprecation_message data
          @seen_deprecations << data
        end

        def deprecation_summary
          printer.deprecation_summary
        end

        def start(example_count=nil)
          #no-op to fix #966
        end

        def deprecation_message_for(data)
          if data[:message]
            SpecifiedDeprecationMessage.new(data)
          else
            GeneratedDeprecationMessage.new(data)
          end
        end

        RAISE_ERROR_CONFIG_NOTICE = <<-EOS.gsub(/^\s+\|/, '')
          |
          |If you need more of the backtrace for any of these deprecations to
          |identify where to make the necessary changes, you can configure
          |`config.raise_errors_for_deprecations!`, and it will turn the
          |deprecation warnings into errors, giving you the full backtrace.
        EOS

        DEPRECATION_STREAM_NOTICE = "Pass `--deprecation-out` or set " +
          "`config.deprecation_stream` to a file for full output."

        SpecifiedDeprecationMessage = Struct.new(:type) do
          def initialize(data)
            @message = data[:message]
            super deprecation_type_for(data)
          end

          def to_s
            output_formatted @message
          end

          def too_many_warnings_message
            msg = "Too many similar deprecation messages reported, disregarding further reports. "
            msg << DEPRECATION_STREAM_NOTICE
            msg
          end

          private

          def output_formatted(str)
            return str unless str.lines.count > 1
            separator = "#{'-' * 80}"
            "#{separator}\n#{str.chomp}\n#{separator}"
          end

          def deprecation_type_for(data)
            data[:message].gsub(/(\w+\/)+\w+\.rb:\d+/, '')
          end
        end

        GeneratedDeprecationMessage = Struct.new(:type) do
          def initialize(data)
            @data = data
            super data.fetch(:type, data[:deprecated])
          end

          def to_s
            msg =  "#{@data[:deprecated]} is deprecated."
            msg << " Use #{@data[:replacement]} instead." if @data[:replacement]
            msg << " Called from #{@data[:call_site]}." if @data[:call_site]
            msg
          end

          def too_many_warnings_message
            msg = "Too many uses of deprecated '#{type}'. "
            msg << DEPRECATION_STREAM_NOTICE
            msg
          end
        end

        class ImmediatePrinter
          attr_reader :deprecation_stream, :summary_stream, :deprecation_formatter

          def initialize(deprecation_stream, summary_stream, deprecation_formatter)
            @deprecation_stream = deprecation_stream

            @summary_stream = summary_stream
            @deprecation_formatter = deprecation_formatter
          end

          def print_deprecation_message(data)
            deprecation_message = deprecation_formatter.deprecation_message_for(data)
            deprecation_stream.puts deprecation_message.to_s
          end

          def deprecation_summary
            return if deprecation_formatter.count.zero?
            deprecation_stream.summarize(summary_stream, deprecation_formatter.count)
          end
        end

        class DelayedPrinter
          TOO_MANY_USES_LIMIT = 4

          include ::RSpec::Core::Formatters::Helpers

          attr_reader :deprecation_stream, :summary_stream, :deprecation_formatter

          def initialize(deprecation_stream, summary_stream, deprecation_formatter)
            @deprecation_stream = deprecation_stream
            @summary_stream = summary_stream
            @deprecation_formatter = deprecation_formatter
            @seen_deprecations = Hash.new { 0 }
            @deprecation_messages = Hash.new { |h, k| h[k] = [] }
          end

          def print_deprecation_message(data)
            deprecation_message = deprecation_formatter.deprecation_message_for(data)
            @seen_deprecations[deprecation_message] += 1

            stash_deprecation_message(deprecation_message)
          end

          def stash_deprecation_message(deprecation_message)
            if @seen_deprecations[deprecation_message] < TOO_MANY_USES_LIMIT
              @deprecation_messages[deprecation_message] << deprecation_message.to_s
            elsif @seen_deprecations[deprecation_message] == TOO_MANY_USES_LIMIT
              @deprecation_messages[deprecation_message] << deprecation_message.too_many_warnings_message
            end
          end

          def deprecation_summary
            return unless @deprecation_messages.any?

            print_deferred_deprecation_warnings
            deprecation_stream.puts RAISE_ERROR_CONFIG_NOTICE

            summary_stream.puts "\n#{pluralize(deprecation_formatter.count, 'deprecation warning')} total"
          end

          def print_deferred_deprecation_warnings
            deprecation_stream.puts "\nDeprecation Warnings:\n\n"
            @deprecation_messages.keys.sort_by(&:type).each do |deprecation|
              messages = @deprecation_messages[deprecation]
              messages.each { |msg| deprecation_stream.puts msg }
              deprecation_stream.puts
            end
          end
        end

        # Not really a stream, but is usable in place of one.
        class RaiseErrorStream
          include ::RSpec::Core::Formatters::Helpers

          def puts(message)
            raise DeprecationError, message
          end

          def summarize(summary_stream, deprecation_count)
            summary_stream.puts "\n#{pluralize(deprecation_count, 'deprecation')} found."
          end
        end

        # Wraps a File object and provides file-specific operations.
        class FileStream
          include ::RSpec::Core::Formatters::Helpers

          def initialize(file)
            @file = file

            # In one of my test suites, I got lots of duplicate output in the
            # deprecation file (e.g. 200 of the same deprecation, even though
            # the `puts` below was only called 6 times). Setting `sync = true`
            # fixes this (but we really have no idea why!).
            @file.sync = true
          end

          def puts(*args)
            @file.puts(*args)
          end

          def summarize(summary_stream, deprecation_count)
            summary_stream.puts "\n#{pluralize(deprecation_count, 'deprecation')} logged to #{@file.path}"
            puts RAISE_ERROR_CONFIG_NOTICE
          end
        end

      end
    end

    DeprecationError = Class.new(StandardError)
  end
end
