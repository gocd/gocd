require 'tsort'
require 'forwardable'

module Bundler
  class SpecSet
    extend Forwardable
    include TSort, Enumerable

    def_delegators :@specs, :<<, :length, :add, :remove
    def_delegators :sorted, :each

    def initialize(specs)
      @specs = specs.sort_by { |s| s.name }
    end

    def for(dependencies, skip = [], check = false, match_current_platform = false)
      handled, deps, specs = {}, dependencies.dup, []
      skip << 'bundler'

      until deps.empty?
        dep = deps.shift
        next if handled[dep] || skip.include?(dep.name)

        spec = lookup[dep.name].find do |s|
          if match_current_platform
            Gem::Platform.match(s.platform)
          else
            s.match_platform(dep.__platform)
          end
        end

        handled[dep] = true

        if spec
          specs << spec

          spec.dependencies.each do |d|
            next if d.type == :development
            d = DepProxy.new(d, dep.__platform) unless match_current_platform
            deps << d
          end
        elsif check
          return false
        end
      end

      if spec = lookup['bundler'].first
        specs << spec
      end

      check ? true : SpecSet.new(specs)
    end

    def valid_for?(deps)
      self.for(deps, [], true)
    end

    def [](key)
      key = key.name if key.respond_to?(:name)
      lookup[key].reverse
    end

    def []=(key, value)
      @specs << value
      @lookup = nil
      @sorted = nil
      value
    end

    def sort!
      self
    end

    def to_a
      sorted.dup
    end

    def to_hash
      lookup.dup
    end

    def materialize(deps, missing_specs = nil)
      materialized = self.for(deps, [], false, true).to_a
      deps = materialized.map {|s| s.name }.uniq
      materialized.map! do |s|
        next s unless s.is_a?(LazySpecification)
        s.source.dependency_names = deps if s.source.respond_to?(:dependency_names=)
        spec = s.__materialize__
        if missing_specs
          missing_specs << s unless spec
        else
          raise GemNotFound, "Could not find #{s.full_name} in any of the sources" unless spec
        end
        spec if spec
      end
      SpecSet.new(materialized.compact)
    end

    def merge(set)
      arr = sorted.dup
      set.each do |s|
        next if arr.any? { |s2| s2.name == s.name && s2.version == s.version && s2.platform == s.platform }
        arr << s
      end
      SpecSet.new(arr)
    end

  private

    def sorted
      rake = @specs.find { |s| s.name == 'rake' }
      begin
        @sorted ||= ([rake] + tsort).compact.uniq
      rescue TSort::Cyclic => error
        cgems = extract_circular_gems(error)
        raise CyclicDependencyError, "Your Gemfile requires gems that depend" \
          " depend on each other, creating an infinite loop. Please remove" \
          " either gem '#{cgems[1]}' or gem '#{cgems[0]}' and try again."
      end
    end

    def extract_circular_gems(error)
      if Bundler.current_ruby.mri? && Bundler.current_ruby.on_19?
        error.message.scan(/(\w+) \([^)]/).flatten
      else
        error.message.scan(/@name="(.*?)"/).flatten
      end
    end

    def lookup
      @lookup ||= begin
        lookup = Hash.new { |h,k| h[k] = [] }
        specs = @specs.sort_by do |s|
          s.platform.to_s == 'ruby' ? "\0" : s.platform.to_s
        end
        specs.reverse_each do |s|
          lookup[s.name] << s
        end
        lookup
      end
    end

    def tsort_each_node
      @specs.each { |s| yield s }
    end

    def tsort_each_child(s)
      s.dependencies.sort_by { |d| d.name }.each do |d|
        next if d.type == :development
        lookup[d.name].each { |s2| yield s2 }
      end
    end
  end
end
