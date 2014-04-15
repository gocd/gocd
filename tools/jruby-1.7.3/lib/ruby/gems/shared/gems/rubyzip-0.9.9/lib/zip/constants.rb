module Zip
  VERSION = '0.9.7'
  RUBY_MINOR_VERSION = RUBY_VERSION.split(".")[1].to_i
  RUNNING_ON_WINDOWS = RbConfig::CONFIG['host_os'] =~ /mswin|mingw|cygwin/i

  # Ruby 1.7.x compatibility
  # In ruby 1.6.x and 1.8.0 reading from an empty stream returns 
  # an empty string the first time and then nil.
  #  not so in 1.7.x
  EMPTY_FILE_RETURNS_EMPTY_STRING_FIRST = RUBY_MINOR_VERSION != 7
end
