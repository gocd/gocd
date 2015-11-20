module Bundler
  class CLI::Inject
    attr_reader :options, :name, :version, :gems
    def initialize(options, name, version, gems)
      @options = options
      @name = name
      @version = version
      @gems = gems
    end

    def run
      # The required arguments allow Thor to give useful feedback when the arguments
      # are incorrect. This adds those first two arguments onto the list as a whole.
      gems.unshift(version).unshift(name)

      # Build an array of Dependency objects out of the arguments
      deps = []
      gems.each_slice(2) do |gem_name, gem_version|
        deps << Bundler::Dependency.new(gem_name, gem_version)
      end

      added = Injector.inject(deps)

      if added.any?
        Bundler.ui.confirm "Added to Gemfile:"
        Bundler.ui.confirm added.map{ |g| "  #{g}" }.join("\n")
      else
        Bundler.ui.confirm "All injected gems were already present in the Gemfile"
      end
    end

  end
end
