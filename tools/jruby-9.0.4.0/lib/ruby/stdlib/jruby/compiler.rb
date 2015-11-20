require 'optparse'
require 'fileutils'
require 'digest/sha1'
require 'jruby'
require 'jruby/compiler/java_class'

module JRuby::Compiler
  BAIS = java.io.ByteArrayInputStream
  Mangler = org.jruby.util.JavaNameMangler
  Opcodes = org.objectweb.asm.Opcodes rescue org.jruby.org.objectweb.asm.Opcodes
  ClassWriter = org.objectweb.asm.ClassWriter rescue org.jruby.org.objectweb.asm.ClassWriter
  SkinnyMethodAdapter = org.jruby.compiler.impl.SkinnyMethodAdapter
  ByteArrayOutputStream = java.io.ByteArrayOutputStream
  IRWriterStream = org.jruby.ir.persistence.IRWriterStream
  IRWriter = org.jruby.ir.persistence.IRWriter
  JavaFile = java.io.File
  MethodSignatureNode = org.jruby.ast.java_signature.MethodSignatureNode
  DEFAULT_PREFIX = ""

  def default_options
    {
      :basedir => Dir.pwd,
      :prefix => DEFAULT_PREFIX,
      :target => Dir.pwd,
      :java => false,
      :javac => false,
      :classpath => [],
      :javac_options => [],
      :sha1 => false,
      :handles => false,
      :verbose => false
    }
  end
  module_function :default_options
  
  def compile_argv(argv)
    options = default_options

    OptionParser.new("", 24, '  ') do |opts|
      opts.banner = "jrubyc [options] (FILE|DIRECTORY)"
      opts.separator ""

      opts.on("-d", "--dir DIR", "Use DIR as the base path") do |dir|
        options[:basedir] = dir
      end

      opts.on("-p", "--prefix PREFIX", "Prepend PREFIX to the file path and package. Default is no prefix.") do |pre|
        options[:prefix] = pre
      end

      opts.on("-t", "--target TARGET", "Output files to TARGET directory") do |tgt|
        options[:target] = tgt
      end

      opts.on("-J OPTION", "Pass OPTION to javac for javac compiles") do |tgt|
        options[:javac_options] << tgt
      end

      opts.on("-5"," --jdk5", "Generate JDK 5 classes (version 49)") do |x|
        options[:jdk5] = true
      end

      opts.on("--java", "Generate .java classes to accompany the script") do
        options[:java] = true
      end

      opts.on("--javac", "Generate and compile .java classes to accompany the script") do
        options[:javac] = true
      end

      opts.on("-c", "--classpath CLASSPATH", "Add a jar to the classpath for building") do |cp|
        options[:classpath].concat cp.split(':')
      end

      opts.on("--sha1", "Compile to a class named using the SHA1 hash of the source file") do
        options[:sha1] = true
      end

      opts.on("--handles", "Also generate all direct handle classes for the source file") do
        options[:handles] = true
      end
      
      opts.on("--verbose", "Log verbose output while compile") do
        options[:verbose] = true
      end

      opts.parse!(argv)
    end

    if (argv.length == 0)
      raise "No files or directories specified"
    end

    compile_files_with_options(argv, options)
  end
  module_function :compile_argv

  # deprecated, but retained for backward compatibility
  def compile_files(filenames, basedir = Dir.pwd, prefix = DEFAULT_PREFIX, target = Dir.pwd, java = false, javac = false, javac_options = [], classpath = [])
    compile_files_with_options(
      filenames,
      :basedir => basedir,
      :prefix => prefix,
      :target => target,
      :java => java,
      :javac => javac,
      :javac_options => javac_options,
      :classpath => classpath,
      :sha1 => false,
      :handles => false,
      :verbose => false
    )
  end
  module_function :compile_files
  
  def compile_files_with_options(filenames, options = default_options)
    runtime = JRuby.runtime

    unless File.exist? options[:target]
      raise "Target dir not found: #{options[:target]}"
    end

    files = []

    # The compilation code
    compile_proc = proc do |filename|
      begin
        file = File.open(filename, "r:ASCII-8BIT")

        if options[:sha1]
          pathname = "ruby.jit.FILE_" + Digest::SHA1.hexdigest(File.read(filename)).upcase
        else
          pathname = Mangler.mangle_filename_for_classpath(filename, options[:basedir], options[:prefix], true, false)
        end

        source = file.read

        if options[:java] || options[:javac]
          node = runtime.parse_file(BAIS.new(source.to_java_bytes), filename, nil)

          ruby_script = JavaGenerator.generate_java(node, filename)
          ruby_script.classes.each do |cls|
            java_dir = File.join(options[:target], cls.package.gsub('.', '/'))

            FileUtils.mkdir_p java_dir

            java_src = File.join(java_dir, cls.name + ".java")
            puts "Generating Java class #{cls.name} to #{java_src}" if options[:verbose]
            
            files << java_src

            File.open(java_src, 'w') do |f|
              f.write(cls.to_s)
            end
          end
        else
          puts "Compiling #{filename}" if options[:verbose]

          scope = JRuby.compile_ir(source, filename)
          bytes = ByteArrayOutputStream.new
          stream = IRWriterStream.new(bytes)
          IRWriter.persist(stream, scope)
          string = String.from_java_bytes(bytes.to_byte_array, 'BINARY')

          # bust it up into 32k-1 chunks
          pieces = string.scan(/.{1,32767}/m)

          cls = ClassWriter.new(ClassWriter::COMPUTE_MAXS | ClassWriter::COMPUTE_FRAMES)
          cls.visit(
              Opcodes::V1_7,
              Opcodes::ACC_PUBLIC,
              pathname.gsub(".", "/"),
              nil,
              "java/lang/Object",
              nil
          )
          cls.visit_source filename, nil

          cls.visit_field(
              Opcodes::ACC_PRIVATE | Opcodes::ACC_STATIC | Opcodes::ACC_FINAL,
              "script_ir",
              "Ljava/lang/String;",
              nil,
              nil
          )

          static = SkinnyMethodAdapter.new(
              cls,
              Opcodes::ACC_PUBLIC | Opcodes::ACC_STATIC,
              "<clinit>",
              "()V",
              nil,
              nil)
          static.start

          # put String back together
          static.newobj("java/lang/StringBuilder")
          static.dup
          static.invokespecial("java/lang/StringBuilder", "<init>", "()V")
          pieces.each do |piece|
            static.ldc(piece)
            static.invokevirtual("java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;")
          end
          static.invokevirtual("java/lang/Object", "toString", "()Ljava/lang/String;")
          static.putstatic(pathname, "script_ir", "Ljava/lang/String;")
          static.voidreturn
          static.end
          
          main = SkinnyMethodAdapter.new(
              cls,
              Opcodes::ACC_PUBLIC | Opcodes::ACC_STATIC,
              "main",
              "([Ljava/lang/String;)V",
              nil,
              nil)
          main.start
          main.invokestatic("org/jruby/Ruby", "newInstance", "()Lorg/jruby/Ruby;")
          main.astore(1)
          main.aload(1)
          main.aload(1)

          main.getstatic(pathname, "script_ir", "Ljava/lang/String;")

          main.ldc("ISO-8859-1")
          main.invokevirtual("java/lang/String", "getBytes", "(Ljava/lang/String;)[B")
          main.ldc(filename) # TODO: can we determine actual path to this class?
          main.invokestatic("org/jruby/ir/runtime/IRRuntimeHelpers", "decodeScopeFromBytes", "(Lorg/jruby/Ruby;[BLjava/lang/String;)Lorg/jruby/ir/IRScope;")
          main.invokevirtual("org/jruby/Ruby", "runInterpreter", "(Lorg/jruby/ParseResult;)Lorg/jruby/runtime/builtin/IRubyObject;")
          main.voidreturn
          main.end

          loadIR = SkinnyMethodAdapter.new(
              cls,
              Opcodes::ACC_PUBLIC | Opcodes::ACC_STATIC,
              "loadIR",
              "(Lorg/jruby/Ruby;Ljava/lang/String;)Lorg/jruby/ir/IRScope;",
              nil,
              nil)
          loadIR.start
          loadIR.aload(0)

          loadIR.getstatic(pathname, "script_ir", "Ljava/lang/String;")

          loadIR.ldc("ISO-8859-1")
          loadIR.invokevirtual("java/lang/String", "getBytes", "(Ljava/lang/String;)[B")
          loadIR.aload(1)
          loadIR.invokestatic("org/jruby/ir/runtime/IRRuntimeHelpers", "decodeScopeFromBytes", "(Lorg/jruby/Ruby;[BLjava/lang/String;)Lorg/jruby/ir/IRScope;")
          loadIR.areturn
          loadIR.end

          # prepare target
          class_filename = filename.sub(/(\.rb)?$/, '.class')
          target_file = File.join(options[:target], class_filename)
          target_dir = File.dirname(target_file)
          FileUtils.mkdir_p(target_dir)

          # write class
          File.open(target_file, 'wb') do |f|
            f.write(cls.to_byte_array)
          end
        end

        0
      # rescue Exception
      #   puts "Failure during compilation of file #{filename}:\n#{$!}"
      #   puts $!.backtrace
      #   1
      ensure
        file.close unless file.nil?
      end
    end

    errors = 0
    # Process all the file arguments
    Dir[*filenames].each do |filename|
      unless File.exists? filename
        puts "Error -- file not found: #{filename}"
        errors += 1
        next
      end

      if (File.directory?(filename))
        puts "Compiling all in '#{File.expand_path(filename)}'..." if options[:verbose]
        Dir.glob(filename + "/**/*.rb").each { |filename|
          errors += compile_proc[filename]
	}
      else
        if filename =~ /\.java$/
          files << filename
        else
          errors += compile_proc[filename]
        end
      end
    end

    if options[:javac]
      javac_string = JavaGenerator.generate_javac(files, options)
      puts javac_string if options[:verbose]
      system javac_string
    end

    errors
  end
  module_function :compile_files_with_options
end
