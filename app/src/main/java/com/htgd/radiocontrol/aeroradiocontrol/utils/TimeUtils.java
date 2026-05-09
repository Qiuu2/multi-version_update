package com.htgd.radiocontrol.aeroradiocontrol.utils;

import java.io.DataOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;


public class TimeUtils {


    private static int year;
    private static int month;
    private static int day;
    private static int hour;
    private static int minute;
    private static int second;
    private   TimeUtils mInstance;
    private String mTAG = "TimeUtils";

    public TimeUtils( ) {

    }
    public   TimeUtils getInstance() {
        if (mInstance == null  ) {
            mInstance = new TimeUtils();
        }

        return mInstance;
    }

    //获取日期时间
    public static String getDate() {
        SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String date = sDateFormat.format(new Date());
        return date.toString();
    }

    //获取月份
    public static String getMonth() {
        Calendar c = Calendar.getInstance();
        String month = String.valueOf(c.get(Calendar.MONTH));
        return month;
    }

    //获取当前日期时间
    public static String getDateAndTime() {
        SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date = sDateFormat.format(new Date());
        return date.toString();
    }
    //获取当前时间
    public static String getTime() {
        SimpleDateFormat sDateFormat = new SimpleDateFormat("HH:mm:ss");
        String date = sDateFormat.format(new Date());
        return date.toString();
    }

    public static  void setTime(String time) {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("date -s 20120419.024012; \n");
            os.writeBytes("date -s "+"20120419.024012"+"; \n");
            os.writeBytes("date -s "+time+"; \n");
        } catch (Exception e) {
            LogUtils.setLog(  "error==" + e.toString());
            e.printStackTrace();
        }
    }

    //获取时间
    public static String getDateTime() {
        Calendar c = Calendar.getInstance();
        String hour = String.valueOf(c.get(Calendar.HOUR));
        String mins = String.valueOf(c.get(Calendar.MINUTE));
        String seconds = String.valueOf(c.get(Calendar.SECOND));
        StringBuffer sbBuffer = new StringBuffer();
        if (hour.length() != 2) {
            hour = 0 + hour;
        }
        if (mins.length() != 2) {
            mins = 0 + mins;
        }
        if (seconds.length() != 2) {
            seconds = 0 + seconds;
        }
        sbBuffer.append(hour + ":" +
                mins + ":" + seconds);
        return sbBuffer.toString();
    }

    //
    public static String  setHMS(int lengths){
        String    time=new String ();
        if (lengths / 60 < 1) {
            time = lengths % 60 + "s";
        } else if (lengths / 3600 < 1) {
            time = lengths / 60 + "m" + lengths % 60 + "s";
        } else if (lengths / 86400 < 1) {
            time = lengths / 3600 + "h" + (lengths % 3600) / 60 + "m" + (lengths % 3600) % 60 + "s";
        } else if (lengths / 2592000 < 1) {
            time = lengths / 86400 + "d" + (lengths % 86400) / 3600 + "h" + ((lengths % 86400) % 3600) / 60 + "m" + ((lengths % 86400) % 3600) % 60 + "s";
        }
        return time;
    }
    //
    public static String getWeek(String datetime) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
        String[] weekDays = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
        Calendar cal = Calendar.getInstance(); // 获得一个日历
        Date datet = null;
        try {
            datet = f.parse(datetime);
            cal.setTime(datet);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        int w = cal.get(Calendar.DAY_OF_WEEK) - 1; // 指示一个星期中的某天。
        if (w < 0)
            w = 0;
        return weekDays[w];
    }


    /**
     * 常规自动日期格式识别
     * @param str 时间字符串
     * @return Date
     * @author dc
     */
    public static String getDateFormat(String str) {
        boolean year = false;
        Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
        if(pattern.matcher(str.substring(0, 4)).matches()) {
            year = true;
        }
        StringBuilder sb = new StringBuilder();
        int index = 0;
        if(!year) {
            if(str.contains("月") || str.contains("-") || str.contains("/")) {
                if(Character.isDigit(str.charAt(0))) {
                    index = 1;
                }
            }else {
                index = 3;
            }
        }
        for (int i = 0; i < str.length(); i++) {
            char chr = str.charAt(i);
            if(Character.isDigit(chr)) {
                if(index==0) {
                    sb.append("y");
                }
                if(index==1) {
                    sb.append("M");
                }
                if(index==2) {
                    sb.append("d");
                }
                if(index==3) {
                    sb.append("H");
                }
                if(index==4) {
                    sb.append("m");
                }
                if(index==5) {
                    sb.append("s");
                }
                if(index==6) {
                    sb.append("S");
                }
            }else {
                if(i>0) {
                    char lastChar = str.charAt(i-1);
                    if(Character.isDigit(lastChar)) {
                        index++;
                    }
                }
                sb.append(chr);
            }
        }
        return sb.toString();
    }
    public ArrayList<Integer> buildTime(String time){
        int year=Integer.parseInt(time.substring(0,time.indexOf("-")));
        time= time.replace(year+"-","");
        LogUtils.setLog(mTAG,"shiji~"+ year );
        int month=Integer.parseInt(time.substring(0,time.indexOf("-")));
        time=time.replaceAll(time.substring(0,time.indexOf("-"))+"-","");
        LogUtils.setLog(mTAG,"shiji~~"+ month );
        int day=Integer.parseInt(time.substring(0,time.indexOf(" ")));
        time= time.replaceAll(time.substring(0,time.indexOf(" "))+" ","");
        LogUtils.setLog(mTAG,"shiji~~~"+ day );
        int hour=Integer.parseInt(time.substring(0,time.indexOf(":")));
        time= time.replaceFirst(time.substring(0,time.indexOf(":"))+":","");

        int minute=Integer.parseInt(time.substring(0,time.indexOf(":")));
        int second=Integer.parseInt(time.replaceFirst(time.substring(0,time.indexOf(":"))+":",""));
        ArrayList<Integer> integers = new ArrayList<>();
        integers.add(year);
        integers.add(month);
        integers.add(day);
        integers.add(hour);
        integers.add(minute);
        integers.add(second);
        LogUtils.setLog(mTAG,"shijianaaa"+ year+month+day+hour+minute+second );
        return  integers;
    }

}
