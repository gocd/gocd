module ActiveModel
  module Type
    class Time < Value # :nodoc:
      include Helpers::TimeValue
      include Helpers::AcceptsMultiparameterTime.new(
        defaults: { 1 => 1970, 2 => 1, 3 => 1, 4 => 0, 5 => 0 }
      )

      def type
        :time
      end

      def user_input_in_time_zone(value)
        return unless value.present?

        case value
        when ::String
          value = "2000-01-01 #{value}"
        when ::Time
          value = value.change(year: 2000, day: 1, month: 1)
        end

        super(value)
      end

      private

        def cast_value(value)
          return value unless value.is_a?(::String)
          return if value.empty?

          if value.start_with?("2000-01-01")
            dummy_time_value = value
          else
            dummy_time_value = "2000-01-01 #{value}"
          end

          fast_string_to_time(dummy_time_value) || begin
            time_hash = ::Date._parse(dummy_time_value)
            return if time_hash[:hour].nil?
            new_time(*time_hash.values_at(:year, :mon, :mday, :hour, :min, :sec, :sec_fraction, :offset))
          end
        end
    end
  end
end
