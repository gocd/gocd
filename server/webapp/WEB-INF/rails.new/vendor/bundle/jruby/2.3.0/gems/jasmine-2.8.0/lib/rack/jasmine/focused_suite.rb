module Rack
  module Jasmine

    class FocusedSuite
      def initialize(runner_config)
        @runner_config = runner_config
      end

      def call(env)
        run_adapter = Rack::Jasmine::RunAdapter.new(@runner_config)
        run_adapter.run(env["PATH_INFO"])
      end
    end

  end
end

