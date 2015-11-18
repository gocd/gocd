module Validatable
  module Understandable #:nodoc:
    module ClassMethods #:nodoc:
      def understands(*args)
        understandings.concat args
      end

      def understandings
        @understandings ||= []
      end

      def all_understandings
        return understandings + self.superclass.all_understandings if self.superclass.respond_to? :all_understandings
        understandings
      end
    end

    def self.included(klass)
      klass.extend ClassMethods
    end

    def must_understand(hash)
      invalid_options = hash.inject([]) do |errors, (key, value)|
        errors << key.to_s unless self.class.all_understandings.include?(key)
        errors
      end
      raise ArgumentError.new("invalid options: #{invalid_options.join(', ')}") if invalid_options.any?
      true
    end
  end
end