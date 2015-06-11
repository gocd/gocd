#!/usr/bin/env ruby

$LOAD_PATH.unshift("#{File.dirname(__FILE__)}/../lib") if __FILE__ == $0

puts "Checking for test cases:"
Dir['tc*.rb'].each do |testcase|
  puts "\t#{testcase}"
  require testcase
end
puts " "
