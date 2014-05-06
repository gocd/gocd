module Bundler
  class CLI::Exec
    attr_reader :options, :args

    def initialize(options, args)
      @options = options
      @args = args
    end

    def run
      Bundler.definition.validate_ruby!
      Bundler.load.setup_environment

      begin
        if RUBY_VERSION >= "2.0"
          @args << { :close_others => !options.keep_file_descriptors? }
        elsif options.keep_file_descriptors?
          Bundler.ui.warn "Ruby version #{RUBY_VERSION} defaults to keeping non-standard file descriptors on Kernel#exec."
        end

        # Run
        Kernel.exec(*args)
      rescue Errno::EACCES
        Bundler.ui.error "bundler: not executable: #{args.first}"
        exit 126
      rescue Errno::ENOENT
        Bundler.ui.error "bundler: command not found: #{args.first}"
        Bundler.ui.warn  "Install missing gem executables with `bundle install`"
        exit 127
      rescue ArgumentError
        Bundler.ui.error "bundler: exec needs a command to run"
        exit 128
      end
    end

  end
end
