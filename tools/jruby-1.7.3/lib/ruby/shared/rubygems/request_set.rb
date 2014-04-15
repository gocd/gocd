require 'rubygems'
require 'rubygems/dependency'
require 'rubygems/dependency_resolver'

module Gem
  class RequestSet
    def initialize(*deps)
      @dependencies = deps

      yield self if block_given?
    end

    # Declare that a gem of name +name+ with +reqs+ requirements
    # is needed.
    #
    def gem(name, *reqs)
      @dependencies << Gem::Dependency.new(name, reqs)
    end

    # Resolve the requested dependencies and return an Array of
    # Specification objects to be activated.
    #
    def resolve
      r = Gem::DependencyResolver.new(@dependencies)
      r.resolve
    end

    # Load a Bundler-style Gemfile as much as possible.
    #
    def load_gemfile(path)
      gf = GemFile.new(self, path)
      gf.load
    end

    # A semi-compatible DSL for Bundler's Gemfile format
    #
    class GemFile
      def initialize(set, path)
        @set = set
        @path = path
      end

      def load
        instance_eval File.read(@path), @path, 1
      end

      # DSL

      def source(url)
      end

      def gem(name, *reqs)
        # Ignore the opts for now.
        opts = reqs.pop if reqs.last.kind_of?(Hash)

        @set.gem name, *reqs
      end

      def platform(what)
        if what == :ruby
          yield
        end
      end

      alias_method :platforms, :platform

      def group(*what)
      end
    end
  end
end
