# ## Built-in Formatters
#
# * progress (default) - prints dots for passing examples, `F` for failures, `*` for pending
# * documentation - prints the docstrings passed to `describe` and `it` methods (and their aliases)
# * html
# * textmate - html plus links to editor
# * json - useful for archiving data for subsequent analysis
#
# The progress formatter is the default, but you can choose any one or more of
# the other formatters by passing with the `--format` (or `-f` for short)
# command-line option, e.g.
#
#     rspec --format documentation
#
# You can also send the output of multiple formatters to different streams, e.g.
#
#     rspec --format documentation --format html --out results.html
#
# This example sends the output of the documentation formatter to `$stdout`, and
# the output of the html formatter to results.html.
#
# ## Custom Formatters
#
# You can tell RSpec to use a custom formatter by passing its path and name to
# the `rspec` commmand. For example, if you define MyCustomFormatter in
# path/to/my_custom_formatter.rb, you would type this command:
#
#     rspec --require path/to/my_custom_formatter.rb --format MyCustomFormatter
#
# The reporter calls every formatter with this protocol:
#
# * `start(expected_example_count)`
# * zero or more of the following
#   * `example_group_started(group)`
#   * `example_started(example)`
#   * `example_passed(example)`
#   * `example_failed(example)`
#   * `example_pending(example)`
#   * `message(string)`
# * `stop`
# * `start_dump`
# * `dump_pending`
# * `dump_failures`
# * `dump_summary(duration, example_count, failure_count, pending_count)`
# * `seed(value)`
# * `close`
#
# You can either implement all of those methods or subclass
# `RSpec::Core::Formatters::BaseTextFormatter` and override the methods you want
# to enhance.
#
# @see RSpec::Core::Formatters::BaseTextFormatter
# @see RSpec::Core::Reporter
module RSpec::Core::Formatters
  autoload :DocumentationFormatter, 'rspec/core/formatters/documentation_formatter'
  autoload :HtmlFormatter,          'rspec/core/formatters/html_formatter'
  autoload :ProgressFormatter,      'rspec/core/formatters/progress_formatter'
  autoload :JsonFormatter,          'rspec/core/formatters/json_formatter'
  autoload :TextMateFormatter,      'rspec/core/formatters/text_mate_formatter'

  # @api private
  #
  # `RSpec::Core::Formatters::Loader` is an internal class for
  # managing formatters used by a particular configuration. It is
  # not expected to be used directly, but only through the configuration
  # interface.
  class Loader

    # @api private
    def initialize(reporter)
      @formatters = []
      @reporter = reporter
      @setup = false
      @default_formatter = 'progress'
    end

    # @return [Array] the loaded formatters
    attr_reader :formatters

    # @return [Reporter] the reporter
    attr_reader :reporter

    # @private
    def setup_default(output_stream, deprecation_stream)
      if @formatters.empty?
        add @default_formatter, output_stream
      end
      unless @formatters.any? { |formatter| DeprecationFormatter === formatter }
        add DeprecationFormatter, deprecation_stream, output_stream
      end
      @formatters.each do |formatter|
        @reporter.register_listener formatter, *RSpec::Core::Reporter::NOTIFICATIONS
      end
      @setup = true
    end

    # @private
    def add(formatter_to_use, *paths)
      formatter_class = find_formatter(formatter_to_use)

      args = paths.map { |p| p.respond_to?(:puts) ? p : file_at(p) }

      formatter = formatter_class.new(*args)
      if @setup
        @reporter.register_listener formatter, *RSpec::Core::Reporter::NOTIFICATIONS
      end
      @formatters << formatter unless duplicate_formatter_exists?(formatter)

      formatter
    end

  private

    def find_formatter(formatter_to_use)
      built_in_formatter(formatter_to_use) ||
      custom_formatter(formatter_to_use)   ||
      (raise ArgumentError, "Formatter '#{formatter_to_use}' unknown - maybe you meant 'documentation' or 'progress'?.")
    end

    def duplicate_formatter_exists?(new_formatter)
      @formatters.any? do |formatter|
        formatter.class === new_formatter && formatter.output == new_formatter.output
      end
    end

    def built_in_formatter(key)
      case key.to_s
      when 'd', 'doc', 'documentation'
        DocumentationFormatter
      when 's', 'n', 'spec', 'nested'
        RSpec.deprecate "Using `#{key.to_s}` as a shortcut for the DocumentationFormatter",
          :replacement => "`d`, `doc`, or `documentation`"
        DocumentationFormatter
      when 'h', 'html'
        HtmlFormatter
      when 'p', 'progress'
        ProgressFormatter
      when 'j', 'json'
        JsonFormatter
      when 't', 'textmate'
        if defined?(::RSpec::Mate::Formatters::TextMateFormatter)
          RSpec.deprecate "Using the text`#{key.to_s}` as a shortcut for the TextMateFormatter",
          :replacement => "`::RSpec::Mate::Formatters::TextMateFormatter`"
        else
          RSpec.deprecate "Using rspec-core's `::RSpec::Core::TextMateFormatter`",
          :replacement => "the `rspec-tmbundle` gem and it's `::RSpec::Mate::Formatters::TextMateFormatter`"
        end
        TextMateFormatter
      end
    end

    def custom_formatter(formatter_ref)
      if Class === formatter_ref
        formatter_ref
      elsif string_const?(formatter_ref)
        begin
          formatter_ref.gsub(/^::/,'').split('::').inject(Object) { |const,string| const.const_get string }
        rescue NameError
          require( path_for(formatter_ref) ) ? retry : raise
        end
      end
    end

    def string_const?(str)
      str.is_a?(String) && /\A[A-Z][a-zA-Z0-9_:]*\z/ =~ str
    end

    def path_for(const_ref)
      underscore_with_fix_for_non_standard_rspec_naming(const_ref)
    end

    def underscore_with_fix_for_non_standard_rspec_naming(string)
      underscore(string).sub(%r{(^|/)r_spec($|/)}, '\\1rspec\\2')
    end

    # activesupport/lib/active_support/inflector/methods.rb, line 48
    def underscore(camel_cased_word)
      word = camel_cased_word.to_s.dup
      word.gsub!(/::/, '/')
      word.gsub!(/([A-Z]+)([A-Z][a-z])/,'\1_\2')
      word.gsub!(/([a-z\d])([A-Z])/,'\1_\2')
      word.tr!("-", "_")
      word.downcase!
      word
    end

    def file_at(path)
      FileUtils.mkdir_p(File.dirname(path))
      File.new(path, 'w')
    end
  end
end
