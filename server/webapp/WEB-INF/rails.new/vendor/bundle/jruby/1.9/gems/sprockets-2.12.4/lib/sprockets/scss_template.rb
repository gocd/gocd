require 'sprockets/sass_template'

module Sprockets
  # Scss handler to replace Tilt's builtin one. See `SassTemplate` and
  # `SassImporter` for more infomation.
  class ScssTemplate < SassTemplate
    self.default_mime_type = 'text/css'

    def syntax
      :scss
    end
  end
end
