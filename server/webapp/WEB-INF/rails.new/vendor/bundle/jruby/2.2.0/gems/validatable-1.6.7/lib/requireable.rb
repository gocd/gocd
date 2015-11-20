module Validatable
  module Requireable #:nodoc:
    module ClassMethods #:nodoc:
      def requires(*args)
        required_options.concat args        
      end
      
      def required_options
        @required_options ||= []
      end
    end
    
    def self.included(klass)
      klass.extend ClassMethods
    end
    
    def requires(options)
      required_options = self.class.required_options.inject([]) do |errors, attribute|
        errors << attribute.to_s unless options.has_key?(attribute)
        errors
      end
      raise ArgumentError.new("#{self.class} requires options: #{required_options.join(', ')}") if required_options.any?
      true
    end
  end
end