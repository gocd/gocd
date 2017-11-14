# encoding: UTF-8

# This file contains data derived from the IANA Time Zone Database
# (http://www.iana.org/time-zones).

module TZInfo
  module Data
    module Definitions
      module Asia
        module Tokyo
          include TimezoneDefinition
          
          timezone 'Asia/Tokyo' do |tz|
            tz.offset :o0, 33539, 0, :LMT
            tz.offset :o1, 32400, 0, :JST
            tz.offset :o2, 32400, 3600, :JDT
            
            tz.transition 1887, 12, :o1, -2587712400, 19285097, 8
            tz.transition 1948, 5, :o2, -683794800, 58384157, 24
            tz.transition 1948, 9, :o1, -672393600, 14596831, 6
            tz.transition 1949, 4, :o2, -654764400, 58392221, 24
            tz.transition 1949, 9, :o1, -640944000, 14599015, 6
            tz.transition 1950, 5, :o2, -620290800, 58401797, 24
            tz.transition 1950, 9, :o1, -609494400, 14601199, 6
            tz.transition 1951, 5, :o2, -588841200, 58410533, 24
            tz.transition 1951, 9, :o1, -578044800, 14603383, 6
          end
        end
      end
    end
  end
end
