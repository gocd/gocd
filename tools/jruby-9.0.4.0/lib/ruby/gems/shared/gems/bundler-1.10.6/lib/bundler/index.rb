require "set"

module Bundler
  class Index
    include Enumerable

    def self.build
      i = new
      yield i
      i
    end

    attr_reader :specs, :all_specs, :sources
    protected   :specs, :all_specs

    def initialize
      @sources = []
      @cache = {}
      @specs = Hash.new { |h,k| h[k] = Hash.new }
      @all_specs = Hash.new { |h,k| h[k] = [] }
    end

    def initialize_copy(o)
      super
      @sources = @sources.dup
      @cache = {}
      @specs = Hash.new { |h,k| h[k] = Hash.new }
      @all_specs = Hash.new { |h,k| h[k] = [] }

      o.specs.each do |name, hash|
        @specs[name] = hash.dup
      end
      o.all_specs.each do |name, array|
        @all_specs[name] = array.dup
      end
    end

    def inspect
      "#<#{self.class}:0x#{object_id} sources=#{sources.map{|s| s.inspect}} specs.size=#{specs.size}>"
    end

    def empty?
      each { return false }
      true
    end

    def search_all(name)
      all_matches = @all_specs[name] + local_search(name)
      @sources.each do |source|
        all_matches.concat(source.search_all(name))
      end
      all_matches
    end

    # Search this index's specs, and any source indexes that this index knows
    # about, returning all of the results.
    def search(query, base = nil)
      results = local_search(query, base)
      seen = Set.new(results.map { |spec| [spec.name, spec.version, spec.platform] })

      @sources.each do |source|
        source.search(query, base).each do |spec|
          lookup = [spec.name, spec.version, spec.platform]
          unless seen.include?(lookup)
            results << spec
            seen << lookup
          end
        end
      end

      results.sort_by {|s| [s.version, s.platform.to_s == 'ruby' ? "\0" : s.platform.to_s] }
    end

    def local_search(query, base = nil)
      case query
      when Gem::Specification, RemoteSpecification, LazySpecification, EndpointSpecification then search_by_spec(query)
      when String then specs_by_name(query)
      when Gem::Dependency then search_by_dependency(query, base)
      else
        raise "You can't search for a #{query.inspect}."
      end
    end

    alias [] search

    def <<(spec)
      @specs[spec.name]["#{spec.version}-#{spec.platform}"] = spec

      spec
    end

    def each(&blk)
      specs.values.each do |spec_sets|
        spec_sets.values.each(&blk)
      end
    end

    # returns a list of the dependencies
    def unmet_dependency_names
      names = dependency_names
      names.delete_if{|n| n == "bundler" }
      names.select{|n| search(n).empty? }
    end

    def dependency_names
      names = []
      each{|s| names.push(*s.dependencies.map{|d| d.name }) }
      names.uniq
    end

    def use(other, override_dupes = false)
      return unless other
      other.each do |s|
        if (dupes = search_by_spec(s)) && dupes.any?
          @all_specs[s.name] = [s] + dupes
          next unless override_dupes
          self << s
        end
        self << s
      end
      self
    end

    def size
      @sources.inject(@specs.size) do |size, source|
        size += source.size
      end
    end

    def ==(o)
      all? do |spec|
        other_spec = o[spec].first
        (spec.dependencies & other_spec.dependencies).empty? && spec.source == other_spec.source
      end
    end

    def add_source(index)
      if index.is_a?(Index)
        @sources << index
        @sources.uniq! # need to use uniq! here instead of checking for the item before adding
      else
        raise ArgumentError, "Source must be an index, not #{index.class}"
      end
    end

  private

    def specs_by_name(name)
      @specs[name].values
    end

    def search_by_dependency(dependency, base = nil)
      @cache[base || false] ||= {}
      @cache[base || false][dependency] ||= begin
        specs = specs_by_name(dependency.name) + (base || [])
        found = specs.select do |spec|
          if base # allow all platforms when searching from a lockfile
            dependency.matches_spec?(spec)
          else
            dependency.matches_spec?(spec) && Gem::Platform.match(spec.platform)
          end
        end

        wants_prerelease = dependency.requirement.prerelease?
        only_prerelease  = specs.all? {|spec| spec.version.prerelease? }

        unless wants_prerelease || only_prerelease
          found.reject! { |spec| spec.version.prerelease? }
        end

        found
      end
    end

    def search_by_spec(spec)
      spec = @specs[spec.name]["#{spec.version}-#{spec.platform}"]
      spec ? [spec] : []
    end

    if RUBY_VERSION < '1.9'
      def same_version?(a, b)
        regex = /^(.*?)(?:\.0)*$/
        a.to_s[regex, 1] == b.to_s[regex, 1]
      end
    else
      def same_version?(a, b)
        a == b
      end
    end

    def spec_satisfies_dependency?(spec, dep)
      return false unless dep.name == spec.name
      dep.requirement.satisfied_by?(spec.version)
    end

  end
end
