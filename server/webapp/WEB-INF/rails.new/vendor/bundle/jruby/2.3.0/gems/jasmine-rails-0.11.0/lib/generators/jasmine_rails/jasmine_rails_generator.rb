require 'rails/generators/named_base'

module JasmineRails
  module Generators
    class JasmineRailsGenerator < Rails::Generators::NamedBase
      namespace "jasmine_rails"
      source_root File.expand_path("../templates", __FILE__)
    end
  end
end
