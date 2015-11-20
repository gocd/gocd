module Bundler
  class CLI::Check
    attr_reader :options
    def initialize(options)
      @options = options
    end

    def run
      Bundler.settings[:path] = File.expand_path(options[:path]) if options[:path]
      begin
        definition = Bundler.definition
        definition.validate_ruby!
        not_installed = definition.missing_specs
      rescue GemNotFound, VersionConflict
        Bundler.ui.error "Bundler can't satisfy your Gemfile's dependencies."
        Bundler.ui.warn  "Install missing gems with `bundle install`."
        exit 1
      end

      if not_installed.any?
        Bundler.ui.error "The following gems are missing"
        not_installed.each { |s| Bundler.ui.error " * #{s.name} (#{s.version})" }
        Bundler.ui.warn "Install missing gems with `bundle install`"
        exit 1
      elsif !Bundler.default_lockfile.exist? && Bundler.settings[:frozen]
        Bundler.ui.error "This bundle has been frozen, but there is no Gemfile.lock present"
        exit 1
      else
        Bundler.load.lock(:preserve_bundled_with => true) unless options[:"dry-run"]
        Bundler.ui.info "The Gemfile's dependencies are satisfied"
      end
    end

  end
end
