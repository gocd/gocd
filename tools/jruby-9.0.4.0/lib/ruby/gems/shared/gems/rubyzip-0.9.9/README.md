# rubyzip [![Build Status](https://secure.travis-ci.org/aussiegeek/rubyzip.png)](http://travis-ci.org/aussiegeek/rubyzip)

rubyzip is a ruby library for reading and writing zip files.

## Installation
rubyzip is available on RubyGems, so:

```
gem install rubyzip
```

Or in your Gemfile:

```ruby
gem 'rubyzip'
```

## Usage

### Basic zip archive creation

```ruby
require 'rubygems'
require 'zip/zip'
 
folder = "Users/me/Desktop/stuff_to_zip"
input_filenames = ['image.jpg', 'description.txt', 'stats.csv']
 
zipfile_name = "/Users/me/Desktop/archive.zip"
 
Zip::ZipFile.open(zipfile_name, Zip::ZipFile::CREATE) do |zipfile|
  input_filenames.each do |filename|
    # Two arguments:
    # - The name of the file as it will appear in the archive
    # - The original file, including the path to find it
    zipfile.add(filename, folder + '/' + filename)
  end
end
```

## Further Documentation

There is more than one way to access or create a zip archive with
rubyzip. The basic API is modeled after the classes in
java.util.zip from the Java SDK. This means there are classes such
as Zip::ZipInputStream, Zip::ZipOutputStream and
Zip::ZipFile. Zip::ZipInputStream provides a basic interface for
iterating through the entries in a zip archive and reading from the
entries in the same way as from a regular File or IO
object. ZipOutputStream is the corresponding basic output
facility. Zip::ZipFile provides a mean for accessing the archives
central directory and provides means for accessing any entry without
having to iterate through the archive. Unlike Java's
java.util.zip.ZipFile rubyzip's Zip::ZipFile is mutable, which means
it can be used to change zip files as well.

Another way to access a zip archive with rubyzip is to use rubyzip's
Zip::ZipFileSystem API. Using this API files can be read from and
written to the archive in much the same manner as ruby's builtin
classes allows files to be read from and written to the file system.

rubyzip also features the
zip/ziprequire.rb[link:files/lib/zip/ziprequire_rb.html] module which
allows ruby to load ruby modules from zip archives.

For details about the specific behaviour of classes and methods refer
to the test suite. Finally you can generate the rdoc documentation or
visit http://rubyzip.sourceforge.net.


## Configuration

By default, rubyzip will not overwrite files if they already exist inside of the extracted path.  To change this behavior, you may specify a configuration option like so:

```
Zip.options[:on_exists_proc] = true
```

If you're using rubyzip with rails, consider placing this snippet of code in an initializer file such as `config/initializers/rubyzip.rb`

Additionally, if you want to configure rubyzip to overwrite existing files while creating a .zip file, you can do so with the following:

```
Zip.options[:continue_on_exists_proc] = true
```

## Developing

To run tests you need run next commands:

```
bundle install
rake
```

## Website and Project Home

http://github.com/aussiegeek/rubyzip

http://rdoc.info/github/aussiegeek/rubyzip/master/frames

## Authors

Alexander Simonov ( alex at simonov.me)

Alan Harper ( alan at aussiegeek.net)

Thomas Sondergaard (thomas at sondergaard.cc)

Technorama Ltd. (oss-ruby-zip at technorama.net)

extra-field support contributed by Tatsuki Sugiura (sugi at nemui.org)

## License

rubyzip is distributed under the same license as ruby. See
http://www.ruby-lang.org/en/LICENSE.txt
