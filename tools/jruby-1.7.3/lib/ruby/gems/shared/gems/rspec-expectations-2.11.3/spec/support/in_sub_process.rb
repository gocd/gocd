module InSubProcess
  # Useful as a way to isolate a global change to a subprocess.
  def in_sub_process
    readme, writeme = IO.pipe

    pid = Process.fork do
      value = nil
      begin
        yield
      rescue => e
        value = e
      end

      writeme.write Marshal.dump(value)

      readme.close
      writeme.close
      exit! # prevent at_exit hooks from running (e.g. minitest)
    end

    writeme.close
    Process.waitpid(pid)

    if exception = Marshal.load(readme.read)
      raise exception
    end

    readme.close
  end
end

