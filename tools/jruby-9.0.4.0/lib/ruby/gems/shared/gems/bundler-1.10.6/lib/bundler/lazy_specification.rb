require "uri"
require "rubygems/spec_fetcher"
require "bundler/match_platform"

module Bundler
  class LazySpecification
    include MatchPlatform

    attr_reader :name, :version, :dependencies, :platform
    attr_accessor :source, :remote

    def initialize(name, version, platform, source = nil)
      @name          = name
      @version       = version
      @dependencies  = []
      @platform      = platform
      @source        = source
      @specification = nil
    end

    def full_name
      if platform == Gem::Platform::RUBY or platform.nil? then
        "#{@name}-#{@version}"
      else
        "#{@name}-#{@version}-#{platform}"
      end
    end

    def ==(other)
      identifier == other.identifier
    end

    def satisfies?(dependency)
      @name == dependency.name && dependency.requirement.satisfied_by?(Gem::Version.new(@version))
    end

    def to_lock
      if platform == Gem::Platform::RUBY or platform.nil?
        out = "    #{name} (#{version})\n"
      else
        out = "    #{name} (#{version}-#{platform})\n"
      end

      dependencies.sort_by {|d| d.to_s }.uniq.each do |dep|
        next if dep.type == :development
        out << "    #{dep.to_lock}\n"
      end

      out
    end

    def __materialize__
      @specification = source.specs.search(Gem::Dependency.new(name, version)).last
    end

    def respond_to?(*args)
      super || @specification.respond_to?(*args)
    end

    def to_s
      @__to_s ||= "#{name} (#{version})"
    end

    def identifier
      @__identifier ||= [name, version, source, platform, dependencies].hash
    end

  private

    def to_ary
      nil
    end

    def method_missing(method, *args, &blk)
      raise "LazySpecification has not been materialized yet (calling :#{method} #{args.inspect})" unless @specification

      return super unless respond_to?(method)

      @specification.send(method, *args, &blk)
    end

  end
end
