require 'thread'

module ActiveSupport #:nodoc:
  module Dependencies #:nodoc:
    extend self

    # Should we turn on Ruby warnings on the first load of dependent files?
    mattr_accessor :warnings_on_first_load
    self.warnings_on_first_load = false

    # All files ever loaded.
    mattr_accessor :history
    self.history = Set.new

    # All files currently loaded.
    mattr_accessor :loaded
    self.loaded = Set.new

    # Should we load files or require them?
    mattr_accessor :mechanism
    self.mechanism = :load

    # The set of directories from which we may automatically load files. Files
    # under these directories will be reloaded on each request in development mode,
    # unless the directory also appears in autoload_once_paths.
    mattr_accessor :autoload_paths
    self.autoload_paths = []
    
    # Deprecated, use autoload_paths.
    def self.load_paths
      ActiveSupport::Deprecation.warn("ActiveSupport::Dependencies.load_paths is deprecated, please use autoload_paths instead", caller)
      autoload_paths
    end

    # Deprecated, use autoload_paths=.
    def self.load_paths=(paths)
      ActiveSupport::Deprecation.warn("ActiveSupport::Dependencies.load_paths= is deprecated, please use autoload_paths= instead", caller)
      self.autoload_paths = paths
    end

    # The set of directories from which automatically loaded constants are loaded
    # only once. All directories in this set must also be present in +autoload_paths+.
    mattr_accessor :autoload_once_paths
    self.autoload_once_paths = []

    # Deprecated, use autoload_once_paths.
    def self.load_once_paths
      ActiveSupport::Deprecation.warn("ActiveSupport::Dependencies.load_once_paths is deprecated and removed in Rails 3, please use autoload_once_paths instead", caller)
      autoload_once_paths
    end

    # Deprecated, use autoload_once_paths=.
    def self.load_once_paths=(paths)
      ActiveSupport::Deprecation.warn("ActiveSupport::Dependencies.load_once_paths= is deprecated and removed in Rails 3, please use autoload_once_paths= instead", caller)
      self.autoload_once_paths = paths
    end

    # An array of qualified constant names that have been loaded. Adding a name to
    # this array will cause it to be unloaded the next time Dependencies are cleared.
    mattr_accessor :autoloaded_constants
    self.autoloaded_constants = []

    # An array of constant names that need to be unloaded on every request. Used
    # to allow arbitrary constants to be marked for unloading.
    mattr_accessor :explicitly_unloadable_constants
    self.explicitly_unloadable_constants = []

    # The logger is used for generating information on the action run-time (including benchmarking) if available.
    # Can be set to nil for no logging. Compatible with both Ruby's own Logger and Log4r loggers.
    mattr_accessor :logger

    # Set to true to enable logging of const_missing and file loads
    mattr_accessor :log_activity
    self.log_activity = false

    # An internal stack used to record which constants are loaded by any block.
    mattr_accessor :constant_watch_stack
    self.constant_watch_stack = []

    mattr_accessor :constant_watch_stack_mutex
    self.constant_watch_stack_mutex = Mutex.new

    # Module includes this module
    module ModuleConstMissing #:nodoc:
      def self.included(base) #:nodoc:
        base.class_eval do
          unless defined? const_missing_without_dependencies
            alias_method_chain :const_missing, :dependencies
          end
        end
      end

      def self.excluded(base) #:nodoc:
        base.class_eval do
          if defined? const_missing_without_dependencies
            undef_method :const_missing
            alias_method :const_missing, :const_missing_without_dependencies
            undef_method :const_missing_without_dependencies
          end
        end
      end

      # Use const_missing to autoload associations so we don't have to
      # require_association when using single-table inheritance.
      def const_missing_with_dependencies(class_id)
        ActiveSupport::Dependencies.load_missing_constant self, class_id
      end

      def unloadable(const_desc = self)
        super(const_desc)
      end
    end

    # Class includes this module
    module ClassConstMissing #:nodoc:
      def const_missing(const_name)
        if [Object, Kernel].include?(self) || parent == self
          super
        else
          begin
            begin
              Dependencies.load_missing_constant self, const_name
            rescue NameError
              parent.send :const_missing, const_name
            end
          rescue NameError => e
            # Make sure that the name we are missing is the one that caused the error
            parent_qualified_name = Dependencies.qualified_name_for parent, const_name
            raise unless e.missing_name? parent_qualified_name
            qualified_name = Dependencies.qualified_name_for self, const_name
            raise NameError.new("uninitialized constant #{qualified_name}").copy_blame!(e)
          end
        end
      end
    end

    # Object includes this module
    module Loadable #:nodoc:
      def self.included(base) #:nodoc:
        base.class_eval do
          unless defined? load_without_new_constant_marking
            alias_method_chain :load, :new_constant_marking
          end
        end
      end

      def self.excluded(base) #:nodoc:
        base.class_eval do
          if defined? load_without_new_constant_marking
            undef_method :load
            alias_method :load, :load_without_new_constant_marking
            undef_method :load_without_new_constant_marking
          end
        end
      end

      def require_or_load(file_name)
        Dependencies.require_or_load(file_name)
      end

      def require_dependency(file_name)
        Dependencies.depend_on(file_name)
      end

      def require_association(file_name)
        Dependencies.associate_with(file_name)
      end

      def load_with_new_constant_marking(file, *extras) #:nodoc:
        if Dependencies.load?
          Dependencies.new_constants_in(Object) { load_without_new_constant_marking(file, *extras) }
        else
          load_without_new_constant_marking(file, *extras)
        end
      rescue Exception => exception  # errors from loading file
        exception.blame_file! file
        raise
      end

      def require(file, *extras) #:nodoc:
        if Dependencies.load?
          Dependencies.new_constants_in(Object) { super }
        else
          super
        end
      rescue Exception => exception  # errors from required file
        exception.blame_file! file
        raise
      end

      # Mark the given constant as unloadable. Unloadable constants are removed each
      # time dependencies are cleared.
      #
      # Note that marking a constant for unloading need only be done once. Setup
      # or init scripts may list each unloadable constant that may need unloading;
      # each constant will be removed for every subsequent clear, as opposed to for
      # the first clear.
      #
      # The provided constant descriptor may be a (non-anonymous) module or class,
      # or a qualified constant name as a string or symbol.
      #
      # Returns true if the constant was not previously marked for unloading, false
      # otherwise.
      def unloadable(const_desc)
        Dependencies.mark_for_unload const_desc
      end
    end

    # Exception file-blaming
    module Blamable #:nodoc:
      def blame_file!(file)
        (@blamed_files ||= []).unshift file
      end

      def blamed_files
        @blamed_files ||= []
      end

      def describe_blame
        return nil if blamed_files.empty?
        "This error occurred while loading the following files:\n   #{blamed_files.join "\n   "}"
      end

      def copy_blame!(exc)
        @blamed_files = exc.blamed_files.clone
        self
      end
    end

    def hook!
      Object.instance_eval { include Loadable }
      Module.instance_eval { include ModuleConstMissing }
      Class.instance_eval { include ClassConstMissing }
      Exception.instance_eval { include Blamable }
      true
    end

    def unhook!
      ModuleConstMissing.excluded(Module)
      Loadable.excluded(Object)
      true
    end

    def load?
      mechanism == :load
    end

    def depend_on(file_name, swallow_load_errors = false)
      path = search_for_file(file_name)
      require_or_load(path || file_name)
    rescue LoadError
      raise unless swallow_load_errors
    end

    def associate_with(file_name)
      depend_on(file_name, true)
    end

    def clear
      log_call
      loaded.clear
      remove_unloadable_constants!
    end

    def require_or_load(file_name, const_path = nil)
      log_call file_name, const_path
      file_name = $1 if file_name =~ /^(.*)\.rb$/
      expanded = File.expand_path(file_name)
      return if loaded.include?(expanded)

      # Record that we've seen this file *before* loading it to avoid an
      # infinite loop with mutual dependencies.
      loaded << expanded

      begin
        if load?
          log "loading #{file_name}"

          # Enable warnings iff this file has not been loaded before and
          # warnings_on_first_load is set.
          load_args = ["#{file_name}.rb"]
          load_args << const_path unless const_path.nil?

          if !warnings_on_first_load or history.include?(expanded)
            result = load_file(*load_args)
          else
            enable_warnings { result = load_file(*load_args) }
          end
        else
          log "requiring #{file_name}"
          result = require file_name
        end
      rescue Exception
        loaded.delete expanded
        raise
      end

      # Record history *after* loading so first load gets warnings.
      history << expanded
      return result
    end

    # Is the provided constant path defined?
    def qualified_const_defined?(path)
      raise NameError, "#{path.inspect} is not a valid constant name!" unless
        /^(::)?([A-Z]\w*)(::[A-Z]\w*)*$/ =~ path

      names = path.to_s.split('::')
      names.shift if names.first.empty?

      # We can't use defined? because it will invoke const_missing for the parent
      # of the name we are checking.
      names.inject(Object) do |mod, name|
        return false unless uninherited_const_defined?(mod, name)
        mod.const_get name
      end
      return true
    end

    if Module.method(:const_defined?).arity == 1
      # Does this module define this constant?
      # Wrapper to accomodate changing Module#const_defined? in Ruby 1.9
      def uninherited_const_defined?(mod, const)
        mod.const_defined?(const)
      end
    else
      def uninherited_const_defined?(mod, const) #:nodoc:
        mod.const_defined?(const, false)
      end
    end

    # Given +path+, a filesystem path to a ruby file, return an array of constant
    # paths which would cause Dependencies to attempt to load this file.
    def loadable_constants_for_path(path, bases = autoload_paths)
      path = $1 if path =~ /\A(.*)\.rb\Z/
      expanded_path = File.expand_path(path)

      bases.collect do |root|
        expanded_root = File.expand_path(root)
        next unless %r{\A#{Regexp.escape(expanded_root)}(/|\\)} =~ expanded_path

        nesting = expanded_path[(expanded_root.size)..-1]
        nesting = nesting[1..-1] if nesting && nesting[0] == ?/
        next if nesting.blank?
        nesting_camel = nesting.camelize
        begin
          qualified_const_defined?(nesting_camel)
        rescue NameError
          next
        end
        [ nesting_camel ]
      end.flatten.compact.uniq
    end

    # Search for a file in autoload_paths matching the provided suffix.
    def search_for_file(path_suffix)
      path_suffix = path_suffix + '.rb' unless path_suffix.ends_with? '.rb'
      autoload_paths.each do |root|
        path = File.join(root, path_suffix)
        return path if File.file? path
      end
      nil # Gee, I sure wish we had first_match ;-)
    end

    # Does the provided path_suffix correspond to an autoloadable module?
    # Instead of returning a boolean, the autoload base for this module is returned.
    def autoloadable_module?(path_suffix)
      autoload_paths.each do |load_path|
        return load_path if File.directory? File.join(load_path, path_suffix)
      end
      nil
    end

    def load_once_path?(path)
      autoload_once_paths.any? { |base| path.starts_with? base }
    end

    # Attempt to autoload the provided module name by searching for a directory
    # matching the expect path suffix. If found, the module is created and assigned
    # to +into+'s constants with the name +const_name+. Provided that the directory
    # was loaded from a reloadable base path, it is added to the set of constants
    # that are to be unloaded.
    def autoload_module!(into, const_name, qualified_name, path_suffix)
      return nil unless base_path = autoloadable_module?(path_suffix)
      mod = Module.new
      into.const_set const_name, mod
      autoloaded_constants << qualified_name unless autoload_once_paths.include?(base_path)
      return mod
    end

    # Load the file at the provided path. +const_paths+ is a set of qualified
    # constant names. When loading the file, Dependencies will watch for the
    # addition of these constants. Each that is defined will be marked as
    # autoloaded, and will be removed when Dependencies.clear is next called.
    #
    # If the second parameter is left off, then Dependencies will construct a set
    # of names that the file at +path+ may define. See
    # +loadable_constants_for_path+ for more details.
    def load_file(path, const_paths = loadable_constants_for_path(path))
      log_call path, const_paths
      const_paths = [const_paths].compact unless const_paths.is_a? Array
      parent_paths = const_paths.collect { |const_path| /(.*)::[^:]+\Z/ =~ const_path ? $1 : :Object }

      result = nil
      newly_defined_paths = new_constants_in(*parent_paths) do
        result = load_without_new_constant_marking path
      end

      autoloaded_constants.concat newly_defined_paths unless load_once_path?(path)
      autoloaded_constants.uniq!
      log "loading #{path} defined #{newly_defined_paths * ', '}" unless newly_defined_paths.empty?
      return result
    end

    # Return the constant path for the provided parent and constant name.
    def qualified_name_for(mod, name)
      mod_name = to_constant_name mod
      (%w(Object Kernel).include? mod_name) ? name.to_s : "#{mod_name}::#{name}"
    end

    # Load the constant named +const_name+ which is missing from +from_mod+. If
    # it is not possible to load the constant into from_mod, try its parent module
    # using const_missing.
    def load_missing_constant(from_mod, const_name)
      log_call from_mod, const_name
      if from_mod == Kernel
        if ::Object.const_defined?(const_name)
          log "Returning Object::#{const_name} for Kernel::#{const_name}"
          return ::Object.const_get(const_name)
        else
          log "Substituting Object for Kernel"
          from_mod = Object
        end
      end

      # If we have an anonymous module, all we can do is attempt to load from Object.
      from_mod = Object if from_mod.name.blank?

      unless qualified_const_defined?(from_mod.name) && from_mod.name.constantize.object_id == from_mod.object_id
        raise ArgumentError, "A copy of #{from_mod} has been removed from the module tree but is still active!"
      end

      raise ArgumentError, "#{from_mod} is not missing constant #{const_name}!" if uninherited_const_defined?(from_mod, const_name)

      qualified_name = qualified_name_for from_mod, const_name
      path_suffix = qualified_name.underscore
      name_error = NameError.new("uninitialized constant #{qualified_name}")

      file_path = search_for_file(path_suffix)
      if file_path && ! loaded.include?(File.expand_path(file_path)) # We found a matching file to load
        require_or_load file_path
        raise LoadError, "Expected #{file_path} to define #{qualified_name}" unless uninherited_const_defined?(from_mod, const_name)
        return from_mod.const_get(const_name)
      elsif mod = autoload_module!(from_mod, const_name, qualified_name, path_suffix)
        return mod
      elsif (parent = from_mod.parent) && parent != from_mod &&
            ! from_mod.parents.any? { |p| uninherited_const_defined?(p, const_name) }
        # If our parents do not have a constant named +const_name+ then we are free
        # to attempt to load upwards. If they do have such a constant, then this
        # const_missing must be due to from_mod::const_name, which should not
        # return constants from from_mod's parents.
        begin
          return parent.const_missing(const_name)
        rescue NameError => e
          raise unless e.missing_name? qualified_name_for(parent, const_name)
          raise name_error
        end
      else
        raise name_error
      end
    end

    # Remove the constants that have been autoloaded, and those that have been
    # marked for unloading.
    def remove_unloadable_constants!
      autoloaded_constants.each { |const| remove_constant const }
      autoloaded_constants.clear
      explicitly_unloadable_constants.each { |const| remove_constant const }
    end

    # Determine if the given constant has been automatically loaded.
    def autoloaded?(desc)
      # No name => anonymous module.
      return false if desc.is_a?(Module) && desc.name.blank?
      name = to_constant_name desc
      return false unless qualified_const_defined? name
      return autoloaded_constants.include?(name)
    end

    # Will the provided constant descriptor be unloaded?
    def will_unload?(const_desc)
      autoloaded?(const_desc) ||
        explicitly_unloadable_constants.include?(to_constant_name(const_desc))
    end

    # Mark the provided constant name for unloading. This constant will be
    # unloaded on each request, not just the next one.
    def mark_for_unload(const_desc)
      name = to_constant_name const_desc
      if explicitly_unloadable_constants.include? name
        return false
      else
        explicitly_unloadable_constants << name
        return true
      end
    end

    # Run the provided block and detect the new constants that were loaded during
    # its execution. Constants may only be regarded as 'new' once -- so if the
    # block calls +new_constants_in+ again, then the constants defined within the
    # inner call will not be reported in this one.
    #
    # If the provided block does not run to completion, and instead raises an
    # exception, any new constants are regarded as being only partially defined
    # and will be removed immediately.
    def new_constants_in(*descs)
      log_call(*descs)

      # Build the watch frames. Each frame is a tuple of
      #   [module_name_as_string, constants_defined_elsewhere]
      watch_frames = descs.collect do |desc|
        if desc.is_a? Module
          mod_name = desc.name
          initial_constants = desc.local_constant_names
        elsif desc.is_a?(String) || desc.is_a?(Symbol)
          mod_name = desc.to_s

          # Handle the case where the module has yet to be defined.
          initial_constants = if qualified_const_defined?(mod_name)
            mod_name.constantize.local_constant_names
          else
            []
          end
        else
          raise Argument, "#{desc.inspect} does not describe a module!"
        end

        [mod_name, initial_constants]
      end

      constant_watch_stack_mutex.synchronize do
        constant_watch_stack.concat watch_frames
      end

      aborting = true
      begin
        yield # Now yield to the code that is to define new constants.
        aborting = false
      ensure
        # Find the new constants.
        new_constants = watch_frames.collect do |mod_name, prior_constants|
          # Module still doesn't exist? Treat it as if it has no constants.
          next [] unless qualified_const_defined?(mod_name)

          mod = mod_name.constantize
          next [] unless mod.is_a? Module
          new_constants = mod.local_constant_names - prior_constants

          # Make sure no other frames takes credit for these constants.
          constant_watch_stack_mutex.synchronize do
            constant_watch_stack.each do |frame_name, constants|
              constants.concat new_constants if frame_name == mod_name
            end
          end

          new_constants.collect do |suffix|
            mod_name == "Object" ? suffix : "#{mod_name}::#{suffix}"
          end
        end.flatten

        log "New constants: #{new_constants * ', '}"

        if aborting
          log "Error during loading, removing partially loaded constants "
          new_constants.each { |name| remove_constant name }
          new_constants.clear
        end
      end

      return new_constants
    ensure
      # Remove the stack frames that we added.
      if defined?(watch_frames) && ! watch_frames.blank?
        frame_ids = watch_frames.collect { |frame| frame.object_id }
        constant_watch_stack_mutex.synchronize do
          constant_watch_stack.delete_if do |watch_frame|
            frame_ids.include? watch_frame.object_id
          end
        end
      end
    end

    class LoadingModule #:nodoc:
      # Old style environment.rb referenced this method directly.  Please note, it doesn't
      # actually *do* anything any more.
      def self.root(*args)
        if defined?(Rails) && Rails.logger
          Rails.logger.warn "Your environment.rb uses the old syntax, it may not continue to work in future releases."
          Rails.logger.warn "For upgrade instructions please see: http://manuals.rubyonrails.com/read/book/19"
        end
      end
    end

    # Convert the provided const desc to a qualified constant name (as a string).
    # A module, class, symbol, or string may be provided.
    def to_constant_name(desc) #:nodoc:
      name = case desc
        when String then desc.starts_with?('::') ? desc[2..-1] : desc
        when Symbol then desc.to_s
        when Module
          raise ArgumentError, "Anonymous modules have no name to be referenced by" if desc.name.blank?
          desc.name
        else raise TypeError, "Not a valid constant descriptor: #{desc.inspect}"
      end
    end

    def remove_constant(const) #:nodoc:
      return false unless qualified_const_defined? const

      const = $1 if /\A::(.*)\Z/ =~ const.to_s
      names = const.to_s.split('::')
      if names.size == 1 # It's under Object
        parent = Object
      else
        parent = (names[0..-2] * '::').constantize
      end

      log "removing constant #{const}"
      parent.instance_eval { remove_const names.last }
      return true
    end

    protected
      def log_call(*args)
        if logger && log_activity
          arg_str = args.collect { |arg| arg.inspect } * ', '
          /in `([a-z_\?\!]+)'/ =~ caller(1).first
          selector = $1 || '<unknown>'
          log "called #{selector}(#{arg_str})"
        end
      end

      def log(msg)
        if logger && log_activity
          logger.debug "Dependencies: #{msg}"
        end
      end
  end
end

ActiveSupport::Dependencies.hook!
