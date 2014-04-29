module Bundler
  class CLI::Update
    attr_reader :options, :gems
    def initialize(options, gems)
      @options = options
      @gems = gems
    end

    def run

      sources = Array(options[:source])
      groups  = Array(options[:group]).map(&:to_sym)
      Bundler.ui.level = "warn" if options[:quiet]

      if gems.empty? && sources.empty? && groups.empty?
        # We're doing a full update
        Bundler.definition(true)
      else
        unless Bundler.default_lockfile.exist?
          raise GemfileLockNotFound, "This Bundle hasn't been installed yet. " \
            "Run `bundle install` to update and install the bundled gems."
        end
        # cycle through the requested gems, just to make sure they exist
        names = Bundler.locked_gems.specs.map{ |s| s.name }
        gems.each do |g|
          next if names.include?(g)
          require "bundler/cli/common"
          raise GemNotFound, Bundler::CLI::Common.gem_not_found_message(g, names)
        end

        if groups.any?
          specs = Bundler.definition.specs_for groups
          sources.concat(specs.map(&:name))
        end

        Bundler.definition(:gems => gems, :sources => sources)
      end

      Bundler::Fetcher.disable_endpoint = options["full-index"]

      opts = options.dup
      opts["update"] = true
      opts["local"] = options[:local]

      Bundler.settings[:jobs] = opts["jobs"] if opts["jobs"]

      # rubygems plugins sometimes hook into the gem install process
      Gem.load_env_plugins if Gem.respond_to?(:load_env_plugins)

      Bundler.definition.validate_ruby!
      Installer.install Bundler.root, Bundler.definition, opts
      Bundler.load.cache if Bundler.root.join("vendor/cache").exist?

      if Bundler.settings[:clean] && Bundler.settings[:path]
        require "bundler/cli/clean"
        Bundler::CLI::Clean.new(options).run
      end

      Bundler.ui.confirm "Your bundle is updated!"
      without_groups_messages
    end

  private

    def without_groups_messages
      if Bundler.settings.without.any?
        require "bundler/cli/common"
        Bundler.ui.confirm Bundler::CLI::Common.without_groups_message
      end
    end

  end
end
