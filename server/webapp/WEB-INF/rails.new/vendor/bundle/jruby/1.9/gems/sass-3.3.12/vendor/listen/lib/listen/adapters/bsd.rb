module Listen
  module Adapters

    # Listener implementation for BSD's `kqueue`.
    #
    class BSD < Adapter
      # Watched kqueue events
      #
      # @see http://www.freebsd.org/cgi/man.cgi?query=kqueue
      # @see https://github.com/nex3/rb-kqueue/blob/master/lib/rb-kqueue/queue.rb
      #
      EVENTS = [:delete, :write, :extend, :attrib, :link, :rename, :revoke]

      def self.target_os_regex; /freebsd/i; end
      def self.adapter_gem; 'rb-kqueue'; end

      private

      # Initializes a kqueue Queue and adds a watcher for each files in
      # the directories passed to the adapter.
      #
      # @return [INotify::Notifier] initialized kqueue
      #
      # @see Listen::Adapter#initialize_worker
      #
      def initialize_worker
        require 'find'

        callback = lambda do |event|
          path = event.watcher.path
          mutex.synchronize do
            # kqueue watches everything, but Listen only needs the
            # directory where stuffs happens.
            @changed_directories << (File.directory?(path) ? path : File.dirname(path))

            # If it is a directory, and it has a write flag, it means a
            # file has been added so find out which and deal with it.
            # No need to check for removed files, kqueue will forget them
            # when the vfs does.
            if File.directory?(path) && event.flags.include?(:write)
              queue = event.watcher.queue
              Find.find(path) do |file|
                unless queue.watchers.detect { |k,v| v.path == file.to_s }
                  queue.watch_file(file, *EVENTS, &callback)
                end
              end
            end
          end
        end

        KQueue::Queue.new.tap do |queue|
          directories.each do |directory|
            Find.find(directory) do |path|
              queue.watch_file(path, *EVENTS, &callback)
            end
          end
        end
      end

      # Starts the worker in a new thread.
      #
      # @see Listen::Adapter#start_worker
      #
      def start_worker
        @worker_thread = Thread.new do
          until stopped
            worker.poll
            sleep(latency)
          end
        end
      end
    end

  end
end
