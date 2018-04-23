RSpec::Support.require_rspec_core "bisect/server"
RSpec::Support.require_rspec_core "bisect/runner"
RSpec::Support.require_rspec_core "bisect/example_minimizer"
RSpec::Support.require_rspec_core "formatters/bisect_progress_formatter"

module RSpec
  module Core
    module Bisect
      # @private
      # The main entry point into the bisect logic. Coordinates among:
      #   - Bisect::Server: Receives suite results.
      #   - Bisect::Runner: Runs a set of examples and directs the results
      #     to the server.
      #   - Bisect::ExampleMinimizer: Contains the core bisect logic.
      #   - Formatters::BisectProgressFormatter: provides progress updates
      #     to the user.
      class Coordinator
        def self.bisect_with(original_cli_args, configuration, formatter)
          new(original_cli_args, configuration, formatter).bisect
        end

        def initialize(original_cli_args, configuration, formatter)
          @original_cli_args = original_cli_args
          @configuration     = configuration
          @formatter         = formatter
        end

        def bisect
          @configuration.add_formatter @formatter

          reporter.close_after do
            repro = Server.run do |server|
              runner    = Runner.new(server, @original_cli_args)
              minimizer = ExampleMinimizer.new(runner, reporter)

              gracefully_abort_on_sigint(minimizer)
              minimizer.find_minimal_repro
              minimizer.repro_command_for_currently_needed_ids
            end

            reporter.publish(:bisect_repro_command, :repro => repro)
          end

          true
        rescue BisectFailedError => e
          reporter.publish(:bisect_failed, :failure_explanation => e.message)
          false
        end

      private

        def reporter
          @configuration.reporter
        end

        def gracefully_abort_on_sigint(minimizer)
          trap('INT') do
            repro = minimizer.repro_command_for_currently_needed_ids
            reporter.publish(:bisect_aborted, :repro => repro)
            exit(1)
          end
        end
      end
    end
  end
end
