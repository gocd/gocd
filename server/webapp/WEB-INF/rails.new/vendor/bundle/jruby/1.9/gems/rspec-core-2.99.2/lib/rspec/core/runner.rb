module RSpec
  module Core
    class Runner

      # Register an at_exit hook that runs the suite.
      def self.autorun
        return if autorun_disabled? || installed_at_exit? || running_in_drb?
        at_exit do
          # Don't bother running any specs and just let the program terminate
          # if we got here due to an unrescued exception (anything other than
          # SystemExit, which is raised when somebody calls Kernel#exit).
          next unless $!.nil? || $!.kind_of?(SystemExit)

          # We got here because either the end of the program was reached or
          # somebody called Kernel#exit.  Run the specs and then override any
          # existing exit status with RSpec's exit status if any specs failed.
          status = run(ARGV, $stderr, $stdout).to_i
          exit status if status != 0
        end
        @installed_at_exit = true
      end
      AT_EXIT_HOOK_BACKTRACE_LINE = "#{__FILE__}:#{__LINE__ - 2}:in `autorun'"

      def self.disable_autorun!
        @autorun_disabled = true
      end

      def self.autorun_disabled?
        @autorun_disabled ||= false
      end

      def self.installed_at_exit?
        @installed_at_exit ||= false
      end

      def self.running_in_drb?
        defined?(DRb) &&
        (DRb.current_server rescue false) &&
         DRb.current_server.uri =~ /druby\:\/\/127.0.0.1\:/
      end

      def self.trap_interrupt
        trap('INT') do
          exit!(1) if RSpec.wants_to_quit
          RSpec.wants_to_quit = true
          STDERR.puts "\nExiting... Interrupt again to exit immediately."
        end
      end


      # @private
      # Warns that RSpec 3.0.0 will no longer call reset for users
      def self.warn_about_calling_reset
        RSpec.configuration.deprecation_stream.puts(<<-EOD)
Calling `RSpec::Core::Runner.run` will no longer implicitly invoke
`RSpec.reset` as of RSpec 3.0.0. If you need RSpec to be reset between your
calls to `RSpec::Core::Runner.run` please invoke `RSpec.reset` manually in the
appropriate place.
        EOD
      end

      # Run a suite of RSpec examples.
      #
      # This is used internally by RSpec to run a suite, but is available
      # for use by any other automation tool.
      #
      # If you want to run this multiple times in the same process, and you
      # want files like spec_helper.rb to be reloaded, be sure to load `load`
      # instead of `require`.
      #
      # #### Parameters
      # * +args+ - an array of command-line-supported arguments
      # * +err+ - error stream (Default: $stderr)
      # * +out+ - output stream (Default: $stdout)
      #
      # #### Returns
      # * +Fixnum+ - exit status code (0/1)
      def self.run(args, err=$stderr, out=$stdout)
        warn_about_calling_reset if RSpec.resets_required > 0
        RSpec.resets_required += 1
        trap_interrupt
        options = ConfigurationOptions.new(args)
        options.parse_options

        major, minor, point = RUBY_VERSION.split('.').map { |v| v.to_i }

        if major == 1 && ( (minor == 9 && point < 2) || (minor == 8 && point < 7) )
          RSpec.deprecate "RSpec support for Ruby #{RUBY_VERSION}",
                          :replacement => "1.8.7 or >= 1.9.2",
                          :call_site   => nil
        end


        if options.options[:drb]
          require 'rspec/core/drb_command_line'
          begin
            DRbCommandLine.new(options).run(err, out)
          rescue DRb::DRbConnError
            err.puts "No DRb server is running. Running in local process instead ..."
            new(options).run(err, out)
          end
        else
          new(options).run(err, out)
        end
      ensure
        RSpec.internal_reset
      end
    end

  end
end
