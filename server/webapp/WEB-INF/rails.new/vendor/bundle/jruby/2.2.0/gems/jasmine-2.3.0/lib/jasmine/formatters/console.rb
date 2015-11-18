module Jasmine
  module Formatters
    class Console
      def initialize(outputter = Kernel)
        @results = []
        @outputter = outputter
      end

      def format(results_batch)
        outputter.print(chars(results_batch))
        @results += results_batch
      end

      def done
        outputter.puts

        failure_count = results.count(&:failed?)
        if failure_count > 0
          outputter.puts('Failures:')
          outputter.puts(failures(@results))
          outputter.puts
        end

        pending_count = results.count(&:pending?)
        if pending_count > 0
          outputter.puts('Pending:')
          outputter.puts(pending(@results))
          outputter.puts
        end
        summary = "#{pluralize(results.size, 'spec')}, " +
          "#{pluralize(failure_count, 'failure')}"

        summary += ", #{pluralize(pending_count, 'pending spec')}" if pending_count > 0

        outputter.puts(summary)
      end

      private
      attr_reader :results, :outputter

      def failures(results)
        results.select(&:failed?).map { |f| failure_message(f) }.join("\n\n")
      end

      def pending(results)
        results.select(&:pending?).map { |spec| pending_message(spec) }.join("\n\n")
      end

      def chars(results)
        results.map do |result|
          if result.succeeded?
            "\e[32m.\e[0m"
          elsif result.pending?
            "\e[33m*\e[0m"
          elsif result.disabled?
            ""
          else
            "\e[31mF\e[0m"
          end
        end.join('')
      end

      def pluralize(count, str)
        word = (count == 1) ? str : str + 's'
        "#{count} #{word}"
      end

      def pending_message(spec)
        reason = 'No reason given'
        reason = spec.pending_reason if spec.pending_reason && spec.pending_reason != ''

        "\t#{spec.full_name}\n\t  \e[33m#{reason}\e[0m"
      end

      def failure_message(failure)
        template = <<-FM
          #{failure.full_name}\n
        FM

        template += failure.failed_expectations.map { |fe| expectation_message(fe) }.join("\n")
      end

      def expectation_message(expectation)
        <<-FE
          #{expectation.message}
          #{expectation.stack}
        FE
      end
    end
  end
end
