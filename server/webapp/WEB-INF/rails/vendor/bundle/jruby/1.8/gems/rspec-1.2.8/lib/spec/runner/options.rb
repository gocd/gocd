require 'ostruct'

module Spec
  module Runner
    class Options
      FILE_SORTERS = {
        'mtime' => lambda {|file_a, file_b| File.mtime(file_b) <=> File.mtime(file_a)}
      }

      EXAMPLE_FORMATTERS = { # Load these lazily for better speed
                'silent' => ['spec/runner/formatter/silent_formatter',                 'Formatter::SilentFormatter'],
                     'l' => ['spec/runner/formatter/silent_formatter',                 'Formatter::SilentFormatter'],
               'specdoc' => ['spec/runner/formatter/specdoc_formatter',                'Formatter::SpecdocFormatter'],
                     's' => ['spec/runner/formatter/specdoc_formatter',                'Formatter::SpecdocFormatter'],
                'nested' => ['spec/runner/formatter/nested_text_formatter',            'Formatter::NestedTextFormatter'],
                     'n' => ['spec/runner/formatter/nested_text_formatter',            'Formatter::NestedTextFormatter'],
                  'html' => ['spec/runner/formatter/html_formatter',                   'Formatter::HtmlFormatter'],
                     'h' => ['spec/runner/formatter/html_formatter',                   'Formatter::HtmlFormatter'],
              'progress' => ['spec/runner/formatter/progress_bar_formatter',           'Formatter::ProgressBarFormatter'],
                     'p' => ['spec/runner/formatter/progress_bar_formatter',           'Formatter::ProgressBarFormatter'],
      'failing_examples' => ['spec/runner/formatter/failing_examples_formatter',       'Formatter::FailingExamplesFormatter'],
                     'e' => ['spec/runner/formatter/failing_examples_formatter',       'Formatter::FailingExamplesFormatter'],
'failing_example_groups' => ['spec/runner/formatter/failing_example_groups_formatter', 'Formatter::FailingExampleGroupsFormatter'],
                     'g' => ['spec/runner/formatter/failing_example_groups_formatter', 'Formatter::FailingExampleGroupsFormatter'],
               'profile' => ['spec/runner/formatter/profile_formatter',                'Formatter::ProfileFormatter'],
                     'o' => ['spec/runner/formatter/profile_formatter',                'Formatter::ProfileFormatter'],
              'textmate' => ['spec/runner/formatter/text_mate_formatter',              'Formatter::TextMateFormatter']
      }

      attr_accessor(
        :autospec, # hack to tell 
        :filename_pattern,
        :backtrace_tweaker,
        :context_lines,
        :diff_format,
        :dry_run,
        :profile,
        :heckle_runner,
        :debug,
        :line_number,
        :loadby,
        :reporter,
        :reverse,
        :timeout,
        :verbose,
        :user_input_for_runner,
        :error_stream,
        :output_stream,
        # TODO: BT - Figure out a better name
        :argv
      )
      attr_reader :colour, :differ_class, :files, :examples, :example_groups
      
      def initialize(error_stream, output_stream)
        @error_stream = error_stream
        @output_stream = output_stream
        @filename_pattern = "**/*_spec.rb"
        @backtrace_tweaker = QuietBacktraceTweaker.new
        @examples = []
        @colour = false
        @profile = false
        @dry_run = false
        @debug = false
        @reporter = Reporter.new(self)
        @context_lines = 3
        @diff_format  = :unified
        @files = []
        @example_groups = []
        @result = nil
        @examples_run = false
        @examples_should_be_run = nil
        @user_input_for_runner = nil
        @after_suite_parts = []
        @files_loaded = false
        @out_used = nil
      end
      
      def add_example_group(example_group)
        @example_groups << example_group
      end
      
      def line_number_requested?
        !!line_number
      end
      
      def example_line
        Spec::Runner::LineNumberQuery.new(self).example_line_for(files.first, line_number)
      end

      def remove_example_group(example_group)
        @example_groups.delete(example_group)
      end

      def require_ruby_debug
        require 'rubygems' unless ENV['NO_RUBYGEMS']
        require 'ruby-debug'
      end

      def run_examples
        require_ruby_debug if debug
        return true unless examples_should_be_run?
        success = true
        begin
          runner = custom_runner || ExampleGroupRunner.new(self)

          unless @files_loaded
            runner.load_files(files_to_load)
            @files_loaded = true
          end
          
          define_predicate_matchers
          plugin_mock_framework

          # TODO - this has to happen after the files get loaded,
          # otherwise the before_suite_parts are not populated
          # from the configuration. There is no spec for this
          # directly, but features/before_and_after_blocks/before_and_after_blocks.story
          # will fail if this happens before the files are loaded.
          before_suite_parts.each do |part|
            part.call
          end

          if example_groups.empty?
            true
          else
            set_spec_from_line_number if line_number
            success = runner.run
            @examples_run = true
            heckle if heckle_runner
            success
          end
        ensure
          after_suite_parts.each do |part|
            part.arity < 1 ? part.call : part.call(success)
          end
        end
      end
      
      def before_suite_parts
        Spec::Example::BeforeAndAfterHooks.before_suite_parts
      end
      
      def after_suite_parts
        Spec::Example::BeforeAndAfterHooks.after_suite_parts
      end

      def examples_run?
        @examples_run
      end

      def examples_should_not_be_run
        @examples_should_be_run = false
      end
      
      def mock_framework
        # TODO - don't like this dependency - perhaps store this in here instead?
        Spec::Runner.configuration.mock_framework
      end

      def colour=(colour)
        @colour = colour
        if @colour && RUBY_PLATFORM =~ /mswin|mingw/ ;\
          begin ;\
            replace_output = @output_stream.equal?($stdout) ;\
            require 'rubygems' unless ENV['NO_RUBYGEMS'] ;\
            require 'Win32/Console/ANSI' ;\
            @output_stream = $stdout if replace_output ;\
          rescue LoadError ;\
            warn "You must 'gem install win32console' to use colour on Windows" ;\
            @colour = false ;\
          end
        end
      end

      def parse_diff(format)
        case format
        when :context, 'context', 'c'
          @diff_format  = :context
          default_differ
        when :unified, 'unified', 'u', '', nil
          @diff_format  = :unified
          default_differ
        else
          @diff_format  = :custom
          self.differ_class = load_class(format, 'differ', '--diff')
        end
      end

      def parse_example(example)
        if(File.file?(example))
          @examples = [File.open(example).read.split("\n")].flatten
        else
          @examples = [example]
        end
      end

      def parse_format(format_arg)
        format, where = ClassAndArgumentsParser.parse(format_arg)
        unless where
          raise "When using several --format options only one of them can be without a file" if @out_used
          where = @output_stream
          @out_used = true
        end
        @format_options ||= []
        @format_options << [format, where]
      end
      
      def formatters
        @format_options ||= [['progress', @output_stream]]
        @formatters ||= load_formatters(@format_options, EXAMPLE_FORMATTERS)
      end

      def load_formatters(format_options, formatters)
        format_options.map do |format, where|
          formatter_type = if formatters[format]
            require formatters[format][0]
            eval(formatters[format][1], binding, __FILE__, __LINE__)
          else
            load_class(format, 'formatter', '--format')
          end
          formatter_type.new(formatter_options, where)
        end
      end
      
      def formatter_options
        @formatter_options ||= OpenStruct.new(
          :colour   => colour,
          :autospec => autospec,
          :dry_run  => dry_run
        )
      end

      def load_heckle_runner(heckle)
        @format_options ||= [['silent', @output_stream]]
        suffix = ([/mswin/, /java/].detect{|p| p =~ RUBY_PLATFORM} || Spec::Ruby.version.to_f == 1.9) ? '_unsupported' : ''
        require "spec/runner/heckle_runner#{suffix}"
        @heckle_runner = ::Spec::Runner::HeckleRunner.new(heckle)
      end

      def number_of_examples
        return examples.size unless examples.empty?
        @example_groups.inject(0) {|sum, group| sum + group.number_of_examples}
      end

      def files_to_load
        result = []
        sorted_files.each do |file|
          if File.directory?(file)
            filename_pattern.split(",").each do |pattern|
              result += Dir[File.expand_path("#{file}/#{pattern.strip}")]
            end
          elsif File.file?(file)
            result << file
          else
            raise "File or directory not found: #{file}"
          end
        end
        result
      end
      
      def dry_run?
        @dry_run == true
      end
      
    protected

      def define_predicate_matchers
        Spec::Runner.configuration.predicate_matchers.each_pair do |matcher_method, method_on_object|
          Spec::Example::ExampleMethods::__send__ :define_method, matcher_method do |*args|
            eval("be_#{method_on_object.to_s.gsub('?','')}(*args)")
          end
        end
      end
      
      def plugin_mock_framework
        case mock_framework
        when Module
          Spec::Example::ExampleMethods.__send__ :include, mock_framework
        else
          require mock_framework
          Spec::Example::ExampleMethods.__send__ :include, Spec::Adapters::MockFramework
        end
      end

      def examples_should_be_run?
        return @examples_should_be_run unless @examples_should_be_run.nil?
        @examples_should_be_run = true
      end
      
      def differ_class=(klass)
        return unless klass
        @differ_class = klass
        Spec::Expectations.differ = self.differ_class.new(self)
      end

      def load_class(name, kind, option)
        if name =~ /\A(?:::)?([A-Z]\w*(?:::[A-Z]\w*)*)\z/
          arg = $2 == "" ? nil : $2
          [$1, arg]
        else
          m = "#{name.inspect} is not a valid class name"
          @error_stream.puts m
          raise m
        end
        begin
          eval(name, binding, __FILE__, __LINE__)
        rescue NameError => e
          @error_stream.puts "Couldn't find #{kind} class #{name}"
          @error_stream.puts "Make sure the --require option is specified *before* #{option}"
          if $_spec_spec ; raise e ; else exit(1) ; end
        end
      end
      
      def custom_runner
        return nil unless custom_runner?
        klass_name, arg = ClassAndArgumentsParser.parse(user_input_for_runner)
        runner_type = load_class(klass_name, 'example group runner', '--runner')
        return runner_type.new(self, arg)
      end

      def custom_runner?
        return user_input_for_runner ? true : false
      end
      
      def heckle
        heckle_runner = self.heckle_runner
        self.heckle_runner = nil
        heckle_runner.heckle_with
      end
      
      def sorted_files
        return sorter ? files.sort(&sorter) : files
      end

      def sorter
        FILE_SORTERS[loadby]
      end

      def default_differ
        require 'spec/runner/differs/default'
        self.differ_class = ::Spec::Expectations::Differs::Default
      end

      def set_spec_from_line_number
        if examples.empty?
          if files.length == 1
            if File.directory?(files[0])
              error_stream.puts "You must specify one file, not a directory when providing a line number"
              exit(1) if stderr?
            else
              example = LineNumberQuery.new(self).spec_name_for(files[0], line_number)
              @examples = [example]
            end
          else
            error_stream.puts "Only one file can be specified when providing a line number: #{files.inspect}"
            exit(3) if stderr?
          end
        else
          error_stream.puts "You cannot use --example and specify a line number"
          exit(4) if stderr?
        end
      end

      def stderr?
        @error_stream == $stderr
      end
    end
  end
end
