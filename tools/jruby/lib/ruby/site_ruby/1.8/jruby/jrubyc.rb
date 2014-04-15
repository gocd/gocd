require 'optparse'
require 'jruby'

module JRubyCompiler
  BAIS = java.io.ByteArrayInputStream
  Mangler = org.jruby.util.JavaNameMangler
  BytecodeCompiler = org.jruby.compiler.impl.StandardASMCompiler
  ASTCompiler = org.jruby.compiler.ASTCompiler
  JavaFile = java.io.File

  module_function
  def compile_argv(argv)
    basedir = Dir.pwd
    prefix = ""
    target = Dir.pwd

    opt_parser = OptionParser.new("", 24, '  ') do |opts|
      opts.banner = "jrubyc [options] (FILE|DIRECTORY)"
      opts.separator ""

      opts.on("-d", "--dir DIR", "Use DIR as the root of the compiled package and filename") do |dir|
        basedir = dir
      end

      opts.on("-p", "--prefix PREFIX", "Prepend PREFIX to the file path and package. Default is no prefix.") do |pre|
        prefix = pre
      end

      opts.on("-t", "--target TARGET", "Output files to TARGET directory") do |tgt|
        target = tgt
      end

      opts.parse!(argv)
    end

    if (argv.length == 0)
      raise "No files or directories specified"
    end

    compile_files(argv, basedir, prefix, target)
  end

  def compile_files(filenames, basedir = Dir.pwd, prefix = "ruby", target = Dir.pwd)
    runtime = JRuby.runtime
    
    unless File.exist? target
      raise "Target dir not found: #{target}"
    end

    # The compilation code
    compile_proc = proc do |filename|
      begin
        file = File.open(filename)

        classpath = Mangler.mangle_filename_for_classpath(filename, basedir, prefix)
        puts "Compiling #{filename} to class #{classpath}"

        inspector = org.jruby.compiler.ASTInspector.new

        source = file.read
        node = runtime.parse_file(BAIS.new(source.to_java_bytes), filename, nil)

        inspector.inspect(node)

        asmCompiler = BytecodeCompiler.new(classpath, filename)
        compiler = ASTCompiler.new
        compiler.compile_root(node, asmCompiler, inspector)

        asmCompiler.write_class(JavaFile.new(target))
      rescue Exception
        puts "Failure during compilation of file #{filename}:\n#{$!}"
      ensure
        file.close unless file.nil?
      end
    end

    # Process all the file arguments
    filenames.each do |filename|
      unless File.exists? filename
        puts "Error -- file not found: #{filename}"
        next
      end

      if (File.directory?(filename))
        puts "Compiling all in '#{File.expand_path(filename)}'..."
        Dir.glob(filename + "/**/*.rb").each(&compile_proc)
      else
        compile_proc[filename]
      end
    end
  end
end
