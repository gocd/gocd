module Listen
  module Adapters

    DEFAULT_POLLING_LATENCY = 1.0

    # Polling Adapter that works cross-platform and
    # has no dependencies. This is the adapter that
    # uses the most CPU processing power and has higher
    # file IO than the other implementations.
    #
    class Polling < Adapter
      private

      # The default delay between checking for changes.
      #
      # @see Listen::Adapter#default_latency
      #
      def default_latency
        1.0
      end

      # The thread on which the main thread should wait
      # when the adapter has been started in blocking mode.
      #
      # @see Listen::Adapter#blocking_thread
      #
      def blocking_thread
        poller_thread
      end

      # @see Listen::Adapter#start_worker
      #
      # @see Listen::Adapter#start_worker
      #
      def start_worker
        # The polling adapter has no worker! Sad panda! :'(
      end

      # Poll listener directory for file system changes.
      #
      # @see Listen::Adapter#poll_changed_directories
      #
      def poll_changed_directories
        until stopped
          next if paused

          start = Time.now.to_f
          callback.call(directories.dup, :recursive => true)
          turnstile.signal
          nap_time = latency - (Time.now.to_f - start)
          sleep(nap_time) if nap_time > 0
        end
      rescue Interrupt
      end
    end

  end
end
