module Listen
  module Adapters

    # The default delay between checking for changes.
    DEFAULT_POLLING_LATENCY = 1.0

    # Polling Adapter that works cross-platform and
    # has no dependencies. This is the adapter that
    # uses the most CPU processing power and has higher
    # file IO that the other implementations.
    #
    class Polling < Adapter
      extend DependencyManager

      # Initialize the Adapter. See {Listen::Adapter#initialize} for more info.
      #
      def initialize(directories, options = {}, &callback)
        @latency ||= DEFAULT_POLLING_LATENCY
        super
      end

      # Start the adapter.
      #
      # @param [Boolean] blocking whether or not to block the current thread after starting
      #
      def start(blocking = true)
        @mutex.synchronize do
          return if @stop == false
          super
        end

        @poll_thread = Thread.new { poll }
        @poll_thread.join if blocking
      end

      # Stop the adapter.
      #
      def stop
        @mutex.synchronize do
          return if @stop == true
          super
        end

        @poll_thread.join
      end

    private

      # Poll listener directory for file system changes.
      #
      def poll
        until @stop
          next if @paused

          start = Time.now.to_f
          @callback.call(@directories.dup, :recursive => true)
          @turnstile.signal
          nap_time = @latency - (Time.now.to_f - start)
          sleep(nap_time) if nap_time > 0
        end
      rescue Interrupt
      end

    end

  end
end
