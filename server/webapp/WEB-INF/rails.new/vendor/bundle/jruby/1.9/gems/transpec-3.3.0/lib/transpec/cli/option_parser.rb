# coding: utf-8

require 'transpec/config'
require 'transpec/git'
require 'transpec/version'
require 'optparse'
require 'rainbow'
require 'rainbow/ext/string' unless String.respond_to?(:color)

module Transpec
  class CLI
    class OptionParser # rubocop:disable ClassLength
      VALID_BOOLEAN_MATCHER_TYPES = %w(truthy,falsey truthy,falsy true,false)

      attr_reader :config

      def initialize(config = Config.new)
        @config = config
        setup_parser
      end

      def parse(args)
        args = convert_deprecated_options(args)
        @parser.parse!(args)
        args
      end

      def help
        @parser.help
      end

      private

      def setup_parser # rubocop:disable MethodLength
        @parser = create_parser

        define_option('-f', '--force') do
          config.forced = true
        end

        define_option('-c', '--rspec-command COMMAND') do |command|
          config.rspec_command = command
        end

        define_option('-k', '--keep TYPE[,TYPE...]') do |types|
          configure_conversion(types, false)
        end

        define_option('-v', '--convert TYPE[,TYPE...]') do |types|
          configure_conversion(types, true)
        end

        define_option('-o', '--convert-only TYPE[,TYPE...]') do |types|
          Config.conversion_types.each do |type|
            config.conversion[type] = false
          end

          configure_conversion(types, true)
        end

        define_option('-s', '--skip-dynamic-analysis') do
          config.skip_dynamic_analysis = true
        end

        define_option('-n', '--negative-form FORM') do |form|
          config.negative_form_of_to = form
        end

        define_option('-b', '--boolean-matcher TYPE') do |type|
          configure_boolean_matcher(type)
        end

        define_option('-e', '--explicit-spec-type') do
          config.add_explicit_type_metadata_to_example_group = true
        end

        define_option('-a', '--no-yield-any-instance') do
          config.add_receiver_arg_to_any_instance_implementation_block = false
        end

        define_option('-p', '--no-parens-matcher-arg') do
          config.parenthesize_matcher_arg = false
        end

        define_option('--no-color') do
          Rainbow.enabled = false
        end

        define_option('--version') do
          puts Version.to_s
          exit
        end
      end

      def create_parser
        banner = "Usage: transpec [options] [files or directories]\n\n"
        summary_width = 34
        indentation = ' ' * 2
        ::OptionParser.new(banner, summary_width, indentation)
      end

      def define_option(*options, &block)
        description_lines = descriptions[options.first]
        description_lines = description_lines.map { |line| highlight_text(line) }
        @parser.on(*options, *description_lines, &block)
      end

      # rubocop:disable AlignHash
      def descriptions # rubocop:disable MethodLength
        @descriptions ||= {
          '-f' => [
            'Force processing even if the current Git repository is not',
            'clean.'
          ],
          '-s' => [
            'Skip dynamic analysis and convert with only static analysis.',
            'The use of this option is basically *discouraged* since it',
            'significantly decreases the overall conversion accuracy.'
          ],
          '-c' => [
            'Specify a command to run your specs that is used for dynamic',
            'analysis.',
            'Default: "bundle exec rspec"'
          ],
          '-k' => [
            'Keep specific syntaxes by disabling conversions.',
            'Conversion Types:',
            '  *should* (to `expect(obj).to`)',
            '  *oneliner* (`it { should ... }` to `it { is_expected.to ... }`)',
            '  *should_receive* (to `expect(obj).to receive`)',
            '  *stub*  (to `allow(obj).to receive`)',
            '  *have_items* (to `expect(collection.size).to eq(n)`)',
            "  *its* (to `describe '#attr' { subject { }; it { } }`)",
            '  *pending* (to `skip`)',
            '  *deprecated* (all other deprecated syntaxes to latest syntaxes)',
            'These conversions are enabled by default.'
          ],
          '-v' => [
            'Enable specific conversions that are disabled by default.',
            'Conversion Types:',
            '  *example_group* (`describe` to `RSpec.describe`)',
            '  *hook_scope* (`before(:all)` to `before(:context)`)',
            '  *stub_with_hash* (`obj.stub(:msg => val)` to',
            '                  `allow(obj).to receive(:msg).and_return(val)`)',
            'These conversions are disabled by default.'
          ],
          '-o' => [
            'Convert specific syntaxes while keeping all other syntaxes.'
          ],
          '-n' => [
            'Specify a negative form of `to` that is used in the',
            '`expect(...).to syntax. Either *not_to* or *to_not*.',
            'Default: *not_to*'
          ],
          '-b' => [
            'Specify a matcher type that `be_true` and `be_false` will be',
            'converted to.',
            '  *truthy,falsey* (conditional semantics)',
            '  *truthy,falsy*  (alias of `falsey`)',
            '  *true,false*    (exact equality)',
            'Default: *truthy,falsey*'
          ],
          '-e' => [
            'Add explicit `:type` metadata to example groups in a project',
            'using rspec-rails.'
          ],
          '-a' => [
            'Suppress yielding receiver instances to `any_instance`',
            'implementation blocks as the first block argument.'
          ],
          '-p' => [
            'Suppress parenthesizing arguments of matchers when converting',
            '`should` with operator matcher to `expect` with non-operator',
            'matcher. Note that it will be parenthesized even if this option',
            'is specified when parentheses are necessary to keep the meaning',
            'of the expression. By default, arguments of the following',
            'operator matchers will be parenthesized.',
            '  `== 10` to `eq(10)`',
            '  `=~ /pattern/` to `match(/pattern/)`',
            '  `=~ [1, 2]` to `match_array([1, 2])`'
          ],
          '--no-color' => [
            'Disable color in the output.'
          ],
          '--version' => [
            'Show Transpec version.'
          ]
        }
      end
      # rubocop:enable AlignHash

      def highlight_text(text)
        text.gsub(/`.+?`/) { |code| code.delete('`').underline }
          .gsub(/\*.+?\*/) { |code| code.delete('*').bright }
      end

      def convert_deprecated_options(raw_args)
        raw_args.each_with_object([]) do |arg, args|
          case arg
          when '--no-parentheses-matcher-arg'
            deprecate('--no-parentheses-matcher-arg option', '--no-parens-matcher-arg')
            args << '--no-parens-matcher-arg'
          else
            args << arg
          end
        end
      end

      def deprecate(subject, alternative = nil)
        message =  "DEPRECATION: #{subject} is deprecated."
        message << " Use #{alternative} instead." if alternative
        warn message
      end

      def configure_conversion(inputted_types, boolean)
        inputted_types.split(',').each do |type|
          unless Config.valid_conversion_type?(type)
            fail ArgumentError, "Unknown syntax type #{type.inspect}"
          end

          config.conversion[type] = boolean
        end
      end

      def configure_boolean_matcher(type)
        unless VALID_BOOLEAN_MATCHER_TYPES.include?(type)
          types = VALID_BOOLEAN_MATCHER_TYPES.map(&:inspect).join(', ')
          fail ArgumentError, "Boolean matcher type must be any of #{types}"
        end

        config.boolean_matcher_type = type.include?('truthy') ? :conditional : :exact
        config.form_of_be_falsey = type.include?('falsy') ? 'be_falsy' : 'be_falsey'
      end
    end
  end
end
