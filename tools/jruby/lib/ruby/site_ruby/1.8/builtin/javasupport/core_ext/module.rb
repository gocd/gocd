# Extensions to the standard Module package.
class Module
  private

  ##
  # Includes a Java package into this class/module. The Java classes in the
  # package will become available in this class/module, unless a constant
  # with the same name as a Java class is already defined.
  #
  def include_package(package_name)
    if defined? @included_packages
      @included_packages << package_name      
      return
    end
    @included_packages = [package_name]
    @java_aliases ||= {}
    
    
      def self.const_missing(constant)
        real_name = @java_aliases[constant] || constant

        java_class = nil
        return super unless @included_packages.detect {|package|
            java_class = JavaUtilities.get_java_class(package + '.' + real_name.to_s)
        }
        
        JavaUtilities.create_proxy_class(constant, java_class, self)
      end
  end
  
  # Imports the package specified by +package_name+, first by trying to scan JAR resources
  # for the file in question, and failing that by adding a const_missing hook
  # to try that package when constants are missing.
  def import(package_name, &b)
    if package_name.respond_to?(:java_class) || (String === package_name && package_name.split(/\./).last =~ /^[A-Z]/)
      return super(package_name, &b)
    end

    package_name = package_name.package_name if package_name.respond_to?(:package_name)
    return include_package(package_name, &b)
  end

  def java_alias(new_id, old_id)
    @java_aliases[new_id] = old_id
  end
end
