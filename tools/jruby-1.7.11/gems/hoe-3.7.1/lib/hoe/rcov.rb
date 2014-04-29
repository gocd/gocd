##
# RCov plugin for hoe.
#
# === Tasks Provided:
#
# rcov::               Analyze code coverage with tests

module Hoe::RCov

  ##
  # Activate the rcov dependencies.

  def activate_rcov_deps
    dependency "rcov", "~> 0.9", :development
  end

  ##
  # Define tasks for plugin.

  def define_rcov_tasks
    begin # take a whack at defining rcov tasks
      task :isolate # ensure it exists

      task :rcov => :isolate do
        sh(*make_rcov_cmd)
      end

      task :clobber_rcov do
        rm_rf "coverage"
      end

      task :clobber => :clobber_rcov

      # this is for my emacs rcov overlay stuff on emacswiki.
      task :rcov_overlay do
        path = ENV["FILE"]
        rcov, eol = Marshal.load(File.read("coverage.info")).last[path], 1
        puts rcov[:lines].zip(rcov[:coverage]).map { |line, coverage|
          bol, eol = eol, eol + line.length
          [bol, eol, "#ffcccc"] unless coverage
        }.compact.inspect
      end
    rescue LoadError
      # skip
      task :clobber_rcov # in case rcov didn't load
    end
  end

  def make_rcov_cmd # :nodoc:
    rcov  = Gem.bin_wrapper "rcov"
    tests = test_globs.sort.map { |g| Dir.glob(g) }.flatten.map(&:inspect)

    cmd = %W[#{rcov}
             #{Hoe::RUBY_FLAGS}
             --text-report
             --no-color
             --save coverage.info
             -x ^/
             -x tmp/isolate
             --sort coverage
             --sort-reverse
             -o coverage
            ] + tests

    cmd
  end
end

task :clean => :clobber_rcov
