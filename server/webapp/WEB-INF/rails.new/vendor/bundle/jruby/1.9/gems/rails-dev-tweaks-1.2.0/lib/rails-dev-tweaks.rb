require 'active_support'
require 'active_support/dependencies'

module RailsDevTweaks
  LIB_PATH = File.expand_path('..', __FILE__)
end

# Ironically, we use autoloading ourselves to enforce our file structure, and less typing.
ActiveSupport::Dependencies.autoload_paths      << RailsDevTweaks::LIB_PATH
ActiveSupport::Dependencies.autoload_once_paths << RailsDevTweaks::LIB_PATH # But don't allow *auto-reloading*!

# Reference the railtie to force it to load
RailsDevTweaks::Railtie
