Dir.chdir File.dirname( __FILE__ )
Dir["tc_*.rb"].each { |test| load test }
