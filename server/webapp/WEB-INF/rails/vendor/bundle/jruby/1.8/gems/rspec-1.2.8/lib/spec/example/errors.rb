module Spec
  module Example
    class ExamplePendingError < StandardError; end

    class NotYetImplementedError < ExamplePendingError
      MESSAGE = "Not Yet Implemented"
      def initialize
        super(MESSAGE)
      end
    end

    class PendingExampleFixedError < StandardError; end

    class NoDescriptionError < ArgumentError
      class << self
        def message(kind, location)
          "No description supplied for #{kind} declared on #{location}"
        end
      end
      def initialize(kind, location)
        super(self.class.message(kind, location))
      end
    end
  end
end
