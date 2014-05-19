# -*- ruby encoding: utf-8 -*-

$LOAD_PATH.unshift File.expand_path('../../lib', __FILE__)
require 'benchmark'

class Benchmarker
  def self.benchmark(repeats)
    new(repeats.to_i).benchmark
  end

  def initialize(repeats = nil)
    @cache_file = File.expand_path('../cache.mtc', __FILE__)
    @repeats    = repeats.to_i
    @repeats    = 50 if repeats.zero?
  end

  def reload_mime_types(repeats = 1, force_load = false)
    path = File.expand_path('../../lib', __FILE__)

    repeats.times {
      Object.send(:remove_const, :MIME) if defined? MIME
      $LOADED_FEATURES.delete_if { |n| n =~ /#{path}/ }
      require 'mime/types'
      MIME::Types.send(:__types__) if force_load
    }
  end

  def benchmark
    remove_cache

    Benchmark.bm(17) do |mark|
      mark.report("Normal:") { reload_mime_types(@repeats) }

      ENV['RUBY_MIME_TYPES_LAZY_LOAD'] = 'yes'
      mark.report("Lazy:") { reload_mime_types(@repeats) }
      mark.report("Lazy+Load:") { reload_mime_types(@repeats, true) }

      ENV.delete('RUBY_MIME_TYPES_LAZY_LOAD')

      ENV['RUBY_MIME_TYPES_CACHE'] = @cache_file
      reload_mime_types

      mark.report("Cached:") { reload_mime_types(@repeats) }
      ENV['RUBY_MIME_TYPES_LAZY_LOAD'] = 'yes'
      mark.report("Lazy Cached:") { reload_mime_types(@repeats) }
      mark.report("Lazy Cached Load:") { reload_mime_types(@repeats, true) }
    end
  ensure
    remove_cache
  end

  def remove_cache
    File.unlink(@cache_file) if File.exist?(@cache_file)
  end
end
