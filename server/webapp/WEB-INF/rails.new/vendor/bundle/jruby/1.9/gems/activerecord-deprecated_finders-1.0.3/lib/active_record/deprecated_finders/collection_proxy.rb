module ActiveRecord
  module Associations
    class CollectionProxy
      module InterceptDynamicInstantiators
        def method_missing(method, *args, &block)
          match = DynamicMatchers::Method.match(klass, method)

          if match && match.is_a?(DynamicMatchers::Instantiator)
            scoping do
              klass.send(method, *args) do |record|

                sanitized_method = match.class.prefix + match.class.suffix
                if %w(find_or_create_by find_or_create_by!).include?(sanitized_method) && proxy_association.reflection.options[:through].present?
                  proxy_association.send(:save_through_record, record)
                else
                  proxy_association.add_to_target(record)
                end
                yield record if block_given?
              end
            end
          else
            super
          end
        end
      end

      def self.inherited(subclass)
        subclass.class_eval do
          # Ensure this get included first
          include ActiveRecord::Delegation::ClassSpecificRelation

          # Now override the method_missing definition
          include InterceptDynamicInstantiators
        end
      end
    end
  end
end
