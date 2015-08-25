require 'set'
require 'rubygems'

module Listen
  module Adapters

    # Adapter implementation for Windows `wdm`.
    #
    class Windows < Adapter

      BUNDLER_DECLARE_GEM = <<-EOS.gsub(/^ {6}/, '')
        Please add the following to your Gemfile to avoid polling for changes:
          require 'rbconfig'
          gem 'wdm', '>= 0.1.0' if RbConfig::CONFIG['target_os'] =~ /mswin|mingw/i
      EOS

      def self.target_os_regex; /mswin|mingw/i; end
      def self.adapter_gem; 'wdm'; end

      # Checks if the adapter is usable on target OS.
      #
      # @return [Boolean] whether usable or not
      #
      def self.usable?
        super if mri? && at_least_ruby_1_9?
      end

      # Load the adapter gem
      #
      # @return [Boolean] whether required or not
      #
      def self.load_dependent_adapter
        super
      rescue Gem::LoadError
        Kernel.warn BUNDLER_DECLARE_GEM
      end

      private

      # Checks if Ruby engine is MRI.
      #
      # @return [Boolean]
      #
      def self.mri?
        defined?(RUBY_ENGINE) && RUBY_ENGINE == 'ruby'
      end

      # Checks if Ruby engine is MRI.
      #
      # @return [Boolean]
      #
      def self.at_least_ruby_1_9?
        Gem::Version.new(RUBY_VERSION.dup) >= Gem::Version.new('1.9.2')
      end

      # Initializes a WDM monitor and adds a watcher for
      # each directory passed to the adapter.
      #
      # @return [WDM::Monitor] initialized worker
      #
      # @see Listen::Adapter#initialize_worker
      #
      def initialize_worker
        callback = Proc.new do |change|
          next if paused

          mutex.synchronize do
            @changed_directories << File.dirname(change.path)
          end
        end

        WDM::Monitor.new.tap do |worker|
          directories.each { |dir| worker.watch_recursively(dir, &callback) }
        end
      end

      # Start the worker in a new thread and sleep 0.1 second.
      #
      # @see Listen::Adapter#start_worker
      #
      def start_worker
        @worker_thread = Thread.new { worker.run! }
        # Wait for the worker to start. This is needed to avoid a deadlock
        # when stopping immediately after starting.
        sleep 0.1
      end

    end

  end
end
