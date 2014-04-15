
require 'java'
require 'rdoc/rdoc'

module JRuby
  class RDoc
    class AnnotationParser
      attr_accessor :progress
      # prepare to parse a Java class with annotations
      def initialize(top_level, clazz, options, stats)
        @options = options
        @top_level = top_level
        @classes = Hash.new
        @progress = $stderr unless options.quiet
        @clazz = clazz
        @stats = stats
      end

      def scan
        extract_class_information(@clazz)
        @top_level
      end

      #######
      private
      #######

      def progress(char)
        unless @options.quiet
          @progress.print(char)
          @progress.flush
        end
      end

      def warn(msg)
        $stderr.puts
        $stderr.puts msg
        $stderr.flush
      end      

      JRubyMethodAnnotation = org.jruby.anno.JRubyMethod.java_class
      JRubyClassAnnotation = org.jruby.anno.JRubyClass.java_class
      JRubyModuleAnnotation = org.jruby.anno.JRubyModule.java_class
      
      def handle_class_module(clazz, class_mod, annotation, type_annotation, enclosure)
        type_annotation.name.to_a.each do |name|
          progress(class_mod[0, 1])

          parent = class_mod == 'class' ? type_annotation.parent : nil
          

          if class_mod == "class" 
            cm = enclosure.add_class(::RDoc::NormalClass, name, parent)
            @stats.num_classes += 1
          else
            cm = enclosure.add_module(::RDoc::NormalModule, name)
            @stats.num_modules += 1
          end

          cm.record_location(enclosure.toplevel)
          type_annotation.include.to_a.each do |inc|
            cm.add_include(::RDoc::Include.new(inc, ""))
          end
          
          find_class_comment(clazz, annotation, cm)
          
          handle_methods(clazz, cm)
        end
      end
      
      def handle_methods(clazz, enclosure)
        clazz.java_class.declared_class_methods.each do |method|
          if method.annotation_present?(JRubyMethodAnnotation)
            handle_method(clazz, method, enclosure)
          end
        end
        
        clazz.java_class.declared_instance_methods.each do |method|
          if method.annotation_present?(JRubyMethodAnnotation)
            handle_method(clazz, method, enclosure)
          end
        end
      end
      
      def handle_method(clazz, method, enclosure)
        progress(".")

        @stats.num_methods += 1

        anno = method.annotation(JRubyMethodAnnotation)
        
        meth_name = anno.name.to_a.first || method.name
        type = anno.meta ? "singleton_method" : "instance_method"

        if meth_name == "initialize"
          meth_name = "new"
          type = "singleton_method"
        end

        meth_obj = ::RDoc::AnyMethod.new("", meth_name)
        meth_obj.singleton = type == "singleton_method"

        
        p_count = (anno.optional == 0 && !anno.rest) ? anno.required : -1
        
        if p_count < 0
          meth_obj.params = "(...)"
        elsif p_count == 0
          meth_obj.params = "()"
        else
          meth_obj.params = "(" +
                            (1..p_count).map{|i| "p#{i}"}.join(", ") + 
                                                ")"
        end

        find_method_comment(nil,
                            #method.annotation(RDocAnnotation),
                            meth_obj)

        enclosure.add_method(meth_obj)

        meth_obj.visibility = case anno.visibility
                                when org.jruby.runtime.Visibility::PUBLIC: :public
                                when org.jruby.runtime.Visibility::PROTECTED: :protected
                                when org.jruby.runtime.Visibility::PRIVATE: :private
                                when org.jruby.runtime.Visibility::MODULE_FUNCTION: :public
                              end
        
        if anno.name.to_a.length > 1
          anno.name.to_a[1..-1].each do |al|
            new_meth = ::RDoc::AnyMethod.new("", al)
            new_meth.is_alias_for = meth_obj
            new_meth.singleton    = meth_obj.singleton
            new_meth.params       = meth_obj.params
            new_meth.comment = "Alias for \##{meth_obj.name}"
            meth_obj.add_alias(new_meth)
            enclosure.add_method(new_meth)
            new_meth.visibility = meth_obj.visibility
          end
        end
      end

      def find_class_comment(clazz, doc_annotation, class_meth)
        if doc_annotation
          class_meth.comment = doc_annotation.doc
        end
      end

      def find_method_comment(doc_annotation, meth)
        if doc_annotation
          call_seq = (doc_annotation.call_seq || []).to_a.join("\n")

          if call_seq != ""
            call_seq << "\n\n"
          end

          meth.comment = call_seq + doc_annotation.doc
        end
      end
      
      def extract_class_information(clazz)
        class_anno = clazz.java_class.annotation(JRubyClassAnnotation)
        module_anno = clazz.java_class.annotation(JRubyModuleAnnotation)
