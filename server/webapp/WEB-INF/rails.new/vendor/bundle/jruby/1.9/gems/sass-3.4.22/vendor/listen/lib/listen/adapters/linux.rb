module Listen
  module Adapters

    # Listener implementation for Linux `inotify`.
    #
    class Linux < Adapter
      # Watched inotify events
      #
      # @see http://www.tin.org/bin/man.cgi?section=7&topic=inotify
      # @see https://github.com/nex3/rb-inotify/blob/master/lib/rb-inotify/notifier.rb#L99-L177
      #
      EVENTS = [:recursive, :attrib, :create, :delete, :move, :close_write]

      # The message to show when the limit of inotify watchers is not enough
      #
      INOTIFY_LIMIT_MESSAGE = <<-EOS.gsub(/^\s*/, '')
        Listen error: unable to monitor directories for changes.

        Please head to https://github.com/guard/listen/wiki/Increasing-the-amount-of-inotify-watchers
        for information on how to solve this issue.
      EOS

      def self.target_os_regex; /linux/i; end
      def self.adapter_gem; 'rb-inotify'; end

      # Initializes the Adapter.
      #
      # @see Listen::Adapter#initialize
      #
      def initialize(directories, options = {}, &callback)
        super
      rescue Errno::ENOSPC
        abort(INOTIFY_LIMIT_MESSAGE)
      end

      private

      # Initializes a INotify worker and adds a watcher for
      # each directory passed to the adapter.
      #
      # @return [INotify::Notifier] initialized worker
      #
      # @see Listen::Adapter#initialize_worker
      #
      def initialize_worker
        callback = lambda do |event|
          if paused || (
            # Event on root directory
            event.name == ""
          ) || (
            # INotify reports changes to files inside directories as events
            # on the directories themselves too.
            #
            # @see http://linux.die.net/man/7/inotify
            event.flags.include?(:isdir) and (event.flags & [:close, :modify]).any?
          )
            # Skip all of these!
            next
          end

          mutex.synchronize do
            @changed_directories << File.dirname(event.absolute_name)
          end
        end

        INotify::Notifier.new.tap do |worker|
          directories.each { |dir| worker.watch(dir, *EVENTS, &callback) }
        end
      end

      # Starts the worker in a new thread.
      #
      # @see Listen::Adapter#start_worker
      #
      def start_worker
        @worker_thread = Thread.new { worker.run }
      end
    end

  end
end
