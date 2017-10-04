require 'drb/drb'
require 'drb/acl'

module RSpec
  module Core
    # @private
    module Bisect
      # @private
      BisectFailedError = Class.new(StandardError)

      # @private
      # A DRb server that receives run results from a separate RSpec process
      # started by the bisect process.
      class Server
        def self.run
          server = new
          server.start
          yield server
        ensure
          server.stop
        end

        def capture_run_results(files_or_directories_to_run=[], expected_failures=[])
          self.expected_failures  = expected_failures
          self.files_or_directories_to_run = files_or_directories_to_run
          self.latest_run_results = nil
          run_output = yield

          if latest_run_results.nil? || latest_run_results.all_example_ids.empty?
            raise_bisect_failed(run_output)
          end

          latest_run_results
        end

        def start
          # Only allow remote DRb requests from this machine.
          DRb.install_acl ACL.new(%w[ deny all allow localhost allow 127.0.0.1 ])

          # We pass `nil` as the first arg to allow it to pick a DRb port.
          @drb = DRb.start_service(nil, self)
        end

        def stop
          @drb.stop_service
        end

        def drb_port
          @drb_port ||= Integer(@drb.uri[/\d+$/])
        end

        # Fetched via DRb by the BisectFormatter to determine when to abort.
        attr_accessor :expected_failures

        # Set via DRb by the BisectFormatter with the results of the run.
        attr_accessor :latest_run_results

        # Fetched via DRb to tell clients which files to run
        attr_accessor :files_or_directories_to_run

      private

        def raise_bisect_failed(run_output)
          raise BisectFailedError, "Failed to get results from the spec " \
                "run. Spec run output:\n\n#{run_output}"
        end
      end
    end
  end
end
