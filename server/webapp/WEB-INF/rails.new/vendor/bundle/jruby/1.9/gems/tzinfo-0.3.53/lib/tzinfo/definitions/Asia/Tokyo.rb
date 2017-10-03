# encoding: UTF-8

module TZInfo
  module Definitions
    module Asia
      module Tokyo
        include TimezoneDefinition
        
        timezone 'Asia/Tokyo' do |tz|
          tz.offset :o0, 33539, 0, :LMT
          tz.offset :o1, 32400, 0, :JST
          tz.offset :o2, 32400, 3600, :JDT
          
          tz.transition 1887, 12, :o1, 19285097, 8
          tz.transition 1948, 5, :o2, 58384157, 24
          tz.transition 1948, 9, :o1, 14596831, 6
          tz.transition 1949, 4, :o2, 58392221, 24
          tz.transition 1949, 9, :o1, 14599015, 6
          tz.transition 1950, 5, :o2, 58401797, 24
          tz.transition 1950, 9, :o1, 14601199, 6
          tz.transition 1951, 5, :o2, 58410533, 24
          tz.transition 1951, 9, :o1, 14603383, 6
        end
      end
    end
  end
end
