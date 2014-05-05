module Spec
  module Example
    # Lightweight proxy for an example. This is the object that is passed to
    # example-related methods in Spec::Runner::Formatter::BaseFormatter
    class ExampleProxy

      def initialize(description=nil, options={}, location=nil) # :nodoc:
        @description, @options, @location = description, options, location
      end

      # Optional hash passed to the example declaration
      attr_reader :options

      # This is the docstring passed to the <tt>it()</tt> method or any
      # of its aliases
      attr_reader :description

      # The file and line number at which the represented example
      # was declared. This is extracted from <tt>caller</tt>, and is therefore
      # formatted as an individual line in a backtrace.
      attr_reader :location

      # Deprecated - use location()
      def backtrace
        Spec.deprecate("ExampleProxy#backtrace","ExampleProxy#location")
        location
      end

      # Convenience method for example group - updates the value of
      # <tt>description</tt> and returns self.
      def update(description) # :nodoc:
        @description = description
        self
      end

      def ==(other) # :nodoc:
        (other.description == description) & (other.location == location)
      end
    end
  end
end
