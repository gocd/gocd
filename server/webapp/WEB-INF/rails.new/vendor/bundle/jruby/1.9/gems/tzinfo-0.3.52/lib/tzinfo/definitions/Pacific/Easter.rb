# encoding: UTF-8

module TZInfo
  module Definitions
    module Pacific
      module Easter
        include TimezoneDefinition
        
        timezone 'Pacific/Easter' do |tz|
          tz.offset :o0, -26248, 0, :LMT
          tz.offset :o1, -26248, 0, :EMT
          tz.offset :o2, -25200, 0, :EAST
          tz.offset :o3, -25200, 3600, :EASST
          tz.offset :o4, -21600, 0, :EAST
          tz.offset :o5, -21600, 3600, :EASST
          
          tz.transition 1890, 1, :o1, 26042783081, 10800
          tz.transition 1932, 9, :o2, 26211079481, 10800
          tz.transition 1968, 11, :o3, 7320491, 3
          tz.transition 1969, 3, :o2, 19522485, 8
          tz.transition 1969, 11, :o3, 7321646, 3
          tz.transition 1970, 3, :o2, 7527600
          tz.transition 1970, 10, :o3, 24465600
          tz.transition 1971, 3, :o2, 37767600
          tz.transition 1971, 10, :o3, 55915200
          tz.transition 1972, 3, :o2, 69217200
          tz.transition 1972, 10, :o3, 87969600
          tz.transition 1973, 3, :o2, 100666800
          tz.transition 1973, 9, :o3, 118209600
          tz.transition 1974, 3, :o2, 132116400
          tz.transition 1974, 10, :o3, 150868800
          tz.transition 1975, 3, :o2, 163566000
          tz.transition 1975, 10, :o3, 182318400
          tz.transition 1976, 3, :o2, 195620400
          tz.transition 1976, 10, :o3, 213768000
          tz.transition 1977, 3, :o2, 227070000
          tz.transition 1977, 10, :o3, 245217600
          tz.transition 1978, 3, :o2, 258519600
          tz.transition 1978, 10, :o3, 277272000
          tz.transition 1979, 3, :o2, 289969200
          tz.transition 1979, 10, :o3, 308721600
          tz.transition 1980, 3, :o2, 321418800
          tz.transition 1980, 10, :o3, 340171200
          tz.transition 1981, 3, :o2, 353473200
          tz.transition 1981, 10, :o3, 371620800
          tz.transition 1982, 3, :o4, 384922800
          tz.transition 1982, 10, :o5, 403070400
          tz.transition 1983, 3, :o4, 416372400
          tz.transition 1983, 10, :o5, 434520000
          tz.transition 1984, 3, :o4, 447822000
          tz.transition 1984, 10, :o5, 466574400
          tz.transition 1985, 3, :o4, 479271600
          tz.transition 1985, 10, :o5, 498024000
          tz.transition 1986, 3, :o4, 510721200
          tz.transition 1986, 10, :o5, 529473600
          tz.transition 1987, 4, :o4, 545194800
          tz.transition 1987, 10, :o5, 560923200
          tz.transition 1988, 3, :o4, 574225200
          tz.transition 1988, 10, :o5, 592372800
          tz.transition 1989, 3, :o4, 605674800
          tz.transition 1989, 10, :o5, 624427200
          tz.transition 1990, 3, :o4, 637124400
          tz.transition 1990, 9, :o5, 653457600
          tz.transition 1991, 3, :o4, 668574000
          tz.transition 1991, 10, :o5, 687326400
          tz.transition 1992, 3, :o4, 700628400
          tz.transition 1992, 10, :o5, 718776000
          tz.transition 1993, 3, :o4, 732078000
          tz.transition 1993, 10, :o5, 750225600
          tz.transition 1994, 3, :o4, 763527600
          tz.transition 1994, 10, :o5, 781675200
          tz.transition 1995, 3, :o4, 794977200
          tz.transition 1995, 10, :o5, 813729600
          tz.transition 1996, 3, :o4, 826426800
          tz.transition 1996, 10, :o5, 845179200
          tz.transition 1997, 3, :o4, 859690800
          tz.transition 1997, 10, :o5, 876628800
          tz.transition 1998, 3, :o4, 889930800
          tz.transition 1998, 9, :o5, 906868800
          tz.transition 1999, 4, :o4, 923194800
          tz.transition 1999, 10, :o5, 939528000
          tz.transition 2000, 3, :o4, 952830000
          tz.transition 2000, 10, :o5, 971582400
          tz.transition 2001, 3, :o4, 984279600
          tz.transition 2001, 10, :o5, 1003032000
          tz.transition 2002, 3, :o4, 1015729200
          tz.transition 2002, 10, :o5, 1034481600
          tz.transition 2003, 3, :o4, 1047178800
          tz.transition 2003, 10, :o5, 1065931200
          tz.transition 2004, 3, :o4, 1079233200
          tz.transition 2004, 10, :o5, 1097380800
          tz.transition 2005, 3, :o4, 1110682800
          tz.transition 2005, 10, :o5, 1128830400
          tz.transition 2006, 3, :o4, 1142132400
          tz.transition 2006, 10, :o5, 1160884800
          tz.transition 2007, 3, :o4, 1173582000
          tz.transition 2007, 10, :o5, 1192334400
          tz.transition 2008, 3, :o4, 1206846000
          tz.transition 2008, 10, :o5, 1223784000
          tz.transition 2009, 3, :o4, 1237086000
          tz.transition 2009, 10, :o5, 1255233600
          tz.transition 2010, 4, :o4, 1270350000
          tz.transition 2010, 10, :o5, 1286683200
          tz.transition 2011, 5, :o4, 1304823600
          tz.transition 2011, 8, :o5, 1313899200
          tz.transition 2012, 4, :o4, 1335668400
          tz.transition 2012, 9, :o5, 1346558400
          tz.transition 2013, 4, :o4, 1367118000
          tz.transition 2013, 9, :o5, 1378612800
          tz.transition 2014, 4, :o4, 1398567600
          tz.transition 2014, 9, :o5, 1410062400
          tz.transition 2016, 5, :o4, 1463281200
          tz.transition 2016, 8, :o5, 1471147200
          tz.transition 2017, 5, :o4, 1494730800
          tz.transition 2017, 8, :o5, 1502596800
          tz.transition 2018, 5, :o4, 1526180400
          tz.transition 2018, 8, :o5, 1534046400
          tz.transition 2019, 5, :o4, 1557630000
          tz.transition 2019, 8, :o5, 1565496000
          tz.transition 2020, 5, :o4, 1589079600
          tz.transition 2020, 8, :o5, 1596945600
          tz.transition 2021, 5, :o4, 1620529200
          tz.transition 2021, 8, :o5, 1629000000
          tz.transition 2022, 5, :o4, 1652583600
          tz.transition 2022, 8, :o5, 1660449600
          tz.transition 2023, 5, :o4, 1684033200
          tz.transition 2023, 8, :o5, 1691899200
          tz.transition 2024, 5, :o4, 1715482800
          tz.transition 2024, 8, :o5, 1723348800
          tz.transition 2025, 5, :o4, 1746932400
          tz.transition 2025, 8, :o5, 1754798400
          tz.transition 2026, 5, :o4, 1778382000
          tz.transition 2026, 8, :o5, 1786248000
          tz.transition 2027, 5, :o4, 1809831600
          tz.transition 2027, 8, :o5, 1818302400
          tz.transition 2028, 5, :o4, 1841886000
          tz.transition 2028, 8, :o5, 1849752000
          tz.transition 2029, 5, :o4, 1873335600
          tz.transition 2029, 8, :o5, 1881201600
          tz.transition 2030, 5, :o4, 1904785200
          tz.transition 2030, 8, :o5, 1912651200
          tz.transition 2031, 5, :o4, 1936234800
          tz.transition 2031, 8, :o5, 1944100800
          tz.transition 2032, 5, :o4, 1967684400
          tz.transition 2032, 8, :o5, 1976155200
          tz.transition 2033, 5, :o4, 1999738800
          tz.transition 2033, 8, :o5, 2007604800
          tz.transition 2034, 5, :o4, 2031188400
          tz.transition 2034, 8, :o5, 2039054400
          tz.transition 2035, 5, :o4, 2062638000
          tz.transition 2035, 8, :o5, 2070504000
          tz.transition 2036, 5, :o4, 2094087600
          tz.transition 2036, 8, :o5, 2101953600
          tz.transition 2037, 5, :o4, 2125537200
          tz.transition 2037, 8, :o5, 2133403200
          tz.transition 2038, 5, :o4, 19724421, 8
          tz.transition 2038, 8, :o5, 7396952, 3
          tz.transition 2039, 5, :o4, 19727389, 8
          tz.transition 2039, 8, :o5, 7398044, 3
          tz.transition 2040, 5, :o4, 19730301, 8
          tz.transition 2040, 8, :o5, 7399136, 3
          tz.transition 2041, 5, :o4, 19733213, 8
          tz.transition 2041, 8, :o5, 7400228, 3
          tz.transition 2042, 5, :o4, 19736125, 8
          tz.transition 2042, 8, :o5, 7401320, 3
          tz.transition 2043, 5, :o4, 19739037, 8
          tz.transition 2043, 8, :o5, 7402412, 3
          tz.transition 2044, 5, :o4, 19742005, 8
          tz.transition 2044, 8, :o5, 7403525, 3
          tz.transition 2045, 5, :o4, 19744917, 8
          tz.transition 2045, 8, :o5, 7404617, 3
          tz.transition 2046, 5, :o4, 19747829, 8
          tz.transition 2046, 8, :o5, 7405709, 3
          tz.transition 2047, 5, :o4, 19750741, 8
          tz.transition 2047, 8, :o5, 7406801, 3
          tz.transition 2048, 5, :o4, 19753653, 8
          tz.transition 2048, 8, :o5, 7407893, 3
          tz.transition 2049, 5, :o4, 19756565, 8
          tz.transition 2049, 8, :o5, 7409006, 3
          tz.transition 2050, 5, :o4, 19759533, 8
        end
      end
    end
  end
end
