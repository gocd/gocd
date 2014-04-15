unless respond_to?(:instance_exec)
  # based on Bounded Spec InstanceExec (Mauricio Fernandez)
  # http://eigenclass.org/hiki/bounded+space+instance_exec
  class Object
    module InstanceExecHelper; end
    include InstanceExecHelper
    def instance_exec(*args, &block)
      begin
        orig_critical, Thread.critical = Thread.critical, true
        n = 0
        n += 1 while respond_to?(method_name="__instance_exec#{n}")
        InstanceExecHelper.module_eval{ define_method(method_name, &block) }
      ensure
        Thread.critical = orig_critical
      end
      begin
        return send(method_name, *args)
      ensure
        InstanceExecHelper.module_eval{ remove_method(method_name) } rescue nil
      end
    end
  end
end
