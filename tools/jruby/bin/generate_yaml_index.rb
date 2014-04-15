#!/usr/bin/env jruby

# Generate the yaml/yaml.Z index files for a gem server directory.
#
# Usage:  generate_yaml_index.rb --dir DIR [--verbose]

$:.unshift '~/rubygems' if File.exist? "~/rubygems"

require 'optparse'
require 'rubygems'
require 'zlib'

Gem.manage_gems

class Indexer

  def initialize(options)
    @options = options
    @directory = options[:directory]
  end

  def gem_file_list
    Dir.glob(File.join(@directory, "gems", "*.gem"))
  end

  def build_index
    build_uncompressed_index
    build_compressed_index
  end
  
  def build_uncompressed_index
    puts "Building yaml file" if @options[:verbose]
    File.open(File.join(@directory, "yaml"), "w") do |file|
      file.puts "--- !ruby/object:Gem::Cache"
      file.puts "gems:"
      gem_file_list.each do |gemfile|
        spec = Gem::Format.from_file_by_path(gemfile).spec
	puts "   ... adding #{spec.full_name}" if @options[:verbose]
        file.puts "  #{spec.full_name}: #{spec.to_yaml.gsub(/\n/, "\n    ")[4..-1]}"
      end
    end
  end

  def build_compressed_index
    puts "Building yaml.Z file" if @options[:verbose]
    File.open(File.join(@directory, "yaml.Z"), "w") do |file|
      file.write(Zlib::Deflate.deflate(File.read(File.join(@directory, "yaml"))))
    end
  end
end


options = {
  :directory => '.',
  :verbose => false,
}

ARGV.options do |opts|
  opts.on_tail("--help", "show this message") {puts opts; exit}
  opts.on('-d', '--dir=DIRNAME', "base directory containing gems subdirectory", String) do |value|
    options[:directory] = value
  end
  opts.on('-v', '--verbose', "show verbose output") do |value|
    options[:verbose] = value
  end
  opts.parse!
end

if options[:directory].nil?
  puts "Error, must specify directory name. Use --help"
  exit
elsif ! File.exist?(options[:directory]) ||
    ! File.directory?(options[:directory])
  puts "Error, unknown directory name #{directory}."
  exit
end

Indexer.new(options).build_index
