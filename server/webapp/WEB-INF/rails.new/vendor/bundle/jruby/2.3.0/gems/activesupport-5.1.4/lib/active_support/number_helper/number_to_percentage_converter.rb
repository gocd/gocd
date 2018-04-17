module ActiveSupport
  module NumberHelper
    class NumberToPercentageConverter < NumberConverter # :nodoc:
      self.namespace = :percentage

      def convert
        rounded_number = NumberToRoundedConverter.convert(number, options)
        options[:format].gsub("%n".freeze, rounded_number)
      end
    end
  end
end
