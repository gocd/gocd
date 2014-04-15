class Object
  # Prevent methods added to Object from being added to the 
  # blank-slate JavaPackageModuleTemplate
  class << self
    alias_method :java_package_method_added, :method_added

    def method_added(name)
      # If someone added a new method_added since we aliased original, then 
      # lets defer to that.  Otherwise run one we aliased.
      if self.class.superclass.instance_method(:method_added) != method(:java_package_method_added)
        result = super 
      else
        result = java_package_method_added(name)
      end
      JavaPackageModuleTemplate.__block__(name) if self == Object
      result
    end
    private :method_added
  end

  # include the class specified by +include_class+ into the current namespace,
  # using either its base name or by using a name returned from an optional block,
  # passing all specified classes in turn and providing the block package name
  # and base class name.
  def include_class(include_class)
    if include_class.respond_to? "java_class"
      # FIXME: When I changed this user const_set instead of eval below Comparator got lost
      # which means I am missing something.
      constant = include_class.java_class.to_s.split(".").last
      if (Module === self)
        return class_eval("#{constant} = include_class", __FILE__, __LINE__)
      else
        return eval("#{constant} = include_class", binding, __FILE__, __LINE__)
      end
    end
    
    if include_class.respond_to? "_name"
      return self.class.instance_eval { import(include_class._name) }
    end
    
    # else, pull in the class
    class_names = include_class.to_a

    class_names.each do |full_class_name|
      package_name, class_name = full_class_name.match(/((.*)\.)?([^\.]*)/)[2,3]

      if block_given?
        constant = yield(package_name, class_name)
      else
        constant = class_name
      end
      
      cls = self.kind_of?(Module) ? self : self.class

	  # Constant already exists...do not let proxy get created unless the collision is the proxy
	  # you are trying to include.
      existing_constant = cls.instance_eval(constant) rescue nil
      proxy = nil
      if existing_constant
        proxy = JavaUtilities.get_proxy_class(full_class_name)
        warn "redefining #{constant}" unless existing_constant == proxy
      end

      if existing_constant && existing_constant == proxy
        return proxy
      end

      # FIXME: When I changed this user const_set instead of eval below Comparator got lost
      # which means I am missing something.
      if (Module === self)
        class_eval("#{constant} = JavaUtilities.get_proxy_class(\"#{full_class_name}\")", __FILE__, __LINE__)
      else
        eval("#{constant} = JavaUtilities.get_proxy_class(\"#{full_class_name}\")", binding, __FILE__, __LINE__)
      end
    end
  end
  
  # TODO: this can go away now, but people may be using it
  def java_kind_of?(other)
    return true if self.kind_of?(other)
    return false unless self.respond_to?(:java_class) && other.respond_to?(:java_class) &&
      other.kind_of?(Module) && !self.kind_of?(Module) 
    return other.java_class.assignable_from?(self.java_class)
  end

  def java_import(*args, &block)
    include_class(*args, &block)
  end
  
  private :java_import

  def handle_different_imports(*args, &block)
    if args.first.respond_to?(:java_class)
      java_import(*args, &block)
    else
      other_import(*args, &block)
    end
  end
  
  if respond_to?(:import)
    alias :other_import :import
    alias :import :handle_different_imports
  else
    alias :import :java_import
    
    class << self
      alias_method :method_added_without_import_checking, :method_added
      
      def method_added(name)
        if name.to_sym == :import && defined?(@adding) && !@adding
          @adding = true
          alias_method :other_import, :import
          alias_method :import, :handle_different_imports
          @adding = false
        end
        method_added_without_import_checking(name)
      end
    end
  end
end
