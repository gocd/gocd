# encoding: UTF-8

module TZInfo
  module Definitions
    module Asia
      module Yekaterinburg
        include TimezoneDefinition
        
        timezone 'Asia/Yekaterinburg' do |tz|
          tz.offset :o0, 14553, 0, :LMT
          tz.offset :o1, 13505, 0, :PMT
          tz.offset :o2, 14400, 0, :SVET
          tz.offset :o3, 18000, 0, :SVET
          tz.offset :o4, 18000, 3600, :SVEST
          tz.offset :o5, 14400, 3600, :SVEST
          tz.offset :o6, 18000, 0, :YEKT
          tz.offset :o7, 18000, 3600, :YEKST
          tz.offset :o8, 21600, 0, :YEKT
          
          tz.transition 1916, 7, :o1, 7747351461, 3200
          tz.transition 1919, 7, :o2, 41854829939, 17280
          tz.transition 1930, 6, :o3, 7278445, 3
          tz.transition 1981, 3, :o4, 354913200
          tz.transition 1981, 9, :o3, 370720800
          tz.transition 1982, 3, :o4, 386449200
          tz.transition 1982, 9, :o3, 402256800
          tz.transition 1983, 3, :o4, 417985200
          tz.transition 1983, 9, :o3, 433792800
          tz.transition 1984, 3, :o4, 449607600
          tz.transition 1984, 9, :o3, 465339600
          tz.transition 1985, 3, :o4, 481064400
          tz.transition 1985, 9, :o3, 496789200
          tz.transition 1986, 3, :o4, 512514000
          tz.transition 1986, 9, :o3, 528238800
          tz.transition 1987, 3, :o4, 543963600
          tz.transition 1987, 9, :o3, 559688400
          tz.transition 1988, 3, :o4, 575413200
          tz.transition 1988, 9, :o3, 591138000
          tz.transition 1989, 3, :o4, 606862800
          tz.transition 1989, 9, :o3, 622587600
          tz.transition 1990, 3, :o4, 638312400
          tz.transition 1990, 9, :o3, 654642000
          tz.transition 1991, 3, :o5, 670366800
          tz.transition 1991, 9, :o2, 686095200
          tz.transition 1992, 1, :o6, 695772000
          tz.transition 1992, 3, :o7, 701816400
          tz.transition 1992, 9, :o6, 717541200
          tz.transition 1993, 3, :o7, 733266000
          tz.transition 1993, 9, :o6, 748990800
          tz.transition 1994, 3, :o7, 764715600
          tz.transition 1994, 9, :o6, 780440400
          tz.transition 1995, 3, :o7, 796165200
          tz.transition 1995, 9, :o6, 811890000
          tz.transition 1996, 3, :o7, 828219600
          tz.transition 1996, 10, :o6, 846363600
          tz.transition 1997, 3, :o7, 859669200
          tz.transition 1997, 10, :o6, 877813200
          tz.transition 1998, 3, :o7, 891118800
          tz.transition 1998, 10, :o6, 909262800
          tz.transition 1999, 3, :o7, 922568400
          tz.transition 1999, 10, :o6, 941317200
          tz.transition 2000, 3, :o7, 954018000
          tz.transition 2000, 10, :o6, 972766800
          tz.transition 2001, 3, :o7, 985467600
          tz.transition 2001, 10, :o6, 1004216400
          tz.transition 2002, 3, :o7, 1017522000
          tz.transition 2002, 10, :o6, 1035666000
          tz.transition 2003, 3, :o7, 1048971600
          tz.transition 2003, 10, :o6, 1067115600
          tz.transition 2004, 3, :o7, 1080421200
          tz.transition 2004, 10, :o6, 1099170000
          tz.transition 2005, 3, :o7, 1111870800
          tz.transition 2005, 10, :o6, 1130619600
          tz.transition 2006, 3, :o7, 1143320400
          tz.transition 2006, 10, :o6, 1162069200
          tz.transition 2007, 3, :o7, 1174770000
          tz.transition 2007, 10, :o6, 1193518800
          tz.transition 2008, 3, :o7, 1206824400
          tz.transition 2008, 10, :o6, 1224968400
          tz.transition 2009, 3, :o7, 1238274000
          tz.transition 2009, 10, :o6, 1256418000
          tz.transition 2010, 3, :o7, 1269723600
          tz.transition 2010, 10, :o6, 1288472400
          tz.transition 2011, 3, :o8, 1301173200
          tz.transition 2014, 10, :o6, 1414267200
        end
      end
    end
  end
end
