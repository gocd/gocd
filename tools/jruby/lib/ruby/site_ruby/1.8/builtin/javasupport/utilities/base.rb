module JavaUtilities
  def JavaUtilities.extend_proxy(java_class_name, &block)
    java_class = Java::JavaClass.for_name(java_class_name)
    java_class.extend_proxy JavaInterfaceExtender.new(java_class_name, &block)
  end

  def JavaUtilities.print_class(java_type, indent="")
     while (!java_type.nil? && java_type.name != "java.lang.Class")
        puts "#{indent}Name:  #{java_type.name}, access: #{ JavaUtilities.access(java_type) }  Interfaces: "
        java_type.interfaces.each { |i| print_class(i, "  #{indent}") }
        puts "#{indent}SuperClass: "
        print_class(java_type.superclass, "  #{indent}")
        java_type = java_type.superclass
     end
  end

end
