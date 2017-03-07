module ConfigView
  module ConfigViewHelper
    def plugin_task_type task
      task_view_service.getViewModel(task, "new").getTypeForDisplay()
    end

    def plugin_properties task
      props = task.getPropertiesForDisplay().collect do |property|
        [property.getName(), property.getValue()]
      end

      Hash[props]
    end

    def is_a_pluggable_task task
      task.getTaskType().start_with?(com.thoughtworks.go.config.pluggabletask.PluggableTask::PLUGGABLE_TASK_PREFIX)
    end
  end
end
