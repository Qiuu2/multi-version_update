package com.htgd.radiocontrol.aeroradiocontrol.utils;

import android.text.TextUtils;
import android.view.View;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class ValueUtil {
    private static String ip;
    private static String originalHost;
//    public static String TempSymbol = "°C";
//    public static String YuanSymbol = "￥";
//    public static final int LineNumber = 10;// 每个分页的行数
//    public static final int HistoryActLineNumber = 7;// 每个活动历史分页的行数
//    private static final int KB = 1024;
//    private static final int MG = KB * KB;
//    private static final int GB = MG * KB;

    /**
     * @param value
     * @return
     * @author carlos E-mail:carloscyy@gmail.com
     * @version 创建时间:2011-11-8 下午04:11:55
     */
    public static boolean isEmpty(String value) {
        if (null == value || "".equals(value.trim())) {
            return true;
        } else {
            // 判断是否全是全角空格
            value = value.replaceAll(" ", "").trim();
            if (null == value || "".equals(value.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 把字符串转换为 正确的格式
     *
     * @return 时间
     */

    public static String getNowDate(String dateString) {
        String newdate = "";
        if (dateString != null && dateString.length() == 14) {
            newdate = dateString.substring(0, 4) + "-" + dateString.substring(4, 6) + "-" + dateString.substring(6, 8) + "   " + dateString.substring(8, 10) + ":" + dateString.substring(10, 12) + ":" + dateString.substring(12, 14);
        } else if (dateString != null) {
            newdate = dateString;
        }
        return newdate;
    }

//    public static short parseShort(String shortString) {
//        if (shortString != null && !shortString.trim().equals("")) {
//            try {
//                return Short.valueOf(shortString);
//            } catch (Exception e) {
//            }
//        }
//        return 0;
//    }

    public static int parseInt(String intString) {
        if (intString != null && !intString.trim().equals("")) {
            try {
                return Integer.valueOf(intString);
            } catch (Exception e) {
            }
        }
        return 0;
    }

    public static float parseFloat(String intString) {
        if (intString != null && !intString.trim().equals("")) {
            try {
                return Float.parseFloat(intString);
            } catch (Exception e) {
            }
        }
        return 0;
    }

    public static String toBin(int data, int len, String defaultHold) {
        StringBuilder bin = new StringBuilder(Integer.toBinaryString(data));
        int size = len - bin.length();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                bin.insert(0, defaultHold);
            }
        }
        return bin.toString();
    }

    /**
     * 验证手机格式
     */
    public static boolean isMobileNO(String mobiles) {
        /*
         * 移动：134、135、136、137、138、139、150、151、157(TD)、158、159、187、188
		 * 联通：130、131、132、152、155、156、185、186 电信：133、153、180、189、（1349卫通）177
		 * 总结起来就是第一位必定为1，第二位必定为3或5或8，其他位置的可以为0-9
		 */
        String telRegex = "[1][3578]\\d{9}";// "[1]"代表第1位为数字1，"[358]"代表第二位可以为3、5、8中的一个，"\\d{9}"代表后面是可以是0～9的数字，有9位。
        if (TextUtils.isEmpty(mobiles)) return false;
        else return mobiles.matches(telRegex);
    }

    public static boolean analyzDns(String ipAddress) {
        URL url = null;
        try {
            url = new URL(ipAddress);
        } catch (MalformedURLException e) {
            return isIP(ipAddress);
        }
        originalHost = url.getHost();
        // 同步接口获取IP
        try {
            InetAddress  x = InetAddress.getByName(originalHost);
          ip = x.getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return isIP(ip);
    }
    public static boolean isIP(String addr) {
        if (addr.length() < 7 || addr.length() > 15 || "".equals(addr)) {
            return false;
        }
        /**
         * 判断IP格式和范围
         */
        String rexp = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";
        Pattern pat = Pattern.compile(rexp);
        Matcher mat = pat.matcher(addr);
        boolean ipAddress = mat.find();
        //============对之前的ip判断的bug在进行判断
        if (ipAddress == true) {
            String ips[] = addr.split("\\.");
            if (ips.length == 4) {
                try {
                    for (String ip : ips) {
                        if (Integer.parseInt(ip) < 0 || Integer.parseInt(ip) > 255) {
                            return false;
                        }
                    }
                } catch (Exception e) {
                    return false;
                }
                return true;
            } else {
                return false;
            }
        }
        return ipAddress;
    }

    // 将整形数变为7位的2进制数 位数不够在前面自动补齐
    public static int[] getWorkDays(int number){
        int day[] = new int[7];
        String a = Integer.toBinaryString(number);
        if(a.length()<7){
            for(int i = 0; i<7-a.length();i++){
                day[i] = 0;
            }
            int b = 7-a.length();
            for(int i = b; i<7;i++){
                day[i] = Integer.parseInt(a.charAt(i-b)+"");
            }
        }
        return day;
    }


}
