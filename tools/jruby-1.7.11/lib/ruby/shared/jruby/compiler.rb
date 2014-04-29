require 'optparse'
require 'fileutils'
require 'digest/sha1'
require 'jruby'
require 'jruby/compiler/java_class'

module JRuby::Compiler
  BAIS = java.io.ByteArrayInputStream
  Mangler = org.jruby.util.JavaNameMangler
  BytecodeCompiler = org.jruby.compiler.impl.StandardASMCompiler
  ASTCompiler = RUBY_VERSION =~ /1\.9/ ? org.jruby.compiler.ASTCompiler19 : org.jruby.compiler.ASTCompiler
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
        file = File.open(filename)

        if options[:sha1]
          pathname = "ruby.jit.FILE_" + Digest::SHA1.hexdigest(File.read(filename)).upcase
        else
          pathname = Mangler.mangle_filename_for_classpath(filename, options[:basedir], options[:prefix], true, false)
        end

        inspector = org.jruby.compiler.ASTInspector.new

        source = file.read
        node = runtime.parse_file(BAIS.new(source.to_java_bytes), filename, nil)

        if options[:java] || options[:javac]
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

          inspector.inspect(node)

          asmCompiler = BytecodeCompiler.new(pathname.gsub(".", "/"), filename)
          if options[:jdk5]
            asmCompiler.java_version= 49
          end

          compiler = ASTCompiler.new
          compiler.compile_root(node, asmCompiler, inspector)

          class_bytes = String.from_java_bytes(asmCompiler.class_byte_array)
          
          # prepare target
          class_filename = filename.sub(/(\.rb)?$/, '.class')
          target_file = File.join(options[:target], class_filename)
          target_dir = File.dirname(target_file)
          FileUtils.mkdir_p(target_dir)
          
          # write class
          File.open(target_file, 'wb') do |f|
            f.write(class_bytes)
          end

          if options[:handles]
            puts "Generating direct handles for #{filename}"# if options[:verbose]

            asmCompiler.write_invokers(options[:target])
          end
        end

        0
      rescue Exception
        puts "Failure during compilation of file #{filename}:\n#{$!}"
        puts $!.backtrace
        1
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
