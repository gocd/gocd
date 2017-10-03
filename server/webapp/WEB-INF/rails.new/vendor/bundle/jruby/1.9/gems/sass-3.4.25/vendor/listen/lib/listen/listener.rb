require 'pathname'

module Listen
  class Listener
    attr_reader :directories, :directories_records, :block, :adapter, :adapter_options, :use_relative_paths

    BLOCKING_PARAMETER_DEPRECATION_MESSAGE = <<-EOS.gsub(/^\s*/, '')
      The blocking parameter of Listen::Listener#start is deprecated.\n
      Please use Listen::Adapter#start for a non-blocking listener and Listen::Listener#start! for a blocking one.
    EOS

    RELATIVE_PATHS_WITH_MULTIPLE_DIRECTORIES_WARNING_MESSAGE = "The relative_paths option doesn't work when listening to multiple diretories."

    # Initializes the directories listener.
    #
    # @param [String] directory the directories to listen to
    # @param [Hash] options the listen options
    # @option options [Regexp] ignore a pattern for ignoring paths
    # @option options [Regexp] filter a pattern for filtering paths
    # @option options [Float] latency the delay between checking for changes in seconds
    # @option options [Boolean] relative_paths whether or not to use relative-paths in the callback
    # @option options [Boolean] force_polling whether to force the polling adapter or not
    # @option options [String, Boolean] polling_fallback_message to change polling fallback message or remove it
    # @option options [Class] force_adapter force the use of this adapter class, skipping usual adapter selection
    #
    # @yield [modified, added, removed] the changed files
    # @yieldparam [Array<String>] modified the list of modified files
    # @yieldparam [Array<String>] added the list of added files
    # @yieldparam [Array<String>] removed the list of removed files
    #
    def initialize(*args, &block)
      options     = args.last.is_a?(Hash) ? args.pop : {}
      directories = args.flatten
      initialize_directories_and_directories_records(directories)
      initialize_relative_paths_usage(options)
      @block = block

      ignore(*options.delete(:ignore))
      filter(*options.delete(:filter))

      @adapter_options = options
    end

    # Starts the listener by initializing the adapter and building
    # the directory record concurrently, then it starts the adapter to watch
    # for changes. The current thread is not blocked after starting.
    #
    # @see Listen::Listener#start!
    #
    def start(deprecated_blocking = nil)
      Kernel.warn "[Listen warning]:\n#{BLOCKING_PARAMETER_DEPRECATION_MESSAGE}" unless deprecated_blocking.nil?
      setup
      adapter.start
    end

    # Starts the listener by initializing the adapter and building
    # the directory record concurrently, then it starts the adapter to watch
    # for changes. The current thread is blocked after starting.
    #
    # @see Listen::Listener#start
    #
    # @since 1.0.0
    #
    def start!
      setup
      adapter.start!
    end

    # Stops the listener.
    #
    def stop
      adapter && adapter.stop
    end

    # Pauses the listener.
    #
    # @return [Listen::Listener] the listener
    #
    def pause
      adapter.pause
      self
    end

    # Unpauses the listener.
    #
    # @return [Listen::Listener] the listener
    #
    def unpause
      build_directories_records
      adapter.unpause
      self
    end

    # Returns whether the listener is paused or not.
    #
    # @return [Boolean] adapter paused status
    #
    def paused?
      !!adapter && adapter.paused?
    end

    # Adds ignoring patterns to the listener.
    #
    # @param (see Listen::DirectoryRecord#ignore)
    #
    # @return [Listen::Listener] the listener
    #
    # @see Listen::DirectoryRecord#ignore
    #
    def ignore(*regexps)
      directories_records.each { |r| r.ignore(*regexps) }
      self
    end

    # Replaces ignoring patterns in the listener.
    #
    # @param (see Listen::DirectoryRecord#ignore!)
    #
    # @return [Listen::Listener] the listener
    #
    # @see Listen::DirectoryRecord#ignore!
    #
    def ignore!(*regexps)
      directories_records.each { |r| r.ignore!(*regexps) }
      self
    end

    # Adds filtering patterns to the listener.
    #
    # @param (see Listen::DirectoryRecord#filter)
    #
    # @return [Listen::Listener] the listener
    #
    # @see Listen::DirectoryRecord#filter
    #
    def filter(*regexps)
      directories_records.each { |r| r.filter(*regexps) }
      self
    end

    # Replaces filtering patterns in the listener.
    #
    # @param (see Listen::DirectoryRecord#filter!)
    #
    # @return [Listen::Listener] the listener
    #
    # @see Listen::DirectoryRecord#filter!
    #
    def filter!(*regexps)
      directories_records.each { |r| r.filter!(*regexps) }
      self
    end

    # Sets the latency for the adapter. This is a helper method
    # to simplify changing the latency directly from the listener.
    #
    # @example Wait 0.5 seconds each time before checking changes
    #   latency 0.5
    #
    # @param [Float] seconds the amount of delay, in seconds
    #
    # @return [Listen::Listener] the listener
    #
    def latency(seconds)
      @adapter_options[:latency] = seconds
      self
    end

    # Sets whether the use of the polling adapter
    # should be forced or not.
    #
    # @example Forcing the use of the polling adapter
    #   force_polling true
    #
    # @param [Boolean] value whether to force the polling adapter or not
    #
    # @return [Listen::Listener] the listener
    #
    def force_polling(value)
      @adapter_options[:force_polling] = value
      self
    end

    # Sets whether to force the use of a particular adapter, rather than
    # going through usual adapter selection process on start.
    #
    # @example Force use of Linux polling
    #   force_adapter Listen::Adapters::Linux
    #
    # @param [Class] adapter class to use for file system event notification.
    #
    # @return [Listen::Listener] the listener
    #
    def force_adapter(adapter_class)
      @adapter_options[:force_adapter] = adapter_class
      self
    end

    # Sets whether the paths in the callback should be
    # relative or absolute.
    #
    # @example Enabling relative paths in the callback
    #   relative_paths true
    #
    # @param [Boolean] value whether to enable relative paths in the callback or not
    #
    # @return [Listen::Listener] the listener
    #
    def relative_paths(value)
      @use_relative_paths = value
      self
    end

    # Defines a custom polling fallback message or disable it.
    #
    # @example Disabling the polling fallback message
    #   polling_fallback_message false
    #
    # @param [String, Boolean] value to change polling fallback message or remove it
    #
    # @return [Listen::Listener] the listener
    #
    def polling_fallback_message(value)
      @adapter_options[:polling_fallback_message] = value
      self
    end

    # Sets the callback that gets called on changes.
    #
    # @example Assign a callback to be called on changes
    #   callback = lambda { |modified, added, removed| ... }
    #   change &callback
    #
    # @param [Proc] block the callback proc
    #
    # @return [Listen::Listener] the listener
    #
    def change(&block) # modified, added, removed
      @block = block
      self
    end

    # Runs the callback passing it the changes if there are any.
    #
    # @param (see Listen::DirectoryRecord#fetch_changes)
    #
    # @see Listen::DirectoryRecord#fetch_changes
    #
    def on_change(directories, options = {})
      changes = fetch_records_changes(directories, options)
      unless changes.values.all? { |paths| paths.empty? }
        block.call(changes[:modified], changes[:added], changes[:removed])
      end
    rescue => ex
      Kernel.warn "[Listen warning]: Change block raise an execption: #{$!}"
      Kernel.warn "Backtrace:\n\t#{ex.backtrace.join("\n\t")}"
    end

    private

    # Initializes the directories to watch as well as the directories records.
    #
    # @see Listen::DirectoryRecord
    #
    def initialize_directories_and_directories_records(directories)
      @directories = directories.map { |d| Pathname.new(d).realpath.to_s }
      @directories_records = directories.map { |d| DirectoryRecord.new(d) }
    end

    # Initializes whether or not using relative paths.
    #
    def initialize_relative_paths_usage(options)
      if directories.size > 1 && options[:relative_paths]
        Kernel.warn "[Listen warning]: #{RELATIVE_PATHS_WITH_MULTIPLE_DIRECTORIES_WARNING_MESSAGE}"
      end
      @use_relative_paths = directories.one? && options.delete(:relative_paths) { false }
    end

    # Build the directory record concurrently and initialize the adapter.
    #
    def setup
      t = Thread.new { build_directories_records }
      @adapter = initialize_adapter
      t.join
    end

    # Initializes an adapter passing it the callback and adapters' options.
    #
    def initialize_adapter
      callback = lambda { |changed_directories, options| self.on_change(changed_directories, options) }
      Adapter.select_and_initialize(directories, adapter_options, &callback)
    end

    # Build the watched directories' records.
    #
    def build_directories_records
      directories_records.each { |r| r.build }
    end

    # Returns the sum of all the changes to the directories records
    #
    # @param (see Listen::DirectoryRecord#fetch_changes)
    #
    # @return [Hash] the changes
    #
    def fetch_records_changes(directories_to_search, options)
      directories_records.inject({}) do |h, r|
        # directory records skips paths outside their range, so passing the
        # whole `directories` array is not a problem.
        record_changes = r.fetch_changes(directories_to_search, options.merge(:relative_paths => use_relative_paths))

        if h.empty?
          h.merge!(record_changes)
        else
          h.each { |k, v| h[k] += record_changes[k] }
        end

        h
      end
    end

  end
end
