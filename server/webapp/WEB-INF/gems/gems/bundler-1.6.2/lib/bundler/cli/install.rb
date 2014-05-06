module Bundler
  class CLI::Install
    attr_reader :options
    def initialize(options)
      @options = options.dup
    end

    def run
      warn_if_root

      if options[:without]
        options[:without] = options[:without].map{|g| g.tr(' ', ':') }
      end

      ENV['RB_USER_INSTALL'] = '1' if Bundler::FREEBSD

      # Just disable color in deployment mode
      Bundler.ui.shell = Thor::Shell::Basic.new if options[:deployment]

      if (options[:path] || options[:deployment]) && options[:system]
        Bundler.ui.error "You have specified both a path to install your gems to, \n" \
                         "as well as --system. Please choose."
        exit 1
      end

      if (options["trust-policy"])
        unless (Bundler.rubygems.security_policies.keys.include?(options["trust-policy"]))
          Bundler.ui.error "Rubygems doesn't know about trust policy '#{options["trust-policy"]}'. " \
            "The known policies are: #{Bundler.rubygems.security_policies.keys.join(', ')}."
          exit 1
        end
        Bundler.settings["trust-policy"] = options["trust-policy"]
      else
        Bundler.settings["trust-policy"] = nil if Bundler.settings["trust-policy"]
      end

      if options[:deployment] || options[:frozen]
        unless Bundler.default_lockfile.exist?
          flag = options[:deployment] ? '--deployment' : '--frozen'
          raise ProductionError, "The #{flag} flag requires a Gemfile.lock. Please make " \
                                 "sure you have checked your Gemfile.lock into version control " \
                                 "before deploying."
        end

        if Bundler.root.join("vendor/cache").exist?
          options[:local] = true
        end

        Bundler.settings[:frozen] = '1'
      end

      # When install is called with --no-deployment, disable deployment mode
      if options[:deployment] == false
        Bundler.settings.delete(:frozen)
        options[:system] = true
      end

      Bundler.settings[:path]     = nil if options[:system]
      Bundler.settings[:path]     = "vendor/bundle" if options[:deployment]
      Bundler.settings[:path]     = options["path"] if options["path"]
      Bundler.settings[:path]     ||= "bundle" if options["standalone"]
      Bundler.settings[:bin]      = options["binstubs"] if options["binstubs"]
      Bundler.settings[:bin]      = nil if options["binstubs"] && options["binstubs"].empty?
      Bundler.settings[:shebang]  = options["shebang"] if options["shebang"]
      Bundler.settings[:jobs]     = options["jobs"] if options["jobs"]
      Bundler.settings[:no_prune] = true if options["no-prune"]
      Bundler.settings[:clean]    = options["clean"] if options["clean"]
      Bundler.settings.without    = options[:without]
      Bundler.ui.level            = "warn" if options[:quiet]
      Bundler::Fetcher.disable_endpoint = options["full-index"]
      Bundler.settings[:disable_shared_gems] = Bundler.settings[:path] ? '1' : nil

      # rubygems plugins sometimes hook into the gem install process
      Gem.load_env_plugins if Gem.respond_to?(:load_env_plugins)

      definition = Bundler.definition
      definition.validate_ruby!
      Installer.install(Bundler.root, definition, options)
      Bundler.load.cache if Bundler.root.join("vendor/cache").exist? && !options["no-cache"] && !Bundler.settings[:frozen]

      if Bundler.settings[:path]
        absolute_path = File.expand_path(Bundler.settings[:path])
        relative_path = absolute_path.sub(File.expand_path('.'), '.')
        Bundler.ui.confirm "Your bundle is complete!"
        without_groups_messages
        Bundler.ui.confirm "It was installed into #{relative_path}"
      else
        Bundler.ui.confirm "Your bundle is complete!"
        without_groups_messages
        Bundler.ui.confirm "Use `bundle show [gemname]` to see where a bundled gem is installed."
      end
      Installer.post_install_messages.to_a.each do |name, msg|
        Bundler.ui.confirm "Post-install message from #{name}:"
        Bundler.ui.info msg
      end

      if Bundler.settings[:clean] && Bundler.settings[:path]
        require "bundler/cli/clean"
        Bundler::CLI::Clean.new(options).run
      end
    rescue GemNotFound, VersionConflict => e
      if options[:local] && Bundler.app_cache.exist?
        Bundler.ui.warn "Some gems seem to be missing from your vendor/cache directory."
      end

      if Bundler.definition.rubygems_remotes.empty?
        Bundler.ui.warn <<-WARN, :wrap => true
          Your Gemfile has no gem server sources. If you need gems that are \
          not already on your machine, add a line like this to your Gemfile:
          source 'https://rubygems.org'
        WARN
      end
      raise e
    end

  private

    def warn_if_root
      return unless Process.uid.zero?
      Bundler.ui.warn "Don't run Bundler as root. Bundler can ask for sudo " \
        "if it is needed, and installing your bundle as root will break this " \
        "application for all non-root users on this machine.", :wrap => true
    end

    def without_groups_messages
      if Bundler.settings.without.any?
        require "bundler/cli/common"
        Bundler.ui.confirm Bundler::CLI::Common.without_groups_message
      end
    end
  end
end
