module InSubProcess
  if RUBY_PLATFORM == 'java'
    def in_sub_process
      pending "This spec requires forking to work properly, " +
              "and JRuby does not support forking"
    end
  else
    # Useful as a way to isolate a global change to a subprocess.
    def in_sub_process
      readme, writeme = IO.pipe

      pid = Process.fork do
        exception = nil
        begin
          yield
        rescue Exception => e
          exception = e
        end

        writeme.write Marshal.dump(exception)

        readme.close
        writeme.close
        exit! # prevent at_exit hooks from running (e.g. minitest)
      end

      writeme.close
      Process.waitpid(pid)

      exception = Marshal.load(readme.read)
      readme.close

      raise exception if exception
    end
  end
end

