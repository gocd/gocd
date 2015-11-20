module TZInfo
  module Definitions
    module Asia
      module Yerevan
        include TimezoneDefinition
        
        timezone 'Asia/Yerevan' do |tz|
          tz.offset :o0, 10680, 0, :LMT
          tz.offset :o1, 10800, 0, :YERT
          tz.offset :o2, 14400, 0, :YERT
          tz.offset :o3, 14400, 3600, :YERST
          tz.offset :o4, 10800, 3600, :YERST
          tz.offset :o5, 10800, 3600, :AMST
          tz.offset :o6, 10800, 0, :AMT
          tz.offset :o7, 14400, 0, :AMT
          tz.offset :o8, 14400, 3600, :AMST
          
          tz.transition 1924, 5, :o1, 1745213311, 720
          tz.transition 1957, 2, :o2, 19487187, 8
          tz.transition 1981, 3, :o3, 354916800
          tz.transition 1981, 9, :o2, 370724400
          tz.transition 1982, 3, :o3, 386452800
          tz.transition 1982, 9, :o2, 402260400
          tz.transition 1983, 3, :o3, 417988800
          tz.transition 1983, 9, :o2, 433796400
          tz.transition 1984, 3, :o3, 449611200
          tz.transition 1984, 9, :o2, 465343200
          tz.transition 1985, 3, :o3, 481068000
          tz.transition 1985, 9, :o2, 496792800
          tz.transition 1986, 3, :o3, 512517600
          tz.transition 1986, 9, :o2, 528242400
          tz.transition 1987, 3, :o3, 543967200
          tz.transition 1987, 9, :o2, 559692000
          tz.transition 1988, 3, :o3, 575416800
          tz.transition 1988, 9, :o2, 591141600
          tz.transition 1989, 3, :o3, 606866400
          tz.transition 1989, 9, :o2, 622591200
          tz.transition 1990, 3, :o3, 638316000
          tz.transition 1990, 9, :o2, 654645600
          tz.transition 1991, 3, :o4, 670370400
          tz.transition 1991, 9, :o5, 685569600
          tz.transition 1991, 9, :o6, 686098800
          tz.transition 1992, 3, :o5, 701812800
          tz.transition 1992, 9, :o6, 717534000
          tz.transition 1993, 3, :o5, 733273200
          tz.transition 1993, 9, :o6, 748998000
          tz.transition 1994, 3, :o5, 764722800
          tz.transition 1994, 9, :o6, 780447600
          tz.transition 1995, 3, :o5, 796172400
          tz.transition 1995, 9, :o7, 811897200
          tz.transition 1997, 3, :o8, 859672800
          tz.transition 1997, 10, :o7, 877816800
          tz.transition 1998, 3, :o8, 891122400
          tz.transition 1998, 10, :o7, 909266400
          tz.transition 1999, 3, :o8, 922572000
          tz.transition 1999, 10, :o7, 941320800
          tz.transition 2000, 3, :o8, 954021600
          tz.transition 2000, 10, :o7, 972770400
          tz.transition 2001, 3, :o8, 985471200
          tz.transition 2001, 10, :o7, 1004220000
          tz.transition 2002, 3, :o8, 1017525600
          tz.transition 2002, 10, :o7, 1035669600
          tz.transition 2003, 3, :o8, 1048975200
          tz.transition 2003, 10, :o7, 1067119200
          tz.transition 2004, 3, :o8, 1080424800
          tz.transition 2004, 10, :o7, 1099173600
          tz.transition 2005, 3, :o8, 1111874400
          tz.transition 2005, 10, :o7, 1130623200
          tz.transition 2006, 3, :o8, 1143324000
          tz.transition 2006, 10, :o7, 1162072800
          tz.transition 2007, 3, :o8, 1174773600
          tz.transition 2007, 10, :o7, 1193522400
          tz.transition 2008, 3, :o8, 1206828000
          tz.transition 2008, 10, :o7, 1224972000
          tz.transition 2009, 3, :o8, 1238277600
          tz.transition 2009, 10, :o7, 1256421600
          tz.transition 2010, 3, :o8, 1269727200
          tz.transition 2010, 10, :o7, 1288476000
          tz.transition 2011, 3, :o8, 1301176800
          tz.transition 2011, 10, :o7, 1319925600
        end
      end
    end
  end
end
