# encoding: UTF-8

module TZInfo
  module Definitions
    module Pacific
      module Apia
        include TimezoneDefinition
        
        timezone 'Pacific/Apia' do |tz|
          tz.offset :o0, 45184, 0, :LMT
          tz.offset :o1, -41216, 0, :LMT
          tz.offset :o2, -41400, 0, :WSST
          tz.offset :o3, -39600, 0, :SST
          tz.offset :o4, -39600, 3600, :SDT
          tz.offset :o5, 46800, 3600, :WSDT
          tz.offset :o6, 46800, 0, :WSST
          
          tz.transition 1879, 7, :o1, 3250172219, 1350
          tz.transition 1911, 1, :o2, 3265701269, 1350
          tz.transition 1950, 1, :o3, 116797583, 48
          tz.transition 2010, 9, :o4, 1285498800
          tz.transition 2011, 4, :o3, 1301752800
          tz.transition 2011, 9, :o4, 1316872800
          tz.transition 2011, 12, :o5, 1325239200
          tz.transition 2012, 3, :o6, 1333202400
          tz.transition 2012, 9, :o5, 1348927200
          tz.transition 2013, 4, :o6, 1365256800
          tz.transition 2013, 9, :o5, 1380376800
          tz.transition 2014, 4, :o6, 1396706400
          tz.transition 2014, 9, :o5, 1411826400
          tz.transition 2015, 4, :o6, 1428156000
          tz.transition 2015, 9, :o5, 1443276000
          tz.transition 2016, 4, :o6, 1459605600
          tz.transition 2016, 9, :o5, 1474725600
          tz.transition 2017, 4, :o6, 1491055200
          tz.transition 2017, 9, :o5, 1506175200
          tz.transition 2018, 3, :o6, 1522504800
          tz.transition 2018, 9, :o5, 1538229600
          tz.transition 2019, 4, :o6, 1554559200
          tz.transition 2019, 9, :o5, 1569679200
          tz.transition 2020, 4, :o6, 1586008800
          tz.transition 2020, 9, :o5, 1601128800
          tz.transition 2021, 4, :o6, 1617458400
          tz.transition 2021, 9, :o5, 1632578400
          tz.transition 2022, 4, :o6, 1648908000
          tz.transition 2022, 9, :o5, 1664028000
          tz.transition 2023, 4, :o6, 1680357600
          tz.transition 2023, 9, :o5, 1695477600
          tz.transition 2024, 4, :o6, 1712412000
          tz.transition 2024, 9, :o5, 1727532000
          tz.transition 2025, 4, :o6, 1743861600
          tz.transition 2025, 9, :o5, 1758981600
          tz.transition 2026, 4, :o6, 1775311200
          tz.transition 2026, 9, :o5, 1790431200
          tz.transition 2027, 4, :o6, 1806760800
          tz.transition 2027, 9, :o5, 1821880800
          tz.transition 2028, 4, :o6, 1838210400
          tz.transition 2028, 9, :o5, 1853330400
          tz.transition 2029, 3, :o6, 1869660000
          tz.transition 2029, 9, :o5, 1885384800
          tz.transition 2030, 4, :o6, 1901714400
          tz.transition 2030, 9, :o5, 1916834400
          tz.transition 2031, 4, :o6, 1933164000
          tz.transition 2031, 9, :o5, 1948284000
          tz.transition 2032, 4, :o6, 1964613600
          tz.transition 2032, 9, :o5, 1979733600
          tz.transition 2033, 4, :o6, 1996063200
          tz.transition 2033, 9, :o5, 2011183200
          tz.transition 2034, 4, :o6, 2027512800
          tz.transition 2034, 9, :o5, 2042632800
          tz.transition 2035, 3, :o6, 2058962400
          tz.transition 2035, 9, :o5, 2074687200
          tz.transition 2036, 4, :o6, 2091016800
          tz.transition 2036, 9, :o5, 2106136800
          tz.transition 2037, 4, :o6, 2122466400
          tz.transition 2037, 9, :o5, 2137586400
          tz.transition 2038, 4, :o6, 29586205, 12
          tz.transition 2038, 9, :o5, 29588305, 12
          tz.transition 2039, 4, :o6, 29590573, 12
          tz.transition 2039, 9, :o5, 29592673, 12
          tz.transition 2040, 3, :o6, 29594941, 12
          tz.transition 2040, 9, :o5, 29597125, 12
          tz.transition 2041, 4, :o6, 29599393, 12
          tz.transition 2041, 9, :o5, 29601493, 12
          tz.transition 2042, 4, :o6, 29603761, 12
          tz.transition 2042, 9, :o5, 29605861, 12
          tz.transition 2043, 4, :o6, 29608129, 12
          tz.transition 2043, 9, :o5, 29610229, 12
          tz.transition 2044, 4, :o6, 29612497, 12
          tz.transition 2044, 9, :o5, 29614597, 12
          tz.transition 2045, 4, :o6, 29616865, 12
          tz.transition 2045, 9, :o5, 29618965, 12
          tz.transition 2046, 3, :o6, 29621233, 12
          tz.transition 2046, 9, :o5, 29623417, 12
          tz.transition 2047, 4, :o6, 29625685, 12
          tz.transition 2047, 9, :o5, 29627785, 12
          tz.transition 2048, 4, :o6, 29630053, 12
          tz.transition 2048, 9, :o5, 29632153, 12
          tz.transition 2049, 4, :o6, 29634421, 12
          tz.transition 2049, 9, :o5, 29636521, 12
          tz.transition 2050, 4, :o6, 29638789, 12
        end
      end
    end
  end
end
