#!/bin/env ruby

rubygems = false
begin
  require 'rubygems'
  rubygems = true
rescue LoadError
end

require 'open-uri'
require 'fileutils'

rubyzip = false
begin
  require 'zip/zipfilesystem'
  rubyzip = true
rescue LoadError
end


@address = "www.rubyquiz.com"
first = last = nil

first = ARGV[0].to_i if ARGV.size > 1
last  = ARGV[1].to_i if ARGV.size > 2



#
# Download a binary file from the rubyquiz url
#
def download(file, todir = '.')
  begin
    puts "Downloading file #{file} from #{@address}"
    c = open("http://#{@address}/#{file}").read
    Dir.mkdir(todir) if not File.directory?(todir)
    f = open("#{todir}/#{file}", 'wb')
    f.puts c
    f.close
  rescue => e
    if not File.exists?(fullfile)
      $stderr.puts "Could not download file #{file} form #{@address}."
      $stderr.puts e.to_s
    end
  end
end


#
# Unzip the file using GNU's stand-alone unzip utility. 
#
def unzip_gnu(file)
  puts `unzip -o #{file}`
end


#
# Unzip the file using rubyzip library.
#
def unzip(x)
  outdir = x.sub(/.*\//, '')
  outdir = '.' if outdir == ""
  Zip::ZipFile::open(x) { |zf|
    zf.each { |e|
      fpath = File.join(outdir, e.name)
      FileUtils.mkdir_p(File.dirname(fpath))
      zf.extract(e, fpath)
    }
  }
end



def get_index
  c = open("http://#{@address}/").read
  quizzes = c.scan /quiz(\d+).html/
  return [ quizzes[0][0], quizzes[-1][0] ]
end

if not first or not last
  f, l = get_index()
  last = l  unless last
  first = f unless first
end

first = first.to_i
last  = last.to_i


puts "Downloading quizzess #{first} to #{last}"
quizzes = (first..last)
quizzes.each { |q|
  dir      = "quiz#{q}"

  #### Download HTML description
  file     = "quiz#{q}.html"
  fullfile = "#{dir}/#{file}"
  if not File.exists?(fullfile)
    download( file, dir )
  end

  #### Download zip file
  file     = "quiz#{q}_sols.zip"
  fullfile = "#{dir}/#{file}"

  if not File.exists?(fullfile)
    download( file, dir )
  end

  # Unzip and remove .zip file
  Dir.chdir dir
  if rubyzip
    unzip file
  else
    unzip_gnu file
  end
  FileUtils.rm file
  Dir.chdir '..'
}
