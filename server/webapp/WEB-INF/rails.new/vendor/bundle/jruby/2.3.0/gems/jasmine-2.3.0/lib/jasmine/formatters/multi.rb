module Jasmine
  module Formatters
    class Multi
      def initialize(formatters)
        @formatters = formatters
      end

      def format(results)
        @formatters.each { |formatter| formatter.format(results) }
      end

      def done
        @formatters.each(&:done)
      end
    end
  end
end
