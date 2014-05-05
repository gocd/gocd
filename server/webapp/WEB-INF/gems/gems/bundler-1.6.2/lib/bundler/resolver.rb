require 'set'
# This is the latest iteration of the gem dependency resolving algorithm. As of now,
# it can resolve (as a success or failure) any set of gem dependencies we throw at it
# in a reasonable amount of time. The most iterations I've seen it take is about 150.
# The actual implementation of the algorithm is not as good as it could be yet, but that
# can come later.

# Extending Gem classes to add necessary tracking information
module Gem
  class Specification
    def required_by
      @required_by ||= []
    end
  end
  class Dependency
    def required_by
      @required_by ||= []
    end
  end
end

module Bundler
  class Resolver

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
          @activated << platform
          return __dependencies[platform] || []
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

    attr_reader :errors, :started_at, :iteration_rate, :iteration_counter

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
      Bundler.ui.info "Resolving dependencies...", false
      base = SpecSet.new(base) unless base.is_a?(SpecSet)
      resolver = new(index, source_requirements, base)
      result = resolver.start(requirements)
      Bundler.ui.info "" # new line now that dots are done
      SpecSet.new(result)
    rescue => e
      Bundler.ui.info "" # new line before the error
      raise e
    end

    def initialize(index, source_requirements, base)
      @errors               = {}
      @stack                = []
      @base                 = base
      @index                = index
      @deps_for             = {}
      @missing_gems         = Hash.new(0)
      @source_requirements  = source_requirements
      @iteration_counter    = 0
      @started_at           = Time.now
    end

    def debug
      if ENV['DEBUG_RESOLVER']
        debug_info = yield
        debug_info = debug_info.inspect unless debug_info.is_a?(String)
        $stderr.puts debug_info
      end
    end

    def successify(activated)
      activated.values.map { |s| s.to_specs }.flatten.compact
    end

    def start(reqs)
      activated = {}
      @gems_size = Hash[reqs.map { |r| [r, gems_size(r)] }]

      resolve(reqs, activated)
    end

    class State < Struct.new(:reqs, :activated, :requirement, :possibles, :depth)
      def name
        requirement.name
      end
    end

    def handle_conflict(current, states, existing=nil)
      until current.nil? && existing.nil?
        current_state = find_state(current, states)
        existing_state = find_state(existing, states)
        return current if state_any?(current_state)
        return existing if state_any?(existing_state)
        existing = existing.required_by.last if existing
        current = current.required_by.last if current
      end
    end

    def state_any?(state)
      state && state.possibles.any?
    end

    def find_state(current, states)
      states.detect { |i| current && current.name == i.name }
    end

    def other_possible?(conflict, states)
      return unless conflict
      state = states.detect { |i| i.name == conflict.name }
      state && state.possibles.any?
    end

    def find_conflict_state(conflict, states)
      return unless conflict
      until states.empty? do
        state = states.pop
        return state if conflict.name == state.name
      end
    end

    def activate_gem(reqs, activated, requirement, current)
      requirement.required_by.replace current.required_by
      requirement.required_by << current
      activated[requirement.name] = requirement

      debug { "  Activating: #{requirement.name} (#{requirement.version})" }
      debug { requirement.required_by.map { |d| "    * #{d.name} (#{d.requirement})" }.join("\n") }

      dependencies = requirement.activate_platform(current.__platform)

      debug { "    Dependencies"}
      dependencies.each do |dep|
        next if dep.type == :development
        dep.required_by.replace(current.required_by)
        dep.required_by << current
        @gems_size[dep] ||= gems_size(dep)
        reqs << dep
      end
    end

    def resolve_for_conflict(state)
      raise version_conflict if state.nil? || state.possibles.empty?
      reqs, activated, depth = state.reqs.dup, state.activated.dup, state.depth
      requirement = state.requirement
      possible = state.possibles.pop

      activate_gem(reqs, activated, possible, requirement)

      return reqs, activated, depth
    end

    def resolve_conflict(current, states)
      # Find the state where the conflict has occurred
      state = find_conflict_state(current, states)

      debug { "    -> Going to: #{current.name} state" } if current

      # Resolve the conflicts by rewinding the state
      # when the conflicted gem was activated
      reqs, activated, depth = resolve_for_conflict(state)

      # Keep the state around if it still has other possibilities
      states << state unless state.possibles.empty?
      clear_search_cache

      return reqs, activated, depth
    end

    def resolve(reqs, activated)
      states = []
      depth = 0

      until reqs.empty?

        indicate_progress

        debug { print "\e[2J\e[f" ; "==== Iterating ====\n\n" }

        reqs = reqs.sort_by do |a|
          [ activated[a.name] ? 0 : 1,
            a.requirement.prerelease? ? 0 : 1,
            @errors[a.name]   ? 0 : 1,
            activated[a.name] ? 0 : @gems_size[a] ]
        end

        debug { "Activated:\n" + activated.values.map {|a| "  #{a}" }.join("\n") }
        debug { "Requirements:\n" + reqs.map {|r| "  #{r}"}.join("\n") }

        current = reqs.shift

        $stderr.puts "#{' ' * depth}#{current}" if ENV['DEBUG_RESOLVER_TREE']

        debug { "Attempting:\n  #{current}"}

        existing = activated[current.name]


        if existing || current.name == 'bundler'
          # Force the current
          if current.name == 'bundler' && !existing
            existing = search(DepProxy.new(Gem::Dependency.new('bundler', VERSION), Gem::Platform::RUBY)).first
            raise GemNotFound, %Q{Bundler could not find gem "bundler" (#{VERSION})} unless existing
            existing.required_by << existing
            activated['bundler'] = existing
          end

          if current.requirement.satisfied_by?(existing.version)
            debug { "    * [SUCCESS] Already activated" }
            @errors.delete(existing.name)
            dependencies = existing.activate_platform(current.__platform)
            reqs.concat dependencies

            dependencies.each do |dep|
              next if dep.type == :development
              @gems_size[dep] ||= gems_size(dep)
            end

            depth += 1
            next
          else
            debug { "    * [FAIL] Already activated" }
            @errors[existing.name] = [existing, current]

            parent = current.required_by.last
            if existing.respond_to?(:required_by)
              parent = handle_conflict(current, states, existing.required_by[-2]) unless other_possible?(parent, states)
            else
              parent = handle_conflict(current, states) unless other_possible?(parent, states)
            end

            raise version_conflict if parent.nil? || parent.name == 'bundler'


            reqs, activated, depth = resolve_conflict(parent, states)
          end
        else
          matching_versions = search(current)

          # If we found no versions that match the current requirement
          if matching_versions.empty?
            # If this is a top-level Gemfile requirement
            if current.required_by.empty?
              if base = @base[current.name] and !base.empty?
                version = base.first.version
                message = "You have requested:\n" \
                  "  #{current.name} #{current.requirement}\n\n" \
                  "The bundle currently has #{current.name} locked at #{version}.\n" \
                  "Try running `bundle update #{current.name}`"
              elsif current.source
                name = current.name
                versions = @source_requirements[name][name].map { |s| s.version }
                message  = "Could not find gem '#{current}' in #{current.source}.\n"
                if versions.any?
                  message << "Source contains '#{name}' at: #{versions.join(', ')}"
                else
                  message << "Source does not contain any versions of '#{current}'"
                end
              else
                message = "Could not find gem '#{current}' "
                if @index.source_types.include?(Bundler::Source::Rubygems)
                  message << "in any of the gem sources listed in your Gemfile."
                else
                  message << "in the gems available on this machine."
                end
              end
              raise GemNotFound, message
              # This is not a top-level Gemfile requirement
            else
              @errors[current.name] = [nil, current]
              parent = handle_conflict(current, states)
              reqs, activated, depth = resolve_conflict(parent, states)
              next
            end
          end

          state = State.new(reqs.dup, activated.dup, current, matching_versions, depth)
          states << state
          requirement = state.possibles.pop
          activate_gem(reqs, activated, requirement, current)
        end
      end
      successify(activated)
    end

    def gems_size(dep)
      search(dep).size
    end

    def clear_search_cache
      @deps_for = {}
    end

    def search(dep)
      if base = @base[dep.name] and base.any?
        reqs = [dep.requirement.as_list, base.first.version.to_s].flatten.compact
        d = Gem::Dependency.new(base.first.name, *reqs)
      else
        d = dep.dep
      end

      @deps_for[d.hash] ||= begin
        index = @source_requirements[d.name] || @index
        results = index.search(d, @base[d.name])

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
          deps = nested.map{|a| SpecGroup.new(a) }.select{|sg| sg.for?(dep.__platform) }
        else
          deps = []
        end
      end
    end

    def clean_req(req)
      if req.to_s.include?(">= 0")
        req.to_s.gsub(/ \(.*?\)$/, '')
      else
        req.to_s.gsub(/\, (runtime|development)\)$/, ')')
      end
    end

    def version_conflict
      VersionConflict.new(errors.keys, error_message)
    end

    # For a given conflicted requirement, print out what exactly went wrong
    def gem_message(requirement, required_by=[])
      m = ""

      # A requirement that is required by itself is actually in the Gemfile, and does
      # not "depend on" itself
      if requirement.required_by.first && requirement.required_by.first.name != requirement.name
        dependency_tree(m, required_by)
        m << "#{clean_req(requirement)}\n"
      else
        m << "    #{clean_req(requirement)}\n"
      end
      m << "\n"
    end

    def dependency_tree(m, requirements)
      requirements.each_with_index do |i, j|
        m << "    " << ("  " * j)
        m << "#{clean_req(i)}"
        m << " depends on\n"
      end
      m << "    " << ("  " * requirements.size)
    end

    def error_message
      errors.inject("") do |o, (conflict, (origin, requirement))|

        # origin is the SpecSet of specs from the Gemfile that is conflicted with
        if origin

          o << %{Bundler could not find compatible versions for gem "#{origin.name}":\n}
          o << "  In Gemfile:\n"

          required_by = requirement.required_by
          o << gem_message(requirement, required_by)

          # If the origin is "bundler", the conflict is us
          if origin.name == "bundler"
            o << "  Current Bundler version:\n"
            other_bundler_required = !requirement.requirement.satisfied_by?(origin.version)
          # If the origin is a LockfileParser, it does not respond_to :required_by
          elsif !origin.respond_to?(:required_by) || !(origin.required_by.first)
            o << "  In snapshot (Gemfile.lock):\n"
          end

          required_by = origin.required_by[0..-2]
          o << gem_message(origin, required_by)

          # If the bundle wants a newer bundler than the running bundler, explain
          if origin.name == "bundler" && other_bundler_required
            o << "This Gemfile requires a different version of Bundler.\n"
            o << "Perhaps you need to update Bundler by running `gem install bundler`?"
          end

        # origin is nil if the required gem and version cannot be found in any of
        # the specified sources
        else

          # if the gem cannot be found because of a version conflict between lockfile and gemfile,
          # print a useful error that suggests running `bundle update`, which may fix things
          #
          # @base is a SpecSet of the gems in the lockfile
          # conflict is the name of the gem that could not be found
          if locked = @base[conflict].first
            o << "Bundler could not find compatible versions for gem #{conflict.inspect}:\n"
            o << "  In snapshot (Gemfile.lock):\n"
            o << "    #{clean_req(locked)}\n\n"

            o << "  In Gemfile:\n"

            required_by = requirement.required_by
            o << gem_message(requirement, required_by)
            o << "Running `bundle update` will rebuild your snapshot from scratch, using only\n"
            o << "the gems in your Gemfile, which may resolve the conflict.\n"

          # the rest of the time, the gem cannot be found because it does not exist in the known sources
          else
            if requirement.required_by.first
              o << "Could not find gem '#{clean_req(requirement)}', which is required by "
              o << "gem '#{clean_req(requirement.required_by.first)}', in any of the sources."
            else
              o << "Could not find gem '#{clean_req(requirement)} in any of the sources\n"
            end
          end

        end
        o
      end
    end

    private

    # Indicates progress by writing a '.' every iteration_rate time which is
    # approximately every second. iteration_rate is calculated in the first
    # second of resolve running.
    def indicate_progress
      @iteration_counter += 1

      if iteration_rate.nil?
        if ((Time.now - started_at) % 3600).round >= 1
          @iteration_rate = iteration_counter
        end
      else
        if ((iteration_counter % iteration_rate) == 0)
          Bundler.ui.info ".", false
        end
      end
    end
  end
end
