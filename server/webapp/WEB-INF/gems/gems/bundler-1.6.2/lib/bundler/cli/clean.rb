module Bundler
  class CLI::Clean
    attr_reader :options

    def initialize(options)
      @options = options
    end

    def run
      if Bundler.settings[:path] || options[:force]
        Bundler.load.clean(options[:"dry-run"])
      else
        Bundler.ui.error "Can only use bundle clean when --path is set or --force is set"
        exit 1
      end
    end

  end
end
