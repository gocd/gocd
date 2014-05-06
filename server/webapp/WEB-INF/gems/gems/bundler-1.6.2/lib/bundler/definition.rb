require "digest/sha1"
require "set"

module Bundler
  class Definition
    include GemHelpers

    attr_reader :dependencies, :platforms, :sources, :ruby_version,
      :locked_deps

    # Given a gemfile and lockfile creates a Bundler definition
    #
    # @param gemfile [Pathname] Path to Gemfile
    # @param lockfile [Pathname,nil] Path to Gemfile.lock
    # @param unlock [Hash, Boolean, nil] Gems that have been requested
    #   to be updated or true if all gems should be updated
    # @return [Bundler::Definition]
    def self.build(gemfile, lockfile, unlock)
      unlock ||= {}
      gemfile = Pathname.new(gemfile).expand_path

      unless gemfile.file?
        raise GemfileNotFound, "#{gemfile} not found"
      end

      Dsl.evaluate(gemfile, lockfile, unlock)
    end


    #
    # How does the new system work?
    #
    # * Load information from Gemfile and Lockfile
    # * Invalidate stale locked specs
    #  * All specs from stale source are stale
    #  * All specs that are reachable only through a stale
    #    dependency are stale.
    # * If all fresh dependencies are satisfied by the locked
    #  specs, then we can try to resolve locally.
    #
    # @param lockfile [Pathname] Path to Gemfile.lock
    # @param dependencies [Array(Bundler::Dependency)] array of dependencies from Gemfile
    # @param sources [Array(Bundler::Source::Rubygems)]
    # @param unlock [Hash, Boolean, nil] Gems that have been requested
    #   to be updated or true if all gems should be updated
    # @param ruby_version [Bundler::RubyVersion, nil] Requested Ruby Version
    def initialize(lockfile, dependencies, sources, unlock, ruby_version = nil)
      @unlocking = unlock == true || !unlock.empty?

      @dependencies, @sources, @unlock = dependencies, sources, unlock
      @remote            = false
      @specs             = nil
      @lockfile_contents = ""
      @ruby_version      = ruby_version

      if lockfile && File.exist?(lockfile)
        @lockfile_contents = Bundler.read_file(lockfile)
        locked = LockfileParser.new(@lockfile_contents)
        @platforms      = locked.platforms

        if unlock != true
          @locked_deps    = locked.dependencies
          @locked_specs   = SpecSet.new(locked.specs)
          @locked_sources = locked.sources
        else
          @unlock         = {}
          @locked_deps    = []
          @locked_specs   = SpecSet.new([])
          @locked_sources = []
        end
      else
        @unlock         = {}
        @platforms      = []
        @locked_deps    = []
        @locked_specs   = SpecSet.new([])
        @locked_sources = []
      end

      @unlock[:gems] ||= []
      @unlock[:sources] ||= []

      current_platform = Bundler.rubygems.platforms.map { |p| generic(p) }.compact.last
      @new_platform = !@platforms.include?(current_platform)
      @platforms |= [current_platform]

      @path_changes = converge_paths
      eager_unlock = expand_dependencies(@unlock[:gems])
      @unlock[:gems] = @locked_specs.for(eager_unlock).map { |s| s.name }

      @source_changes = converge_sources
      @dependency_changes = converge_dependencies
      @local_changes = converge_locals

      fixup_dependency_types!
    end

    def fixup_dependency_types!
      # XXX This is a temporary workaround for a bug when using rubygems 1.8.15
      # where Gem::Dependency#== matches Gem::Dependency#type. As the lockfile
      # doesn't carry a notion of the dependency type, if you use
      # add_development_dependency in a gemspec that's loaded with the gemspec
      # directive, the lockfile dependencies and resolved dependencies end up
      # with a mismatch on #type.
      # Test coverage to catch a regression on this is in gemspec_spec.rb
      @dependencies.each do |d|
        if ld = @locked_deps.find { |l| l.name == d.name }
          ld.instance_variable_set(:@type, d.type)
        end
      end
    end

    def resolve_with_cache!
      raise "Specs already loaded" if @specs
      @sources.each { |s| s.cached! }
      specs
    end

    def resolve_remotely!
      raise "Specs already loaded" if @specs
      @remote = true
      @sources.each { |s| s.remote! }
      specs
    end

    # For given dependency list returns a SpecSet with Gemspec of all the required
    # dependencies.
    #  1. The method first resolves the dependencies specified in Gemfile
    #  2. After that it tries and fetches gemspec of resolved dependencies
    #
    # @return [Bundler::SpecSet]
    def specs
      @specs ||= begin
        specs = resolve.materialize(requested_dependencies)

        unless specs["bundler"].any?
          local = Bundler.settings[:frozen] ? rubygems_index : index
          bundler = local.search(Gem::Dependency.new('bundler', VERSION)).last
          specs["bundler"] = bundler if bundler
        end

        specs
      end
    end

    def new_specs
      specs - @locked_specs
    end

    def removed_specs
      @locked_specs - specs
    end

    def new_platform?
      @new_platform
    end

    def missing_specs
      missing = []
      resolve.materialize(requested_dependencies, missing)
      missing
    end

    def requested_specs
      @requested_specs ||= begin
        groups = self.groups - Bundler.settings.without
        groups.map! { |g| g.to_sym }
        specs_for(groups)
      end
    end

    def current_dependencies
      dependencies.reject { |d| !d.should_include? }
    end

    def specs_for(groups)
      deps = dependencies.select { |d| (d.groups & groups).any? }
      deps.delete_if { |d| !d.should_include? }
      specs.for(expand_dependencies(deps))
    end

    # Resolve all the dependencies specified in Gemfile. It ensures that
    # dependencies that have been already resolved via locked file and are fresh
    # are reused when resolving dependencies
    #
    # @return [SpecSet] resolved dependencies
    def resolve
      @resolve ||= begin
        if Bundler.settings[:frozen] || (!@unlocking && nothing_changed?)
          @locked_specs
        else
          last_resolve = converge_locked_specs

          # Record the specs available in each gem's source, so that those
          # specs will be available later when the resolver knows where to
          # look for that gemspec (or its dependencies)
          source_requirements = {}
          dependencies.each do |dep|
            next unless dep.source
            source_requirements[dep.name] = dep.source.specs
          end

          # Run a resolve against the locally available gems
          last_resolve.merge Resolver.resolve(expanded_dependencies, index, source_requirements, last_resolve)
        end
      end
    end

    def index
      @index ||= Index.build do |idx|
        dependency_names = @dependencies.dup || []
        dependency_names.map! {|d| d.name }

        @sources.each do |s|
          if s.is_a?(Bundler::Source::Rubygems)
            s.dependency_names = dependency_names.uniq
            idx.add_source s.specs
          else
            source_index = s.specs
            dependency_names += source_index.unmet_dependency_names
            idx.add_source source_index
          end
        end
      end
    end

    # used when frozen is enabled so we can find the bundler
    # spec, even if (say) a git gem is not checked out.
    def rubygems_index
      @rubygems_index ||= Index.build do |idx|
        rubygems = @sources.find{|s| s.is_a?(Source::Rubygems) }
        idx.add_source rubygems.specs
      end
    end

    def rubygems_remotes
      @sources.select{|s| s.is_a?(Source::Rubygems) }.map{|s| s.remotes }.flatten
    end

    def groups
      dependencies.map { |d| d.groups }.flatten.uniq
    end

    def lock(file)
      contents = to_lock

      # Convert to \r\n if the existing lock has them
      # i.e., Windows with `git config core.autocrlf=true`
      contents.gsub!(/\n/, "\r\n") if @lockfile_contents.match("\r\n")

      return if @lockfile_contents == contents

      if Bundler.settings[:frozen]
        Bundler.ui.error "Cannot write a changed lockfile while frozen."
        return
      end

      File.open(file, 'wb'){|f| f.puts(contents) }
    rescue Errno::EACCES
      raise Bundler::InstallError,
        "There was an error while trying to write to Gemfile.lock. It is likely that \n" \
        "you need to allow write permissions for the file at path: \n" \
        "#{File.expand_path(file)}"
    end

    def to_lock
      out = ""

      sorted_sources.each do |source|
        # Add the source header
        out << source.to_lock
        # Find all specs for this source
        resolve.
          select  { |s| s.source == source }.
          # This needs to be sorted by full name so that
          # gems with the same name, but different platform
          # are ordered consistently
          sort_by { |s| s.full_name }.
          each do |spec|
            next if spec.name == 'bundler'
            out << spec.to_lock
        end
        out << "\n"
      end

      out << "PLATFORMS\n"

      platforms.map { |p| p.to_s }.sort.each do |p|
        out << "  #{p}\n"
      end

      out << "\n"
      out << "DEPENDENCIES\n"

      handled = []
      dependencies.
        sort_by { |d| d.to_s }.
        each do |dep|
          next if handled.include?(dep.name)
          out << dep.to_lock
          handled << dep.name
      end

      out
    end

    def ensure_equivalent_gemfile_and_lockfile(explicit_flag = false)
      changes = false

      msg = "You are trying to install in deployment mode after changing\n" \
            "your Gemfile. Run `bundle install` elsewhere and add the\n" \
            "updated Gemfile.lock to version control."

      unless explicit_flag
        msg += "\n\nIf this is a development machine, remove the Gemfile " \
               "freeze \nby running `bundle install --no-deployment`."
      end

      added =   []
      deleted = []
      changed = []

      if @locked_sources != @sources
        new_sources = @sources - @locked_sources
        deleted_sources = @locked_sources - @sources

        if new_sources.any?
          added.concat new_sources.map { |source| "* source: #{source}" }
        end

        if deleted_sources.any?
          deleted.concat deleted_sources.map { |source| "* source: #{source}" }
        end

        changes = true
      end

      both_sources = Hash.new { |h,k| h[k] = ["no specified source", "no specified source"] }
      @dependencies.each { |d| both_sources[d.name][0] = d.source if d.source }
      @locked_deps.each  { |d| both_sources[d.name][1] = d.source if d.source }
      both_sources.delete_if { |k,v| v[0] == v[1] }

      if @dependencies != @locked_deps
        new_deps = @dependencies - @locked_deps
        deleted_deps = @locked_deps - @dependencies

        if new_deps.any?
          added.concat new_deps.map { |d| "* #{pretty_dep(d)}" }
        end

        if deleted_deps.any?
          deleted.concat deleted_deps.map { |d| "* #{pretty_dep(d)}" }
        end

        both_sources.each do |name, sources|
          changed << "* #{name} from `#{sources[0]}` to `#{sources[1]}`"
        end

        changes = true
      end

      msg << "\n\nYou have added to the Gemfile:\n"     << added.join("\n") if added.any?
      msg << "\n\nYou have deleted from the Gemfile:\n" << deleted.join("\n") if deleted.any?
      msg << "\n\nYou have changed in the Gemfile:\n"   << changed.join("\n") if changed.any?
      msg << "\n"

      raise ProductionError, msg if added.any? || deleted.any? || changed.any?
    end

    def validate_ruby!
      return unless ruby_version

      if diff = ruby_version.diff(Bundler.ruby_version)
        problem, expected, actual = diff

        msg = case problem
        when :engine
          "Your Ruby engine is #{actual}, but your Gemfile specified #{expected}"
        when :version
          "Your Ruby version is #{actual}, but your Gemfile specified #{expected}"
        when :engine_version
          "Your #{Bundler.ruby_version.engine} version is #{actual}, but your Gemfile specified #{ruby_version.engine} #{expected}"
        when :patchlevel
          if !expected.is_a?(String)
            "The Ruby patchlevel in your Gemfile must be a string"
          else
            "Your Ruby patchlevel is #{actual}, but your Gemfile specified #{expected}"
          end
        end

        raise RubyVersionMismatch, msg
      end
    end

  private

    def nothing_changed?
      !@source_changes && !@dependency_changes && !@new_platform && !@path_changes && !@local_changes
    end

    def pretty_dep(dep, source = false)
      msg  = "#{dep.name}"
      msg << " (#{dep.requirement})" unless dep.requirement == Gem::Requirement.default
      msg << " from the `#{dep.source}` source" if source && dep.source
      msg
    end

    # Check if the specs of the given source changed
    # according to the locked source. A block should be
    # in order to specify how the locked version of
    # the source should be found.
    def specs_changed?(source, &block)
      locked = @locked_sources.find(&block)

      if locked
        unlocking = locked.specs.any? do |spec|
          @locked_specs.any? do |locked_spec|
            locked_spec.source != locked
          end
        end
      end

      !locked || unlocking || source.specs != locked.specs
    end

    # Get all locals and override their matching sources.
    # Return true if any of the locals changed (for example,
    # they point to a new revision) or depend on new specs.
    def converge_locals
      locals = []

      Bundler.settings.local_overrides.map do |k,v|
        spec   = @dependencies.find { |s| s.name == k }
        source = spec && spec.source
        if source && source.respond_to?(:local_override!)
          source.unlock! if @unlock[:gems].include?(spec.name)
          locals << [ source, source.local_override!(v) ]
        end
      end

      locals.any? do |source, changed|
        changed || specs_changed?(source) { |o| source.class == o.class && source.uri == o.uri }
      end
    end

    def converge_paths
      @sources.any? do |source|
        next unless source.instance_of?(Source::Path)
        specs_changed?(source) do |ls|
          ls.class == source.class && ls.path == source.path
        end
      end
    end

    def converge_sources
      changes = false

      # Get the Rubygems source from the Gemfile.lock
      locked_gem = @locked_sources.find { |s| s.kind_of?(Source::Rubygems) }

      # Get the Rubygems source from the Gemfile
      actual_gem = @sources.find { |s| s.kind_of?(Source::Rubygems) }

      # If there is a Rubygems source in both
      if locked_gem && actual_gem
        # Merge the remotes from the Gemfile into the Gemfile.lock
        changes = changes | locked_gem.replace_remotes(actual_gem)
      end

      # Replace the sources from the Gemfile with the sources from the Gemfile.lock,
      # if they exist in the Gemfile.lock and are `==`. If you can't find an equivalent
      # source in the Gemfile.lock, use the one from the Gemfile.
      @sources.map! do |source|
        @locked_sources.find { |s| s == source } || source
      end
      changes = changes | (Set.new(@sources) != Set.new(@locked_sources))

      @sources.each do |source|
        # If the source is unlockable and the current command allows an unlock of
        # the source (for example, you are doing a `bundle update <foo>` of a git-pinned
        # gem), unlock it. For git sources, this means to unlock the revision, which
        # will cause the `ref` used to be the most recent for the branch (or master) if
        # an explicit `ref` is not used.
        if source.respond_to?(:unlock!) && @unlock[:sources].include?(source.name)
          source.unlock!
          changes = true
        end
      end

      changes
    end

    def converge_dependencies
      (@dependencies + @locked_deps).each do |dep|
        if dep.source
          dep.source = @sources.find { |s| dep.source == s }
        end
      end
      Set.new(@dependencies) != Set.new(@locked_deps)
    end

    # Remove elements from the locked specs that are expired. This will most
    # commonly happen if the Gemfile has changed since the lockfile was last
    # generated
    def converge_locked_specs
      deps = []

      # Build a list of dependencies that are the same in the Gemfile
      # and Gemfile.lock. If the Gemfile modified a dependency, but
      # the gem in the Gemfile.lock still satisfies it, this is fine
      # too.
      @dependencies.each do |dep|
        locked_dep = @locked_deps.find { |d| dep == d }

        if in_locked_deps?(dep, locked_dep) || satisfies_locked_spec?(dep)
          deps << dep
        elsif dep.source.is_a?(Source::Path) && dep.current_platform? && (!locked_dep || dep.source != locked_dep.source)
          @locked_specs.each do |s|
            @unlock[:gems] << s.name if s.source == dep.source
          end

          dep.source.unlock! if dep.source.respond_to?(:unlock!)
          dep.source.specs.each { |s| @unlock[:gems] << s.name }
        end
      end

      converged = []
      @locked_specs.each do |s|
        s.source = @sources.find { |src| s.source == src }

        # Don't add a spec to the list if its source is expired. For example,
        # if you change a Git gem to Rubygems.
        next if s.source.nil? || @unlock[:sources].include?(s.name)
        # If the spec is from a path source and it doesn't exist anymore
        # then we just unlock it.

        # Path sources have special logic
        if s.source.instance_of?(Source::Path)
          other = s.source.specs[s].first

          # If the spec is no longer in the path source, unlock it. This
          # commonly happens if the version changed in the gemspec
          next unless other

          deps2 = other.dependencies.select { |d| d.type != :development }
          # If the dependencies of the path source have changed, unlock it
          next unless s.dependencies.sort == deps2.sort
        end

        converged << s
      end

      resolve = SpecSet.new(converged)
      resolve = resolve.for(expand_dependencies(deps, true), @unlock[:gems])
      diff    = @locked_specs.to_a - resolve.to_a

      # Now, we unlock any sources that do not have anymore gems pinned to it
      @sources.each do |source|
        next unless source.respond_to?(:unlock!)

        unless resolve.any? { |s| s.source == source }
          source.unlock! if !diff.empty? && diff.any? { |s| s.source == source }
        end
      end

      resolve
    end

    def in_locked_deps?(dep, d)
      d && dep.source == d.source
    end

    def satisfies_locked_spec?(dep)
      @locked_specs.any? { |s| s.satisfies?(dep) && (!dep.source || s.source == dep.source) }
    end

    def expanded_dependencies
      @expanded_dependencies ||= expand_dependencies(dependencies, @remote)
    end

    def expand_dependencies(dependencies, remote = false)
      deps = []
      dependencies.each do |dep|
        dep = Dependency.new(dep, ">= 0") unless dep.respond_to?(:name)
        dep.gem_platforms(@platforms).each do |p|
          deps << DepProxy.new(dep, p) if remote || p == generic(Gem::Platform.local)
        end
      end
      deps
    end

    def sorted_sources
      @sources.sort_by do |s|
        # Place GEM at the top
        [ s.is_a?(Source::Rubygems) ? 1 : 0, s.to_s ]
      end
    end

    def requested_dependencies
      groups = self.groups - Bundler.settings.without
      groups.map! { |g| g.to_sym }
      dependencies.reject { |d| !d.should_include? || (d.groups & groups).empty? }
    end
  end
end
