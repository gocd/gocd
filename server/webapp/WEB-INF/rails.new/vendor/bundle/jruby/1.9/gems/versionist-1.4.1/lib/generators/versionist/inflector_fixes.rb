module Versionist
  # Various fixes for quirks in Rails' inflector
  module InflectorFixes
    # Transforms a module name for use in a route
    def module_name_for_route(module_name)
      module_name.gsub(/_{1}/, "__")
    end

    # Transforms a module name for use in a file path
    def module_name_for_path(module_name)
      module_name.underscore
    end
  end
end
