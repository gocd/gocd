module RSpec
  module Core
    module Formatters

      module Helpers
        SUB_SECOND_PRECISION = 5
        DEFAULT_PRECISION = 2

        def format_duration(duration)
          if duration > 60
            minutes = duration.to_i / 60
            seconds = duration - minutes * 60

            "#{pluralize(minutes, 'minute')} #{format_seconds(seconds)} seconds"
          else
            "#{format_seconds(duration)} seconds"
          end
        end

        def format_seconds(float)
          precision ||= (float < 1) ? SUB_SECOND_PRECISION : DEFAULT_PRECISION
          formatted = sprintf("%.#{precision}f", float)
          strip_trailing_zeroes(formatted)
        end

        def strip_trailing_zeroes(string)
          stripped = string.sub(/[^1-9]+$/, '')
          stripped.empty? ? "0" : stripped
        end

        def pluralize(count, string)
          "#{count} #{string}#{'s' unless count == 1}"
        end

      end

    end
  end
end
