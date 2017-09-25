require 'fileutils'
require 'rspec/core/backtrace_cleaner'
require 'rspec/core/ruby_project'
require 'rspec/core/formatters/deprecation_formatter.rb'

module RSpec
  module Core
    # Stores runtime configuration information.
    #
    # Configuration options are loaded from `~/.rspec`, `.rspec`,
    # `.rspec-local`, command line switches, and the `SPEC_OPTS` environment
    # variable (listed in lowest to highest precedence; for example, an option
    # in `~/.rspec` can be overridden by an option in `.rspec-local`).
    #
    # @example Standard settings
    #     RSpec.configure do |c|
    #       c.drb          = true
    #       c.drb_port     = 1234
    #       c.default_path = 'behavior'
    #     end
    #
    # @example Hooks
    #     RSpec.configure do |c|
    #       c.before(:suite) { establish_connection }
    #       c.before(:each)  { log_in_as :authorized }
    #       c.around(:each)  { |ex| Database.transaction(&ex) }
    #     end
    #
    # @see RSpec.configure
    # @see Hooks
    class Configuration
      include RSpec::Core::Hooks

      class MustBeConfiguredBeforeExampleGroupsError < StandardError; end

      # @private
      def self.define_reader(name)
        define_method(name) do
          variable = instance_variable_defined?("@#{name}") ? instance_variable_get("@#{name}") : nil
          value_for(name, variable)
        end
      end

      # @private
      def self.deprecate_alias_key
        RSpec.deprecate("add_setting with :alias option", :replacement => ":alias_with")
      end

      # @private
      def self.define_aliases(name, alias_name)
        alias_method alias_name, name
        alias_method "#{alias_name}=", "#{name}="
        define_predicate_for alias_name
      end

      # @private
      def self.define_predicate_for(*names)
        names.each {|name| alias_method "#{name}?", name}
      end

      # @private
      #
      # Invoked by the `add_setting` instance method. Use that method on a
      # `Configuration` instance rather than this class method.
      def self.add_setting(name, opts={})
        raise "Use the instance add_setting method if you want to set a default" if opts.has_key?(:default)
        if opts[:alias]
          deprecate_alias_key
          define_aliases(opts[:alias], name)
        else
          attr_writer name
          define_reader name
          define_predicate_for name
        end
        [opts[:alias_with]].flatten.compact.each do |alias_name|
          define_aliases(name, alias_name)
        end
      end

      # @macro [attach] add_setting
      #   @attribute $1

      # Path to use if no path is provided to the `rspec` command (default:
      # `"spec"`). Allows you to just type `rspec` instead of `rspec spec` to
      # run all the examples in the `spec` directory.
      add_setting :default_path

      # Run examples over DRb (default: `false`). RSpec doesn't supply the DRb
      # server, but you can use tools like spork.
      add_setting :drb

      # The drb_port (default: nil).
      add_setting :drb_port

      # Default: `$stderr`.
      add_setting :error_stream

      # Default: `$stderr`.
      add_setting :deprecation_stream

      # Clean up and exit after the first failure (default: `false`).
      add_setting :fail_fast

      # The exit code to return if there are any failures (default: 1).
      add_setting :failure_exit_code

      # Determines the order in which examples are run (default: OS standard
      # load order for files, declaration order for groups and examples).
      define_reader :order

      # Indicates files configured to be required
      define_reader :requires

      # Returns dirs that have been prepended to the load path by #lib=
      define_reader :libs

      # Default: `$stdout`.
      # Also known as `output` and `out`
      add_setting :output_stream, :alias_with => [:output, :out]

      # Load files matching this pattern (default: `'**/*_spec.rb'`)
      add_setting :pattern, :alias_with => :filename_pattern

      # Report the times for the slowest examples (default: `false`).
      # Use this to specify the number of examples to include in the profile.
      add_setting :profile_examples

      # Run all examples if none match the configured filters (default: `false`).
      add_setting :run_all_when_everything_filtered

      # Allow user to configure their own success/pending/failure colors
      # @param [Symbol] should be one of the following: [:black, :white, :red, :green, :yellow, :blue, :magenta, :cyan]
      add_setting :success_color
      add_setting :pending_color
      add_setting :failure_color
      add_setting :default_color
      add_setting :fixed_color
      add_setting :detail_color

      # Seed for random ordering (default: generated randomly each run).
      #
      # When you run specs with `--order random`, RSpec generates a random seed
      # for the randomization and prints it to the `output_stream` (assuming
      # you're using RSpec's built-in formatters). If you discover an ordering
      # dependency (i.e. examples fail intermittently depending on order), set
      # this (on Configuration or on the command line with `--seed`) to run
      # using the same seed while you debug the issue.
      #
      # We recommend, actually, that you use the command line approach so you
      # don't accidentally leave the seed encoded.
      define_reader :seed

      # When a block passed to pending fails (as expected), display the failure
      # without reporting it as a failure (default: false).
      add_setting :show_failures_in_pending_blocks

      # Convert symbols to hashes with the symbol as a key with a value of
      # `true` (default: false).
      #
      # This allows you to tag a group or example like this:
      #
      #     describe "something slow", :slow do
      #       # ...
      #     end
      #
      # ... instead of having to type:
      #
      #     describe "something slow", :slow => true do
      #       # ...
      #     end
      add_setting :treat_symbols_as_metadata_keys_with_true_values

      # @private
      add_setting :tty
      # @private
      add_setting :include_or_extend_modules
      # @private
      add_setting :files_to_run
      # @private
      add_setting :expecting_with_rspec
      # @private
      attr_accessor :filter_manager

      attr_reader :backtrace_cleaner

      def initialize
        @expectation_frameworks = []
        @include_or_extend_modules = []
        @mock_framework = nil
        @files_to_run = []
        @formatters = []
        @color = false
        @pattern = '**/*_spec.rb'
        @failure_exit_code = 1

        @backtrace_cleaner = BacktraceCleaner.new

        @default_path = 'spec'
        @deprecation_stream = $stderr
        @filter_manager = FilterManager.new
        @preferred_options = {}
        @seed = srand % 0xFFFF
        @failure_color = :red
        @success_color = :green
        @pending_color = :yellow
        @default_color = :white
        @fixed_color = :blue
        @detail_color = :cyan
        @profile_examples = false
        @requires = []
        @libs = []
      end

      # @private
      #
      # Used to set higher priority option values from the command line.
      def force(hash)
        if hash.has_key?(:seed)
          hash[:order], hash[:seed] = order_and_seed_from_seed(hash[:seed])
        elsif hash.has_key?(:order)
          set_order_and_seed(hash)
        end
        @preferred_options.merge!(hash)
        self.warnings = value_for :warnings, nil
      end

      # @private
      def reset
        @reporter = nil
        @formatters.clear
      end

      # @overload add_setting(name)
      # @overload add_setting(name, opts)
      # @option opts [Symbol] :default
      #
      #   set a default value for the generated getter and predicate methods:
      #
      #       add_setting(:foo, :default => "default value")
      #
      # @option opts [Symbol] :alias_with
      #
      #   Use `:alias_with` to alias the setter, getter, and predicate to another
      #   name, or names:
      #
      #       add_setting(:foo, :alias_with => :bar)
      #       add_setting(:foo, :alias_with => [:bar, :baz])
      #
      # Adds a custom setting to the RSpec.configuration object.
      #
      #     RSpec.configuration.add_setting :foo
      #
      # Used internally and by extension frameworks like rspec-rails, so they
      # can add config settings that are domain specific. For example:
      #
      #     RSpec.configure do |c|
      #       c.add_setting :use_transactional_fixtures,
      #         :default => true,
      #         :alias_with => :use_transactional_examples
      #     end
      #
      # `add_setting` creates three methods on the configuration object, a
      # setter, a getter, and a predicate:
      #
      #     RSpec.configuration.foo=(value)
      #     RSpec.configuration.foo
      #     RSpec.configuration.foo? # returns true if foo returns anything but nil or false
      def add_setting(name, opts={})
        default = opts.delete(:default)
        (class << self; self; end).class_eval do
          add_setting(name, opts)
        end
        send("#{name}=", default) if default
      end

      # Returns the configured mock framework adapter module
      def mock_framework
        mock_with :rspec unless @mock_framework
        @mock_framework
      end

      # Delegates to mock_framework=(framework)
      def mock_framework=(framework)
        mock_with framework
      end

      # The patterns to discard from backtraces. Deprecated, use
      # Configuration#backtrace_exclusion_patterns instead
      #
      # Defaults to RSpec::Core::BacktraceCleaner::DEFAULT_EXCLUSION_PATTERNS
      #
      # One can replace the list by using the setter or modify it through the
      # getter
      #
      # To override this behaviour and display a full backtrace, use
      # `--backtrace`on the command line, in a `.rspec` file, or in the
      # `rspec_options` attribute of RSpec's rake task.
      def backtrace_clean_patterns
        RSpec.deprecate("RSpec::Core::Configuration#backtrace_clean_patterns",
                        :replacement => "RSpec::Core::Configuration#backtrace_exclusion_patterns")
        @backtrace_cleaner.exclusion_patterns
      end

      def backtrace_clean_patterns=(patterns)
        RSpec.deprecate("RSpec::Core::Configuration#backtrace_clean_patterns",
                        :replacement => "RSpec::Core::Configuration#backtrace_exclusion_patterns")
        @backtrace_cleaner.exclusion_patterns = patterns
      end

      # The patterns to always include to backtraces.
      #
      # Defaults to [Regexp.new Dir.getwd] if the current working directory
      # matches any of the exclusion patterns. Otherwise it defaults to empty.
      #
      # One can replace the list by using the setter or modify it through the
      # getter
      def backtrace_inclusion_patterns
        @backtrace_cleaner.inclusion_patterns
      end

      def backtrace_inclusion_patterns=(patterns)
        @backtrace_cleaner.inclusion_patterns = patterns
      end

      # The patterns to discard from backtraces.
      #
      # Defaults to RSpec::Core::BacktraceCleaner::DEFAULT_EXCLUSION_PATTERNS
      #
      # One can replace the list by using the setter or modify it through the
      # getter
      #
      # To override this behaviour and display a full backtrace, use
      # `--backtrace`on the command line, in a `.rspec` file, or in the
      # `rspec_options` attribute of RSpec's rake task.
      def backtrace_exclusion_patterns
        @backtrace_cleaner.exclusion_patterns
      end

      def backtrace_exclusion_patterns=(patterns)
        @backtrace_cleaner.exclusion_patterns = patterns
      end

      # Sets the mock framework adapter module.
      #
      # `framework` can be a Symbol or a Module.
      #
      # Given any of `:rspec`, `:mocha`, `:flexmock`, or `:rr`, configures the
      # named framework.
      #
      # Given `:nothing`, configures no framework. Use this if you don't use
      # any mocking framework to save a little bit of overhead.
      #
      # Given a Module, includes that module in every example group. The module
      # should adhere to RSpec's mock framework adapter API:
      #
      #     setup_mocks_for_rspec
      #       - called before each example
      #
      #     verify_mocks_for_rspec
      #       - called after each example. Framework should raise an exception
      #         when expectations fail
      #
      #     teardown_mocks_for_rspec
      #       - called after verify_mocks_for_rspec (even if there are errors)
      #
      # If the module responds to `configuration` and `mock_with` receives a block,
      # it will yield the configuration object to the block e.g.
      #
      #     config.mock_with OtherMockFrameworkAdapter do |mod_config|
      #       mod_config.custom_setting = true
      #     end
      def mock_with(framework)
        framework_module = case framework
        when Module
          framework
        when String, Symbol
          require case framework.to_s
                  when /rspec/i
                    'rspec/core/mocking/with_rspec'
                  when /mocha/i
                    'rspec/core/mocking/with_mocha'
                  when /rr/i
                    'rspec/core/mocking/with_rr'
                  when /flexmock/i
                    'rspec/core/mocking/with_flexmock'
                  else
                    'rspec/core/mocking/with_absolutely_nothing'
                  end
          RSpec::Core::MockFrameworkAdapter
        end

        new_name, old_name = [framework_module, @mock_framework].map do |mod|
          mod.respond_to?(:framework_name) ?  mod.framework_name : :unnamed
        end

        unless new_name == old_name
          assert_no_example_groups_defined(:mock_framework)
        end

        if block_given?
          raise "#{framework_module} must respond to `configuration` so that mock_with can yield it." unless framework_module.respond_to?(:configuration)
          yield framework_module.configuration
        end

        @mock_framework = framework_module
      end

      # Returns the configured expectation framework adapter module(s)
      def expectation_frameworks
        expect_with :rspec if @expectation_frameworks.empty?
        @expectation_frameworks
      end

      # Delegates to expect_with(framework)
      def expectation_framework=(framework)
        expect_with(framework)
      end

      # Sets the expectation framework module(s) to be included in each example
      # group.
      #
      # `frameworks` can be `:rspec`, `:stdlib`, a custom module, or any
      # combination thereof:
      #
      #     config.expect_with :rspec
      #     config.expect_with :stdlib
      #     config.expect_with :rspec, :stdlib
      #     config.expect_with OtherExpectationFramework
      #
      # RSpec will translate `:rspec` and `:stdlib` into the appropriate
      # modules.
      #
      # ## Configuration
      #
      # If the module responds to `configuration`, `expect_with` will
      # yield the `configuration` object if given a block:
      #
      #     config.expect_with OtherExpectationFramework do |custom_config|
      #       custom_config.custom_setting = true
      #     end
      def expect_with(*frameworks)
        modules = frameworks.map do |framework|
          case framework
          when Module
            framework
          when :rspec
            require 'rspec/expectations'
            self.expecting_with_rspec = true
            ::RSpec::Matchers
          when :stdlib
            require 'test/unit/assertions'
            ::Test::Unit::Assertions
          else
            raise ArgumentError, "#{framework.inspect} is not supported"
          end
        end

        if (modules - @expectation_frameworks).any?
          assert_no_example_groups_defined(:expect_with)
        end

        if block_given?
          raise "expect_with only accepts a block with a single argument. Call expect_with #{modules.length} times, once with each argument, instead." if modules.length > 1
          raise "#{modules.first} must respond to `configuration` so that expect_with can yield it." unless modules.first.respond_to?(:configuration)
          yield modules.first.configuration
        end

        @expectation_frameworks.push(*modules)
      end

      def full_backtrace?
        @backtrace_cleaner.full_backtrace?
      end

      def full_backtrace=(true_or_false)
        @backtrace_cleaner.full_backtrace = true_or_false
      end

      def color(output=output_stream)
        # rspec's built-in formatters all call this with the output argument,
        # but defaulting to output_stream for backward compatibility with
        # formatters in extension libs
        return false unless output_to_tty?(output)
        value_for(:color, @color)
      end

      def color=(bool)
        if bool
          if RSpec.windows_os? and not ENV['ANSICON']
            warn "You must use ANSICON 1.31 or later (http://adoxa.3eeweb.com/ansicon/) to use colour on Windows"
            @color = false
          else
            @color = true
          end
        end
      end

      # TODO - deprecate color_enabled - probably not until the last 2.x
      # release before 3.0
      alias_method :color_enabled, :color
      alias_method :color_enabled=, :color=
      define_predicate_for :color_enabled, :color

      def libs=(libs)
        libs.map do |lib|
          @libs.unshift lib
          $LOAD_PATH.unshift lib
        end
      end

      def requires=(paths)
        RSpec.deprecate("RSpec::Core::Configuration#requires=(paths)",
                        :replacement => "paths.each {|path| require path}")
        paths.map {|path| require path}
        @requires += paths
      end

      def debug=(bool)
        return unless bool
        begin
          require 'ruby-debug'
          Debugger.start
        rescue LoadError => e
          raise <<-EOM

