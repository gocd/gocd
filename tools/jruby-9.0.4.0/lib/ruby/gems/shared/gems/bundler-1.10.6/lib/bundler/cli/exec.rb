module Bundler
  class CLI::Exec
    attr_reader :options, :args, :cmd

    def initialize(options, args)
      @options = options
      @cmd = args.shift
      @args = args

      if RUBY_VERSION >= "2.0"
        @args << { :close_others => !options.keep_file_descriptors? }
      elsif options.keep_file_descriptors?
        Bundler.ui.warn "Ruby version #{RUBY_VERSION} defaults to keeping non-standard file descriptors on Kernel#exec."
      end
    end

    def run
      ui = Bundler.ui
      raise ArgumentError if cmd.nil?

      # First, try to exec directly to something in PATH
      SharedHelpers.set_bundle_environment
      bin_path = Bundler.which(@cmd)
      if bin_path
        Bundler.ui = nil
        Kernel.exec(bin_path, *args)
      end

      # If that didn't work, set up the whole bundle
      Bundler.definition.validate_ruby!
      Bundler.load.setup_environment
      Bundler.ui = nil
      Kernel.exec(@cmd, *args)
    rescue Errno::EACCES
      Bundler.ui = ui
      Bundler.ui.error "bundler: not executable: #{cmd}"
      exit 126
    rescue Errno::ENOENT
      Bundler.ui = ui
      Bundler.ui.error "bundler: command not found: #{cmd}"
      Bundler.ui.warn  "Install missing gem executables with `bundle install`"
      exit 127
    rescue ArgumentError
      Bundler.ui = ui
      Bundler.ui.error "bundler: exec needs a command to run"
      exit 128
    end

  end
end
