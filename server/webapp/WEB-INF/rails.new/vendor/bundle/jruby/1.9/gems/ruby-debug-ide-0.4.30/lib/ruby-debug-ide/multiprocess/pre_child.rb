module Debugger
  module MultiProcess
    class << self
      def pre_child

        require 'socket'
        require 'ostruct'

        host = ENV['DEBUGGER_HOST']
        port = find_free_port(host)

        options = OpenStruct.new(
            'frame_bind'  => false,
            'host'        => host,
            'load_mode'   => false,
            'port'        => port,
            'stop'        => false,
            'tracing'     => false,
            'int_handler' => true,
            'cli_debug'   => (ENV['DEBUGGER_CLI_DEBUG'] == 'true'),
            'notify_dispatcher' => true
        )

        start_debugger(options)
      end

      def start_debugger(options)
        if Debugger.started?
          # we're in forked child, only need to restart control thread
          Debugger.breakpoints.clear
          Debugger.control_thread = nil
          Debugger.start_control(options.host, options.port, options.notify_dispatcher)
        end

        if options.int_handler
          # install interruption handler
          trap('INT') { Debugger.interrupt_last }
        end

        # set options
        Debugger.keep_frame_binding = options.frame_bind
        Debugger.tracing = options.tracing
        Debugger.cli_debug = options.cli_debug

        Debugger.prepare_debugger(options)
      end


      def find_free_port(host)
        server = TCPServer.open(host, 0)
        port   = server.addr[1]
        server.close
        port
      end
    end
  end
end