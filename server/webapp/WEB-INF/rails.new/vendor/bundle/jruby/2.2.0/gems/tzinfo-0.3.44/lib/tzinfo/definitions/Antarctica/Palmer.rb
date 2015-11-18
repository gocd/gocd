module TZInfo
  module Definitions
    module Antarctica
      module Palmer
        include TimezoneDefinition
        
        timezone 'Antarctica/Palmer' do |tz|
          tz.offset :o0, 0, 0, :zzz
          tz.offset :o1, -14400, 3600, :ARST
          tz.offset :o2, -14400, 0, :ART
          tz.offset :o3, -10800, 0, :ART
          tz.offset :o4, -10800, 3600, :ARST
          tz.offset :o5, -14400, 0, :CLT
          tz.offset :o6, -14400, 3600, :CLST
          tz.offset :o7, -10800, 0, :CLT
          
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
          tz.transition 2015, 4, :o7, 1430017200
        end
      end
    end
  end
end
