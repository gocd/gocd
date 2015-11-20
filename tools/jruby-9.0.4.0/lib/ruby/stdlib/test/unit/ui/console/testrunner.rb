#--
#
# Author:: Nathaniel Talbott.
# Copyright::
#   * Copyright (c) 2000-2003 Nathaniel Talbott. All rights reserved.
#   * Copyright (c) 2008-2013 Kouhei Sutou <kou@clear-code.com>
# License:: Ruby license.

require 'test/unit/color-scheme'
require 'test/unit/code-snippet-fetcher'
require 'test/unit/fault-location-detector'
require 'test/unit/diff'
require 'test/unit/ui/testrunner'
require 'test/unit/ui/testrunnermediator'
require 'test/unit/ui/console/outputlevel'

module Test
  module Unit
    module UI
      module Console

        # Runs a Test::Unit::TestSuite on the console.
        class TestRunner < UI::TestRunner
          include OutputLevel

          # Creates a new TestRunner for running the passed
          # suite. If quiet_mode is true, the output while
          # running is limited to progress dots, errors and
          # failures, and the final result. io specifies
          # where runner output should go to; defaults to
          # STDOUT.
          def initialize(suite, options={})
            super
            @output_level = @options[:output_level] || NORMAL
            @output = @options[:output] || STDOUT
            @use_color = @options[:use_color]
            @use_color = guess_color_availability if @use_color.nil?
            @color_scheme = @options[:color_scheme] || ColorScheme.default
            @reset_color = Color.new("reset")
            @progress_row = 0
            @progress_row_max = @options[:progress_row_max]
            @progress_row_max ||= guess_progress_row_max
            @show_detail_immediately = @options[:show_detail_immediately]
            @show_detail_immediately = true if @show_detail_immediately.nil?
            @already_outputted = false
            @indent = 0
            @top_level = true
            @current_output_level = NORMAL
            @faults = []
            @code_snippet_fetcher = CodeSnippetFetcher.new
            @test_suites = []
          end

          private
          def change_output_level(level)
            old_output_level = @current_output_level
            @current_output_level = level
            yield
            @current_output_level = old_output_level
          end

          def setup_mediator
            super
            output_setup_end
          end

          def output_setup_end
            suite_name = @suite.to_s
            suite_name = @suite.name if @suite.kind_of?(Module)
            output("Loaded suite #{suite_name}")
          end

          def attach_to_mediator
            @mediator.add_listener(TestResult::FAULT,
                                   &method(:add_fault))
            @mediator.add_listener(TestRunnerMediator::STARTED,
                                   &method(:started))
            @mediator.add_listener(TestRunnerMediator::FINISHED,
                                   &method(:finished))
            @mediator.add_listener(TestCase::STARTED_OBJECT,
                                   &method(:test_started))
            @mediator.add_listener(TestCase::FINISHED_OBJECT,
                                   &method(:test_finished))
            @mediator.add_listener(TestSuite::STARTED_OBJECT,
                                   &method(:test_suite_started))
            @mediator.add_listener(TestSuite::FINISHED_OBJECT,
                                   &method(:test_suite_finished))
          end

          def add_fault(fault)
            @faults << fault
            output_progress(fault.single_character_display, fault_color(fault))
            output_progress_in_detail(fault) if @show_detail_immediately
            @already_outputted = true if fault.critical?
          end

          def started(result)
            @result = result
            output_started
          end

          def output_started
            output("Started")
          end

          def finished(elapsed_time)
            nl if output?(NORMAL) and !output?(VERBOSE)
            output_faults unless @show_detail_immediately
            nl(PROGRESS_ONLY)
            change_output_level(IMPORTANT_FAULTS_ONLY) do
              output_statistics(elapsed_time)
            end
          end

          def output_faults
            categorized_faults = categorize_faults
            change_output_level(IMPORTANT_FAULTS_ONLY) do
              output_faults_in_detail(categorized_faults[:need_detail_faults])
            end
            output_faults_in_short("Omissions", Omission,
                                   categorized_faults[:omissions])
            output_faults_in_short("Notifications", Notification,
                                   categorized_faults[:notifications])
          end

          def max_digit(max_number)
            (Math.log10(max_number) + 1).truncate
          end

          def output_faults_in_detail(faults)
            return if faults.nil?
            digit = max_digit(faults.size)
            faults.each_with_index do |fault, index|
              nl
              output_single("%#{digit}d) " % (index + 1))
              output_fault_in_detail(fault)
            end
          end

          def output_faults_in_short(label, fault_class, faults)
            return if faults.nil?
            digit = max_digit(faults.size)
            nl
            output_single(label, fault_class_color(fault_class))
            output(":")
            faults.each_with_index do |fault, index|
              output_single("%#{digit}d) " % (index + 1))
              output_fault_in_short(fault)
            end
          end

          def categorize_faults
            faults = {}
            @faults.each do |fault|
              category = categorize_fault(fault)
              faults[category] ||= []
              faults[category] << fault
            end
            faults
          end

          def categorize_fault(fault)
            case fault
            when Omission
              :omissions
            when Notification
              :notifications
            else
              :need_detail_faults
            end
          end

          def output_fault_in_detail(fault)
            if fault.is_a?(Failure) and
                fault.inspected_expected and fault.inspected_actual
              output_single(fault.label, fault_color(fault))
              output(":")
              output(fault.test_name)
              output_fault_backtrace(fault)
              output_failure_message(fault)
            else
              output_single(fault.label, fault_color(fault))
              if fault.is_a?(Error)
                output(": #{fault.test_name}")
                output_fault_message(fault)
              else
                output_fault_message(fault)
                output(fault.test_name)
              end
              output_fault_backtrace(fault)
            end
          end

          def output_fault_message(fault)
            message = fault.message
            return if message.nil?

            if message.include?("\n")
              output(":")
              message.each_line do |line|
                output("  #{line.chomp}")
              end
            else
              output(": #{message}")
            end
          end

          def output_fault_backtrace(fault)
            snippet_is_shown = false
            detector = FaultLocationDetector.new(fault, @code_snippet_fetcher)
            backtrace = fault.location
            # workaround for test-spec. :<
            # see also GitHub:#22
            backtrace ||= []
            backtrace.each_with_index do |entry, i|
              output(entry)
              next if snippet_is_shown
              next unless detector.target?(entry)
              file, line_number, = detector.split_backtrace_entry(entry)
              snippet_is_shown = output_code_snippet(file, line_number,
                                                     fault_color(fault))
            end
          end

          def output_code_snippet(file, line_number, target_line_color=nil)
            lines = @code_snippet_fetcher.fetch(file, line_number)
            return false if lines.empty?

            max_n = lines.collect {|n, line, attributes| n}.max
            digits = (Math.log10(max_n) + 1).truncate
            lines.each do |n, line, attributes|
              if attributes[:target_line?]
                line_color = target_line_color
                current_line_mark = "=>"
              else
                line_color = nil
                current_line_mark = ""
              end
              output("  %2s %*d: %s" % [current_line_mark, digits, n, line],
                     line_color)
            end
            true
          end

          def output_failure_message(failure)
            if failure.expected.respond_to?(:encoding) and
                failure.actual.respond_to?(:encoding) and
                failure.expected.encoding != failure.actual.encoding
              need_encoding = true
            else
              need_encoding = false
            end
            output(failure.user_message) if failure.user_message
            output_single("<")
            output_single(failure.inspected_expected, color("pass"))
            output_single(">")
            if need_encoding
              output_single("(")
              output_single(failure.expected.encoding.name, color("pass"))
              output_single(")")
            end
            output(" expected but was")
            output_single("<")
            output_single(failure.inspected_actual, color("failure"))
            output_single(">")
            if need_encoding
              output_single("(")
              output_single(failure.actual.encoding.name, color("failure"))
              output_single(")")
            end
            output("")
            from, to = prepare_for_diff(failure.expected, failure.actual)
            if from and to
              if need_encoding
                unless from.valid_encoding?
                  from = from.dup.force_encoding("ASCII-8BIT")
                end
                unless to.valid_encoding?
                  to = to.dup.force_encoding("ASCII-8BIT")
                end
              end
              from_lines = from.split(/\r?\n/)
              to_lines = to.split(/\r?\n/)
              if need_encoding
                from_lines << ""
                to_lines << ""
                from_lines << "Encoding: #{failure.expected.encoding.name}"
                to_lines << "Encoding: #{failure.actual.encoding.name}"
              end
              differ = ColorizedReadableDiffer.new(from_lines, to_lines, self)
              if differ.need_diff?
                output("")
                output("diff:")
                differ.diff
              end
            end
          end

          def output_fault_in_short(fault)
            output_single(fault.message, fault_color(fault))
            output(" [#{fault.test_name}]")
            output(fault.location.first)
          end

          def format_fault(fault)
            fault.long_display
          end

          def output_statistics(elapsed_time)
            output("Finished in #{elapsed_time} seconds.")
            nl
            output(@result, result_color)
            output("%g%% passed" % @result.pass_percentage, result_color)
            unless elapsed_time.zero?
              nl
              test_throughput = @result.run_count / elapsed_time
              assertion_throughput = @result.assertion_count / elapsed_time
              throughput = [
                "%.2f tests/s" % test_throughput,
                "%.2f assertions/s" % assertion_throughput,
              ]
              output(throughput.join(", "))
            end
          end

          def test_started(test)
            return unless output?(VERBOSE)

            name = test.name.sub(/\(.+?\)\z/, '')
            right_space = 8 * 2
            left_space = @progress_row_max - right_space
            left_space = left_space - indent.size - name.size
            tab_stop = "\t" * ([left_space - 1, 0].max / 8)
            output_single("#{indent}#{name}:#{tab_stop}", nil, VERBOSE)
            @test_start = Time.now
          end

          def test_finished(test)
            unless @already_outputted
              output_progress(".", color("pass"))
            end
            @already_outputted = false

            return unless output?(VERBOSE)

            output(": (%f)" % (Time.now - @test_start), nil, VERBOSE)
          end

          def suite_name(prefix, suite)
            name = suite.name
            if name.nil?
              "(anonymous)"
            else
              name.sub(/\A#{Regexp.escape(prefix)}/, "")
            end
          end

          def test_suite_started(suite)
            last_test_suite = @test_suites.last
            @test_suites << suite
            if @top_level
              @top_level = false
              return
            end

            output_single(indent, nil, VERBOSE)
            if suite.test_case.nil?
              _color = color("suite")
            else
              _color = color("case")
            end
            prefix = "#{last_test_suite.name}::"
            output_single(suite_name(prefix, suite), _color, VERBOSE)
            output(": ", nil, VERBOSE)
            @indent += 2
          end

          def test_suite_finished(suite)
            @indent -= 2
            @test_suites.pop
          end

          def indent
            if output?(VERBOSE)
              " " * @indent
            else
              ""
            end
          end

          def nl(level=nil)
            output("", nil, level)
          end

          def output(something, color=nil, level=nil)
            return unless output?(level)
            output_single(something, color, level)
            @output.puts
          end

          def output_single(something, color=nil, level=nil)
            return false unless output?(level)
            if @use_color and color
              something = "%s%s%s" % [color.escape_sequence,
                                      something,
                                      @reset_color.escape_sequence]
            end
            @output.write(something)
            @output.flush
            true
          end

          def output_progress(mark, color=nil)
            if output_single(mark, color, PROGRESS_ONLY)
              return unless @progress_row_max > 0
              @progress_row += mark.size
              if @progress_row >= @progress_row_max
                nl unless @output_level == VERBOSE
                @progress_row = 0
              end
            end
          end

          def output_progress_in_detail_marker(fault)
            if @progress_row_max > 0
              output("=" * @progress_row_max, fault_color(fault))
            else
              nl
            end
          end

          def output_progress_in_detail(fault)
            return if @output_level == SILENT
            nl
            output_progress_in_detail_marker(fault)
            if categorize_fault(fault) == :need_detail_faults
              output_fault_in_detail(fault)
            else
              output_fault_in_short(fault)
            end
            output_progress_in_detail_marker(fault)
            @progress_row = 0
          end

          def output?(level)
            (level || @current_output_level) <= @output_level
          end

          def color(name)
            _color = @color_scheme[name]
            _color ||= @color_scheme["success"] if name == "pass"
            _color ||= ColorScheme.default[name]
            _color
          end

          def fault_color(fault)
            fault_class_color(fault.class)
          end

          def fault_class_color(fault_class)
            color(fault_class.name.split(/::/).last.downcase)
          end

          def result_color
            color(@result.status)
          end

          def guess_color_availability
            return false unless @output.tty?
            case ENV["TERM"]
            when /(?:term|screen)(?:-(?:256)?color)?\z/
              true
            else
              return true if ENV["EMACS"] == "t"
              false
            end
          end

          def guess_progress_row_max
            term_width = guess_term_width
            if term_width.zero?
              if ENV["EMACS"] == "t"
                -1
              else
                79
              end
            else
              term_width
            end
          end

          def guess_term_width
            Integer(ENV["COLUMNS"] || ENV["TERM_WIDTH"] || 0)
          rescue ArgumentError
            0
          end
        end

        class ColorizedReadableDiffer < Diff::ReadableDiffer
          def initialize(from, to, runner)
            @runner = runner
            super(from, to)
          end

          def need_diff?(options={})
            operations.each do |tag,|
              return true if [:replace, :equal].include?(tag)
            end
            false
          end

          private
          def output_single(something, color=nil)
            @runner.__send__(:output_single, something, color)
          end

          def output(something, color=nil)
            @runner.__send__(:output, something, color)
          end

          def color(name)
            @runner.__send__(:color, name)
          end

          def cut_off_ratio
            0
          end

          def default_ratio
            0
          end

          def tag(mark, color_name, contents)
            _color = color(color_name)
            contents.each do |content|
              output_single(mark, _color)
              output_single(" ")
              output(content)
            end
          end

          def tag_deleted(contents)
            tag("-", "diff-deleted-tag", contents)
          end

          def tag_inserted(contents)
            tag("+", "diff-inserted-tag", contents)
          end

          def tag_equal(contents)
            tag(" ", "normal", contents)
          end

          def tag_difference(contents)
            tag("?", "diff-difference-tag", contents)
          end

          def diff_line(from_line, to_line)
            to_operations = []
            from_line, to_line, _operations = line_operations(from_line, to_line)

            no_replace = true
            _operations.each do |tag,|
              if tag == :replace
                no_replace = false
                break
              end
            end

            output_single("?", color("diff-difference-tag"))
            output_single(" ")
            _operations.each do |tag, from_start, from_end, to_start, to_end|
              from_width = compute_width(from_line, from_start, from_end)
              to_width = compute_width(to_line, to_start, to_end)
              case tag
              when :replace
                output_single(from_line[from_start...from_end],
                              color("diff-deleted"))
                if (from_width < to_width)
                  output_single(" " * (to_width - from_width))
                end
                to_operations << Proc.new do
                  output_single(to_line[to_start...to_end],
                                color("diff-inserted"))
                  if (to_width < from_width)
                    output_single(" " * (from_width - to_width))
                  end
                end
              when :delete
                output_single(from_line[from_start...from_end],
                              color("diff-deleted"))
                unless no_replace
                  to_operations << Proc.new {output_single(" " * from_width)}
                end
              when :insert
                if no_replace
                  output_single(to_line[to_start...to_end],
                                color("diff-inserted"))
                else
                  output_single(" " * to_width)
                  to_operations << Proc.new do
                    output_single(to_line[to_start...to_end],
                                  color("diff-inserted"))
                  end
                end
              when :equal
                output_single(from_line[from_start...from_end])
                unless no_replace
                  to_operations << Proc.new {output_single(" " * to_width)}
                end
              else
                raise "unknown tag: #{tag}"
              end
            end
            output("")

            unless to_operations.empty?
              output_single("?", color("diff-difference-tag"))
              output_single(" ")
              to_operations.each do |operation|
                operation.call
              end
              output("")
            end
          end
        end
      end
    end
  end
end
