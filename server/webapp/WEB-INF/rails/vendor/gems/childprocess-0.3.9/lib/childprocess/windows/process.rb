module ChildProcess
  module Windows
    class Process < AbstractProcess

      attr_reader :pid

      def io
        @io ||= Windows::IO.new
      end

      def stop(timeout = 3)
        assert_started

        # just kill right away on windows.
        log "sending KILL"
        @handle.send(WIN_SIGKILL)

        poll_for_exit(timeout)
      ensure
        @handle.close
      end

      def wait
        @handle.wait
        @exit_code = @handle.exit_code
        @handle.close

        @exit_code
      end

      def exited?
        return true if @exit_code
        assert_started

        code   = @handle.exit_code
        exited = code != PROCESS_STILL_ACTIVE

        log(:exited? => exited, :code => code)

        if exited
          @exit_code = code
          @handle.close
        end

        exited
      end

      private

      def launch_process
        builder = ProcessBuilder.new(@args)
        builder.inherit     = false
        builder.detach      = detach?
        builder.duplex      = duplex?
        builder.environment = @environment unless @environment.empty?
        builder.cwd         = @cwd

        if @io
          builder.stdout      = @io.stdout
          builder.stderr      = @io.stderr
        end

        @pid = builder.start
        @handle = Handle.open @pid

        if duplex?
          raise Error, "no stdin stream" unless builder.stdin
          io._stdin = builder.stdin
        end

        self
      end

    end # Process
  end # Windows
end # ChildProcess
