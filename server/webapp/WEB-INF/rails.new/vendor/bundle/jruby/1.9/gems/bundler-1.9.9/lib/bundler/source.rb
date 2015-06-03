module Bundler
  class Source
    autoload :Rubygems, 'bundler/source/rubygems'
    autoload :Path,     'bundler/source/path'
    autoload :Git,      'bundler/source/git'

    def self.mirror_for(uri)
      uri = URI(uri.to_s) unless uri.is_a?(URI)

      # Settings keys are all downcased
      mirrors = Bundler.settings.gem_mirrors
      normalized_key = URI(uri.to_s.downcase)

      mirrors[normalized_key] || uri
    end

    attr_accessor :dependency_names

    def unmet_deps
      specs.unmet_dependency_names
    end

    def version_message(spec)
      message = "#{spec.name} #{spec.version}"

      if Bundler.locked_gems
        locked_spec = Bundler.locked_gems.specs.find { |s| s.name == spec.name }
        locked_spec_version = locked_spec.version if locked_spec
        if locked_spec_version && spec.version != locked_spec_version
          message << " (was #{locked_spec_version})"
        end
      end

      message
    end

    def can_lock?(spec)
      spec.source == self
    end

    def include?(other)
      other == self
    end

  end
end
