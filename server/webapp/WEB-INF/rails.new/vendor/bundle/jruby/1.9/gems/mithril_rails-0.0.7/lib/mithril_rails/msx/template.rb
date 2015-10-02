require 'tilt'
require 'rails'
module MithrilRails
  module MSX

    class Template < Tilt::Template
      self.default_mime_type = 'application/javascript'

      def prepare
      end

      def evaluate(scope, locals, &block)
        @output ||= MSX::transform(data)
      end
    end
  end
end