require 'set'

# This is the latest iteration of the gem dependency resolving algorithm. As of now,
# it can resolve (as a success or failure) any set of gem dependencies we throw at it
# in a reasonable amount of time. The most iterations I've seen it take is about 150.
# The actual implementation of the algorithm is not as good as it could be yet, but that
# can come later.

module Bundler
  class Resolver

    require 'bundler/vendored_molinillo'

    class Molinillo::VersionConflict
      def clean_req(req)
        if req.to_s.include?(">= 0")
          req.to_s.gsub(/ \(.*?\)$/, '')
        else
          req.to_s.gsub(/\, (runtime|development)\)$/, ')')
        end
      end

      def message
        conflicts.values.flatten.reduce('') do |o, conflict|
          o << %(Bundler could not find compatible versions for gem "#{conflict.requirement.name}":\n)
          if conflict.locked_requirement
            o << %(  In snapshot (Gemfile.lock):\n)
            o << %(    #{clean_req conflict.locked_requirement}\n)
            o << %(\n)
          end
          o << %(  In Gemfile:\n)
          o << conflict.requirement_trees.map do |tree|
            t = ''
            depth = 2
            tree.each do |req|
              t << '  ' * depth << %(#{clean_req req})
              t << %( depends on) unless tree[-1] == req
              t << %(\n)
              depth += 1
            end
            t
          end.join("\n")

          if conflict.requirement.name == 'bundler'
            o << %(\n  Current Bundler version:\n    bundler (#{Bundler::VERSION}))
            other_bundler_required = !conflict.requirement.requirement.satisfied_by?(Gem::Version.new Bundler::VERSION)
          end

          if conflict.requirement.name == "bundler" && other_bundler_required
            o << "\n"
            o << "This Gemfile requires a different version of Bundler.\n"
            o << "Perhaps you need to update Bundler by running `gem install bundler`?\n"
          end
          if conflict.locked_requirement
            o << "\n"
            o << %(Running `bundle update` will rebuild your snapshot from scratch, using only\n)
            o << %(the gems in your Gemfile, which may resolve the conflict.\n)
          elsif !conflict.existing
            if conflict.requirement_trees.first.size > 1
              o << "Could not find gem '#{clean_req(conflict.requirement)}', which is required by "
              o << "gem '#{clean_req(conflict.requirement_trees.first[-2])}', in any of the sources."
            else
              o << "Could not find gem '#{clean_req(conflict.requirement)} in any of the sources\n"
            end
          end
          o
        end
      end
    end

    ALL = Bundler::Dependency::PLATFORM_MAP.values.uniq.freeze

    class SpecGroup < Array
      include GemHelpers

      attr_reader :activated, :required_by

      def initialize(a)
        super
        @required_by  = []
        @activated    = []
        @dependencies = nil
        @specs        = {}

        ALL.each do |p|
          @specs[p] = reverse.find { |s| s.match_platform(p) }
        end
      end

      def initialize_copy(o)
        super
        @required_by = o.required_by.dup
        @activated   = o.activated.dup
      end

      def to_specs
        specs = {}

        @activated.each do |p|
          if s = @specs[p]
            platform = generic(Gem::Platform.new(s.platform))
            next if specs[platform]

            lazy_spec = LazySpecification.new(name, version, platform, source)
            lazy_spec.dependencies.replace s.dependencies
            specs[platform] = lazy_spec
          end
        end
        specs.values
      end

      def activate_platform(platform)
        unless @activated.include?(platform)
          if for?(platform)
            @activated << platform
            return __dependencies[platform] || []
          end
        end
        []
      end

      def name
        @name ||= first.name
      end

      def version
        @version ||= first.version
      end

      def source
        @source ||= first.source
      end

      def for?(platform)
        @specs[platform]
      end

      def to_s
        "#{name} (#{version})"
      end

      def dependencies_for_activated_platforms
        @activated.map { |p| __dependencies[p] }.flatten
      end

      def platforms_for_dependency_named(dependency)
        __dependencies.select { |p, deps| deps.map(&:name).include? dependency }.keys
      end

    private

      def __dependencies
        @dependencies ||= begin
          dependencies = {}
          ALL.each do |p|
            if spec = @specs[p]
              dependencies[p] = []
              spec.dependencies.each do |dep|
                next if dep.type == :development
                dependencies[p] << DepProxy.new(dep, p)
              end
            end
          end
          dependencies
        end
      end
    end

    # Figures out the best possible configuration of gems that satisfies
    # the list of passed dependencies and any child dependencies without
    # causing any gem activation errors.
    #
    # ==== Parameters
    # *dependencies<Gem::Dependency>:: The list of dependencies to resolve
    #
    # ==== Returns
    # <GemBundle>,nil:: If the list of dependencies can be resolved, a
    #   collection of gemspecs is returned. Otherwise, nil is returned.
    def self.resolve(requirements, index, source_requirements = {}, base = [])
      base = SpecSet.new(base) unless base.is_a?(SpecSet)
      resolver = new(index, source_requirements, base)
      result = resolver.start(requirements)
      SpecSet.new(result)
    end


    def initialize(index, source_requirements, base)
      @index = index
      @source_requirements = source_requirements
      @base = base
      @resolver = Molinillo::Resolver.new(self, self)
      @search_for = {}
      @base_dg = Molinillo::DependencyGraph.new
      @base.each { |ls| @base_dg.add_root_vertex ls.name, Dependency.new(ls.name, ls.version) }
    end

    def start(requirements)
      verify_gemfile_dependencies_are_found!(requirements)
      dg = @resolver.resolve(requirements, @base_dg)
      dg.map(&:payload).map(&:to_specs).flatten
    rescue Molinillo::VersionConflict => e
      raise VersionConflict.new(e.conflicts.keys.uniq, e.message)
    rescue Molinillo::CircularDependencyError => e
      names = e.dependencies.sort_by(&:name).map { |d| "gem '#{d.name}'"}
      raise CyclicDependencyError, "Your Gemfile requires gems that depend" \
        " on each other, creating an infinite loop. Please remove" \
        " #{names.count > 1 ? 'either ' : '' }#{names.join(' or ')}" \
        " and try again."
    end

    include Molinillo::UI

    # Conveys debug information to the user.
    #
    # @param [Integer] depth the current depth of the resolution process.
    # @return [void]
    def debug(depth = 0)
      if debug?
        debug_info = yield
        debug_info = debug_info.inspect unless debug_info.is_a?(String)
        STDERR.puts debug_info.split("\n").map { |s| '  ' * depth + s }
      end
    end

    def debug?
      ENV['DEBUG_RESOLVER'] || ENV['DEBUG_RESOLVER_TREE']
    end

    def before_resolution
      Bundler.ui.info 'Resolving dependencies...', false
    end

    def after_resolution
      Bundler.ui.info ''
    end

    def indicate_progress
      Bundler.ui.info '.', false
    end

    private

    include Molinillo::SpecificationProvider

    def dependencies_for(specification)
      specification.dependencies_for_activated_platforms
    end

    def search_for(dependency)
      platform = dependency.__platform
      dependency = dependency.dep unless dependency.is_a? Gem::Dependency
      search = @search_for[dependency] ||= begin
        index = @source_requirements[dependency.name] || @index
        results = index.search(dependency, @base[dependency.name])
        if vertex = @base_dg.vertex_named(dependency.name)
          locked_requirement = vertex.payload.requirement
        end
        if results.any?
          version = results.first.version
          nested  = [[]]
          results.each do |spec|
            if spec.version != version
              nested << []
              version = spec.version
            end
            nested.last << spec
          end
          groups = nested.map { |a| SpecGroup.new(a) }
          !locked_requirement ? groups : groups.select { |sg| locked_requirement.satisfied_by? sg.version }
        else
          []
        end
      end
      search.select { |sg| sg.for?(platform) }.each { |sg| sg.activate_platform(platform) }
    end

    def name_for(dependency)
      dependency.name
    end

    def name_for_explicit_dependency_source
      'Gemfile'
    end

    def name_for_locking_dependency_source
      'Gemfile.lock'
    end

    def requirement_satisfied_by?(requirement, activated, spec)
      requirement.matches_spec?(spec)
    end

    def sort_dependencies(dependencies, activated, conflicts)
      dependencies.sort_by do |dependency|
        name = name_for(dependency)
        [
          activated.vertex_named(name).payload ? 0 : 1,
          amount_constrained(dependency),
          conflicts[name] ? 0 : 1,
          activated.vertex_named(name).payload ? 0 : search_for(dependency).count,
        ]
      end
    end

    def amount_constrained(dependency)
      @amount_constrained ||= {}
      @amount_constrained[dependency.name] ||= begin
        if base = @base[dependency.name] and !base.empty?
          dependency.requirement.satisfied_by?(base.first.version) ? 0 : 1
        else
          base_dep = Dependency.new dependency.name, '>= 0.a'
          all = search_for(DepProxy.new base_dep, dependency.__platform).size.to_f
          if all.zero?
            0
          elsif (search = search_for(dependency).size.to_f) == all && all == 1
            0
          else
            search / all
          end
        end
      end
    end

    def verify_gemfile_dependencies_are_found!(requirements)
      requirements.each do |requirement|
        next if requirement.name == 'bundler'
        if search_for(requirement).empty?
          if base = @base[requirement.name] and !base.empty?
            version = base.first.version
            message = "You have requested:\n" \
              "  #{requirement.name} #{requirement.requirement}\n\n" \
              "The bundle currently has #{requirement.name} locked at #{version}.\n" \
              "Try running `bundle update #{requirement.name}`"
          elsif requirement.source
            name = requirement.name
            versions = @source_requirements[name][name].map { |s| s.version }
            message  = "Could not find gem '#{requirement}' in #{requirement.source}.\n"
            if versions.any?
              message << "Source contains '#{name}' at: #{versions.join(', ')}"
            else
              message << "Source does not contain any versions of '#{requirement}'"
            end
          else
            message = "Could not find gem '#{requirement}' in any of the gem sources " \
              "listed in your Gemfile or available on this machine."
          end
          raise GemNotFound, message
        end
      end
    end

  end
end
