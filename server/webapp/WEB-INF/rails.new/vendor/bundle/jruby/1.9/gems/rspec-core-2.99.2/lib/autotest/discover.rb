begin
  # try to load rspec/autotest so we can check if the constant is defined below.
  require 'rspec/autotest'
rescue LoadError
  # ignore. We print a deprecation warning later.
end

if File.exist?("./.rspec") && !defined?(::RSpec::Autotest)
  Autotest.add_discovery { "rspec2" }
end
