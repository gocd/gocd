Dir.chdir(File.dirname(__FILE__)) do
  test_files = Dir['**/test_*.rb']
  test_files = test_files.select { |f| f =~ Regexp.new(ENV['ONLY']) } if ENV['ONLY']
  test_files = test_files.reject { |f| f =~ Regexp.new(ENV['EXCEPT']) } if ENV['EXCEPT']
  test_files.each { |file| require(file) }
end