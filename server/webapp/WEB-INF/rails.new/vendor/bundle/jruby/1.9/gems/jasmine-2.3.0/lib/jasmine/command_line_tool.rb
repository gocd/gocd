require 'fileutils'

module Jasmine
  class CommandLineTool
    def process(argv)
      @argv = argv

      if @argv.size > 0 && respond_to?(@argv[0])
        public_send(@argv[0])
      else
        print_help
      end
    end

    def init
      ensure_not_rails!

      copy_file_structure('install')

      update_rakefile

      install_path = File.join(jasmine_gem_path, "lib", "jasmine", "command_line_install.txt")
      puts File.read(install_path)
    end

    def examples
      copy_file_structure('examples')

      puts "Jasmine has installed some examples."
    end

    def copy_boot_js
      destination_path = File.join('spec', 'javascripts', 'support', 'boot.js')
      if File.exists?(destination_path)
        puts "#{destination_path} already exists"
      else
        require 'jasmine-core'
        source = File.join(Jasmine::Core.path, 'boot.js')
        FileUtils.mkdir_p(File.dirname(destination_path))
        FileUtils.cp(source, destination_path)

        puts 'Jasmine has copied an example boot.js to spec/javascripts/support'
        puts 'To use it set the boot_dir and boot_files keys in jasmine.yml'
        puts ' to point to your custom boot.js'
      end
    end

    def license
      puts File.read(File.join(jasmine_gem_path, "MIT.LICENSE"))
    end

    def print_help
      puts "unknown command #{@argv.join(' ')}" unless @argv.empty?
      puts "Usage: jasmine init"
      puts "               examples"
      puts "               copy_boot_js"
      puts "               license"
    end

    private

    def jasmine_gem_path
      File.expand_path('../../..', __FILE__)
    end

    def copy_file_structure(generator)
      source_dir = File.join(jasmine_gem_path, 'lib', 'generators', 'jasmine', generator, 'templates')
      dest_dir = Dir.pwd

      globber = File.join(source_dir, '**', '{*,.*}')
      source_files = Dir.glob(globber).reject { |path| File.directory?(path) }
      source_files.each do |source_path|
        relative_path = source_path.sub(source_dir, '')
        dest_path = File.join(dest_dir, relative_path).sub(/app[\/\\]assets/, 'public')
        unless File.exist?(dest_path)
          FileUtils.mkdir_p(File.dirname(dest_path))
          FileUtils.copy(source_path, dest_path)
          if File.basename(dest_path) == 'jasmine.yml'
            replaced = File.read(dest_path).gsub("assets/application.js", "public/javascripts/**/*.js").gsub("assets/application.css", "stylesheets/**/*.css")
            File.open(dest_path, 'w') do |file|
              file.write(replaced)
            end
          end
        end
      end
    end

    def force?
      @argv.size > 1 && @argv[1] == "--force"
    end

    def ensure_not_rails!
      if File.exist?("Gemfile") && open("Gemfile", 'r').read.include?('rails') && !force?
        puts <<-EOF

You're attempting to run jasmine init in a Rails project. You probably want to use the Rails generator like so:
    rails g jasmine:install

If you're not actually in a Rails application, just run this command again with --force
    jasmine init --force
EOF
        exit 1
      end
    end

    def update_rakefile
      require 'rake'
      write_mode = 'w'
      rakefile_path = File.join(Dir.pwd, 'Rakefile')
      if File.exist?(rakefile_path)
        load rakefile_path
        write_mode = 'a'
      end

      unless Rake::Task.task_defined?('jasmine')
        File.open(rakefile_path, write_mode) do |f|
          f.write(<<-JASMINE_RAKE)
require 'jasmine'
load 'jasmine/tasks/jasmine.rake'
JASMINE_RAKE
        end
      end
    end
  end
end
