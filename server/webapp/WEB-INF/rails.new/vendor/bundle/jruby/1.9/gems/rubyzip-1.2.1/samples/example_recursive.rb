require 'zip'

# This is a simple example which uses rubyzip to
# recursively generate a zip file from the contents of
# a specified directory. The directory itself is not
# included in the archive, rather just its contents.
#
# Usage:
#   directory_to_zip = "/tmp/input"
#   output_file = "/tmp/out.zip"
#   zf = ZipFileGenerator.new(directory_to_zip, output_file)
#   zf.write()
class ZipFileGenerator
  # Initialize with the directory to zip and the location of the output archive.
  def initialize(input_dir, output_file)
    @input_dir = input_dir
    @output_file = output_file
  end

  # Zip the input directory.
  def write
    entries = Dir.entries(@input_dir) - %w(. ..)

    ::Zip::File.open(@output_file, ::Zip::File::CREATE) do |io|
      write_entries entries, '', io
    end
  end

  private

  # A helper method to make the recursion work.
  def write_entries(entries, path, io)
    entries.each do |e|
      zip_file_path = path == '' ? e : File.join(path, e)
      disk_file_path = File.join(@input_dir, zip_file_path)
      puts "Deflating #{disk_file_path}"

      if File.directory? disk_file_path
        recursively_deflate_directory(disk_file_path, io, zip_file_path)
      else
        put_into_archive(disk_file_path, io, zip_file_path)
      end
    end
  end

  def recursively_deflate_directory(disk_file_path, io, zip_file_path)
    io.mkdir zip_file_path
    subdir = Dir.entries(disk_file_path) - %w(. ..)
    write_entries subdir, zip_file_path, io
  end

  def put_into_archive(disk_file_path, io, zip_file_path)
    io.add(zip_file_path, disk_file_path)
  end
end
