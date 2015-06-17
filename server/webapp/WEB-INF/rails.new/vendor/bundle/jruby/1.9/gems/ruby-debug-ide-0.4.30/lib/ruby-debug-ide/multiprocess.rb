if RUBY_VERSION < "1.9"
  require 'ruby-debug-ide/multiprocess/pre_child'
  require 'ruby-debug-ide/multiprocess/monkey'
else
  require_relative 'multiprocess/pre_child'
  require_relative 'multiprocess/monkey'
end