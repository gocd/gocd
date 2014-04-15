require 'bundler'
require 'bundler/similarity_detector'
require 'bundler/vendored_thor'

module Bundler
  class CLI < Thor
    include Thor::Actions

    def initialize(*)
      super
      Bundler.rubygems.ui = UI::RGProxy.new(Bundler.ui)
    rescue UnknownArgumentError => e
      raise InvalidOption, e.message
    ensure
      Bundler.ui = UI::Shell.new(options)
      Bundler.ui.level = "debug" if options["verbose"]
    end

    check_unknown_options!(:except => [:config, :exec])
    stop_on_unknown_option! :exec

    default_task :install
    class_option "no-color", :type => :boolean, :banner => "Disable colorization in output"
    class_option "verbose",  :type => :boolean, :banner => "Enable verbose output mode", :aliases => "-V"

    def help(cli = nil)
      case cli
      when "gemfile" then command = "gemfile.5"
      when nil       then command = "bundle"
      else command = "bundle-#{cli}"
      end

      manpages = %w(
          bundle
          bundle-config
          bundle-exec
          bundle-install
          bundle-package
          bundle-update
          bundle-platform
          gemfile.5)

      if manpages.include?(command)
        root = File.expand_path("../man", __FILE__)

        if Bundler.which("groff") && root !~ %r{^file:/.+!/META-INF/jruby.home/.+}
          groff = "groff -Wall -mtty-char -mandoc -Tascii"
          pager = pager_system

          Kernel.exec "#{groff} #{root}/#{command} | #{pager}"
        else
          puts File.read("#{root}/#{command}.txt")
        end
      else
        super
      end
    end

    desc "init", "Generates a Gemfile into the current working directory"
    long_desc <<-D
      Init generates a default Gemfile in the current working directory. When adding a
      Gemfile to a gem with a gemspec, the --gemspec option will automatically add each
      dependency listed in the gemspec file to the newly created Gemfile.
    D
    method_option "gemspec", :type => :string, :banner => "Use the specified .gemspec to create the Gemfile"
    def init
      opts = options.dup
      if File.exist?("Gemfile")
        Bundler.ui.error "Gemfile already exists at #{Dir.pwd}/Gemfile"
        exit 1
      end

      if opts[:gemspec]
        gemspec = File.expand_path(opts[:gemspec])
        unless File.exist?(gemspec)
          Bundler.ui.error "Gem specification #{gemspec} doesn't exist"
          exit 1
        end
        spec = Gem::Specification.load(gemspec)
        puts "Writing new Gemfile to #{Dir.pwd}/Gemfile"
        File.open('Gemfile', 'wb') do |file|
          file << "# Generated from #{gemspec}\n"
          file << spec.to_gemfile
        end
      else
        puts "Writing new Gemfile to #{Dir.pwd}/Gemfile"
        FileUtils.cp(File.expand_path('../templates/Gemfile', __FILE__), 'Gemfile')
      end
    end

    desc "check", "Checks if the dependencies listed in Gemfile are satisfied by currently installed gems"
    long_desc <<-D
      Check searches the local machine for each of the gems requested in the Gemfile. If
      all gems are found, Bundler prints a success message and exits with a status of 0.
      If not, the first missing gem is listed and Bundler exits status 1.
    D
    method_option "gemfile", :type => :string, :banner =>
      "Use the specified gemfile instead of Gemfile"
    method_option "path", :type => :string, :banner =>
      "Specify a different path than the system default ($BUNDLE_PATH or $GEM_HOME). Bundler will remember this value for future installs on this machine"
    method_option "dry-run", :type => :boolean, :default => false, :banner =>
      "Lock the Gemfile"
    def check
      ENV['BUNDLE_GEMFILE'] = File.expand_path(options[:gemfile]) if options[:gemfile]

      Bundler.settings[:path] = File.expand_path(options[:path]) if options[:path]
      begin
        definition = Bundler.definition
        definition.validate_ruby!
        not_installed = definition.missing_specs
      rescue GemNotFound, VersionConflict
        Bundler.ui.error "Bundler can't satisfy your Gemfile's dependencies."
        Bundler.ui.warn  "Install missing gems with `bundle install`."
        exit 1
      end

      if not_installed.any?
        Bundler.ui.error "The following gems are missing"
        not_installed.each { |s| Bundler.ui.error " * #{s.name} (#{s.version})" }
        Bundler.ui.warn "Install missing gems with `bundle install`"
        exit 1
      elsif !Bundler.default_lockfile.exist? && Bundler.settings[:frozen]
        Bundler.ui.error "This bundle has been frozen, but there is no Gemfile.lock present"
        exit 1
      else
        Bundler.load.lock unless options[:"dry-run"]
        Bundler.ui.info "The Gemfile's dependencies are satisfied"
      end
    end

    desc "install", "Install the current environment to the system"
    long_desc <<-D
      Install will install all of the gems in the current bundle, making them available
      for use. In a freshly checked out repository, this command will give you the same
      gem versions as the last person who updated the Gemfile and ran `bundle update`.

      Passing [DIR] to install (e.g. vendor) will cause the unpacked gems to be installed
      into the [DIR] directory rather than into system gems.

      If the bundle has already been installed, bundler will tell you so and then exit.
    D
    method_option "without", :type => :array, :banner =>
      "Exclude gems that are part of the specified named group."
    method_option "gemfile", :type => :string, :banner =>
      "Use the specified gemfile instead of Gemfile"
    method_option "no-prune", :type => :boolean, :banner =>
      "Don't remove stale gems from the cache."
    method_option "no-cache", :type => :boolean, :banner =>
      "Don't update the existing gem cache."
    method_option "quiet", :type => :boolean, :banner =>
      "Only output warnings and errors."
    method_option "local", :type => :boolean, :banner =>
      "Do not attempt to fetch gems remotely and use the gem cache instead"
    method_option "binstubs", :type => :string, :lazy_default => "bin", :banner =>
      "Generate bin stubs for bundled gems to ./bin"
    method_option "shebang", :type => :string, :banner =>
      "Specify a different shebang executable name than the default (usually 'ruby')"
    method_option "path", :type => :string, :banner =>
      "Specify a different path than the system default ($BUNDLE_PATH or $GEM_HOME). Bundler will remember this value for future installs on this machine"
    method_option "system", :type => :boolean, :banner =>
      "Install to the system location ($BUNDLE_PATH or $GEM_HOME) even if the bundle was previously installed somewhere else for this application"
    method_option "frozen", :type => :boolean, :banner =>
      "Do not allow the Gemfile.lock to be updated after this install"
    method_option "deployment", :type => :boolean, :banner =>
      "Install using defaults tuned for deployment environments"
    method_option "standalone", :type => :array, :lazy_default => [], :banner =>
      "Make a bundle that can work without the Bundler runtime"
    method_option "full-index", :type => :boolean, :banner =>
      "Use the rubygems modern index instead of the API endpoint"
    method_option "clean", :type => :boolean, :banner =>
      "Run bundle clean automatically after install"
    method_option "trust-policy", :alias => "P", :type => :string, :banner =>
      "Gem trust policy (like gem install -P). Must be one of " +
        Bundler.rubygems.security_policies.keys.join('|') unless
        Bundler.rubygems.security_policies.empty?
    def install
      opts = options.dup
      if opts[:without]
        opts[:without] = opts[:without].map{|g| g.tr(' ', ':') }
      end

      # Can't use Bundler.settings for this because settings needs gemfile.dirname
      ENV['BUNDLE_GEMFILE'] = File.expand_path(opts[:gemfile]) if opts[:gemfile]
      ENV['RB_USER_INSTALL'] = '1' if Bundler::FREEBSD

      # Just disable color in deployment mode
      Bundler.ui.shell = Thor::Shell::Basic.new if opts[:deployment]

      if (opts[:path] || opts[:deployment]) && opts[:system]
        Bundler.ui.error "You have specified both a path to install your gems to, \n" \
                         "as well as --system. Please choose."
        exit 1
      end

      if (opts["trust-policy"])
        unless (Bundler.rubygems.security_policies.keys.include?(opts["trust-policy"]))
          Bundler.ui.error "Rubygems doesn't know about trust policy '#{opts["trust-policy"]}'. " \
            "The known policies are: #{Bundler.rubygems.security_policies.keys.join(', ')}."
          exit 1
        end
        Bundler.settings["trust-policy"] = opts["trust-policy"]
      else
        Bundler.settings["trust-policy"] = nil if Bundler.settings["trust-policy"]
      end

      if opts[:deployment] || opts[:frozen]
        unless Bundler.default_lockfile.exist?
          flag = opts[:deployment] ? '--deployment' : '--frozen'
          raise ProductionError, "The #{flag} flag requires a Gemfile.lock. Please make " \
                                 "sure you have checked your Gemfile.lock into version control " \
                                 "before deploying."
        end

        if Bundler.root.join("vendor/cache").exist?
          opts[:local] = true
        end

        Bundler.settings[:frozen] = '1'
      end

      # When install is called with --no-deployment, disable deployment mode
      if opts[:deployment] == false
        Bundler.settings.delete(:frozen)
        opts[:system] = true
      end

      # Can't use Bundler.settings for this because settings needs gemfile.dirname
      Bundler.settings[:path]     = nil if opts[:system]
      Bundler.settings[:path]     = "vendor/bundle" if opts[:deployment]
      Bundler.settings[:path]     = opts[:path] if opts[:path]
      Bundler.settings[:path]     ||= "bundle" if opts[:standalone]
      Bundler.settings[:bin]      = opts["binstubs"] if opts["binstubs"]
      Bundler.settings[:bin]      = nil if opts["binstubs"] && opts["binstubs"].empty?
      Bundler.settings[:shebang]  = opts["shebang"] if opts[:shebang]
      Bundler.settings[:no_prune] = true if opts["no-prune"]
      Bundler.settings[:clean]    = opts[:clean] if opts[:clean]
      Bundler.settings.without    = opts[:without]
      Bundler.ui.level            = "warn" if opts[:quiet]
      Bundler::Fetcher.disable_endpoint = opts["full-index"]
      Bundler.settings[:disable_shared_gems] = Bundler.settings[:path] ? '1' : nil

      # rubygems plugins sometimes hook into the gem install process
      Gem.load_env_plugins if Gem.respond_to?(:load_env_plugins)

      definition = Bundler.definition
      definition.validate_ruby!
      Installer.install(Bundler.root, definition, opts)
      Bundler.load.cache if Bundler.root.join("vendor/cache").exist? && !options["no-cache"]

      if Bundler.settings[:path]
        absolute_path = File.expand_path(Bundler.settings[:path])
        relative_path = absolute_path.sub(File.expand_path('.'), '.')
        Bundler.ui.confirm "Your bundle is complete!"
        Bundler.ui.confirm without_groups_message if Bundler.settings.without.any?
        Bundler.ui.confirm "It was installed into #{relative_path}"
      else
        Bundler.ui.confirm "Your bundle is complete!"
        Bundler.ui.confirm without_groups_message if Bundler.settings.without.any?
        Bundler.ui.confirm "Use `bundle show [gemname]` to see where a bundled gem is installed."
      end
      Installer.post_install_messages.to_a.each do |name, msg|
        Bundler.ui.confirm "Post-install message from #{name}:"
        Bundler.ui.info msg
      end

      clean if Bundler.settings[:clean] && Bundler.settings[:path]
    rescue GemNotFound, VersionConflict => e
      if opts[:local] && Bundler.app_cache.exist?
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

    desc "update", "update the current environment"
    long_desc <<-D
      Update will install the newest versions of the gems listed in the Gemfile. Use
      update when you have changed the Gemfile, or if you want to get the newest
      possible versions of the gems in the bundle.
    D
    method_option "source", :type => :array, :banner => "Update a specific source (and all gems associated with it)"
    method_option "local", :type => :boolean, :banner =>
      "Do not attempt to fetch gems remotely and use the gem cache instead"
    method_option "quiet", :type => :boolean, :banner =>
      "Only output warnings and errors."
    method_option "full-index", :type => :boolean, :banner =>
        "Use the rubygems modern index instead of the API endpoint"
    def update(*gems)
      sources = Array(options[:source])
      Bundler.ui.level = "warn" if options[:quiet]

      if gems.empty? && sources.empty?
        # We're doing a full update
        Bundler.definition(true)
      else
        # cycle through the requested gems, just to make sure they exist
        lock = Bundler.read_file(Bundler.default_lockfile)
        names = LockfileParser.new(lock).specs.map{ |s| s.name }
        gems.each do |g|
          next if names.include?(g)
          raise GemNotFound, not_found_message(g, names)
        end
        Bundler.definition(:gems => gems, :sources => sources)
      end

      Bundler::Fetcher.disable_endpoint = options["full-index"]

      opts = {"update" => true, "local" => options[:local]}
      # rubygems plugins sometimes hook into the gem install process
      Gem.load_env_plugins if Gem.respond_to?(:load_env_plugins)

      Bundler.definition.validate_ruby!
      Installer.install Bundler.root, Bundler.definition, opts
      Bundler.load.cache if Bundler.root.join("vendor/cache").exist?
      clean if Bundler.settings[:clean] && Bundler.settings[:path]
      Bundler.ui.confirm "Your bundle is updated!"
      Bundler.ui.confirm without_groups_message if Bundler.settings.without.any?
    end

    desc "show [GEM]", "Shows all gems that are part of the bundle, or the path to a given gem"
    long_desc <<-D
      Show lists the names and versions of all gems that are required by your Gemfile.
      Calling show with [GEM] will list the exact location of that gem on your machine.
    D
    method_option "paths", :type => :boolean,
      :banner => "List the paths of all gems that are required by your Gemfile."
    def show(gem_name = nil)
      Bundler.ui.silence do
        Bundler.definition.validate_ruby!
        Bundler.load.lock
      end

      if gem_name
        if gem_name == "bundler"
          path = File.expand_path("../../..", __FILE__)
        else
          spec = select_spec(gem_name, :regex_match)
          return unless spec
          path = spec.full_gem_path
          if !File.directory?(path)
            Bundler.ui.warn "The gem #{gem_name} has been deleted. It was installed at:"
          end
        end
        return Bundler.ui.info(path)
      end

      if options[:paths]
        Bundler.load.specs.sort_by { |s| s.name }.map do |s|
          Bundler.ui.info s.full_gem_path
        end
      else
        Bundler.ui.info "Gems included by the bundle:"
        Bundler.load.specs.sort_by { |s| s.name }.each do |s|
          Bundler.ui.info "  * #{s.name} (#{s.version}#{s.git_version})"
        end
      end
    end
    map %w(list) => "show"

    desc "binstubs [GEM]", "install the binstubs of the listed gem"
    long_desc <<-D
      Generate binstubs for executables in [GEM]. Binstubs are put into bin,
      or the --binstubs directory if one has been set.
    D
    method_option "path", :type => :string, :lazy_default => "bin", :banner =>
      "binstub destination directory (default bin)"
    method_option "force", :type => :boolean, :default => false, :banner =>
      "overwrite existing binstubs if they exist"
    def binstubs(gem_name)
      Bundler.definition.validate_ruby!
      Bundler.settings[:bin] = options["path"] if options["path"]
      Bundler.settings[:bin] = nil if options["path"] && options["path"].empty?
      installer = Installer.new(Bundler.root, Bundler.definition)
      spec      = installer.specs.find{|s| s.name == gem_name }
      raise GemNotFound, not_found_message(gem_name, Bundler.definition.specs) unless spec

      if spec.name == "bundler"
        Bundler.ui.warn "Sorry, Bundler can only be run via Rubygems."
      else
        installer.generate_bundler_executable_stubs(spec, :force => options[:force], :binstubs_cmd => true)
      end
    end

    desc "outdated [GEM]", "list installed gems with newer versions available"
    long_desc <<-D
      Outdated lists the names and versions of gems that have a newer version available
      in the given source. Calling outdated with [GEM [GEM]] will only check for newer
      versions of the given gems. Prerelease gems are ignored by default. If your gems
      are up to date, Bundler will exit with a status of 0. Otherwise, it will exit 1.
    D
    method_option "pre", :type => :boolean, :banner => "Check for newer pre-release gems"
    method_option "source", :type => :array, :banner => "Check against a specific source"
    method_option "local", :type => :boolean, :banner =>
      "Do not attempt to fetch gems remotely and use the gem cache instead"
    def outdated(*gems)
      sources = Array(options[:source])
      Bundler.definition.validate_ruby!

      current_specs = Bundler.ui.silence { Bundler.load.specs }

      if gems.empty? && sources.empty?
        # We're doing a full update
        definition = Bundler.definition(true)
      else
        definition = Bundler.definition(:gems => gems, :sources => sources)
      end
      options["local"] ? definition.resolve_with_cache! : definition.resolve_remotely!

      Bundler.ui.info ""

      out_count = 0
      # Loop through the current specs
      current_specs.sort_by { |s| s.name }.each do |current_spec|
        next if !gems.empty? && !gems.include?(current_spec.name)

        active_spec = definition.index[current_spec.name].sort_by { |b| b.version }

        if !current_spec.version.prerelease? && !options[:pre] && active_spec.size > 1
          active_spec = active_spec.delete_if { |b| b.respond_to?(:version) && b.version.prerelease? }
        end

        active_spec = active_spec.last
        next if active_spec.nil?

        gem_outdated = Gem::Version.new(active_spec.version) > Gem::Version.new(current_spec.version)
        git_outdated = current_spec.git_version != active_spec.git_version
        if gem_outdated || git_outdated
          if out_count == 0
            if options["pre"]
              Bundler.ui.info "Outdated gems included in the bundle (including pre-releases):"
            else
              Bundler.ui.info "Outdated gems included in the bundle:"
            end
          end

          spec_version    = "#{active_spec.version}#{active_spec.git_version}"
          current_version = "#{current_spec.version}#{current_spec.git_version}"
          Bundler.ui.info "  * #{active_spec.name} (#{spec_version} > #{current_version})"
          out_count += 1
        end
        Bundler.ui.debug "from #{active_spec.loaded_from}"
      end

      if out_count.zero?
        Bundler.ui.info "Your bundle is up to date!\n"
      else
        exit 1
      end
    end

    desc "cache", "Cache all the gems to vendor/cache", :hide => true
    method_option "no-prune",  :type => :boolean, :banner => "Don't remove stale gems from the cache."
    method_option "all",  :type => :boolean, :banner => "Include all sources (including path and git)."
    def cache
      Bundler.definition.validate_ruby!
      Bundler.definition.resolve_with_cache!
      setup_cache_all
      Bundler.load.cache
      Bundler.settings[:no_prune] = true if options["no-prune"]
      Bundler.load.lock
    rescue GemNotFound => e
      Bundler.ui.error(e.message)
      Bundler.ui.warn "Run `bundle install` to install missing gems."
      exit 1
    end

    desc "package", "Locks and then caches all of the gems into vendor/cache"
    method_option "no-prune",  :type => :boolean, :banner => "Don't remove stale gems from the cache."
    method_option "all",  :type => :boolean, :banner => "Include all sources (including path and git)."
    long_desc <<-D
      The package command will copy the .gem files for every gem in the bundle into the
      directory ./vendor/cache. If you then check that directory into your source
      control repository, others who check out your source will be able to install the
      bundle without having to download any additional gems.
    D
    def package
      setup_cache_all
      install
      # TODO: move cache contents here now that all bundles are locked
      Bundler.load.cache
    end
    map %w(pack) => :package

    desc "exec", "Run the command in context of the bundle"
    long_desc <<-D
      Exec runs a command, providing it access to the gems in the bundle. While using
      bundle exec you can require and call the bundled gems as if they were installed
      into the systemwide Rubygems repository.
    D
    def exec(*args)
      Bundler.definition.validate_ruby!
      Bundler.load.setup_environment

      begin
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

    desc "config NAME [VALUE]", "retrieve or set a configuration value"
    long_desc <<-D
      Retrieves or sets a configuration value. If only one parameter is provided, retrieve the value. If two parameters are provided, replace the
      existing value with the newly provided one.

      By default, setting a configuration value sets it for all projects
      on the machine.

      If a global setting is superceded by local configuration, this command
      will show the current value, as well as any superceded values and
      where they were specified.
    D
    def config(*args)
      peek = args.shift

      if peek && peek =~ /^\-\-/
        name, scope = args.shift, $'
      else
        name, scope = peek, "global"
      end

      unless name
        Bundler.ui.confirm "Settings are listed in order of priority. The top value will be used.\n"

        Bundler.settings.all.each do |setting|
          Bundler.ui.confirm "#{setting}"
          with_padding do
            Bundler.settings.pretty_values_for(setting).each do |line|
              Bundler.ui.info line
            end
          end
          Bundler.ui.confirm ""
        end
        return
      end

      case scope
      when "delete"
        Bundler.settings.set_local(name, nil)
        Bundler.settings.set_global(name, nil)
      when "local", "global"
        if args.empty?
          Bundler.ui.confirm "Settings for `#{name}` in order of priority. The top value will be used"
          with_padding do
            Bundler.settings.pretty_values_for(name).each { |line| Bundler.ui.info line }
          end
          return
        end

        locations = Bundler.settings.locations(name)

        if scope == "global"
          if local = locations[:local]
            Bundler.ui.info "Your application has set #{name} to #{local.inspect}. This will override the " \
              "global value you are currently setting"
          end

          if env = locations[:env]
            Bundler.ui.info "You have a bundler environment variable for #{name} set to #{env.inspect}. " \
              "This will take precedence over the global value you are setting"
          end

          if global = locations[:global]
            Bundler.ui.info "You are replacing the current global value of #{name}, which is currently #{global.inspect}"
          end
        end

        if scope == "local" && local = locations[:local]
          Bundler.ui.info "You are replacing the current local value of #{name}, which is currently #{local.inspect}"
        end

        if name.match(/\Alocal\./)
          pathname = Pathname.new(args.join(" "))
          args = [pathname.expand_path.to_s] if pathname.directory?
        end

        Bundler.settings.send("set_#{scope}", name, args.join(" "))
      else
        Bundler.ui.error "Invalid scope --#{scope} given. Please use --local or --global."
        exit 1
      end
    end

    desc "open GEM", "Opens the source directory of the given bundled gem"
    def open(name)
      editor = [ENV['BUNDLER_EDITOR'], ENV['VISUAL'], ENV['EDITOR']].find{|e| !e.nil? && !e.empty? }
      return Bundler.ui.info("To open a bundled gem, set $EDITOR or $BUNDLER_EDITOR") unless editor
      spec = select_spec(name, :regex_match)
      return unless spec
      Dir.chdir(spec.full_gem_path) do
        command = "#{editor} #{spec.full_gem_path}"
        success = system(command)
        Bundler.ui.info "Could not run '#{command}'" unless success
      end
    end

    desc "console [GROUP]", "Opens an IRB session with the bundle pre-loaded"
    def console(group = nil)
      group ? Bundler.require(:default, *(group.split.map! {|g| g.to_sym })) : Bundler.require
      ARGV.clear

      require 'irb'
      IRB.start
    end

    desc "version", "Prints the bundler's version information"
    def version
      Bundler.ui.info "Bundler version #{Bundler::VERSION}"
    end
    map %w(-v --version) => :version

    desc "licenses", "Prints the license of all gems in the bundle"
    def licenses
      Bundler.load.specs.sort_by { |s| s.license.to_s }.reverse.each do |s|
        gem_name = s.name
        license  = s.license || s.licenses

        if license.empty?
          Bundler.ui.warn "#{gem_name}: Unknown"
        else
          Bundler.ui.info "#{gem_name}: #{license}"
        end
      end
    end

    desc 'viz', "Generates a visual dependency graph"
    long_desc <<-D
      Viz generates a PNG file of the current Gemfile as a dependency graph.
      Viz requires the ruby-graphviz gem (and its dependencies).
      The associated gems must also be installed via 'bundle install'.
    D
    method_option :file, :type => :string, :default => 'gem_graph', :aliases => '-f', :banner => "The name to use for the generated file. see format option"
    method_option :version, :type => :boolean, :default => false, :aliases => '-v', :banner => "Set to show each gem version."
    method_option :requirements, :type => :boolean, :default => false, :aliases => '-r', :banner => "Set to show the version of each required dependency."
    method_option :format, :type => :string, :default => "png", :aliases => '-F', :banner => "This is output format option. Supported format is png, jpg, svg, dot ..."
    def viz
      require 'graphviz'

      output_file = File.expand_path(options[:file])
      graph = Graph.new(Bundler.load, output_file, options[:version], options[:requirements], options[:format])

      begin
        graph.viz
      rescue LoadError => e
        Bundler.ui.error e.inspect
        Bundler.ui.warn "Make sure you have the graphviz ruby gem. You can install it with:"
        Bundler.ui.warn "`gem install ruby-graphviz`"
      rescue StandardError => e
        if e.message =~ /GraphViz not installed or dot not in PATH/
          Bundler.ui.error e.message
          Bundler.ui.warn "The ruby graphviz gem requires GraphViz to be installed"
        else
          raise
        end
      end
    end

    desc "gem GEM", "Creates a skeleton for creating a rubygem"
    method_option :bin, :type => :boolean, :default => false, :aliases => '-b', :banner => "Generate a binary for your library."
    method_option :test, :type => :string, :lazy_default => 'rspec', :aliases => '-t', :banner => "Generate a test directory for your library: 'rspec' is the default, but 'minitest' is also supported."
    method_option :edit, :type => :string, :aliases => "-e",
                  :lazy_default => [ENV['BUNDLER_EDITOR'], ENV['VISUAL'], ENV['EDITOR']].find{|e| !e.nil? && !e.empty? },
                  :required => false, :banner => "/path/to/your/editor",
                  :desc => "Open generated gemspec in the specified editor (defaults to $EDITOR or $BUNDLER_EDITOR)"

    def gem(name)
      name = name.chomp("/") # remove trailing slash if present
      namespaced_path = name.tr('-', '/')
      target = File.join(Dir.pwd, name)
      constant_name = name.split('_').map{|p| p[0..0].upcase + p[1..-1] }.join
      constant_name = constant_name.split('-').map{|q| q[0..0].upcase + q[1..-1] }.join('::') if constant_name =~ /-/
      constant_array = constant_name.split('::')
      git_user_name = `git config user.name`.chomp
      git_user_email = `git config user.email`.chomp
      opts = {
        :name            => name,
        :namespaced_path => namespaced_path,
        :constant_name   => constant_name,
        :constant_array  => constant_array,
        :author          => git_user_name.empty? ? "TODO: Write your name" : git_user_name,
        :email           => git_user_email.empty? ? "TODO: Write your email address" : git_user_email,
        :test            => options[:test]
      }
      gemspec_dest = File.join(target, "#{name}.gemspec")
      template(File.join("newgem/Gemfile.tt"),               File.join(target, "Gemfile"),                             opts)
      template(File.join("newgem/Rakefile.tt"),              File.join(target, "Rakefile"),                            opts)
      template(File.join("newgem/LICENSE.txt.tt"),           File.join(target, "LICENSE.txt"),                         opts)
      template(File.join("newgem/README.md.tt"),             File.join(target, "README.md"),                           opts)
      template(File.join("newgem/gitignore.tt"),             File.join(target, ".gitignore"),                          opts)
      template(File.join("newgem/newgem.gemspec.tt"),        gemspec_dest,                                             opts)
      template(File.join("newgem/lib/newgem.rb.tt"),         File.join(target, "lib/#{namespaced_path}.rb"),           opts)
      template(File.join("newgem/lib/newgem/version.rb.tt"), File.join(target, "lib/#{namespaced_path}/version.rb"),   opts)
      if options[:bin]
        template(File.join("newgem/bin/newgem.tt"),          File.join(target, 'bin', name),                           opts)
      end
      case options[:test]
      when 'rspec'
        template(File.join("newgem/rspec.tt"),               File.join(target, ".rspec"),                              opts)
        template(File.join("newgem/spec/spec_helper.rb.tt"), File.join(target, "spec/spec_helper.rb"),                 opts)
        template(File.join("newgem/spec/newgem_spec.rb.tt"), File.join(target, "spec/#{namespaced_path}_spec.rb"),     opts)
      when 'minitest'
        template(File.join("newgem/test/minitest_helper.rb.tt"), File.join(target, "test/minitest_helper.rb"),         opts)
        template(File.join("newgem/test/test_newgem.rb.tt"),     File.join(target, "test/test_#{namespaced_path}.rb"), opts)
      end
      if options[:test]
        template(File.join("newgem/.travis.yml.tt"),         File.join(target, ".travis.yml"),            opts)
      end
      Bundler.ui.info "Initializating git repo in #{target}"
      Dir.chdir(target) { `git init`; `git add .` }

      if options[:edit]
        run("#{options["edit"]} \"#{gemspec_dest}\"")  # Open gemspec in editor
      end
    end

    def self.source_root
      File.expand_path(File.join(File.dirname(__FILE__), 'templates'))
    end

    desc "clean", "Cleans up unused gems in your bundler directory"
    method_option "dry-run", :type => :boolean, :default => false, :banner =>
      "only print out changes, do not actually clean gems"
    method_option "force", :type => :boolean, :default => false, :banner =>
      "forces clean even if --path is not set"
    def clean
      if Bundler.settings[:path] || options[:force]
        Bundler.load.clean(options[:"dry-run"])
      else
        Bundler.ui.error "Can only use bundle clean when --path is set or --force is set"
        exit 1
      end
    end

    desc "platform", "Displays platform compatibility information"
    method_option "ruby", :type => :boolean, :default => false, :banner =>
      "only display ruby related platform information"
    def platform
      platforms, ruby_version = Bundler.ui.silence do
        [ Bundler.definition.platforms.map {|p| "* #{p}" },
          Bundler.definition.ruby_version ]
      end
      output = []

      if options[:ruby]
        if ruby_version
          output << ruby_version
        else
          output << "No ruby version specified"
        end
      else
        output << "Your platform is: #{RUBY_PLATFORM}"
        output << "Your app has gems that work on these platforms:\n#{platforms.join("\n")}"

        if ruby_version
          output << "Your Gemfile specifies a Ruby version requirement:\n* #{ruby_version}"

          begin
            Bundler.definition.validate_ruby!
            output << "Your current platform satisfies the Ruby version requirement."
          rescue RubyVersionMismatch => e
            output << e.message
          end
        else
          output << "Your Gemfile does not specify a Ruby version requirement."
        end
      end

      Bundler.ui.info output.join("\n\n")
    end

    desc "inject GEM VERSION ...", "Add the named gem(s), with version requirements, to the resolved Gemfile"
    def inject(name, version, *gems)
      # The required arguments allow Thor to give useful feedback when the arguments
      # are incorrect. This adds those first two arguments onto the list as a whole.
      gems.unshift(version).unshift(name)

      # Build an array of Dependency objects out of the arguments
      deps = []
      gems.each_slice(2) do |gem_name, gem_version|
        deps << Bundler::Dependency.new(gem_name, gem_version)
      end

      added = Injector.inject(deps)

      if added.any?
        Bundler.ui.confirm "Added to Gemfile:"
        Bundler.ui.confirm added.map{ |g| "  #{g}" }.join("\n")
      else
        Bundler.ui.confirm "All injected gems were already present in the Gemfile"
      end
    end

    desc "env", "Print information about the environment Bundler is running under"
    def env
      Env.new.write($stdout)
    end

  private

    def setup_cache_all
      Bundler.settings[:cache_all] = options[:all] if options.key?("all")

      if Bundler.definition.sources.any? { |s| !s.is_a?(Source::Rubygems) } && !Bundler.settings[:cache_all]
        Bundler.ui.warn "Your Gemfile contains path and git dependencies. If you want "    \
          "to package them as well, please pass the --all flag. This will be the default " \
          "on Bundler 2.0."
      end
    end

    def select_spec(name, regex_match = nil)
      specs = []
      regexp = Regexp.new(name) if regex_match

      Bundler.definition.specs.each do |spec|
        return spec if spec.name == name
        specs << spec if regexp && spec.name =~ regexp
      end

      case specs.count
      when 0
        raise GemNotFound, not_found_message(name, Bundler.definition.dependencies)
      when 1
        specs.first
      else
        specs.each_with_index do |spec, index|
          Bundler.ui.info "#{index.succ} : #{spec.name}", true
        end
        Bundler.ui.info '0 : - exit -', true

        input = Bundler.ui.ask('> ')
        (num = input.to_i) > 0 ? specs[num - 1] : nil
      end
    end

    def not_found_message(missing_gem_name, alternatives)
      message = "Could not find gem '#{missing_gem_name}'."

      # This is called as the result of a GemNotFound, let's see if
      # there's any similarly named ones we can propose instead
      alternate_names = alternatives.map { |a| a.respond_to?(:name) ? a.name : a }
      suggestions = SimilarityDetector.new(alternate_names).similar_word_list(missing_gem_name)
      message += "\nDid you mean #{suggestions}?" if suggestions
      message
    end

    def pager_system
      pager = ENV['PAGER'] || ENV['MANPAGER']
      pager ||= 'less -R' if Bundler.which("less")
      pager ||= 'more' if Bundler.which("more")
      pager ||= 'cat'
    end

    def without_groups_message
      groups = Bundler.settings.without
      group_list = [groups[0...-1].join(", "), groups[-1..-1]].
        reject{|s| s.to_s.empty? }.join(" and ")
      group_str = (groups.size == 1) ? "group" : "groups"
      "Gems in the #{group_str} #{group_list} were not installed."
    end

  end
end
