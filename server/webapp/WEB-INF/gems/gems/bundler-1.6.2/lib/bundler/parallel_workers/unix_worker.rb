module Bundler
  module ParallelWorkers
    # UnixWorker is used only on platforms where fork is available. The way
    # this code works is, it forks a preconfigured number of workers and then
    # It starts preconfigured number of threads that write to the connected pipe.
    class UnixWorker < Worker

      class JobHandler < Struct.new(:pid, :io_r, :io_w)
        def work(obj)
          Marshal.dump obj, io_w
          Marshal.load io_r
        rescue IOError, Errno::EPIPE
          nil
        end
      end

      def initialize(size, job)
        # Close the persistent connections for the main thread before forking
        Net::HTTP::Persistent.new('bundler', :ENV).shutdown
        super
      end

      private

      # Start forked workers for downloading gems. This version of worker
      # is only used on platforms where fork is available.
      #
      # @param size [Integer] Size of worker pool
      # @param func [Proc] Job that should be executed in the worker
      def prepare_workers(size, func)
        @workers = size.times.map do |num|
          child_read, parent_write = IO.pipe
          parent_read, child_write = IO.pipe

          pid = Process.fork do
            begin
              parent_read.close
              parent_write.close

              while !child_read.eof?
                obj = Marshal.load child_read
                Marshal.dump func.call(obj, num), child_write
              end
            rescue Exception => e
              begin
                Marshal.dump WrappedException.new(e), child_write
              rescue Errno::EPIPE
                nil
              end
            ensure
              child_read.close
              child_write.close
            end
          end

          child_read.close
          child_write.close
          JobHandler.new pid, parent_read, parent_write
        end
      end

      # Start the threads whose job is basically to wait for incoming messages
      # on request queue and write that message to the connected pipe. Also retrieve
      # messages from child worker via connected pipe and write the message to response queue
      #
      # @param size [Integer] Number of threads to be started
      def prepare_threads(size)
        @threads = size.times.map do |i|
          Thread.start do
            worker = @workers[i]
            loop do
              obj = @request_queue.deq
              break if obj.equal? POISON
              @response_queue.enq worker.work(obj)
            end
          end
        end
      end

      # Kill the forked workers by sending SIGINT to them
      def stop_workers
        @workers.each do |worker|
          worker.io_r.close unless worker.io_r.closed?
          worker.io_w.close unless worker.io_w.closed?
          begin
            Process.kill :INT, worker.pid
          rescue Errno::ESRCH
            nil
          end
        end
        @workers.each do |worker|
          begin
            Process.waitpid worker.pid
          rescue Errno::ECHILD
            nil
          end
        end
      end
    end
  end
end