#{'*'*50}
#{e.message}

If you have it installed as a ruby gem, then you need to either require
'rubygems' or configure the RUBYOPT environment variable with the value
'rubygems'.

#{e.backtrace.join("\n")}
#{'*'*50}
EOM
        end
      end

      def debug?
        !!defined?(Debugger)
      end

      # Run examples defined on `line_numbers` in all files to run.
      def line_numbers=(line_numbers)
        filter_run :line_numbers => line_numbers.map{|l| l.to_i}
      end

      def line_numbers
        filter.fetch(:line_numbers,[])
      end

      def full_description=(description)
        filter_run :full_description => Regexp.union(*Array(description).map {|d| Regexp.new(d) })
      end

      def full_description
        filter.fetch :full_description, nil
      end

      # @overload add_formatter(formatter)
      #
      # Adds a formatter to the formatters collection. `formatter` can be a
      # string representing any of the built-in formatters (see
      # `built_in_formatter`), or a custom formatter class.
      #
      # ### Note
      #
      # For internal purposes, `add_formatter` also accepts the name of a class
      # and paths to use for output streams, but you should consider that a
      # private api that may change at any time without notice.
      def add_formatter(formatter_to_use, *paths)
        formatter_class =
          built_in_formatter(formatter_to_use) ||
          custom_formatter(formatter_to_use) ||
          (raise ArgumentError, "Formatter '#{formatter_to_use}' unknown - maybe you meant 'documentation' or 'progress'?.")

        paths << output if paths.empty?
        formatters << formatter_class.new(*paths.map {|p| String === p ? file_at(p) : p})
      end

      alias_method :formatter=, :add_formatter

      def formatters
        @formatters ||= []
      end

      def reporter
        @reporter ||= begin
                        add_formatter('progress') if formatters.empty?
                        add_formatter(RSpec::Core::Formatters::DeprecationFormatter, deprecation_stream, output_stream)
                        Reporter.new(*formatters)
                      end
      end

      # @api private
      #
      # Defaults `profile_examples` to 10 examples when `@profile_examples` is `true`.
      #
      def profile_examples
        profile = value_for(:profile_examples, @profile_examples)
        if profile && !profile.is_a?(Integer)
          10
        else
          profile
        end
      end

      # @private
      def files_or_directories_to_run=(*files)
        files = files.flatten
        files << default_path if (command == 'rspec' || Runner.running_in_drb?) && default_path && files.empty?
        self.files_to_run = get_files_to_run(files)
      end

      # Creates a method that delegates to `example` including the submitted
      # `args`. Used internally to add variants of `example` like `pending`:
      #
      # @example
      #     alias_example_to :pending, :pending => true
      #
      #     # This lets you do this:
      #
      #     describe Thing do
      #       pending "does something" do
      #         thing = Thing.new
      #       end
      #     end
      #
      #     # ... which is the equivalent of
      #
      #     describe Thing do
      #       it "does something", :pending => true do
      #         thing = Thing.new
      #       end
      #     end
      def alias_example_to(new_name, *args)
        extra_options = build_metadata_hash_from(args)
        RSpec::Core::ExampleGroup.alias_example_to(new_name, extra_options)
      end

      # Define an alias for it_should_behave_like that allows different
      # language (like "it_has_behavior" or "it_behaves_like") to be
      # employed when including shared examples.
      #
      # Example:
      #
      #     alias_it_behaves_like_to(:it_has_behavior, 'has behavior:')
      #
      # allows the user to include a shared example group like:
      #
      #     describe Entity do
      #       it_has_behavior 'sortability' do
      #         let(:sortable) { Entity.new }
      #       end
      #     end
      #
      # which is reported in the output as:
      #
      #     Entity
      #       has behavior: sortability
      #         # sortability examples here
      def alias_it_behaves_like_to(new_name, report_label = '')
        RSpec::Core::ExampleGroup.alias_it_behaves_like_to(new_name, report_label)
      end

      alias_method :alias_it_should_behave_like_to, :alias_it_behaves_like_to

      # Adds key/value pairs to the `inclusion_filter`. If the
      # `treat_symbols_as_metadata_keys_with_true_values` config option is set
      # to true and `args` includes any symbols that are not part of a hash,
      # each symbol is treated as a key in the hash with the value `true`.
      #
      # ### Note
      #
      # Filters set using this method can be overridden from the command line
      # or config files (e.g. `.rspec`).
      #
      # @example
      #     # given this declaration
      #     describe "something", :foo => 'bar' do
      #       # ...
      #     end
      #
      #     # any of the following will include that group
      #     config.filter_run_including :foo => 'bar'
      #     config.filter_run_including :foo => /^ba/
      #     config.filter_run_including :foo => lambda {|v| v == 'bar'}
      #     config.filter_run_including :foo => lambda {|v,m| m[:foo] == 'bar'}
      #
      #     # given a proc with an arity of 1, the lambda is passed the value related to the key, e.g.
      #     config.filter_run_including :foo => lambda {|v| v == 'bar'}
      #
      #     # given a proc with an arity of 2, the lambda is passed the value related to the key,
      #     # and the metadata itself e.g.
      #     config.filter_run_including :foo => lambda {|v,m| m[:foo] == 'bar'}
      #
      #     # with treat_symbols_as_metadata_keys_with_true_values = true
      #     filter_run_including :foo # same as filter_run_including :foo => true
      def filter_run_including(*args)
        filter_manager.include_with_low_priority build_metadata_hash_from(args)
      end

      alias_method :filter_run, :filter_run_including

      # Clears and reassigns the `inclusion_filter`. Set to `nil` if you don't
      # want any inclusion filter at all.
      #
      # ### Warning
      #
      # This overrides any inclusion filters/tags set on the command line or in
      # configuration files.
      def inclusion_filter=(filter)
        filter_manager.include! build_metadata_hash_from([filter])
      end

      alias_method :filter=, :inclusion_filter=

      # Returns the `inclusion_filter`. If none has been set, returns an empty
      # hash.
      def inclusion_filter
        filter_manager.inclusions
      end

      alias_method :filter, :inclusion_filter

      # Adds key/value pairs to the `exclusion_filter`. If the
      # `treat_symbols_as_metadata_keys_with_true_values` config option is set
      # to true and `args` excludes any symbols that are not part of a hash,
      # each symbol is treated as a key in the hash with the value `true`.
      #
      # ### Note
      #
      # Filters set using this method can be overridden from the command line
      # or config files (e.g. `.rspec`).
      #
      # @example
      #     # given this declaration
      #     describe "something", :foo => 'bar' do
      #       # ...
      #     end
      #
      #     # any of the following will exclude that group
      #     config.filter_run_excluding :foo => 'bar'
      #     config.filter_run_excluding :foo => /^ba/
      #     config.filter_run_excluding :foo => lambda {|v| v == 'bar'}
      #     config.filter_run_excluding :foo => lambda {|v,m| m[:foo] == 'bar'}
      #
      #     # given a proc with an arity of 1, the lambda is passed the value related to the key, e.g.
      #     config.filter_run_excluding :foo => lambda {|v| v == 'bar'}
      #
      #     # given a proc with an arity of 2, the lambda is passed the value related to the key,
      #     # and the metadata itself e.g.
      #     config.filter_run_excluding :foo => lambda {|v,m| m[:foo] == 'bar'}
      #
      #     # with treat_symbols_as_metadata_keys_with_true_values = true
      #     filter_run_excluding :foo # same as filter_run_excluding :foo => true
      def filter_run_excluding(*args)
        filter_manager.exclude_with_low_priority build_metadata_hash_from(args)
      end

      # Clears and reassigns the `exclusion_filter`. Set to `nil` if you don't
      # want any exclusion filter at all.
      #
      # ### Warning
      #
      # This overrides any exclusion filters/tags set on the command line or in
      # configuration files.
      def exclusion_filter=(filter)
        filter_manager.exclude! build_metadata_hash_from([filter])
      end

      # Returns the `exclusion_filter`. If none has been set, returns an empty
      # hash.
      def exclusion_filter
        filter_manager.exclusions
      end

      # Tells RSpec to include `mod` in example groups. Methods defined in
      # `mod` are exposed to examples (not example groups).  Use `filters` to
      # constrain the groups in which to include the module.
      #
      # @example
      #
      #     module AuthenticationHelpers
      #       def login_as(user)
      #         # ...
      #       end
      #     end
      #
      #     module UserHelpers
      #       def users(username)
      #         # ...
      #       end
      #     end
      #
      #     RSpec.configure do |config|
      #       config.include(UserHelpers) # included in all modules
      #       config.include(AuthenticationHelpers, :type => :request)
      #     end
      #
      #     describe "edit profile", :type => :request do
      #       it "can be viewed by owning user" do
      #         login_as users(:jdoe)
      #         get "/profiles/jdoe"
      #         assert_select ".username", :text => 'jdoe'
      #       end
      #     end
      #
      # @see #extend
      def include(mod, *filters)
        include_or_extend_modules << [:include, mod, build_metadata_hash_from(filters)]
      end

      # Tells RSpec to extend example groups with `mod`.  Methods defined in
      # `mod` are exposed to example groups (not examples).  Use `filters` to
      # constrain the groups to extend.
      #
      # Similar to `include`, but behavior is added to example groups, which
      # are classes, rather than the examples, which are instances of those
      # classes.
      #
      # @example
      #
      #     module UiHelpers
      #       def run_in_browser
      #         # ...
      #       end
      #     end
      #
      #     RSpec.configure do |config|
      #       config.extend(UiHelpers, :type => :request)
      #     end
      #
      #     describe "edit profile", :type => :request do
      #       run_in_browser
      #
      #       it "does stuff in the client" do
      #         # ...
      #       end
      #     end
      #
      # @see #include
      def extend(mod, *filters)
        include_or_extend_modules << [:extend, mod, build_metadata_hash_from(filters)]
      end

      # @private
      #
      # Used internally to extend a group with modules using `include` and/or
      # `extend`.
      def configure_group(group)
        include_or_extend_modules.each do |include_or_extend, mod, filters|
          next unless filters.empty? || group.any_apply?(filters)
          send("safe_#{include_or_extend}", mod, group)
        end
      end

      # @private
      def safe_include(mod, host)
        host.send(:include,mod) unless host < mod
      end

      # @private
      def setup_load_path_and_require(paths)
        directories = ['lib', default_path].select { |p| File.directory? p }
        RSpec::Core::RubyProject.add_to_load_path(*directories)
        paths.each {|path| require path}
        @requires += paths
      end

      # @private
      if RUBY_VERSION.to_f >= 1.9
        def safe_extend(mod, host)
          host.extend(mod) unless (class << host; self; end) < mod
        end
      else
        def safe_extend(mod, host)
          host.extend(mod) unless (class << host; self; end).included_modules.include?(mod)
        end
      end

      # @private
      def configure_mock_framework
        RSpec::Core::ExampleGroup.send(:include, mock_framework)
      end

      # @private
      def configure_expectation_framework
        expectation_frameworks.each do |framework|
          RSpec::Core::ExampleGroup.send(:include, framework)
        end
      end

      # @private
      def load_spec_files
        files_to_run.uniq.each {|f| load File.expand_path(f) }
        raise_if_rspec_1_is_loaded
      end

      # @private
      DEFAULT_FORMATTER = lambda { |string| string }

      # Formats the docstring output using the block provided.
      #
      # @example
      #   # This will strip the descriptions of both examples and example groups.
      #   RSpec.configure do |config|
      #     config.format_docstrings { |s| s.strip }
      #   end
      def format_docstrings(&block)
        @format_docstrings_block = block_given? ? block : DEFAULT_FORMATTER
      end

      # @private
      def format_docstrings_block
        @format_docstrings_block ||= DEFAULT_FORMATTER
      end

      # @api
      #
      # Sets the seed value and sets `order='rand'`
      def seed=(seed)
        order_and_seed_from_seed(seed)
      end

      # @api
      #
      # Sets the order and, if order is `'rand:<seed>'`, also sets the seed.
      def order=(type)
        order_and_seed_from_order(type)
      end

      def randomize?
        order.to_s.match(/rand/)
      end

      # @private
      DEFAULT_ORDERING = lambda { |list| list }

      # @private
      RANDOM_ORDERING = lambda do |list|
        Kernel.srand RSpec.configuration.seed
        ordering = list.sort_by { Kernel.rand(list.size) }
        Kernel.srand # reset random generation
        ordering
      end

      # Sets a strategy by which to order examples.
      #
      # @example
      #   RSpec.configure do |config|
      #     config.order_examples do |examples|
      #       examples.reverse
      #     end
      #   end
      #
      # @see #order_groups
      # @see #order_groups_and_examples
      # @see #order=
      # @see #seed=
      def order_examples(&block)
        @example_ordering_block = block
        @order = "custom" unless built_in_orderer?(block)
      end

      # @private
      def example_ordering_block
        @example_ordering_block ||= DEFAULT_ORDERING
      end

      # Sets a strategy by which to order groups.
      #
      # @example
      #   RSpec.configure do |config|
      #     config.order_groups do |groups|
      #       groups.reverse
      #     end
      #   end
      #
      # @see #order_examples
      # @see #order_groups_and_examples
      # @see #order=
      # @see #seed=
      def order_groups(&block)
        @group_ordering_block = block
        @order = "custom" unless built_in_orderer?(block)
      end

      # @private
      def group_ordering_block
        @group_ordering_block ||= DEFAULT_ORDERING
      end

      # Sets a strategy by which to order groups and examples.
      #
      # @example
      #   RSpec.configure do |config|
      #     config.order_groups_and_examples do |groups_or_examples|
      #       groups_or_examples.reverse
      #     end
      #   end
      #
      # @see #order_groups
      # @see #order_examples
      # @see #order=
      # @see #seed=
      def order_groups_and_examples(&block)
        order_groups(&block)
        order_examples(&block)
      end

      # Set Ruby warnings on or off
      def warnings= value
        $VERBOSE = !!value
      end

      def warnings
        $VERBOSE
      end

    private

      def get_files_to_run(paths)
        paths.map do |path|
          path = path.gsub(File::ALT_SEPARATOR, File::SEPARATOR) if File::ALT_SEPARATOR
          File.directory?(path) ? gather_directories(path) : extract_location(path)
        end.flatten.sort
      end

      def gather_directories(path)
        stripped = "{#{pattern.gsub(/\s*,\s*/, ',')}}"
        files    = pattern =~ /^#{Regexp.escape path}/ ? Dir[stripped] : Dir["#{path}/#{stripped}"]
        files.sort
      end

      def extract_location(path)
        if path =~ /^(.*?)((?:\:\d+)+)$/
          path, lines = $1, $2[1..-1].split(":").map{|n| n.to_i}
          filter_manager.add_location path, lines
        end
        path
      end

      def command
        $0.split(File::SEPARATOR).last
      end

      def value_for(key, default=nil)
        @preferred_options.has_key?(key) ? @preferred_options[key] : default
      end

      def assert_no_example_groups_defined(config_option)
        if RSpec.world.example_groups.any?
          raise MustBeConfiguredBeforeExampleGroupsError.new(
            "RSpec's #{config_option} configuration option must be configured before " +
            "any example groups are defined, but you have already defined a group."
          )
        end
      end

      def raise_if_rspec_1_is_loaded
        if defined?(Spec) && defined?(Spec::VERSION::MAJOR) && Spec::VERSION::MAJOR == 1
          raise <<-MESSAGE

