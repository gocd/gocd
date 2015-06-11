require 'sass/logger'

module Sass
  module Rails
    class Logger < Sass::Logger::Base
      def _log(level, message)

        case level
          when :trace, :debug
            ::Rails.logger.debug message
          when :warn
            ::Rails.logger.warn message
          when :error
            ::Rails.logger.error message
          when :info
            ::Rails.logger.info message
        end
      end
    end
  end
end
