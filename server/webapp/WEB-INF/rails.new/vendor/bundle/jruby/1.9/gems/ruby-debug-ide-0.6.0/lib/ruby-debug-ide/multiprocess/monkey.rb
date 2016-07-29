module Debugger
  module MultiProcess
    def self.create_mp_fork(private=false)
      %Q{
        alias pre_debugger_fork fork
    
        #{private ? "private" : ""}
        def fork(*args)
          if block_given?
            return pre_debugger_fork{Debugger::MultiProcess::pre_child; yield}
          end
          result = pre_debugger_fork
          Debugger::MultiProcess::pre_child unless result
          result
        end
      }
    end

    def self.create_mp_exec(private=false)
      %Q{
        alias pre_debugger_exec exec
        
        #{private ? "private" : ""}
        def exec(*args)
          Debugger.interface.close
          pre_debugger_exec(*args)
        end
      }
    end
  end
end

module Kernel
  class << self
    module_eval Debugger::MultiProcess.create_mp_fork
    module_eval Debugger::MultiProcess.create_mp_exec
  end
  module_eval Debugger::MultiProcess.create_mp_fork(true)
  module_eval Debugger::MultiProcess.create_mp_exec(true)
end

module Process
  class << self
    module_eval Debugger::MultiProcess.create_mp_fork
    module_eval Debugger::MultiProcess.create_mp_exec
  end
end