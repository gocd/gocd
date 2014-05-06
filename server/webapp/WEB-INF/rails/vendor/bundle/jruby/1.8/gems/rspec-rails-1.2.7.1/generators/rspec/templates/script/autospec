#!/usr/bin/env ruby
gem 'test-unit', '1.2.3' if RUBY_VERSION.to_f >= 1.9
ENV['RSPEC'] = 'true'     # allows autotest to discover rspec
ENV['AUTOTEST'] = 'true'  # allows autotest to run w/ color on linux
system((RUBY_PLATFORM =~ /mswin|mingw/ ? 'autotest.bat' : 'autotest'), *ARGV) ||
  $stderr.puts("Unable to find autotest.  Please install ZenTest or fix your PATH")
