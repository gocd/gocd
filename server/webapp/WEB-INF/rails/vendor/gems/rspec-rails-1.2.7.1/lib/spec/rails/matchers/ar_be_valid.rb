if defined?(ActiveRecord::Base)
  module Spec
    module Rails
      module Matchers
    
        class ArBeValid  #:nodoc:
        
          def initialize
            @matcher = Spec::Matchers::Be.new :be_valid
            @matcher.__send__ :handling_predicate!
          end

          def matches?(actual)
            @actual = actual
            @matcher.matches? @actual
          end
      
          def failure_message_for_should
            if @actual.respond_to?(:errors) &&
                ActiveRecord::Errors === @actual.errors
              "Expected #{@actual.inspect} to be valid, but it was not\nErrors: " + @actual.errors.full_messages.join(", ")            
            else
              @matcher.failure_message_for_should
            end
          end
        
          def failure_message_for_should_not
            @matcher.failure_message_for_should_not
          end
        
          def description
            "be valid"
          end
        end

        # :call-seq:
        #   response.should be_valid
        #   response.should_not be_valid
        def be_valid
          ArBeValid.new
        end
    
      end
    end
  end
end
