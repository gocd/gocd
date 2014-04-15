if defined?(ActiveRecord::Base)
  module Spec
    module Rails
      module Extensions
        module ActiveRecord
          module ClassMethods
            # :call-seq:
            #   ModelClass.should have(:no).records
            #   ModelClass.should have(1).record
            #   ModelClass.should have(n).records
            #
            # Extension to enhance <tt>should have</tt> on AR Model classes
            def records
              find(:all)
            end
            alias :record :records
          end

          module InstanceMethods
            # :call-seq:
            #   model.should have(:no).errors_on(:attribute)
            #   model.should have(1).error_on(:attribute)
            #   model.should have(n).errors_on(:attribute)
            #
            # Extension to enhance <tt>should have</tt> on AR Model instances.
            # Calls model.valid? in order to prepare the object's errors
            # object.
            def errors_on(attribute)
              self.valid?
              [self.errors.on(attribute)].flatten.compact
            end
            alias :error_on :errors_on
          end
        end
      end
    end
  end
  
  module ActiveRecord #:nodoc:
    class Base
      extend  Spec::Rails::Extensions::ActiveRecord::ClassMethods
      include Spec::Rails::Extensions::ActiveRecord::InstanceMethods
    end
  end
end