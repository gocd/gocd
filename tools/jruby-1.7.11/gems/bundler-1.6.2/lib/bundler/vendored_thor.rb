if defined?(Thor)
  Bundler.ui.warn "Thor has already been required. " +
    "This may cause Bundler to malfunction in unexpected ways."
end
vendor = File.expand_path('../vendor', __FILE__)
$:.unshift(vendor) unless $:.include?(vendor)
require 'thor'
require 'thor/actions'
