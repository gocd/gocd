require 'thread'

require "bundler/parallel_workers/worker"

module Bundler
  module ParallelWorkers
    autoload :UnixWorker, "bundler/parallel_workers/unix_worker"
    autoload :ThreadWorker, "bundler/parallel_workers/thread_worker"

    def self.worker_pool(size, job)
      if Bundler.current_ruby.mswin? || Bundler.current_ruby.jruby?
        ThreadWorker.new(size, job)
      else
        UnixWorker.new(size, job)
      end
    end
  end
end
