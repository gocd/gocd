require 'set'

module Listen
  module Adapters

    # Adapter implementation for Windows `wdm`.
    #
    class Windows < Adapter
      extend DependencyManager

      # Declare the adapter's dependencies
      dependency 'wdm', '~> 0.1'

      # Initializes the Adapter. See {Listen::Adapter#initialize} for more info.
      #
      def initialize(directories, options = {}, &callback)
        super
        @worker = init_worker
      end

      # Starts the adapter.
      #
      # @param [Boolean] blocking whether or not to block the current thread after starting
      #
      def start(blocking = true)
        @mutex.synchronize do
          return if @stop == false
          super
        end

        @worker_thread = Thread.new { @worker.run! }

        # Wait for the worker to start. This is needed to avoid a deadlock
        # when stopping immediately after starting.
        sleep 0.1

        @poll_thread = Thread.new { poll_changed_dirs } if @report_changes

        @worker_thread.join if blocking
      end

      # Stops the adapter.
      #
      def stop
        @mutex.synchronize do
          return if @stop == true
          super
        end

        @worker.stop
        @worker_thread.join if @worker_thread
        @poll_thread.join if @poll_thread
      end

      # Checks if the adapter is usable on the current OS.
      #
      # @return [Boolean] whether usable or not
      #
      def self.usable?
        return false unless RbConfig::CONFIG['target_os'] =~ /mswin|mingw/i
        super
      end

    private

      # Initializes a WDM monitor and adds a watcher for
      # each directory passed to the adapter.
      #
      # @return [WDM::Monitor] initialized worker
      #
      def init_worker
        callback = Proc.new do |change|
          next if @paused
          @mutex.synchronize do
            @changed_dirs << File.dirname(change.path)
          end
        end

        WDM::Monitor.new.tap do |worker|
          @directories.each { |d| worker.watch_recursively(d, &callback) }
        end
      end

    end

  end
end
