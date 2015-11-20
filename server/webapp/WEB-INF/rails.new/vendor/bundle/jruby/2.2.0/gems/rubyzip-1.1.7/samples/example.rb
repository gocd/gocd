#!/usr/bin/env ruby

$: << "../lib"
system("zip example.zip example.rb gtkRubyzip.rb")

require 'zip'

####### Using ZipInputStream alone: #######

Zip::InputStream.open("example.zip") {
  |zis|
  entry = zis.get_next_entry
  print "First line of '#{entry.name} (#{entry.size} bytes):  "
  puts "'#{zis.gets.chomp}'"
  entry = zis.get_next_entry
  print "First line of '#{entry.name} (#{entry.size} bytes):  "
  puts "'#{zis.gets.chomp}'"
}


####### Using ZipFile to read the directory of a zip file: #######

zf = Zip::File.new("example.zip")
zf.each_with_index {
  |entry, index|

  puts "entry #{index} is #{entry.name}, size = #{entry.size}, compressed size = #{entry.compressed_size}"
  # use zf.get_input_stream(entry) to get a ZipInputStream for the entry
  # entry can be the ZipEntry object or any object which has a to_s method that
  # returns the name of the entry.
}


####### Using ZipOutputStream to write a zip file: #######

Zip::OutputStream.open("exampleout.zip") {
  |zos|
  zos.put_next_entry("the first little entry")
  zos.puts "Hello hello hello hello hello hello hello hello hello"

  zos.put_next_entry("the second little entry")
  zos.puts "Hello again"

  # Use rubyzip or your zip client of choice to verify
  # the contents of exampleout.zip
}

####### Using ZipFile to change a zip file: #######

Zip::File.open("exampleout.zip") {
  |zf|
  zf.add("thisFile.rb", "example.rb")
  zf.rename("thisFile.rb", "ILikeThisName.rb")
  zf.add("Again", "example.rb")
}

# Lets check
Zip::File.open("exampleout.zip") {
  |zf|
  puts "Changed zip file contains: #{zf.entries.join(', ')}"
  zf.remove("Again")
  puts "Without 'Again': #{zf.entries.join(', ')}"
}

####### Using ZipFile to split a zip file: #######

# Creating large zip file for splitting
Zip::OutputStream.open("large_zip_file.zip") do |zos|
  puts "Creating zip file..."
  10.times do |i|
    zos.put_next_entry("large_entry_#{i}.txt")
    zos.puts "Hello" * 104857600
  end
end

# Splitting created large zip file
part_zips_count = Zip::File.split("large_zip_file.zip", 2097152, false)
puts "Zip file splitted in #{part_zips_count} parts"

# Track splitting an archive
Zip::File.split("large_zip_file.zip", 1048576, true, 'part_zip_file') do
  |part_count, part_index, chunk_bytes, segment_bytes|
  puts "#{part_index} of #{part_count} part splitting: #{(chunk_bytes.to_f/segment_bytes.to_f * 100).to_i}%"
end


# For other examples, look at zip.rb and ziptest.rb

# Copyright (C) 2002 Thomas Sondergaard
# rubyzip is free software; you can redistribute it and/or
# modify it under the terms of the ruby license.
