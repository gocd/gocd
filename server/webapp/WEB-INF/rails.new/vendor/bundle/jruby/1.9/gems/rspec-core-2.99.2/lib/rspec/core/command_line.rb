module RSpec
  module Core
    class Runner
      def initialize(options, configuration=RSpec::configuration, world=RSpec::world)
        @options       = options
        @configuration = configuration
        @world         = world
      end

      # Configures and runs a suite
      #
      # @param [IO] err
      # @param [IO] out
      def run(err, out)
        @configuration.error_stream = err
        @configuration.output_stream = out if @configuration.output_stream == $stdout
        @options.configure(@configuration)
        @configuration.load_spec_files
        @world.announce_filters

        @configuration.reporter.report(@world.example_count, @configuration.send(:_randomize?) ? @configuration.seed : nil) do |reporter|
          begin
            @configuration.run_hook(:before, :suite)
            @world.example_groups.ordered.map {|g| g.run(reporter)}.all? ? 0 : @configuration.failure_exit_code
          ensure
            @configuration.run_hook(:after, :suite)
          end
        end
      end
    end

    class CommandLine < Runner
      def initialize(options, configuration=RSpec.configuration, world=RSpec.world)
        if Array === options
          RSpec.deprecate("Instantiating a `RSpec::Core::CommandLine` with an array of options",
                          :replacement => "Instantiate a `RSpec::Core::Runner` with a `RSpec::Core::ConfigurationOptions` instance")
          options = ConfigurationOptions.new(options)
          options.parse_options
        else
          RSpec.deprecate("`RSpec::Core::CommandLine`", :replacement => "`RSpec::Core::Runner`")
        end

        super
      end
    end
  end
end
