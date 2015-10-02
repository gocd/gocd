module Requirejs
  # Raised if requirejs_include_tag appears multiple times on a page.
  class MultipleIncludeError < RuntimeError; end
  # Raised if the configuration fails validation.
  class ConfigError < ArgumentError; end
  # Raised if the builder encounters an error.
  class BuildError < RuntimeError; end
end
