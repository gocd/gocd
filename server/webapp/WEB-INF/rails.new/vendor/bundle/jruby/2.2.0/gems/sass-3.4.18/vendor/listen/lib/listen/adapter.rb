require 'rbconfig'
require 'thread'
require 'set'
require 'fileutils'

module Listen
  class Adapter
    attr_accessor :directories, :callback, :stopped, :paused,
                  :mutex, :changed_directories, :turnstile, :latency,
                  :worker, :worker_thread, :poller_thread

    # The list of existing optimized adapters.
    OPTIMIZED_ADAPTERS = %w[Darwin Linux BSD Windows]

    # The list of existing fallback adapters.
    FALLBACK_ADAPTERS = %w[Polling]

    # The list of all existing adapters.
    ADAPTERS = OPTIMIZED_ADAPTERS + FALLBACK_ADAPTERS

    # The default delay between checking for changes.
    DEFAULT_LATENCY = 0.25

    # The default warning message when falling back to polling adapter.
    POLLING_FALLBACK_MESSAGE = <<-EOS.gsub(/^\s*/, '')
      Listen will be polling for changes. Learn more at https://github.com/guard/listen#polling-fallback.
    EOS

    # Selects the appropriate adapter implementation for the
    # current OS and initializes it.
    #
    # @param [String, Array<String>] directories the directories to watch
    # @param [Hash] options the adapter options
    # @option options [Boolean] force_polling to force polling or not
    # @option options [String, Boolean] polling_fallback_message to change polling fallback message or remove it
    # @option options [Float] latency the delay between checking for changes in seconds
    #
    # @yield [changed_directories, options] callback the callback called when a change happens
    # @yieldparam [Array<String>] changed_directories the changed directories
    # @yieldparam [Hash] options callback options (like recursive: true)
    #
    # @return [Listen::Adapter] the chosen adapter
    #
    def self.select_and_initialize(directories, options = {}, &callback)
      forced_adapter_class = options.delete(:force_adapter)
      force_polling = options.delete(:force_polling)

      if forced_adapter_class
        forced_adapter_class.load_dependent_adapter
        return forced_adapter_class.new(directories, options, &callback)
      end

      return Adapters::Polling.new(directories, options, &callback) if force_polling

      OPTIMIZED_ADAPTERS.each do |adapter|
        namespaced_adapter = Adapters.const_get(adapter)
        if namespaced_adapter.send(:usable_and_works?, directories, options)
          return namespaced_adapter.new(directories, options, &callback)
        end
      end

      self.warn_polling_fallback(options)
      Adapters::Polling.new(directories, options, &callback)
    end

    # Initializes the adapter.
    #
    # @param [String, Array<String>] directories the directories to watch
    # @param [Hash] options the adapter options
    # @option options [Float] latency the delay between checking for changes in seconds
    #
    # @yield [changed_directories, options] callback Callback called when a change happens
    # @yieldparam [Array<String>] changed_directories the changed directories
    # @yieldparam [Hash] options callback options (like recursive: true)
    #
    # @return [Listen::Adapter] the adapter
    #
    def initialize(directories, options = {}, &callback)
      @directories         = Array(directories)
      @callback            = callback
      @stopped             = true
      @paused              = false
      @mutex               = Mutex.new
      @changed_directories = Set.new
      @turnstile           = Turnstile.new
      @latency             = options.fetch(:latency, default_latency)
      @worker              = initialize_worker
    end

    # Starts the adapter and don't block the current thread.
    #
    # @param [Boolean] blocking whether or not to block the current thread after starting
    #
    def start
      mutex.synchronize do
        return unless stopped
        @stopped = false
      end

      start_worker
      start_poller
    end

    # Starts the adapter and block the current thread.
    #
    # @since 1.0.0
    #
    def start!
      start
      blocking_thread.join
    end

    # Stops the adapter.
    #
    def stop
      mutex.synchronize do
        return if stopped
        @stopped = true
        turnstile.signal # ensure no thread is blocked
      end

      worker.stop if worker
      Thread.kill(worker_thread) if worker_thread
      if poller_thread
        poller_thread.kill
        poller_thread.join
      end
    end

    # Pauses the adapter.
    #
    def pause
      @paused = true
    end

    # Unpauses the adapter.
    #
    def unpause
      @paused = false
    end

    # Returns whether the adapter is started or not.
    #
    # @return [Boolean] whether the adapter is started or not
    #
    def started?
      !stopped
    end

    # Returns whether the adapter is paused or not.
    #
    # @return [Boolean] whether the adapter is paused or not
    #
    def paused?
      paused
    end

    # Blocks the main thread until the poll thread
    # runs the callback.
    #
    def wait_for_callback
      turnstile.wait unless paused
    end

    # Blocks the main thread until N changes are
    # detected.
    #
    def wait_for_changes(threshold = 0)
      changes = 0

      loop do
        mutex.synchronize { changes = changed_directories.size }

        return if paused || stopped
        return if changes >= threshold

        sleep(latency)
      end
    end

    # Checks if the adapter is usable and works on the current OS.
    #
    # @param [String, Array<String>] directories the directories to watch
    # @param [Hash] options the adapter options
    # @option options [Float] latency the delay between checking for changes in seconds
    #
    # @return [Boolean] whether the adapter is usable and work or not
    #
    def self.usable_and_works?(directories, options = {})
      usable? && Array(directories).all? { |d| works?(d, options) }
    end

    # Checks if the adapter is usable on target OS.
    #
    # @return [Boolean] whether usable or not
    #
    def self.usable?
      load_dependent_adapter if RbConfig::CONFIG['target_os'] =~ target_os_regex
    end

    # Load the adapter gem
    #
    # @return [Boolean] whether loaded or not
    #
    def self.load_dependent_adapter
      return true if @loaded
      require adapter_gem
      return @loaded = true
    rescue LoadError
      false
    end

    # Runs a tests to determine if the adapter can actually pick up
    # changes in a given directory and returns the result.
    #
    # @note This test takes some time depending on the adapter latency.
    #
    # @param [String, Pathname] directory the directory to watch
    # @param [Hash] options the adapter options
    # @option options [Float] latency the delay between checking for changes in seconds
    #
    # @return [Boolean] whether the adapter works or not
    #
    def self.works?(directory, options = {})
      work      = false
      test_file = "#{directory}/.listen_test"
      callback  = lambda { |*| work = true }
      adapter   = self.new(directory, options, &callback)
      adapter.start

      FileUtils.touch(test_file)

      t = Thread.new { sleep(adapter.latency * 5); adapter.stop }

      adapter.wait_for_callback
      work
    ensure
      Thread.kill(t) if t
      FileUtils.rm(test_file, :force => true)
      adapter.stop if adapter && adapter.started?
    end

    # Runs the callback and passes it the changes if there are any.
    #
    def report_changes
      changed_dirs = nil

      mutex.synchronize do
        return if @changed_directories.empty?
        changed_dirs = @changed_directories.to_a
        @changed_directories.clear
      end

      callback.call(changed_dirs, {})
      turnstile.signal
    end

    private

    # The default delay between checking for changes.
    #
    # @note This method can be overriden on a per-adapter basis.
    #
    def default_latency
      DEFAULT_LATENCY
    end

    # The thread on which the main thread should wait
    # when the adapter has been started in blocking mode.
    #
    # @note This method can be overriden on a per-adapter basis.
    #
    def blocking_thread
      worker_thread
    end

    # Initialize the adpater' specific worker.
    #
    # @note Each adapter must override this method
    #   to initialize its own @worker.
    #
    def initialize_worker
      nil
    end

    # Should start the worker in a new thread.
    #
    # @note Each adapter must override this method
    #   to start its worker on a new @worker_thread thread.
    #
    def start_worker
      raise NotImplementedError, "#{self.class} cannot respond to: #{__method__}"
    end

    # This method starts a new thread which poll for changes detected by
    # the adapter and report them back to the user.
    #
    def start_poller
      @poller_thread = Thread.new { poll_changed_directories }
    end

    # Warn of polling fallback unless the :polling_fallback_message
    # has been set to false.
    #
    # @param [String] warning an existing warning message
    # @param [Hash] options the adapter options
    # @option options [Boolean] polling_fallback_message to change polling fallback message or remove it
    #
    def self.warn_polling_fallback(options)
      return if options[:polling_fallback_message] == false

      warning = options[:polling_fallback_message] || POLLING_FALLBACK_MESSAGE
      Kernel.warn "[Listen warning]:\n#{warning.gsub(/^(.*)/, '  \1')}"
    end

    # Polls changed directories and reports them back when there are changes.
    #
    # @note This method can be overriden on a per-adapter basis.
    #
    def poll_changed_directories
      until stopped
        sleep(latency)
        report_changes
      end
    end
  end
end
