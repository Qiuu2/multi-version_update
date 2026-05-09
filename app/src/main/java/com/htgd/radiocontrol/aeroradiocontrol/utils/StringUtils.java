package com.htgd.radiocontrol.aeroradiocontrol.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wzw on 2018/4/4.
 * 判断特殊字符的工具
 */

public class StringUtils {
    public static boolean isSpecialChar(String str) {
        String regEx = "[ _`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]|\n|\r|\t";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(str);
        return m.find();
    }
}
