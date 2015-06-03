module Bundler
  module RubyDsl
    def ruby(ruby_version, options = {})
      raise GemfileError, "Please define :engine_version" if options[:engine] && options[:engine_version].nil?
      raise GemfileError, "Please define :engine" if options[:engine_version] && options[:engine].nil?

      raise GemfileError, "ruby_version must match the :engine_version for MRI" if options[:engine] == "ruby" && options[:engine_version] && ruby_version != options[:engine_version]
      @ruby_version = RubyVersion.new(ruby_version, options[:patchlevel], options[:engine], options[:engine_version])
    end
  end
end
