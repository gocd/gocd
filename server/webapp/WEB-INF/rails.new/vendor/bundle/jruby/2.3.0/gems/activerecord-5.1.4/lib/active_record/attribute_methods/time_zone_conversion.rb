module ActiveRecord
  module AttributeMethods
    module TimeZoneConversion
      class TimeZoneConverter < DelegateClass(Type::Value) # :nodoc:
        def deserialize(value)
          convert_time_to_time_zone(super)
        end

        def cast(value)
          return if value.nil?

          if value.is_a?(Hash)
            set_time_zone_without_conversion(super)
          elsif value.respond_to?(:in_time_zone)
            begin
              super(user_input_in_time_zone(value)) || super
            rescue ArgumentError
              nil
            end
          else
            map_avoiding_infinite_recursion(super) { |v| cast(v) }
          end
        end

        private

          def convert_time_to_time_zone(value)
            return if value.nil?

            if value.acts_like?(:time)
              value.in_time_zone
            elsif value.is_a?(::Float)
              value
            else
              map_avoiding_infinite_recursion(value) { |v| convert_time_to_time_zone(v) }
            end
          end

          def set_time_zone_without_conversion(value)
            ::Time.zone.local_to_utc(value).try(:in_time_zone) if value
          end

          def map_avoiding_infinite_recursion(value)
            map(value) do |v|
              if value.equal?(v)
                nil
              else
                yield(v)
              end
            end
          end
      end

      extend ActiveSupport::Concern

      included do
        mattr_accessor :time_zone_aware_attributes, instance_writer: false
        self.time_zone_aware_attributes = false

        class_attribute :skip_time_zone_conversion_for_attributes, instance_writer: false
        self.skip_time_zone_conversion_for_attributes = []

        class_attribute :time_zone_aware_types, instance_writer: false
        self.time_zone_aware_types = [:datetime, :time]
      end

      module ClassMethods
        private

          def inherited(subclass)
            super
            # We need to apply this decorator here, rather than on module inclusion. The closure
            # created by the matcher would otherwise evaluate for `ActiveRecord::Base`, not the
            # sub class being decorated. As such, changes to `time_zone_aware_attributes`, or
            # `skip_time_zone_conversion_for_attributes` would not be picked up.
            subclass.class_eval do
              matcher = ->(name, type) { create_time_zone_conversion_attribute?(name, type) }
              decorate_matching_attribute_types(matcher, :_time_zone_conversion) do |type|
                TimeZoneConverter.new(type)
              end
            end
          end

          def create_time_zone_conversion_attribute?(name, cast_type)
            enabled_for_column = time_zone_aware_attributes &&
              !skip_time_zone_conversion_for_attributes.include?(name.to_sym)

            enabled_for_column && time_zone_aware_types.include?(cast_type.type)
          end
      end
    end
  end
end
