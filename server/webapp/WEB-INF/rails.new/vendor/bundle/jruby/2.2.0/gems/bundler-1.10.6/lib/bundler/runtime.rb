require "digest/sha1"

module Bundler
  class Runtime < Environment
    include SharedHelpers

    def setup(*groups)
      groups.map! { |g| g.to_sym }

      # Has to happen first
      clean_load_path

      specs = groups.any? ? @definition.specs_for(groups) : requested_specs

      setup_environment
      Bundler.rubygems.replace_entrypoints(specs)

      # Activate the specs
      specs.each do |spec|
        unless spec.loaded_from
          raise GemNotFound, "#{spec.full_name} is missing. Run `bundle` to get it."
        end

        if activated_spec = Bundler.rubygems.loaded_specs(spec.name) and activated_spec.version != spec.version
          e = Gem::LoadError.new "You have already activated #{activated_spec.name} #{activated_spec.version}, " \
                                 "but your Gemfile requires #{spec.name} #{spec.version}. Prepending " \
                                 "`bundle exec` to your command may solve this."
          e.name = spec.name
          if e.respond_to?(:requirement=)
            e.requirement = Gem::Requirement.new(spec.version.to_s)
          else
            e.version_requirement = Gem::Requirement.new(spec.version.to_s)
          end
          raise e
        end

        Bundler.rubygems.mark_loaded(spec)
        load_paths = spec.load_paths.reject {|path| $LOAD_PATH.include?(path)}
        $LOAD_PATH.unshift(*load_paths)
      end

      setup_manpath

      lock(:preserve_bundled_with => true)

      self
    end

    REQUIRE_ERRORS = [
      /^no such file to load -- (.+)$/i,
      /^Missing \w+ (?:file\s*)?([^\s]+.rb)$/i,
      /^Missing API definition file in (.+)$/i,
      /^cannot load such file -- (.+)$/i,
      /^dlopen\([^)]*\): Library not loaded: (.+)$/i,
    ]

    def require(*groups)
      groups.map! { |g| g.to_sym }
      groups = [:default] if groups.empty?

      @definition.dependencies.each do |dep|
        # Skip the dependency if it is not in any of the requested
        # groups
        next unless ((dep.groups & groups).any? && dep.current_platform?)

        required_file = nil

        begin
          # Loop through all the specified autorequires for the
          # dependency. If there are none, use the dependency's name
          # as the autorequire.
          Array(dep.autorequire || dep.name).each do |file|
            # Allow `require: true` as an alias for `require: <name>`
            file = dep.name if file == true
            required_file = file
            Kernel.require file
          end
        rescue LoadError => e
          REQUIRE_ERRORS.find { |r| r =~ e.message }
          raise if dep.autorequire || $1 != required_file

          if dep.autorequire.nil? && dep.name.include?('-')
            begin
              namespaced_file = dep.name.gsub('-', '/')
              Kernel.require namespaced_file
            rescue LoadError => e
              REQUIRE_ERRORS.find { |r| r =~ e.message }
              raise if $1 != namespaced_file
            end
          end
        end
      end
    end

    def dependencies_for(*groups)
      if groups.empty?
        dependencies
      else
        dependencies.select { |d| (groups & d.groups).any? }
      end
    end

    alias gems specs

    def cache(custom_path = nil)
      cache_path = Bundler.app_cache(custom_path)
      FileUtils.mkdir_p(cache_path) unless File.exist?(cache_path)

      Bundler.ui.info "Updating files in #{Bundler.settings.app_cache_path}"
      specs.each do |spec|
        next if spec.name == 'bundler'
        spec.source.send(:fetch_gem, spec) if Bundler.settings[:cache_all_platforms] && spec.source.respond_to?(:fetch_gem, true)
        spec.source.cache(spec, custom_path) if spec.source.respond_to?(:cache)
      end

      Dir[cache_path.join("*/.git")].each do |git_dir|
        FileUtils.rm_rf(git_dir)
        FileUtils.touch(File.expand_path("../.bundlecache", git_dir))
      end

      prune_cache(cache_path) unless Bundler.settings[:no_prune]
    end

    def prune_cache(cache_path)
      FileUtils.mkdir_p(cache_path) unless File.exist?(cache_path)
      resolve = @definition.resolve
      prune_gem_cache(resolve, cache_path)
      prune_git_and_path_cache(resolve, cache_path)
    end

    def clean(dry_run = false)
      gem_bins             = Dir["#{Gem.dir}/bin/*"]
      git_dirs             = Dir["#{Gem.dir}/bundler/gems/*"]
      git_cache_dirs       = Dir["#{Gem.dir}/cache/bundler/git/*"]
      gem_dirs             = Dir["#{Gem.dir}/gems/*"]
      gem_files            = Dir["#{Gem.dir}/cache/*.gem"]
      gemspec_files        = Dir["#{Gem.dir}/specifications/*.gemspec"]
      spec_gem_paths       = []
      # need to keep git sources around
      spec_git_paths       = @definition.spec_git_paths
      spec_git_cache_dirs  = []
      spec_gem_executables = []
      spec_cache_paths     = []
      spec_gemspec_paths   = []
      specs.each do |spec|
        spec_gem_paths << spec.full_gem_path
        # need to check here in case gems are nested like for the rails git repo
        md = %r{(.+bundler/gems/.+-[a-f0-9]{7,12})}.match(spec.full_gem_path)
        spec_git_paths << md[1] if md
        spec_gem_executables << spec.executables.collect do |executable|
          e = "#{Bundler.rubygems.gem_bindir}/#{executable}"
          [e, "#{e}.bat"]
        end
        spec_cache_paths << spec.cache_file
        spec_gemspec_paths << spec.spec_file
        spec_git_cache_dirs << spec.source.cache_path.to_s if spec.source.is_a?(Bundler::Source::Git)
      end
      spec_gem_paths.uniq!
      spec_gem_executables.flatten!

      stale_gem_bins       = gem_bins - spec_gem_executables
      stale_git_dirs       = git_dirs - spec_git_paths
      stale_git_cache_dirs = git_cache_dirs - spec_git_cache_dirs
      stale_gem_dirs       = gem_dirs - spec_gem_paths
      stale_gem_files      = gem_files - spec_cache_paths
      stale_gemspec_files  = gemspec_files - spec_gemspec_paths

      output = stale_gem_dirs.collect do |gem_dir|
        full_name = Pathname.new(gem_dir).basename.to_s

        parts   = full_name.split('-')
        name    = parts[0..-2].join('-')
        version = parts.last
        output  = "#{name} (#{version})"

        if dry_run
          Bundler.ui.info "Would have removed #{output}"
        else
          Bundler.ui.info "Removing #{output}"
          FileUtils.rm_rf(gem_dir)
        end

        output
      end + stale_git_dirs.collect do |gem_dir|
        full_name = Pathname.new(gem_dir).basename.to_s

        parts    = full_name.split('-')
        name     = parts[0..-2].join('-')
        revision = parts[-1]
        output   = "#{name} (#{revision})"

        if dry_run
          Bundler.ui.info "Would have removed #{output}"
        else
          Bundler.ui.info "Removing #{output}"
          FileUtils.rm_rf(gem_dir)
        end

        output
      end

      unless dry_run
        stale_gem_bins.each { |bin| FileUtils.rm(bin) if File.exist?(bin) }
        stale_gem_files.each { |file| FileUtils.rm(file) if File.exist?(file) }
        stale_gemspec_files.each { |file| FileUtils.rm(file) if File.exist?(file) }
        stale_git_cache_dirs.each { |dir| FileUtils.rm_rf(dir) if File.exist?(dir) }
      end

      output
    end

    def setup_environment
      begin
        ENV["BUNDLE_BIN_PATH"] = Bundler.rubygems.bin_path("bundler", "bundle", VERSION)
      rescue Gem::GemNotFoundException
        ENV["BUNDLE_BIN_PATH"] = File.expand_path("../../../bin/bundle", __FILE__)
      end

      # Set BUNDLE_GEMFILE
      ENV["BUNDLE_GEMFILE"] = default_gemfile.to_s

      SharedHelpers.set_bundle_environment
    end

  private

    def prune_gem_cache(resolve, cache_path)
      cached  = Dir["#{cache_path}/*.gem"]

      cached = cached.delete_if do |path|
        spec = Bundler.rubygems.spec_from_gem path

        resolve.any? do |s|
          s.name == spec.name && s.version == spec.version && !s.source.is_a?(Bundler::Source::Git)
        end
      end

      if cached.any?
        Bundler.ui.info "Removing outdated .gem files from #{Bundler.settings.app_cache_path}"

        cached.each do |path|
          Bundler.ui.info "  * #{File.basename(path)}"
          File.delete(path)
        end
      end
    end

    def prune_git_and_path_cache(resolve, cache_path)
      cached  = Dir["#{cache_path}/*/.bundlecache"]

      cached = cached.delete_if do |path|
        name = File.basename(File.dirname(path))

        resolve.any? do |s|
          source = s.source
          source.respond_to?(:app_cache_dirname) && source.app_cache_dirname == name
        end
      end

      if cached.any?
        Bundler.ui.info "Removing outdated git and path gems from #{Bundler.settings.app_cache_path}"

        cached.each do |path|
          path = File.dirname(path)
          Bundler.ui.info "  * #{File.basename(path)}"
          FileUtils.rm_rf(path)
        end
      end
    end

    def setup_manpath
      # Store original MANPATH for restoration later in with_clean_env()
      ENV['BUNDLE_ORIG_MANPATH'] = ENV['MANPATH']

      # Add man/ subdirectories from activated bundles to MANPATH for man(1)
      manuals = $LOAD_PATH.map do |path|
        man_subdir = path.sub(/lib$/, 'man')
        man_subdir unless Dir[man_subdir + '/man?/'].empty?
      end.compact

      unless manuals.empty?
        ENV['MANPATH'] = manuals.concat(
          ENV['MANPATH'].to_s.split(File::PATH_SEPARATOR)
        ).uniq.join(File::PATH_SEPARATOR)
      end
    end

  end
end
