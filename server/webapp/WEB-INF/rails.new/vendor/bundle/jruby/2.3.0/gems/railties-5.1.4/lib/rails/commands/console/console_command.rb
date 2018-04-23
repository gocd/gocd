require "irb"
require "irb/completion"

require "rails/command/environment_argument"

module Rails
  class Console
    module BacktraceCleaner
      def filter_backtrace(bt)
        if result = super
          Rails.backtrace_cleaner.filter([result]).first
        end
      end
    end

    def self.start(*args)
      new(*args).start
    end

    attr_reader :options, :app, :console

    def initialize(app, options = {})
      @app     = app
      @options = options

      app.sandbox = sandbox?
      app.load_console

      @console = app.config.console || IRB

      if @console == IRB
        IRB::WorkSpace.prepend(BacktraceCleaner)
      end
    end

    def sandbox?
      options[:sandbox]
    end

    def environment
      options[:environment]
    end
    alias_method :environment?, :environment

    def set_environment!
      Rails.env = environment
    end

    def start
      set_environment! if environment?

      if sandbox?
        puts "Loading #{Rails.env} environment in sandbox (Rails #{Rails.version})"
        puts "Any modifications you make will be rolled back on exit"
      else
        puts "Loading #{Rails.env} environment (Rails #{Rails.version})"
      end

      if defined?(console::ExtendCommandBundle)
        console::ExtendCommandBundle.include(Rails::ConsoleMethods)
      end
      console.start
    end
  end

  module Command
    class ConsoleCommand < Base # :nodoc:
      include EnvironmentArgument

      class_option :sandbox, aliases: "-s", type: :boolean, default: false,
        desc: "Rollback database modifications on exit."

      class_option :environment, aliases: "-e", type: :string,
        desc: "Specifies the environment to run this console under (test/development/production)."

      def initialize(args = [], local_options = {}, config = {})
        console_options = []

        # For the same behavior as OptionParser, leave only options after "--" in ARGV.
        termination = local_options.find_index("--")
        if termination
          console_options = local_options[termination + 1..-1]
          local_options = local_options[0...termination]
        end

        ARGV.replace(console_options)
        super(args, local_options, config)
      end

      def perform
        extract_environment_option_from_argument

        # RAILS_ENV needs to be set before config/application is required.
        ENV["RAILS_ENV"] = options[:environment]

        require_application_and_environment!
        Rails::Console.start(Rails.application, options)
      end
    end
  end
end
