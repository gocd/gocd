#--
# Copyright (c) 2008-2013 Philip Ross
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
  # An InvalidZoneinfoFile exception is raised if an attempt is made to load an
  # invalid zoneinfo file.
  class InvalidZoneinfoFile < StandardError
  end

  # Represents a timezone defined by a compiled zoneinfo TZif (\0 or 2) file.
  #
  # @private
  class ZoneinfoTimezoneInfo < TransitionDataTimezoneInfo #:nodoc:
    
    # Constructs the new ZoneinfoTimezoneInfo with an identifier and path
    # to the file.
    def initialize(identifier, file_path)
      super(identifier)
      
      File.open(file_path, 'rb') do |file|
        parse(file)
      end
    end
    
    private
      # Unpack will return unsigned 32-bit integers. Translate to 
      # signed 32-bit.
      def make_signed_int32(long)
        long >= 0x80000000 ? long - 0x100000000 : long
      end
      
      # Unpack will return a 64-bit integer as two unsigned 32-bit integers
      # (most significant first). Translate to signed 64-bit
      def make_signed_int64(high, low)
        unsigned = (high << 32) | low
        unsigned > 0x8000000000000000 ? unsigned - 0x10000000000000000 : unsigned
      end
      
      # Read bytes from file and check that the correct number of bytes could
      # be read. Raises InvalidZoneinfoFile if the number of bytes didn't match
      # the number requested.
      def check_read(file, bytes)
        result = file.read(bytes)
        
        unless result && result.length == bytes
          raise InvalidZoneinfoFile, "Expected #{bytes} bytes reading '#{file.path}', but got #{result ? result.length : 0} bytes"
        end
        
        result
      end
      
      # Zoneinfo doesn't include the offset from standard time (std_offset).
      # Derive the missing offsets by looking at changes in the total UTC
      # offset.
      #
      # This will be run through forwards and then backwards by the parse 
      # method.
      def derive_offsets(transitions, offsets)      
        previous_offset = nil

        transitions.each do |t|
          offset = offsets[t[:offset]]

          if !offset[:std_offset] && offset[:is_dst] && previous_offset
            difference = offset[:utc_total_offset] - previous_offset[:utc_total_offset]
            
            if previous_offset[:is_dst]
              if previous_offset[:std_offset]
                std_offset = previous_offset[:std_offset] + difference
              else
                std_offset = nil
              end
            else
              std_offset = difference
            end
            
            if std_offset && std_offset > 0
              offset[:std_offset] = std_offset
              offset[:utc_offset] = offset[:utc_total_offset] - std_offset
            end
          end
          
          previous_offset = offset
        end
      end
      
      # Parses a zoneinfo file and intializes the DataTimezoneInfo structures.
      def parse(file)
        magic, version, ttisgmtcnt, ttisstdcnt, leapcnt, timecnt, typecnt, charcnt =
          check_read(file, 44).unpack('a4 a x15 NNNNNN')

        if magic != 'TZif'
          raise InvalidZoneinfoFile, "The file '#{file.path}' does not start with the expected header."
        end

        if (version == '2' || version == '3') && RubyCoreSupport.time_supports_64bit
          # Skip the first 32-bit section and read the header of the second 64-bit section
          check_read(file, timecnt * 5 + typecnt * 6 + charcnt + leapcnt * 8 + ttisgmtcnt + ttisstdcnt)
          
          prev_version = version
          
          magic, version, ttisgmtcnt, ttisstdcnt, leapcnt, timecnt, typecnt, charcnt =
            check_read(file, 44).unpack('a4 a x15 NNNNNN')
            
          unless magic == 'TZif' && (version == prev_version)
            raise InvalidZoneinfoFile, "The file '#{file.path}' contains an invalid 64-bit section header."
          end
          
          using_64bit = true
        elsif version != '3' && version != '2' && version != "\0"
          raise InvalidZoneinfoFile, "The file '#{file.path}' contains a version of the zoneinfo format that is not currently supported."
        else
          using_64bit = false
        end
        
        unless leapcnt == 0
          raise InvalidZoneinfoFile, "The zoneinfo file '#{file.path}' contains leap second data. TZInfo requires zoneinfo files that omit leap seconds."
        end
        
        transitions = []
        
        if using_64bit
          (0...timecnt).each do |i|
            high, low = check_read(file, 8).unpack('NN')
            transition_time = make_signed_int64(high, low)
            transitions << {:at => transition_time}          
          end
        else
          (0...timecnt).each do |i|
            transition_time = make_signed_int32(check_read(file, 4).unpack('N')[0])
            transitions << {:at => transition_time}          
          end
        end
        
        (0...timecnt).each do |i|
          localtime_type = check_read(file, 1).unpack('C')[0]
          transitions[i][:offset] = localtime_type
        end
        
        offsets = []
        
        (0...typecnt).each do |i|
          gmtoff, isdst, abbrind = check_read(file, 6).unpack('NCC')
          gmtoff = make_signed_int32(gmtoff)
          isdst = isdst == 1
          offset = {:utc_total_offset => gmtoff, :is_dst => isdst, :abbr_index => abbrind}
          
          unless isdst
            offset[:utc_offset] = gmtoff
            offset[:std_offset] = 0
          end
          
          offsets << offset
        end
        
        abbrev = check_read(file, charcnt)

        offsets.each do |o|
          abbrev_start = o[:abbr_index]         
          raise InvalidZoneinfoFile, "Abbreviation index is out of range in file '#{file.path}'" unless abbrev_start < abbrev.length
          
          abbrev_end = abbrev.index("\0", abbrev_start)
          raise InvalidZoneinfoFile, "Missing abbreviation null terminator in file '#{file.path}'" unless abbrev_end

          o[:abbr] = RubyCoreSupport.force_encoding(abbrev[abbrev_start...abbrev_end], 'UTF-8')
        end
        
        transitions.each do |t|
          if t[:offset] < 0 || t[:offset] >= offsets.length
            raise InvalidZoneinfoFile, "Invalid offset referenced by transition in file '#{file.path}'."
          end
        end
        
        # Derive the offsets from standard time (std_offset).
        derive_offsets(transitions, offsets)
        derive_offsets(transitions.reverse, offsets)
        
        # Assign anything left a standard offset of one hour
        offsets.each do |o|
          if !o[:std_offset] && o[:is_dst]
            o[:std_offset] = 3600
            o[:utc_offset] = o[:utc_total_offset] - 3600
          end
        end
        
        # Find the first non-dst offset. This is used as the offset for the time
        # before the first transition.
        first = nil
        offsets.each_with_index do |o, i|
          if !o[:is_dst]
            first = i
            break
          end
        end
        
        if first
          offset first, offsets[first][:utc_offset], offsets[first][:std_offset], offsets[first][:abbr].untaint.to_sym
        end
        
        
        
        offsets.each_with_index do |o, i|
          offset i, o[:utc_offset], o[:std_offset], o[:abbr].untaint.to_sym unless i == first
        end
        
        transitions.each do |t|
          at = t[:at]
          if using_64bit || at > 0 || RubyCoreSupport.time_supports_negative
            time = Time.at(at).utc
            transition time.year, time.mon, t[:offset], at
          end
        end
      end
  end
end
