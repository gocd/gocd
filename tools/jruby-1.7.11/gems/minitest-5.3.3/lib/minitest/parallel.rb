module Minitest
  module Parallel
    class Executor
      attr_reader :size

      def initialize size
        @size  = size
        @queue = Queue.new
        @pool  = size.times.map {
          Thread.new(@queue) do |queue|
          Thread.current.abort_on_exception = true
            while job = queue.pop
              klass, method, reporter = job
              result = Minitest.run_one_method klass, method
              reporter.synchronize { reporter.record result }
            end
          end
        }
      end

      def << work; @queue << work; end

      def shutdown
        size.times { @queue << nil }
        @pool.each(&:join)
      end
    end

    module Test
      def _synchronize; Test.io_lock.synchronize { yield }; end

      module ClassMethods
        def run_one_method klass, method_name, reporter
          Minitest.parallel_executor << [klass, method_name, reporter]
        end
        def test_order; :parallel; end
      end
    end
  end
end
