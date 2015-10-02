module Listen
  module Adapters

    # Adapter implementation for Mac OS X `FSEvents`.
    #
    class Darwin < Adapter
      LAST_SEPARATOR_REGEX = /\/$/


      def self.target_os_regex; /darwin(1.+)?$/i; end
      def self.adapter_gem; 'rb-fsevent'; end

      private

      # Initializes a FSEvent worker and adds a watcher for
      # each directory passed to the adapter.
      #
      # @return [FSEvent] initialized worker
      #
      # @see Listen::Adapter#initialize_worker
      #
      def initialize_worker
        FSEvent.new.tap do |worker|
          worker.watch(directories.dup, :latency => latency) do |changes|
            next if paused

            mutex.synchronize do
              changes.each { |path| @changed_directories << path.sub(LAST_SEPARATOR_REGEX, '') }
            end
          end
        end
      end

      # Starts the worker in a new thread and sleep 0.1 second.
      #
      # @see Listen::Adapter#start_worker
      #
      def start_worker
        @worker_thread = Thread.new { worker.run }
        # The FSEvent worker needs some time to start up. Turnstiles can't
        # be used to wait for it as it runs in a loop.
        # TODO: Find a better way to block until the worker starts.
        sleep 0.1
      end
    end

  end
end