#{'*'*80}
  You are running rspec-2, but it seems as though rspec-1 has been loaded as
  well.  This is likely due to a statement like this somewhere in the specs:

      require 'spec'

  Please locate that statement, remove it, and try again.
#{'*'*80}
MESSAGE
        end
      end

      def output_to_tty?(output=output_stream)
        tty? || (output.respond_to?(:tty?) && output.tty?)
      end

      def built_in_formatter(key)
        case key.to_s
        when 'd', 'doc', 'documentation', 's', 'n', 'spec', 'nested'
          require 'rspec/core/formatters/documentation_formatter'
          RSpec::Core::Formatters::DocumentationFormatter
        when 'h', 'html'
          require 'rspec/core/formatters/html_formatter'
          RSpec::Core::Formatters::HtmlFormatter
        when 't', 'textmate'
          require 'rspec/core/formatters/text_mate_formatter'
          RSpec::Core::Formatters::TextMateFormatter
        when 'p', 'progress'
          require 'rspec/core/formatters/progress_formatter'
          RSpec::Core::Formatters::ProgressFormatter
        when 'j', 'json'
          require 'rspec/core/formatters/json_formatter'
          RSpec::Core::Formatters::JsonFormatter
        end
      end

      def custom_formatter(formatter_ref)
        if Class === formatter_ref
          formatter_ref
        elsif string_const?(formatter_ref)
          begin
            eval(formatter_ref)
          rescue NameError
            require path_for(formatter_ref)
            eval(formatter_ref)
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

      def order_and_seed_from_seed(value)
        order_groups_and_examples(&RANDOM_ORDERING)
        @order, @seed = 'rand', value.to_i
        [@order, @seed]
      end

      def set_order_and_seed(hash)
        hash[:order], seed = order_and_seed_from_order(hash[:order])
        hash[:seed] = seed if seed
      end

      def order_and_seed_from_order(type)
        order, seed = type.to_s.split(':')
        @order = order
        @seed  = seed = seed.to_i if seed

        if randomize?
          order_groups_and_examples(&RANDOM_ORDERING)
        elsif order == 'default'
          @order, @seed = nil, nil
          order_groups_and_examples(&DEFAULT_ORDERING)
        end

        return order, seed
      end

      def built_in_orderer?(block)
        [DEFAULT_ORDERING, RANDOM_ORDERING].include?(block)
      end

    end
  end
end
