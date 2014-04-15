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
      result = catch(:success) do
        resolver.start(requirements)
        raise resolver.version_conflict
        nil
      end
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

    def resolve(reqs, activated)
      # If the requirements are empty, then we are in a success state. Aka, all
      # gem dependencies have been resolved.
      throw :success, successify(activated) if reqs.empty?

      indicate_progress

      debug { print "\e[2J\e[f" ; "==== Iterating ====\n\n" }

      # Sort dependencies so that the ones that are easiest to resolve are first.
      # Easiest to resolve is defined by:
      #   1) Is this gem already activated?
      #   2) Do the version requirements include prereleased gems?
      #   3) Sort by number of gems available in the source.
      reqs = reqs.sort_by do |a|
        [ activated[a.name] ? 0 : 1,
          a.requirement.prerelease? ? 0 : 1,
          @errors[a.name]   ? 0 : 1,
          activated[a.name] ? 0 : @gems_size[a] ]
      end

      debug { "Activated:\n" + activated.values.map {|a| "  #{a}" }.join("\n") }
      debug { "Requirements:\n" + reqs.map {|r| "  #{r}"}.join("\n") }

      activated = activated.dup

      # Pull off the first requirement so that we can resolve it
      current = reqs.shift

      debug { "Attempting:\n  #{current}"}

      # Check if the gem has already been activated, if it has, we will make sure
      # that the currently activated gem satisfies the requirement.
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
          # Since the current requirement is satisfied, we can continue resolving
          # the remaining requirements.

          # I have no idea if this is the right way to do it, but let's see if it works
          # The current requirement might activate some other platforms, so let's try
          # adding those requirements here.
          dependencies = existing.activate_platform(current.__platform)
          reqs.concat dependencies

          dependencies.each do |dep|
            next if dep.type == :development
            @gems_size[dep] ||= gems_size(dep)
          end

          resolve(reqs, activated)
        else
          debug { "    * [FAIL] Already activated" }
          @errors[existing.name] = [existing, current]
          debug { current.required_by.map {|d| "      * #{d.name} (#{d.requirement})" }.join("\n") }
          # debug { "    * All current conflicts:\n" + @errors.keys.map { |c| "      - #{c}" }.join("\n") }
          # Since the current requirement conflicts with an activated gem, we need
          # to backtrack to the current requirement's parent and try another version
          # of it (maybe the current requirement won't be present anymore). If the
          # current requirement is a root level requirement, we need to jump back to
          # where the conflicting gem was activated.
          parent = current.required_by.last
          # `existing` could not respond to required_by if it is part of the base set
          # of specs that was passed to the resolver (aka, instance of LazySpecification)
          parent ||= existing.required_by.last if existing.respond_to?(:required_by)
          # We track the spot where the current gem was activated because we need
          # to keep a list of every spot a failure happened.
          if parent && parent.name != 'bundler'
            debug { "    -> Jumping to: #{parent.name}" }
            required_by = existing.respond_to?(:required_by) && existing.required_by.last
            throw parent.name, required_by && required_by.name
          else
            # The original set of dependencies conflict with the base set of specs
            # passed to the resolver. This is by definition an impossible resolve.
            raise version_conflict
          end
        end
      else
        # There are no activated gems for the current requirement, so we are going
        # to find all gems that match the current requirement and try them in decending
        # order. We also need to keep a set of all conflicts that happen while trying
        # this gem. This is so that if no versions work, we can figure out the best
        # place to backtrack to.
        conflicts = Set.new

        # Fetch all gem versions matching the requirement
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
          end
        end

        matching_versions.reverse_each do |spec_group|
          conflict = resolve_requirement(spec_group, current, reqs.dup, activated.dup)
          conflicts << conflict if conflict
        end

        # We throw the conflict up the dependency chain if it has not been
        # resolved (in @errors), thus avoiding branches of the tree that have no effect
        # on this conflict.  Note that if the tree has multiple conflicts, we don't
        # care which one we throw, as long as we get out safe
        if !current.required_by.empty? && !conflicts.empty?
          @errors.reverse_each do |req_name, pair|
            if conflicts.include?(req_name)
              # Choose the closest pivot in the stack that will affect the conflict
              errorpivot = (@stack & [req_name, current.required_by.last.name]).last
              debug { "    -> Jumping to: #{errorpivot}" }
              throw errorpivot, req_name
            end
          end
        end

        # If the current requirement is a root level gem and we have conflicts, we
        # can figure out the best spot to backtrack to.
        if current.required_by.empty? && !conflicts.empty?
          # Check the current "catch" stack for the first one that is included in the
          # conflicts set. That is where the parent of the conflicting gem was required.
          # By jumping back to this spot, we can try other version of the parent of
          # the conflicting gem, hopefully finding a combination that activates correctly.
          @stack.reverse_each do |savepoint|
            if conflicts.include?(savepoint)
              debug { "    -> Jumping to: #{savepoint}" }
              throw savepoint
            end
          end
        end
      end
    end

    def resolve_requirement(spec_group, requirement, reqs, activated)
      # We are going to try activating the spec. We need to keep track of stack of
      # requirements that got us to the point of activating this gem.
      spec_group.required_by.replace requirement.required_by
      spec_group.required_by << requirement

      activated[spec_group.name] = spec_group
      debug { "  Activating: #{spec_group.name} (#{spec_group.version})" }
      debug { spec_group.required_by.map { |d| "    * #{d.name} (#{d.requirement})" }.join("\n") }

      dependencies = spec_group.activate_platform(requirement.__platform)

      # Now, we have to loop through all child dependencies and add them to our
      # array of requirements.
      debug { "    Dependencies"}
      dependencies.each do |dep|
        next if dep.type == :development
        debug { "    * #{dep.name} (#{dep.requirement})" }
        dep.required_by.replace(requirement.required_by)
        dep.required_by << requirement
        @gems_size[dep] ||= gems_size(dep)
        reqs << dep
      end

      # We create a savepoint and mark it by the name of the requirement that caused
      # the gem to be activated. If the activated gem ever conflicts, we are able to
      # jump back to this point and try another version of the gem.
      length = @stack.length
      @stack << requirement.name
      retval = catch(requirement.name) do
        # try to resolve the next option
        resolve(reqs, activated)
      end

      # clear the search cache since the catch means we couldn't meet the
      # requirement we need with the current constraints on search
      clear_search_cache

      # Since we're doing a lot of throw / catches. A push does not necessarily match
      # up to a pop. So, we simply slice the stack back to what it was before the catch
      # block.
      @stack.slice!(length..-1)
      retval
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
    def gem_message(requirement)
      m = ""

      # A requirement that is required by itself is actually in the Gemfile, and does
      # not "depend on" itself
      if requirement.required_by.first && requirement.required_by.first.name != requirement.name
        m << "    #{clean_req(requirement.required_by.first)} depends on\n"
        m << "      #{clean_req(requirement)}\n"
      else
        m << "    #{clean_req(requirement)}\n"
      end
      m << "\n"
    end

    def error_message
      errors.inject("") do |o, (conflict, (origin, requirement))|

        # origin is the SpecSet of specs from the Gemfile that is conflicted with
        if origin

          o << %{Bundler could not find compatible versions for gem "#{origin.name}":\n}
          o << "  In Gemfile:\n"

          o << gem_message(requirement)

          # If the origin is "bundler", the conflict is us
          if origin.name == "bundler"
            o << "  Current Bundler version:\n"
            other_bundler_required = !requirement.requirement.satisfied_by?(origin.version)
          # If the origin is a LockfileParser, it does not respond_to :required_by
          elsif !origin.respond_to?(:required_by) || !(origin.required_by.first)
            o << "  In snapshot (Gemfile.lock):\n"
          end

          o << gem_message(origin)

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
            o << gem_message(requirement)
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
    # aproximately every second. iteration_rate is calculated in the first
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
