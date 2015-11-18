require 'rubygems/installer'

module Bundler
  class GemInstaller < Gem::Installer
    def check_executable_overwrite(filename)
      # Bundler needs to install gems regardless of binstub overwriting
    end
  end
end
