module ActionView #:nodoc:
  # = Action View Text Template
  class Template
    class Text #:nodoc:
      attr_accessor :type

      def initialize(string)
        @string = string.to_s
        @type = Types[:text]
      end

      def identifier
        "text template"
      end

      alias_method :inspect, :identifier

      def to_str
        @string
      end

      def render(*args)
        to_str
      end

      def formats
        [@type.ref]
      end
    end
  end
end
