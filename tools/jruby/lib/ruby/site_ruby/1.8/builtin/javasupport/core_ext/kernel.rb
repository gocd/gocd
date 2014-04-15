# Prevent methods added to Kernel from being added to the 
# blank-slate JavaPackageModuleTemplate
module Kernel
  class << self
    alias_method :java_package_method_added, :method_added

    def method_added(name)
      result = java_package_method_added(name)
      JavaPackageModuleTemplate.__block__(name) if self == Kernel
      result
    end
    private :method_added
  end
end

# Create convenience methods for top-level java packages so we do not need to prefix
# with 'Java::'.  We undef these methods within Package in case we run into 'com.foo.com'.
[:java, :javax, :com, :org].each do |meth|
  Kernel.module_eval <<-EOM
    def #{meth}
      JavaUtilities.get_package_module_dot_format('#{meth}')    
    end
  EOM
end
