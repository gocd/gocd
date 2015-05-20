# http://www.ruby-doc.org/stdlib/libdoc/optparse/rdoc/classes/OptionParser.html
require 'optparse'

module RSpec::Core
  class Parser
    def self.parse!(args)
      new.parse!(args)
    end

    class << self
      alias_method :parse, :parse!
    end

    def parse!(args)
      return {} if args.empty?

      convert_deprecated_args(args)

      options = args.delete('--tty') ? {:tty => true} : {}
      begin
        parser(options).parse!(args)
      rescue OptionParser::InvalidOption => e
        abort "#{e.message}\n\nPlease use --help for a listing of valid options"
      end

      options
    end

    def convert_deprecated_args(args)
      args.map! { |arg|
        case arg
        when "--formatter"
          RSpec.deprecate("the --formatter option", :replacement => "-f or --format")
          "--format"
        when "--default_path"
          "--default-path"
        when "--line_number"
          "--line-number"
        else
          arg
        end
      }
    end

    alias_method :parse, :parse!

    def parser(options)
      OptionParser.new do |parser|
        parser.banner = "Usage: rspec [options] [files or directories]\n\n"

        parser.on('-I PATH', 'Specify PATH to add to $LOAD_PATH (may be used more than once).') do |dir|
          options[:libs] ||= []
          options[:libs] << dir
        end

        parser.on('-r', '--require PATH', 'Require a file.') do |path|
          options[:requires] ||= []
          options[:requires] << path
        end

        parser.on('-O', '--options PATH', 'Specify the path to a custom options file.') do |path|
          options[:custom_options_file] = path
        end

        parser.on('--order TYPE[:SEED]', 'Run examples by the specified order type.',
                  '  [default] files are ordered based on the underlying file',
                  '            system\'s order',
                  '  [rand]    randomize the order of files, groups and examples',
                  '  [random]  alias for rand',
                  '  [random:SEED] e.g. --order random:123') do |o|
          options[:order] = o
        end

        parser.on('--seed SEED', Integer, 'Equivalent of --order rand:SEED.') do |seed|
          options[:order] = "rand:#{seed}"
        end

        parser.on('-d', '--debugger', 'Enable debugging.') do |o|
          options[:debug] = true
        end

        parser.on('--fail-fast', 'Abort the run on first failure.') do |o|
          options[:fail_fast] = true
        end

        parser.on('--failure-exit-code CODE', Integer, 'Override the exit code used when there are failing specs.') do |code|
          options[:failure_exit_code] = code
        end

        parser.on('-X', '--[no-]drb', 'Run examples via DRb.') do |o|
          options[:drb] = o
        end

        parser.on('--drb-port PORT', 'Port to connect to the DRb server.') do |o|
          options[:drb_port] = o.to_i
        end

        parser.on('--init', 'Initialize your project with RSpec.') do |cmd|
          ProjectInitializer.new(cmd).run
          exit
        end

        parser.on('--configure', 'Deprecated. Use --init instead.') do |cmd|
          warn "--configure is deprecated with no effect. Use --init instead."
          exit
        end

        parser.separator("\n  **** Output ****\n\n")

        parser.on('-f', '--format FORMATTER', 'Choose a formatter.',
                '  [p]rogress (default - dots)',
                '  [d]ocumentation (group and example names)',
                '  [h]tml',
                '  [t]extmate',
                '  [j]son',
                '  custom formatter class name') do |o|
          options[:formatters] ||= []
          options[:formatters] << [o]
        end

        parser.on('-o', '--out FILE',
                  'Write output to a file instead of $stdout. This option applies',
                  '  to the previously specified --format, or the default format',
                  '  if no format is specified.'
                 ) do |o|
          options[:formatters] ||= [['progress']]
          options[:formatters].last << o
        end

        parser.on('-b', '--backtrace', 'Enable full backtrace.') do |o|
          options[:full_backtrace] = true
        end

        parser.on('-c', '--[no-]color', '--[no-]colour', 'Enable color in the output.') do |o|
          options[:color] = o
        end

        parser.on('-p', '--[no-]profile [COUNT]', 'Enable profiling of examples and list the slowest examples (default: 10).') do |argument|
          options[:profile_examples] = if argument.nil?
                                         true
                                       elsif argument == false
                                         false
                                       else
                                         begin
                                           Integer(argument)
                                         rescue ArgumentError
                                           Kernel.warn "Non integer specified as profile count, seperate " +
                                                       "your path from options with -- e.g. " +
                                                       "`rspec --profile -- #{argument}`"
                                           true
                                         end
                                       end
        end

        parser.on('-w', '--warnings', 'Enable ruby warnings') do
          options[:warnings] = true
        end

        parser.separator <<-FILTERING

  **** Filtering/tags ****

    In addition to the following options for selecting specific files, groups,
    or examples, you can select a single example by appending the line number to
    the filename:

      rspec path/to/a_spec.rb:37

FILTERING

        parser.on('-P', '--pattern PATTERN', 'Load files matching pattern (default: "spec/**/*_spec.rb").') do |o|
          options[:pattern] = o
        end

        parser.on('-e', '--example STRING', "Run examples whose full nested names include STRING (may be",
                                            "  used more than once)") do |o|
          (options[:full_description] ||= []) << Regexp.compile(Regexp.escape(o))
        end

        parser.on('-l', '--line-number LINE', 'Specify line number of an example or group (may be',
                                              '  used more than once).') do |o|
          (options[:line_numbers] ||= []) << o
        end

        parser.on('-t', '--tag TAG[:VALUE]',
                  'Run examples with the specified tag, or exclude examples',
                  'by adding ~ before the tag.',
                  '  - e.g. ~slow',
                  '  - TAG is always converted to a symbol') do |tag|
          filter_type = tag =~ /^~/ ? :exclusion_filter : :inclusion_filter

          name,value = tag.gsub(/^(~@|~|@)/, '').split(':')
          name = name.to_sym

          options[filter_type] ||= {}
          options[filter_type][name] = value.nil? ? true : eval(value) rescue value
        end

        parser.on('--default-path PATH', 'Set the default path where RSpec looks for examples (can',
                                         '  be a path to a file or a directory).') do |path|
          options[:default_path] = path
        end

        parser.separator("\n  **** Utility ****\n\n")

        parser.on('-v', '--version', 'Display the version.') do
          puts RSpec::Core::Version::STRING
          exit
        end

        parser.on_tail('-h', '--help', "You're looking at it.") do
          puts parser
          exit
        end

      end
    end
  end
end
