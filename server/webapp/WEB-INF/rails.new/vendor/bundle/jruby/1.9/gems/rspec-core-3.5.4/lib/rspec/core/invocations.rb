module RSpec
  module Core
    # @private
    module Invocations
      # @private
      class InitializeProject
        def call(*_args)
          RSpec::Support.require_rspec_core "project_initializer"
          ProjectInitializer.new.run
          0
        end
      end

      # @private
      class DRbWithFallback
        def call(options, err, out)
          require 'rspec/core/drb'
          begin
            return DRbRunner.new(options).run(err, out)
          rescue DRb::DRbConnError
            err.puts "No DRb server is running. Running in local process instead ..."
          end
          RSpec::Core::Runner.new(options).run(err, out)
        end
      end

      # @private
      class Bisect
        def call(options, _err, _out)
          RSpec::Support.require_rspec_core "bisect/coordinator"

          success = RSpec::Core::Bisect::Coordinator.bisect_with(
            options.args,
            RSpec.configuration,
            bisect_formatter_for(options.options[:bisect])
          )

          success ? 0 : 1
        end

        private

        def bisect_formatter_for(argument)
          return Formatters::BisectDebugFormatter if argument == "verbose"
          Formatters::BisectProgressFormatter
        end
      end

      # @private
      class PrintVersion
        def call(_options, _err, out)
          out.puts RSpec::Core::Version::STRING
          0
        end
      end

      # @private
      PrintHelp = Struct.new(:parser, :invalid_options) do
        def call(_options, _err, out)
          # Removing the blank invalid options from the output.
          out.puts parser.to_s.gsub(/^\s+(#{invalid_options.join('|')})\s*$\n/, '')
          0
        end
      end
    end
  end
end
