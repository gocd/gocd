module Jasmine
  class CiRunner
    def initialize(config, options={})
      @config = config
      @thread = options.fetch(:thread, Thread)
      @application_factory = options.fetch(:application_factory, Jasmine::Application)
      @server_factory = options.fetch(:server_factory, Jasmine::Server)
      @outputter = options.fetch(:outputter, Kernel)
    end

    def run
      formatters = config.formatters.map { |formatter_class| formatter_class.new }

      exit_code_formatter = Jasmine::Formatters::ExitCode.new
      formatters << exit_code_formatter

      url = "#{config.host}:#{config.port(:ci)}/?throwFailures=#{config.stop_spec_on_expectation_failure}"
      runner = config.runner.call(Jasmine::Formatters::Multi.new(formatters), url)

      if runner.respond_to?(:boot_js)
        config.runner_boot_dir = File.dirname(runner.boot_js)
        config.runner_boot_files = lambda { [runner.boot_js] }
      end

      server = @server_factory.new(config.port(:ci), app, config.rack_options)

      t = @thread.new do
        server.start
      end
      t.abort_on_exception = true

      Jasmine::wait_for_listener(config.port(:ci), 'jasmine server')
      @outputter.puts 'jasmine server started'

      runner.run

      exit_code_formatter.succeeded?
    end

    private

    attr_reader :config

    def app
      @application_factory.app(@config)
    end
  end
end
