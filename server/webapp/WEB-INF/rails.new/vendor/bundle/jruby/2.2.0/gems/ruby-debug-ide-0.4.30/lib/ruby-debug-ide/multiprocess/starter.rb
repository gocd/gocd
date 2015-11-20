if ENV['IDE_PROCESS_DISPATCHER']
  require 'rubygems'
  ENV['DEBUGGER_STORED_RUBYLIB'].split(File::PATH_SEPARATOR).each do |path|
    next unless path =~ /ruby-debug-ide|ruby-debug-base|linecache|debase/
    $LOAD_PATH << path
  end
  require 'ruby-debug-ide'
  require 'ruby-debug-ide/multiprocess'
  Debugger::MultiProcess::pre_child
end