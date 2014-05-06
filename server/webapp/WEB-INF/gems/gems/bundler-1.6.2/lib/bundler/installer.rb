require 'erb'
require 'rubygems/dependency_installer'
require 'bundler/parallel_workers'

module Bundler
  class Installer < Environment
    class << self
      attr_accessor :post_install_messages
    end

    # Begins the installation process for Bundler.
    # For more information see the #run method on this class.
    def self.install(root, definition, options = {})
      installer = new(root, definition)
      installer.run(options)
      installer
    end

    # Runs the install procedures for a specific Gemfile.
    #
    # Firstly, this method will check to see if Bundler.bundle_path exists
    # and if not then will create it. This is usually the location of gems
    # on the system, be it RVM or at a system path.
    #
    # Secondly, it checks if Bundler has been configured to be "frozen"
    # Frozen ensures that the Gemfile and the Gemfile.lock file are matching.
    # This stops a situation where a developer may update the Gemfile but may not run
    # `bundle install`, which leads to the Gemfile.lock file not being correctly updated.
    # If this file is not correctly updated then any other developer running
    # `bundle install` will potentially not install the correct gems.
    #
    # Thirdly, Bundler checks if there are any dependencies specified in the Gemfile using
    # Bundler::Environment#dependencies. If there are no dependencies specified then
    # Bundler returns a warning message stating so and this method returns.
    #
    # Fourthly, Bundler checks if the default lockfile (Gemfile.lock) exists, and if so
    # then proceeds to set up a defintion based on the default gemfile (Gemfile) and the
    # default lock file (Gemfile.lock). However, this is not the case if the platform is different
    # to that which is specified in Gemfile.lock, or if there are any missing specs for the gems.
    #
    # Fifthly, Bundler resolves the dependencies either through a cache of gems or by remote.
    # This then leads into the gems being installed, along with stubs for their executables,
    # but only if the --binstubs option has been passed or Bundler.options[:bin] has been set
    # earlier.
    #
    # Sixthly, a new Gemfile.lock is created from the installed gems to ensure that the next time
    # that a user runs `bundle install` they will receive any updates from this process.
    #
    # Finally: TODO add documentation for how the standalone process works.
    def run(options)
      create_bundle_path

      if Bundler.settings[:frozen]
        @definition.ensure_equivalent_gemfile_and_lockfile(options[:deployment])
      end

      if dependencies.empty?
        Bundler.ui.warn "The Gemfile specifies no dependencies"
        lock
        return
      end

      if Bundler.default_lockfile.exist? && !options["update"]
        local = Bundler.ui.silence do
          begin
            tmpdef = Definition.build(Bundler.default_gemfile, Bundler.default_lockfile, nil)
            true unless tmpdef.new_platform? || tmpdef.missing_specs.any?
          rescue BundlerError
          end
        end
      end

      # Since we are installing, we can resolve the definition
      # using remote specs
      unless local
        options["local"] ? @definition.resolve_with_cache! : @definition.resolve_remotely!
      end
      # Must install gems in the order that the resolver provides
      # as dependencies might actually affect the installation of
      # the gem.
      Installer.post_install_messages = {}

      # the order that the resolver provides is significant, since
      # dependencies might actually affect the installation of a gem.
      # that said, it's a rare situation (other than rake), and parallel
      # installation is just SO MUCH FASTER. so we let people opt in.
      jobs = [Bundler.settings[:jobs].to_i-1, 1].max
      if jobs > 1 && can_install_parallely?
        install_in_parallel jobs, options[:standalone]
      else
        install_sequentially options[:standalone]
      end

      lock unless Bundler.settings[:frozen]
      generate_standalone(options[:standalone]) if options[:standalone]
    end

    def install_gem_from_spec(spec, standalone = false, worker = 0)
      # Fetch the build settings, if there are any
      settings             = Bundler.settings["build.#{spec.name}"]
      install_message      = nil
      post_install_message = nil
      debug_message        = nil
      Bundler.rubygems.with_build_args [settings] do
        install_message, post_install_message, debug_message = spec.source.install(spec)
        if install_message.include? 'Installing'
          Bundler.ui.confirm install_message
        else
          Bundler.ui.info install_message
        end
        Bundler.ui.debug debug_message if debug_message
        Bundler.ui.debug "#{worker}:  #{spec.name} (#{spec.version}) from #{spec.loaded_from}"
      end

      if Bundler.settings[:bin] && standalone
        generate_standalone_bundler_executable_stubs(spec)
      elsif Bundler.settings[:bin]
        generate_bundler_executable_stubs(spec, :force => true)
      end

      post_install_message
    rescue Exception => e
      # if install hook failed or gem signature is bad, just die
      raise e if e.is_a?(Bundler::InstallHookError) || e.is_a?(Bundler::SecurityError)

      # other failure, likely a native extension build failure
      Bundler.ui.info ""
      Bundler.ui.warn "#{e.class}: #{e.message}"
      msg = "An error occurred while installing #{spec.name} (#{spec.version}),"
      msg << " and Bundler cannot continue."

      unless spec.source.options["git"]
        msg << "\nMake sure that `gem install"
        msg << " #{spec.name} -v '#{spec.version}'` succeeds before bundling."
      end
      Bundler.ui.debug e.backtrace.join("\n")
      raise Bundler::InstallError, msg
    end

    def generate_bundler_executable_stubs(spec, options = {})
      if options[:binstubs_cmd] && spec.executables.empty?
        options = {}
        spec.runtime_dependencies.each do |dep|
          bins = @definition.specs[dep].first.executables
          options[dep.name] = bins unless bins.empty?
        end
        if options.any?
          Bundler.ui.warn "#{spec.name} has no executables, but you may want " +
            "one from a gem it depends on."
          options.each{|name,bins| Bundler.ui.warn "  #{name} has: #{bins.join(', ')}" }
        else
          Bundler.ui.warn "There are no executables for the gem #{spec.name}."
        end
        return
      end

      # double-assignment to avoid warnings about variables that will be used by ERB
      bin_path = bin_path = Bundler.bin_path
      template = template = File.read(File.expand_path('../templates/Executable', __FILE__))
      relative_gemfile_path = relative_gemfile_path = Bundler.default_gemfile.relative_path_from(bin_path)
      ruby_command = ruby_command = Thor::Util.ruby_command

      exists = []
      spec.executables.each do |executable|
        next if executable == "bundle"

        binstub_path = "#{bin_path}/#{executable}"
        if File.exist?(binstub_path) && !options[:force]
          exists << executable
          next
        end

        File.open(binstub_path, 'w', 0777 & ~File.umask) do |f|
          f.puts ERB.new(template, nil, '-').result(binding)
        end
      end

      if options[:binstubs_cmd] && exists.any?
        case exists.size
        when 1
          Bundler.ui.warn "Skipped #{exists[0]} since it already exists."
        when 2
          Bundler.ui.warn "Skipped #{exists.join(' and ')} since they already exist."
        else
          items = exists[0...-1].empty? ? nil : exists[0...-1].join(', ')
          skipped = [items, exists[-1]].compact.join(' and ')
          Bundler.ui.warn "Skipped #{skipped} since they already exist."
        end
        Bundler.ui.warn "If you want to overwrite skipped stubs, use --force."
      end
    end

  private

    def can_install_parallely?
      min_rubygems = "2.0.7"
      if Bundler.current_ruby.mri? || Bundler.rubygems.provides?(">= #{min_rubygems}")
        true
      else
        Bundler.ui.warn "Rubygems #{Gem::VERSION} is not threadsafe, so your "\
          "gems must be installed one at a time. Upgrade to Rubygems " \
          "#{min_rubygems} or higher to enable parallel gem installation."
        false
      end
    end

    def generate_standalone_bundler_executable_stubs(spec)
      # double-assignment to avoid warnings about variables that will be used by ERB
      bin_path = Bundler.bin_path
      template = File.read(File.expand_path('../templates/Executable.standalone', __FILE__))
      ruby_command = ruby_command = Thor::Util.ruby_command

      spec.executables.each do |executable|
        next if executable == "bundle"
        standalone_path = standalone_path = Pathname(Bundler.settings[:path]).expand_path.relative_path_from(bin_path)
        executable_path = executable_path = Pathname(spec.full_gem_path).join(spec.bindir, executable).relative_path_from(bin_path)
        File.open "#{bin_path}/#{executable}", 'w', 0755 do |f|
          f.puts ERB.new(template, nil, '-').result(binding)
        end
      end
    end

    def generate_standalone(groups)
      standalone_path = Bundler.settings[:path]
      bundler_path = File.join(standalone_path, "bundler")
      FileUtils.mkdir_p(bundler_path)

      paths = []

      if groups.empty?
        specs = @definition.requested_specs
      else
        specs = @definition.specs_for groups.map { |g| g.to_sym }
      end

      specs.each do |spec|
        next if spec.name == "bundler"
        next if spec.require_paths.nil? # builtin gems

        spec.require_paths.each do |path|
          full_path = File.join(spec.full_gem_path, path)
          gem_path = Pathname.new(full_path).relative_path_from(Bundler.root.join(bundler_path))
          paths << gem_path.to_s.sub("#{Bundler.ruby_version.engine}/#{RbConfig::CONFIG['ruby_version']}", '#{ruby_engine}/#{ruby_version}')
        end
      end


      File.open File.join(bundler_path, "setup.rb"), "w" do |file|
        file.puts "require 'rbconfig'"
        file.puts "# ruby 1.8.7 doesn't define RUBY_ENGINE"
        file.puts "ruby_engine = defined?(RUBY_ENGINE) ? RUBY_ENGINE : 'ruby'"
        file.puts "ruby_version = RbConfig::CONFIG[\"ruby_version\"]"
        file.puts "path = File.expand_path('..', __FILE__)"
        paths.each do |path|
          file.puts %{$:.unshift File.expand_path("\#{path}/#{path}")}
        end
      end
    end

    def install_sequentially(standalone)
      specs.each do |spec|
        message = install_gem_from_spec spec, standalone, 0
        if message
          Installer.post_install_messages[spec.name] = message
        end
      end
    end

    def install_in_parallel(size, standalone)
      name2spec = {}
      remains = {}
      enqueued = {}
      specs.each do |spec|
        name2spec[spec.name] = spec
        remains[spec.name] = true
      end

      worker_pool = ParallelWorkers.worker_pool size, lambda { |name, worker|
        spec = name2spec[name]
        message = install_gem_from_spec spec, standalone, worker
        { :name => spec.name, :post_install => message }
      }

      # Keys in the remains hash represent uninstalled gems specs.
      # We enqueue all gem specs that do not have any dependencies.
      # Later we call this lambda again to install specs that depended on
      # previously installed specifications. We continue until all specs
      # are installed.
      enqueue_remaining_specs = lambda do
        remains.keys.each do |name|
          next if enqueued[name]
          spec = name2spec[name]
          if ready_to_install?(spec, remains)
            worker_pool.enq name
            enqueued[name] = true
          end
        end
      end
      enqueue_remaining_specs.call

      until remains.empty?
        message = worker_pool.deq
        remains.delete message[:name]
        if message[:post_install]
          Installer.post_install_messages[message[:name]] = message[:post_install]
        end
        enqueue_remaining_specs.call
      end
      message
    ensure
      worker_pool && worker_pool.stop
    end

    # We only want to install a gem spec if all its dependencies are met.
    # If the dependency is no longer in the `remains` hash then it has been met.
    # If a dependency is only development or is self referential it can be ignored.
    def ready_to_install?(spec, remains)
      spec.dependencies.none? do |dep|
        next if dep.type == :development || dep.name == spec.name
        remains[dep.name]
      end
    end

    def create_bundle_path
      Bundler.mkdir_p(Bundler.bundle_path.to_s) unless Bundler.bundle_path.exist?
    rescue Errno::EEXIST
      raise PathError, "Could not install to path `#{Bundler.settings[:path]}` " +
        "because of an invalid symlink. Remove the symlink so the directory can be created."
    end

  end
end
