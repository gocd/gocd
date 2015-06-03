require 'rbconfig'
require 'thread'
require 'set'
require 'fileutils'

module Listen
  class Adapter
    attr_accessor :directories, :latency, :paused

    # The default delay between checking for changes.
    DEFAULT_LATENCY = 0.25

    # The default warning message when there is a missing dependency.
    MISSING_DEPENDENCY_MESSAGE = <<-EOS.gsub(/^\s*/, '')
      For a better performance, it's recommended that you satisfy the missing dependency.
    EOS

    # The default warning message when falling back to polling adapter.
    POLLING_FALLBACK_MESSAGE = <<-EOS.gsub(/^\s*/, '')
      Listen will be polling changes. Learn more at https://github.com/guard/listen#polling-fallback.
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
    # @yield [changed_dirs, options] callback Callback called when a change happens
    # @yieldparam [Array<String>] changed_dirs the changed directories
    # @yieldparam [Hash] options callback options (like :recursive => true)
    #
    # @return [Listen::Adapter] the chosen adapter
    #
    def self.select_and_initialize(directories, options = {}, &callback)
      return Adapters::Polling.new(directories, options, &callback) if options.delete(:force_polling)

      warning = ''

      begin
        if Adapters::Darwin.usable_and_works?(directories, options)
          return Adapters::Darwin.new(directories, options, &callback)
        elsif Adapters::Linux.usable_and_works?(directories, options)
          return Adapters::Linux.new(directories, options, &callback)
        elsif Adapters::BSD.usable_and_works?(directories, options)
          return Adapters::BSD.new(directories, options, &callback)
        elsif Adapters::Windows.usable_and_works?(directories, options)
          return Adapters::Windows.new(directories, options, &callback)
        end
      rescue DependencyManager::Error => e
        warning += e.message + "\n" + MISSING_DEPENDENCY_MESSAGE
      end

      unless options[:polling_fallback_message] == false
        warning += options[:polling_fallback_message] || POLLING_FALLBACK_MESSAGE
        Kernel.warn "[Listen warning]:\n" + warning.gsub(/^(.*)/, '  \1')
      end

      Adapters::Polling.new(directories, options, &callback)
    end

    # Initializes the adapter.
    #
    # @param [String, Array<String>] directories the directories to watch
    # @param [Hash] options the adapter options
    # @option options [Float] latency the delay between checking for changes in seconds
    # @option options [Boolean] report_changes whether or not to automatically report changes (run the callback)
    #
    # @yield [changed_dirs, options] callback Callback called when a change happens
    # @yieldparam [Array<String>] changed_dirs the changed directories
    # @yieldparam [Hash] options callback options (like :recursive => true)
    #
    # @return [Listen::Adapter] the adapter
    #
    def initialize(directories, options = {}, &callback)
      @directories  = Array(directories)
      @callback     = callback
      @paused       = false
      @mutex        = Mutex.new
      @changed_dirs = Set.new
      @turnstile    = Turnstile.new
      @latency    ||= DEFAULT_LATENCY
      @latency      = options[:latency] if options[:latency]
      @report_changes = options[:report_changes].nil? ? true : options[:report_changes]
    end

    # Starts the adapter.
    #
    # @param [Boolean] blocking whether or not to block the current thread after starting
    #
    def start(blocking = true)
      @stop = false
    end

    # Stops the adapter.
    #
    def stop
      @stop = true
      @turnstile.signal # ensure no thread is blocked
    end

    # Returns whether the adapter is statred or not
    #
    # @return [Boolean] whether the adapter is started or not
    #
    def started?
      @stop.nil? ? false : !@stop
    end

    # Blocks the main thread until the poll thread
    # runs the callback.
    #
    def wait_for_callback
      @turnstile.wait unless @paused
    end

    # Blocks the main thread until N changes are
    # detected.
    #
    def wait_for_changes(goal = 0)
      changes = 0

      loop do
        @mutex.synchronize { changes = @changed_dirs.size }

        return if @paused || @stop
        return if changes >= goal

        sleep(@latency)
      end
    end

    # Checks if the adapter is usable on the current OS.
    #
    # @return [Boolean] whether usable or not
    #
    def self.usable?
      load_depenencies
      dependencies_loaded?
    end

    # Checks if the adapter is usable and works on the current OS.
    #
    # @param [String, Array<String>] directories the directories to watch
    # @param [Hash] options the adapter options
    # @option options [Float] latency the delay between checking for changes in seconds
    #
    # @return [Boolean] whether usable and work or not
    #
    def self.usable_and_works?(directories, options = {})
      usable? && Array(directories).all? { |d| works?(d, options) }
    end

    # Runs a tests to determine if the adapter can actually pick up
    # changes in a given directory and returns the result.
    #
    # @note This test takes some time depending the adapter latency.
    #
    # @param [String, Pathname] directory the directory to watch
    # @param [Hash] options the adapter options
    # @option options [Float] latency the delay between checking for changes in seconds
    #
    # @return [Boolean] whether the adapter works or not
    #
    def self.works?(directory, options = {})
      work = false
      test_file = "#{directory}/.listen_test"
      callback = lambda { |*| work = true }
      adapter  = self.new(directory, options, &callback)
      adapter.start(false)

      FileUtils.touch(test_file)

      t = Thread.new { sleep(adapter.latency * 5); adapter.stop }

      adapter.wait_for_callback
      work
    ensure
      Thread.kill(t) if t
      FileUtils.rm(test_file) if File.exists?(test_file)
      adapter.stop if adapter && adapter.started?
    end

    # Runs the callback and passes it the changes if there are any.
    #
    def report_changes
      changed_dirs = nil

      @mutex.synchronize do
        return if @changed_dirs.empty?
        changed_dirs = @changed_dirs.to_a
        @changed_dirs.clear
      end

      @callback.call(changed_dirs, {})
      @turnstile.signal
    end

    private

    # Polls changed directories and reports them back
    # when there are changes.
    #
    def poll_changed_dirs
      until @stop
        sleep(@latency)
        report_changes
      end
    end
  end
end
