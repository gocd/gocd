# encoding: UTF-8

module TZInfo
  module Definitions
    module Pacific
      module Norfolk
        include TimezoneDefinition
        
        timezone 'Pacific/Norfolk' do |tz|
          tz.offset :o0, 40312, 0, :LMT
          tz.offset :o1, 40320, 0, :NMT
          tz.offset :o2, 41400, 0, :NFT
          tz.offset :o3, 41400, 3600, :NFST
          tz.offset :o4, 39600, 0, :NFT
          
          tz.transition 1900, 12, :o1, 26086158361, 10800
          tz.transition 1950, 12, :o2, 73009411, 30
          tz.transition 1974, 10, :o3, 152029800
          tz.transition 1975, 3, :o2, 162912600
          tz.transition 2015, 10, :o4, 1443882600
        end
      end
    end
  end
end
