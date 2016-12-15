module RSpec
  module Support
    module InSubProcess
      if Process.respond_to?(:fork) && !(Ruby.jruby? && RUBY_VERSION == '1.8.7')

        # Useful as a way to isolate a global change to a subprocess.

        # rubocop:disable MethodLength
        def in_sub_process(prevent_warnings=true)
          readme, writeme = IO.pipe

          pid = Process.fork do
            exception = nil
            warning_preventer = $stderr = RSpec::Support::StdErrSplitter.new($stderr)

            begin
              yield
              warning_preventer.verify_no_warnings! if prevent_warnings
            rescue Support::AllExceptionsExceptOnesWeMustNotRescue => e
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
        # rubocop:enable MethodLength
        alias :in_sub_process_if_possible :in_sub_process
      else
        def in_sub_process(*)
          skip "This spec requires forking to work properly, " \
               "and your platform does not support forking"
        end

        def in_sub_process_if_possible(*)
          yield
        end
      end
    end
  end
end
