#--
# Copyright (c) 2012-2013 Philip Ross
# 
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
# 
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.
#++

module TZInfo
  # An InvalidZoneinfoDirectory exception is raised if the DataSource is
  # set to a specific zoneinfo path, which is not a valid zoneinfo directory
  # (i.e. a directory containing index files named iso3166.tab and zone.tab
  # as well as other timezone files).
  class InvalidZoneinfoDirectory < StandardError
  end
  
  # A ZoneinfoDirectoryNotFound exception is raised if no valid zoneinfo 
  # directory could be found when checking the paths listed in
  # ZoneinfoDataSource.search_path. A valid zoneinfo directory is one that
  # contains index files named iso3166.tab and zone.tab as well as other 
  # timezone files.
  class ZoneinfoDirectoryNotFound < StandardError
  end
  
  # A DataSource that loads data from a 'zoneinfo' directory containing
  # compiled "TZif" version 2 (or earlier) files in addition to zones.tab 
  # and iso3166.tab index files.
  #
  # To have TZInfo load the system zoneinfo files, call TZInfo::DataSource.set 
  # as follows:
  #
  #   TZInfo::DataSource.set(:zoneinfo)
  #
  # To load zoneinfo files from a particular directory, pass the directory to 
  # TZInfo::DataSource.set:
  #
  #   TZInfo::DataSource.set(:zoneinfo, directory)
  #
  # Note that the platform used at runtime may limit the range of available
  # transition data that can be loaded from zoneinfo files. There are two
  # factors to consider:
  #
  # First of all, the zoneinfo support in TZInfo makes use of Ruby's Time class. 
  # On 32-bit builds of Ruby 1.8, the Time class only supports 32-bit 
  # timestamps. This means that only Times between 1901-12-13 20:45:52 and
  # 2038-01-19 03:14:07 can be represented. Furthermore, certain platforms only
  # allow for positive 32-bit timestamps (notably Windows), making the earliest
  # representable time 1970-01-01 00:00:00.
  #
  # 64-bit builds of Ruby 1.8 and all builds of Ruby 1.9 support 64-bit 
  # timestamps. This means that there is no practical restriction on the range
  # of the Time class on these platforms.
  #
  # TZInfo will only load transitions that fall within the supported range of
  # the Time class. Any queries performed on times outside of this range may
  # give inaccurate results.
  #
  # The second factor concerns the zoneinfo files. Versions of the 'zic' tool
  # (used to build zoneinfo files) that were released prior to February 2006
  # created zoneinfo files that used 32-bit integers for transition timestamps.
  # Later versions of zic produce zoneinfo files that use 64-bit integers. If
  # you have 32-bit zoneinfo files on your system, then any queries falling
  # outside of the range 1901-12-13 20:45:52 to 2038-01-19 03:14:07 may be
  # inaccurate.
  #
  # Most modern platforms include 64-bit zoneinfo files. However, Mac OS X (up
  # to at least 10.8.4) still uses 32-bit zoneinfo files.
  #
  # To check whether your zoneinfo files contain 32-bit or 64-bit transition
  # data, you can run the following code (substituting the identifier of the 
  # zone you want to test for zone_identifier):
  #
  #   TZInfo::DataSource.set(:zoneinfo)
  #   dir = TZInfo::DataSource.get.zoneinfo_dir
  #   File.open(File.join(dir, zone_identifier), 'r') {|f| f.read(5) }
  #
  # If the last line returns "TZif\\x00", then you have a 32-bit zoneinfo file.
  # If it returns "TZif2" or "TZif3" then you have a 64-bit zoneinfo file.
  #
  # If you require support for 64-bit transitions, but are restricted to 32-bit
  # zoneinfo support, then you may want to consider using TZInfo::RubyDataSource 
  # instead.
  class ZoneinfoDataSource < DataSource
    # The default value of ZoneInfoDataSource.search_path.
    DEFAULT_SEARCH_PATH = ['/usr/share/zoneinfo', '/usr/share/lib/zoneinfo', '/etc/zoneinfo'].freeze
    
    # Paths to be checked to find the system zoneinfo directory.
    @@search_path = DEFAULT_SEARCH_PATH.dup
    
    # An Array of directories that will be checked to find the system zoneinfo
    # directory.
    #
    # Directories are checked in the order they appear in the Array.
    #
    # The default value is ['/usr/share/zoneinfo', '/usr/share/lib/zoneinfo', '/etc/zoneinfo'].
    def self.search_path
      @@search_path
    end
    
    # Sets the directories to be checked when locating the system zoneinfo 
    # directory.
    #
    # Can be set to an Array of directories or a String containing directories
    # separated with File::PATH_SEPARATOR.
    #
    # Directories are checked in the order they appear in the Array or String.
    #
    # Set to nil to revert to the default paths.
    def self.search_path=(search_path)
      if search_path
        if search_path.kind_of?(String)
          @@search_path = search_path.split(File::PATH_SEPARATOR)
        else
          @@search_path = search_path.collect {|p| p.to_s}
        end
      else
        @@search_path = DEFAULT_SEARCH_PATH.dup
      end
    end
    
    # The zoneinfo directory being used.
    attr_reader :zoneinfo_dir
    
    # Creates a new ZoneinfoDataSource.
    #
    # If zoneinfo_dir is specified, it will be checked and used as the source
    # of zoneinfo files. If the directory does not contain zone.tab and 
    # iso3166.tab files, InvalidZoneinfoDirectory will be raised.
    # 
    # If zoneinfo_dir is not specified or nil, the paths referenced in
    # search_path are searched in order to find a valid zoneinfo directory 
    # (one that contains files named zone.tab and iso3166.tab). If no valid 
    # zoneinfo directory is found ZoneinfoDirectoryNotFound will be raised.
    def initialize(zoneinfo_dir = nil)
      if zoneinfo_dir
        unless valid_zoneinfo_dir?(zoneinfo_dir)
          raise InvalidZoneinfoDirectory, "#{zoneinfo_dir} is not a directory or doesn't contain iso3166.tab and zone.tab files." 
        end
        @zoneinfo_dir = zoneinfo_dir
      else
        @zoneinfo_dir = self.class.search_path.detect do |path|
          valid_zoneinfo_dir?(path)
        end
        
        unless @zoneinfo_dir
          raise ZoneinfoDirectoryNotFound, "None of the paths included in TZInfo::ZoneinfoDataSource.search_path are valid zoneinfo directories."
        end
      end
      
      @zoneinfo_dir = File.expand_path(@zoneinfo_dir).freeze
      @zoneinfo_prefix = (@zoneinfo_dir + File::SEPARATOR).freeze
      @timezone_index = load_timezone_index.freeze
      @country_index = load_country_index.freeze
    end
    
    # Returns a TimezoneInfo instance for a given identifier. 
    # Raises InvalidTimezoneIdentifier if the timezone is not found or the 
    # identifier is invalid.
    def load_timezone_info(identifier)
      begin
        if @timezone_index.include?(identifier)
          path = File.join(@zoneinfo_dir, identifier)
          
          # Untaint path rather than identifier. We don't want to modify 
          # identifier. identifier may also be frozen and therefore cannot be
          # untainted.
          path.untaint
          
          begin
            ZoneinfoTimezoneInfo.new(identifier, path)
          rescue InvalidZoneinfoFile => e
            raise InvalidTimezoneIdentifier, e.message
          end
        else
          raise InvalidTimezoneIdentifier, 'Invalid identifier'
        end
      rescue Errno::ENOENT, Errno::ENAMETOOLONG, Errno::ENOTDIR
        raise InvalidTimezoneIdentifier, 'Invalid identifier'
      rescue Errno::EACCES => e
        raise InvalidTimezoneIdentifier, e.message
      end
    end    
    
    # Returns an array of all the available timezone identifiers.
    def timezone_identifiers
      @timezone_index
    end
    
    # Returns an array of all the available timezone identifiers for
    # data timezones (i.e. those that actually contain definitions).
    #
    # For ZoneinfoDataSource, this will always be identical to 
    # timezone_identifers.
    def data_timezone_identifiers
      @timezone_index
    end
    
    # Returns an array of all the available timezone identifiers that
    # are links to other timezones.
    #
    # For ZoneinfoDataSource, this will always be an empty array.
    def linked_timezone_identifiers
      [].freeze
    end
    
    # Returns a CountryInfo instance for the given ISO 3166-1 alpha-2
    # country code. Raises InvalidCountryCode if the country could not be found
    # or the code is invalid.
    def load_country_info(code)
      info = @country_index[code]
      raise InvalidCountryCode.new, 'Invalid country code' unless info
      info
    end
    
    # Returns an array of all the available ISO 3166-1 alpha-2
    # country codes.
    def country_codes
      @country_index.keys.freeze
    end
    
    # Returns the name and information about this DataSource.
    def to_s
      "Zoneinfo DataSource: #{@zoneinfo_dir}"
    end
    
    # Returns internal object state as a programmer-readable string.
    def inspect
      "#<#{self.class}: #{@zoneinfo_dir}>"
    end    
    
    private
    
    # Tests whether a path represents a valid zoneinfo directory (i.e.
    # is a directory and contains zone.tab and iso3166.tab files).
    def valid_zoneinfo_dir?(path)
      File.directory?(path) && File.file?(File.join(path, 'zone.tab')) && File.file?(File.join(path, 'iso3166.tab'))
    end
       
    # Scans @zoneinfo_dir and returns an Array of available timezone 
    # identifiers.
    def load_timezone_index
      index = []
      
      # Ignoring particular files:
      # +VERSION is included in Mac OS X.
      # localtime current local timezone (may be a link).
      # posix, posixrules and right are directories containing other versions of the zoneinfo files.
      # Factory is the compiled in default timezone.
      
      enum_timezones(nil, ['+VERSION', 'localtime', 'posix', 'posixrules', 'right', 'Factory']) do |identifier|
        index << identifier
      end
      
      index.sort
    end
    
    # Recursively scans a directory of timezones, calling the passed in block
    # for each identifier found.
    def enum_timezones(dir, exclude = [], &block)
      Dir.foreach(dir ? File.join(@zoneinfo_dir, dir) : @zoneinfo_dir) do |entry|
        unless entry =~ /\./ || exclude.include?(entry)
          entry.untaint
          path = dir ? File.join(dir, entry) : entry
          full_path = File.join(@zoneinfo_dir, path)
 
          if File.directory?(full_path)
            enum_timezones(path, [], &block)
          elsif File.file?(full_path)
            yield path
          end
        end
      end
    end
    
    # Uses the iso3166.tab and zone.tab files to build an index of the 
    # available countries and their timezones.
    def load_country_index
      zones = {}
      
      File.open(File.join(@zoneinfo_dir, 'zone.tab')) do |file|
        file.each_line do |line|
          line.chomp!
          
          if line =~ /\A([A-Z]{2})\t(?:([+\-])(\d{2})(\d{2})([+\-])(\d{3})(\d{2})|([+\-])(\d{2})(\d{2})(\d{2})([+\-])(\d{3})(\d{2})(\d{2}))\t([^\t]+)(?:\t([^\t]+))?\z/
            code = $1
            
            if $2
              latitude = dms_to_rational($2, $3, $4)
              longitude = dms_to_rational($5, $6, $7)
            else
              latitude = dms_to_rational($8, $9, $10, $11)
              longitude = dms_to_rational($12, $13, $14, $15)
            end
            
            zone_identifier = $16
            description = $17
            
            (zones[code] ||= []) << 
              CountryTimezone.new(zone_identifier, latitude.numerator, latitude.denominator, 
                                  longitude.numerator, longitude.denominator, description)
          end
        end
      end
      
      countries = {}
      
      File.open(File.join(@zoneinfo_dir, 'iso3166.tab')) do |file|
        file.each_line do |line|
          line.chomp!
          
          if line =~ /\A([A-Z]{2})\t(.+)\z/
            code = $1
            name = $2
            countries[code] = ZoneinfoCountryInfo.new(code, name, zones[code] || [])
          end
        end
      end
      
      countries
    end
    
    # Converts degrees, miunutes and seconds to a Rational
    def dms_to_rational(sign, degrees, minutes, seconds = nil)
      result = degrees.to_i + Rational(minutes.to_i, 60)
      result += Rational(seconds.to_i, 3600) if seconds
      result = -result if sign == '-'
      result
    end
  end
end
