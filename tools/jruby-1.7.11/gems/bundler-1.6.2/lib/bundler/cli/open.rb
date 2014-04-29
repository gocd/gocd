require 'bundler/cli/common'

module Bundler
  class CLI::Open
    attr_reader :options, :name
    def initialize(options, name)
      @options = options
      @name = name
    end

    def run
      editor = [ENV['BUNDLER_EDITOR'], ENV['VISUAL'], ENV['EDITOR']].find{|e| !e.nil? && !e.empty? }
      return Bundler.ui.info("To open a bundled gem, set $EDITOR or $BUNDLER_EDITOR") unless editor
      spec = Bundler::CLI::Common.select_spec(name, :regex_match)
      return unless spec
      full_gem_path = spec.full_gem_path
      Dir.chdir(full_gem_path) do
        command = "#{editor} #{full_gem_path}"
        success = system(command)
        Bundler.ui.info "Could not run '#{command}'" unless success
      end
    end

  end
end
