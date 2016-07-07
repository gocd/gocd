# encoding: UTF-8

module TZInfo
  module Definitions
    module Antarctica
      module Palmer
        include TimezoneDefinition
        
        timezone 'Antarctica/Palmer' do |tz|
          tz.offset :o0, 0, 0, :'-00'
          tz.offset :o1, -14400, 3600, :ARST
          tz.offset :o2, -14400, 0, :ART
          tz.offset :o3, -10800, 0, :ART
          tz.offset :o4, -10800, 3600, :ARST
          tz.offset :o5, -14400, 0, :CLT
          tz.offset :o6, -14400, 3600, :CLST
          
          tz.transition 1965, 1, :o1, 4877523, 2
          tz.transition 1965, 3, :o2, 19510565, 8
          tz.transition 1965, 10, :o1, 7317146, 3
          tz.transition 1966, 3, :o2, 19513485, 8
          tz.transition 1966, 10, :o1, 7318241, 3
          tz.transition 1967, 4, :o2, 19516661, 8
          tz.transition 1967, 10, :o1, 7319294, 3
          tz.transition 1968, 4, :o2, 19519629, 8
          tz.transition 1968, 10, :o1, 7320407, 3
          tz.transition 1969, 4, :o2, 19522541, 8
          tz.transition 1969, 10, :o3, 7321499, 3
          tz.transition 1974, 1, :o4, 128142000
          tz.transition 1974, 5, :o3, 136605600
          tz.transition 1982, 5, :o5, 389070000
          tz.transition 1982, 10, :o6, 403070400
          tz.transition 1983, 3, :o5, 416372400
          tz.transition 1983, 10, :o6, 434520000
          tz.transition 1984, 3, :o5, 447822000
          tz.transition 1984, 10, :o6, 466574400
          tz.transition 1985, 3, :o5, 479271600
          tz.transition 1985, 10, :o6, 498024000
          tz.transition 1986, 3, :o5, 510721200
          tz.transition 1986, 10, :o6, 529473600
          tz.transition 1987, 4, :o5, 545194800
          tz.transition 1987, 10, :o6, 560923200
          tz.transition 1988, 3, :o5, 574225200
          tz.transition 1988, 10, :o6, 592372800
          tz.transition 1989, 3, :o5, 605674800
          tz.transition 1989, 10, :o6, 624427200
          tz.transition 1990, 3, :o5, 637124400
          tz.transition 1990, 9, :o6, 653457600
          tz.transition 1991, 3, :o5, 668574000
          tz.transition 1991, 10, :o6, 687326400
          tz.transition 1992, 3, :o5, 700628400
          tz.transition 1992, 10, :o6, 718776000
          tz.transition 1993, 3, :o5, 732078000
          tz.transition 1993, 10, :o6, 750225600
          tz.transition 1994, 3, :o5, 763527600
          tz.transition 1994, 10, :o6, 781675200
          tz.transition 1995, 3, :o5, 794977200
          tz.transition 1995, 10, :o6, 813729600
          tz.transition 1996, 3, :o5, 826426800
          tz.transition 1996, 10, :o6, 845179200
          tz.transition 1997, 3, :o5, 859690800
          tz.transition 1997, 10, :o6, 876628800
          tz.transition 1998, 3, :o5, 889930800
          tz.transition 1998, 9, :o6, 906868800
          tz.transition 1999, 4, :o5, 923194800
          tz.transition 1999, 10, :o6, 939528000
          tz.transition 2000, 3, :o5, 952830000
          tz.transition 2000, 10, :o6, 971582400
          tz.transition 2001, 3, :o5, 984279600
          tz.transition 2001, 10, :o6, 1003032000
          tz.transition 2002, 3, :o5, 1015729200
          tz.transition 2002, 10, :o6, 1034481600
          tz.transition 2003, 3, :o5, 1047178800
          tz.transition 2003, 10, :o6, 1065931200
          tz.transition 2004, 3, :o5, 1079233200
          tz.transition 2004, 10, :o6, 1097380800
          tz.transition 2005, 3, :o5, 1110682800
          tz.transition 2005, 10, :o6, 1128830400
          tz.transition 2006, 3, :o5, 1142132400
          tz.transition 2006, 10, :o6, 1160884800
          tz.transition 2007, 3, :o5, 1173582000
          tz.transition 2007, 10, :o6, 1192334400
          tz.transition 2008, 3, :o5, 1206846000
          tz.transition 2008, 10, :o6, 1223784000
          tz.transition 2009, 3, :o5, 1237086000
          tz.transition 2009, 10, :o6, 1255233600
          tz.transition 2010, 4, :o5, 1270350000
          tz.transition 2010, 10, :o6, 1286683200
          tz.transition 2011, 5, :o5, 1304823600
          tz.transition 2011, 8, :o6, 1313899200
          tz.transition 2012, 4, :o5, 1335668400
          tz.transition 2012, 9, :o6, 1346558400
          tz.transition 2013, 4, :o5, 1367118000
          tz.transition 2013, 9, :o6, 1378612800
          tz.transition 2014, 4, :o5, 1398567600
          tz.transition 2014, 9, :o6, 1410062400
          tz.transition 2016, 5, :o5, 1463281200
          tz.transition 2016, 8, :o6, 1471147200
          tz.transition 2017, 5, :o5, 1494730800
          tz.transition 2017, 8, :o6, 1502596800
          tz.transition 2018, 5, :o5, 1526180400
          tz.transition 2018, 8, :o6, 1534046400
          tz.transition 2019, 5, :o5, 1557630000
          tz.transition 2019, 8, :o6, 1565496000
          tz.transition 2020, 5, :o5, 1589079600
          tz.transition 2020, 8, :o6, 1596945600
          tz.transition 2021, 5, :o5, 1620529200
          tz.transition 2021, 8, :o6, 1629000000
          tz.transition 2022, 5, :o5, 1652583600
          tz.transition 2022, 8, :o6, 1660449600
          tz.transition 2023, 5, :o5, 1684033200
          tz.transition 2023, 8, :o6, 1691899200
          tz.transition 2024, 5, :o5, 1715482800
          tz.transition 2024, 8, :o6, 1723348800
          tz.transition 2025, 5, :o5, 1746932400
          tz.transition 2025, 8, :o6, 1754798400
          tz.transition 2026, 5, :o5, 1778382000
          tz.transition 2026, 8, :o6, 1786248000
          tz.transition 2027, 5, :o5, 1809831600
          tz.transition 2027, 8, :o6, 1818302400
          tz.transition 2028, 5, :o5, 1841886000
          tz.transition 2028, 8, :o6, 1849752000
          tz.transition 2029, 5, :o5, 1873335600
          tz.transition 2029, 8, :o6, 1881201600
          tz.transition 2030, 5, :o5, 1904785200
          tz.transition 2030, 8, :o6, 1912651200
          tz.transition 2031, 5, :o5, 1936234800
          tz.transition 2031, 8, :o6, 1944100800
          tz.transition 2032, 5, :o5, 1967684400
          tz.transition 2032, 8, :o6, 1976155200
          tz.transition 2033, 5, :o5, 1999738800
          tz.transition 2033, 8, :o6, 2007604800
          tz.transition 2034, 5, :o5, 2031188400
          tz.transition 2034, 8, :o6, 2039054400
          tz.transition 2035, 5, :o5, 2062638000
          tz.transition 2035, 8, :o6, 2070504000
          tz.transition 2036, 5, :o5, 2094087600
          tz.transition 2036, 8, :o6, 2101953600
          tz.transition 2037, 5, :o5, 2125537200
          tz.transition 2037, 8, :o6, 2133403200
          tz.transition 2038, 5, :o5, 19724421, 8
          tz.transition 2038, 8, :o6, 7396952, 3
          tz.transition 2039, 5, :o5, 19727389, 8
          tz.transition 2039, 8, :o6, 7398044, 3
          tz.transition 2040, 5, :o5, 19730301, 8
          tz.transition 2040, 8, :o6, 7399136, 3
          tz.transition 2041, 5, :o5, 19733213, 8
          tz.transition 2041, 8, :o6, 7400228, 3
          tz.transition 2042, 5, :o5, 19736125, 8
          tz.transition 2042, 8, :o6, 7401320, 3
          tz.transition 2043, 5, :o5, 19739037, 8
          tz.transition 2043, 8, :o6, 7402412, 3
          tz.transition 2044, 5, :o5, 19742005, 8
          tz.transition 2044, 8, :o6, 7403525, 3
          tz.transition 2045, 5, :o5, 19744917, 8
          tz.transition 2045, 8, :o6, 7404617, 3
          tz.transition 2046, 5, :o5, 19747829, 8
          tz.transition 2046, 8, :o6, 7405709, 3
          tz.transition 2047, 5, :o5, 19750741, 8
          tz.transition 2047, 8, :o6, 7406801, 3
          tz.transition 2048, 5, :o5, 19753653, 8
          tz.transition 2048, 8, :o6, 7407893, 3
          tz.transition 2049, 5, :o5, 19756565, 8
          tz.transition 2049, 8, :o6, 7409006, 3
          tz.transition 2050, 5, :o5, 19759533, 8
        end
      end
    end
  end
end
