module Listen
  class MultiListener < Listener
    attr_reader :directories, :directories_records, :adapter

    # Initializes the multiple directories listener.
    #
    # @param [String] directories the directories to listen to
    # @param [Hash] options the listen options
    # @option options [Regexp] ignore a pattern for ignoring paths
    # @option options [Regexp] filter a pattern for filtering paths
    # @option options [Float] latency the delay between checking for changes in seconds
    # @option options [Boolean] force_polling whether to force the polling adapter or not
    # @option options [String, Boolean] polling_fallback_message to change polling fallback message or remove it
    #
    # @yield [modified, added, removed] the changed files
    # @yieldparam [Array<String>] modified the list of modified files
    # @yieldparam [Array<String>] added the list of added files
    # @yieldparam [Array<String>] removed the list of removed files
    #
    def initialize(*args, &block)
      options     = args.last.is_a?(Hash) ? args.pop : {}
      directories = args

      @block               = block
      @directories         = directories.map  { |d| Pathname.new(d).realpath.to_s }
      @directories_records = @directories.map { |d| DirectoryRecord.new(d) }

      ignore(*options.delete(:ignore)) if options[:ignore]
      filter(*options.delete(:filter)) if options[:filter]

      @adapter_options = options
    end

    # Starts the listener by initializing the adapter and building
    # the directory record concurrently, then it starts the adapter to watch
    # for changes.
    #
    # @param [Boolean] blocking whether or not to block the current thread after starting
    #
    def start(blocking = true)
      t = Thread.new { @directories_records.each { |r| r.build } }
      @adapter = initialize_adapter
      t.join
      @adapter.start(blocking)
    end

    # Unpauses the listener.
    #
    # @return [Listen::Listener] the listener
    #
    def unpause
      @directories_records.each { |r| r.build }
      @adapter.paused = false
      self
    end

    # Adds ignored paths to the listener.
    #
    # @param (see Listen::DirectoryRecord#ignore)
    #
    # @return [Listen::Listener] the listener
    #
    def ignore(*paths)
      @directories_records.each { |r| r.ignore(*paths) }
      self
    end

    # Replaces ignored paths in the listener.
    #
    # @param (see Listen::DirectoryRecord#ignore!)
    #
    # @return [Listen::Listener] the listener
    #
    def ignore!(*paths)
      @directories_records.each { |r| r.ignore!(*paths) }
      self
    end

    # Adds file filters to the listener.
    #
    # @param (see Listen::DirectoryRecord#filter)
    #
    # @return [Listen::Listener] the listener
    #
    def filter(*regexps)
      @directories_records.each { |r| r.filter(*regexps) }
      self
    end

    # Replaces file filters in the listener.
    #
    # @param (see Listen::DirectoryRecord#filter!)
    #
    # @return [Listen::Listener] the listener
    #
    def filter!(*regexps)
      @directories_records.each { |r| r.filter!(*regexps) }
      self
    end

    # Runs the callback passing it the changes if there are any.
    #
    # @param (see Listen::DirectoryRecord#fetch_changes)
    #
    def on_change(directories_to_search, options = {})
      changes = fetch_records_changes(directories_to_search, options)
      unless changes.values.all? { |paths| paths.empty? }
        @block.call(changes[:modified],changes[:added],changes[:removed])
      end
    end

    private

    # Initializes an adapter passing it the callback and adapters' options.
    #
    def initialize_adapter
      callback = lambda { |changed_dirs, options| self.on_change(changed_dirs, options) }
      Adapter.select_and_initialize(@directories, @adapter_options, &callback)
    end

    # Returns the sum of all the changes to the directories records
    #
    # @param (see Listen::DirectoryRecord#fetch_changes)
    #
    # @return [Hash] the changes
    #
    def fetch_records_changes(directories_to_search, options)
      @directories_records.inject({}) do |h, r|
        # directory records skips paths outside their range, so passing the
        # whole `directories` array is not a problem.
        record_changes = r.fetch_changes(directories_to_search, options.merge(:relative_paths => DEFAULT_TO_RELATIVE_PATHS))

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
