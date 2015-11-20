module ActionView
  module Helpers
    module Tags # :nodoc:
      class WeekField < DatetimeField # :nodoc:
        private

          def format_date(value)
            value.try(:strftime, "%Y-W%W")
          end
      end
    end
  end
end
