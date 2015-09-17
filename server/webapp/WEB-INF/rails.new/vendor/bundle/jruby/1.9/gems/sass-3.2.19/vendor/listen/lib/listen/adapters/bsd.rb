module Listen
  module Adapters

    # Listener implementation for BSD's `kqueue`.
    #
    class BSD < Adapter
      extend DependencyManager

      # Declare the adapter's dependencies
      dependency 'rb-kqueue', '~> 0.2'

      # Watched kqueue events
      #
      # @see http://www.freebsd.org/cgi/man.cgi?query=kqueue
      # @see https://github.com/nex3/rb-kqueue/blob/master/lib/rb-kqueue/queue.rb
      #
      EVENTS = [ :delete, :write, :extend, :attrib, :link, :rename, :revoke ]

      # Initializes the Adapter. See {Listen::Adapter#initialize} for
      # more info.
      #
      def initialize(directories, options = {}, &callback)
        super
        @kqueue = init_kqueue
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

        @kqueue_thread = Thread.new do
          until @stop
            @kqueue.poll
            sleep(@latency)
          end
        end
        @poll_thread   = Thread.new { poll_changed_dirs } if @report_changes

        @kqueue_thread.join if blocking
      end

      # Stops the adapter.
      #
      def stop
        @mutex.synchronize do
          return if @stop == true
          super
        end

        @kqueue.stop
        Thread.kill(@kqueue_thread) if @kqueue_thread
        @poll_thread.join if @poll_thread
      end

      # Checks if the adapter is usable on the current OS.
      #
      # @return [Boolean] whether usable or not
      #
      def self.usable?
        return false unless RbConfig::CONFIG['target_os'] =~ /freebsd/i
        super
      end

      private

      # Initializes a kqueue Queue and adds a watcher for each files in
      # the directories passed to the adapter.
      #
      # @return [INotify::Notifier] initialized kqueue
      #
      def init_kqueue
        require 'find'

        callback = lambda do |event|
          path = event.watcher.path
          @mutex.synchronize do
            # kqueue watches everything, but Listen only needs the
            # directory where stuffs happens.
            @changed_dirs << (File.directory?(path) ? path : File.dirname(path))

            # If it is a directory, and it has a write flag, it means a
            # file has been added so find out which and deal with it.
            # No need to check for removed file, kqueue will forget them
            # when the vfs does..
            if File.directory?(path) && !(event.flags & [:write]).empty?
              queue = event.watcher.queue
              Find.find(path) do |file|
                unless queue.watchers.detect {|k,v| v.path == file.to_s}
                  queue.watch_file(file, *EVENTS, &callback)
                end
              end
            end
          end
        end

        KQueue::Queue.new.tap do |queue|
          @directories.each do |directory|
            Find.find(directory) do |path|
              queue.watch_file(path, *EVENTS, &callback)
            end
          end
        end
      end
    end
  end
end
