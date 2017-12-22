# frozen_string_literal: true
module Rake

  # Same as a regular task, but the immediate prerequisites are done in
  # parallel using Ruby threads.
  #
  class MultiTask < Task

    # Same as invoke, but explicitly pass a call chain to detect
    # circular dependencies. This is largely copied from Rake::Task
    # but has been updated such that if multiple tasks depend on this
    # one in parallel, they will all fail if the first execution of
    # this task fails.
    def invoke_with_call_chain(task_args, invocation_chain)
      new_chain = Rake::InvocationChain.append(self, invocation_chain)
      @lock.synchronize do
        begin
          if @already_invoked
            if @invocation_exception
              if application.options.trace
                application.trace "** Previous invocation of #{name} failed #{format_trace_flags}"
              end
              raise @invocation_exception
            else
              return
            end
          end

          if application.options.trace
            application.trace "** Invoke #{name} #{format_trace_flags}"
          end
          @already_invoked = true

          invoke_prerequisites(task_args, new_chain)
          execute(task_args) if needed?
        rescue Exception => ex
          add_chain_to(ex, new_chain)
          @invocation_exception = ex
          raise
        end
      end
    end

    private
    def invoke_prerequisites(task_args, invocation_chain) # :nodoc:
      invoke_prerequisites_concurrently(task_args, invocation_chain)
    end
  end

end