#        doc_anno = clazz.java_class.annotation(RDocAnnotation)
        doc_anno = nil
        if (class_anno || module_anno)
          $stderr.printf("%70s: ", clazz.java_class.to_s) unless @options.quiet
          class_mod = if class_anno
                        "class"
                      else
                        "module"
                      end
          
          handle_class_module(clazz, class_mod, doc_anno, class_anno || module_anno, @top_level)

          if class_anno && module_anno
            handle_class_module(clazz, "module", doc_anno, module_anno, @top_level)
          end
          
          $stderr.puts unless @options.quiet
        end
      end
    end
    
    INTERNAL_PACKAGES = %w(org.jruby.yaml org.jruby.util org.jruby.runtime org.jruby.ast org.jruby.internal org.jruby.lexer org.jruby.evaluator org.jruby.compiler org.jruby.parser org.jruby.exceptions org.jruby.demo org.jruby.environment org.jruby.JRubyApplet)
    INTERNAL_PACKAGES_RE = INTERNAL_PACKAGES.map{ |ip| /^#{ip}/ }
    INTERNAL_PACKAGE_RE = Regexp::union(*INTERNAL_PACKAGES_RE)
    
    class << self
      def find_classes_from_jar(jar, package)
        file = java.util.jar.JarFile.new(jar.toString[5..-1], false)
        beginning = %r[^(#{package.empty? ? '' : (package.join("/") + "/")}.*)\.class$]

        result = []
        file.entries.each do |e|
          if /Invoker\$/ !~ e.to_s && beginning =~ e.to_s
            class_name = $1.gsub('/', '.')
            if INTERNAL_PACKAGE_RE !~ class_name
              result << class_name
            end
          end
        end

        result
      end

      def find_classes_from_directory(dir, package)
        raise "not implemented yet"
      end

      def find_classes_from_location(location, package)
        if /\.jar$/ =~ location.to_s
          find_classes_from_jar(location, package)
        else 
          find_classes_from_directory(location, package)
        end
      end

      
      # Executes a block inside of a context where the named method on the object in question will just return nil
      # without actually doing anything. Useful to stub out things temporarily
      def returning_nil(object, method_name) 
        singleton_object = (class << object; self; end)
        singleton_object.send :alias_method, :"#{method_name}_with_real_functionality", method_name
        singleton_object.send :define_method, method_name do |*args|
          nil
        end

        begin 
          result = yield
        ensure
          singleton_object.send(:alias_method, method_name, :"#{method_name}_with_real_functionality")
        end
        result
      end
    
      # Returns an array of TopLevel
      def extract_rdoc_information_from_classes(classes, options, stats)
        result = []
        classes.each do |clzz|
          tp = returning_nil(File, :stat) { ::RDoc::TopLevel.new(clzz.java_class.to_s) }
          result << AnnotationParser.new(tp, clzz, options, stats).scan
          stats.num_files += 1
        end
        result
      end
      
      def install_doc(package = [])
        r = ::RDoc::RDoc.new
        
        # So parse_files should actually return an array of TopLevel objects
        (class << r; self; end).send(:define_method, :parse_files) do |options|
          location = org.jruby.Ruby.java_class.protection_domain.code_source.location

          class_names = JRuby::RDoc::find_classes_from_location(location, package)

          classes = class_names.map {|c| JavaUtilities.get_proxy_class(c) }

          JRuby::RDoc::extract_rdoc_information_from_classes(classes, options, @stats)
        end
        
        r.document(%w(--all --ri-system))
      end
    end
  end
end
