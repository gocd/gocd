require 'rspec/core/backward_compatibility'
require 'rake'
require 'rake/tasklib'
require 'shellwords'

module RSpec
  module Core
    class RakeTask < ::Rake::TaskLib
      include ::Rake::DSL if defined?(::Rake::DSL)

      # Name of task.
      #
      # default:
      #   :spec
      attr_accessor :name

      # Glob pattern to match files.
      #
      # default:
      #   'spec/**/*_spec.rb'
      attr_accessor :pattern

      # @deprecated
      # Has no effect. The rake task now checks ENV['BUNDLE_GEMFILE'] instead.
      def skip_bundler=(*)
        deprecate("RSpec::Core::RakeTask#skip_bundler=")
      end

      # @deprecated
      # Has no effect. The rake task now checks ENV['BUNDLE_GEMFILE'] instead.
      def gemfile=(*)
        deprecate("RSpec::Core::RakeTask#gemfile=", :replacement => 'ENV["BUNDLE_GEMFILE"]')
      end

      # @deprecated
      # Use ruby_opts="-w" instead.
      #
      # When true, requests that the specs be run with the warning flag set.
      # e.g. "ruby -w"
      #
      # default:
      #   false
      def warning=(true_or_false)
        deprecate("RSpec::Core::RakeTask#warning=", :replacement => 'ruby_opts="-w"')
        @warning = true_or_false
      end

      # Whether or not to fail Rake when an error occurs (typically when examples fail).
      #
      # default:
      #   true
      attr_accessor :fail_on_error

      # A message to print to stderr when there are failures.
      attr_accessor :failure_message

      # Use verbose output. If this is set to true, the task will print the
      # executed spec command to stdout.
      #
      # default:
      #   true
      attr_accessor :verbose

      # Use rcov for code coverage?
      #
      # Due to the many ways `rcov` can run, if this option is enabled, it is
      # required that `require 'rspec/autorun'` appears in `spec_helper`.rb
      #
      # default:
      #   false
      attr_accessor :rcov

      # Path to rcov.
      #
      # default:
      #   'rcov'
      attr_accessor :rcov_path

      # Command line options to pass to rcov.
      #
      # default:
      #   nil
      attr_accessor :rcov_opts

      # Command line options to pass to ruby.
      #
      # default:
      #   nil
      attr_accessor :ruby_opts

      # Path to rspec
      #
      # default:
      #   'rspec'
      attr_accessor :rspec_path

      # Command line options to pass to rspec.
      #
      # default:
      #   nil
      attr_accessor :rspec_opts

      # @deprecated
      # Use rspec_opts instead.
      #
      # Command line options to pass to rspec.
      #
      # default:
      #   nil
      def spec_opts=(opts)
        deprecate('RSpec::Core::RakeTask#spec_opts=', :replacement => 'rspec_opts=')
        @rspec_opts = opts
      end

      def initialize(*args, &task_block)
        setup_ivars(args)

        desc "Run RSpec code examples" unless ::Rake.application.last_comment

        task name, *args do |_, task_args|
          RakeFileUtils.send(:verbose, verbose) do
            task_block.call(*[self, task_args].slice(0, task_block.arity)) if task_block
            run_task verbose
          end
        end
      end

      def setup_ivars(args)
        @name = args.shift || :spec
        @rcov_opts, @ruby_opts, @rspec_opts = nil, nil, nil
        @warning, @rcov = false, false
        @verbose, @fail_on_error = true, true

        @rcov_path  = 'rcov'
        @rspec_path = 'rspec'
        @pattern    = './spec{,/*/**}/*_spec.rb'
      end

      def run_task(verbose)
        command = spec_command

        begin
          puts command if verbose
          success = system(command)
        rescue
          puts failure_message if failure_message
        end
        abort("#{command} failed") if fail_on_error unless success
      end

    private

      if "".respond_to?(:shellescape)
        def shellescape(string)
          string.shellescape
        end
      else # 1.8.6's shellwords doesn't provide shellescape :(.
        def shellescape(string)
          string.gsub(/"/, '\"').gsub(/'/, "\\\\'")
        end
      end

      def files_to_run
        if ENV['SPEC']
          FileList[ ENV['SPEC'] ].sort
        else
          FileList[ pattern ].sort.map { |f| shellescape(f) }
        end
      end

      def spec_command
        cmd_parts = []
        cmd_parts << RUBY
        cmd_parts << ruby_opts
        cmd_parts << "-w" if @warning
        cmd_parts << "-S" << runner
        cmd_parts << "-Ispec:lib" << rcov_opts if rcov
        cmd_parts << files_to_run
        cmd_parts << "--" if rcov && rspec_opts
        cmd_parts << rspec_opts
        cmd_parts.flatten.reject(&blank).join(" ")
      end

      def runner
        rcov ? rcov_path : rspec_path
      end

      def blank
        lambda {|s| s.nil? || s == ""}
      end

      def deprecate deprecated, opts = {}
        # unless RSpec is loaded, deprecate won't work (simply requiring the
        # deprecate file isn't enough) so this is a check for "is rspec already
        # loaded?" "ok use the main deprecate hook" otherwise "simple fallback"
        # Note that we don't need rspec to be loaded for the rake task to work
        if RSpec.respond_to?(:deprecate)
          RSpec.deprecate deprecated, opts
        else
          warn "DEPRECATION: #{deprecated} is deprecated."
        end
      end

    end
  end
end
