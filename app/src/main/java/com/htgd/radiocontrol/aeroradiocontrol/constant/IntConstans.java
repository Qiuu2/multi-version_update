package com.htgd.radiocontrol.aeroradiocontrol.constant;

/**
 * 作者：wzq
 * 时间：2019/5/10:10:01
 * 邮箱：535708929
 * 说明：数字型常量,设置界面无法设置的，编写时写死的
 */
public class IntConstans {
   public static  int TEMP_VOICE_DELETE=3;//3个月以上就删除
   public static  int STARTANIMATION=4000;//开机动画时长4s
   public static  int RECONNECT_WHEN_OUTLINE=30000;//断线时重连间隔时间
   public static  int RECORD_MAX_TIME=120;//录音最长时间
   public static   long RefreshPeriodTimeInCall=2000;
   public static   long RefreshPeriodTimeInMap=5000;
   public static  int ds=4000;//
   public static  int maxcontent=1000;//

   public static int dialogwidth = 385;//弹框默认宽度
   public static int dialogheight = 454;//弹框默认高度

   public static final int keyGuangbo = 2; //文件广播标识
   public static final int keyZuoxi = 1;  //作息方案标识

   public static final int keyCaibo = 3; //采播管理

   public static final int keyGongfang = 5; // 终端功放

   public static final int keyDiantai = 10; // 网络电台
   public static final int keyWenZiYuYin = 17; // 文字语音

   //网络请求时的筛选
   public static final int FLAG_XUNHU = 1;  //寻呼
   public static final int FLAG_DUIJIANG = 2; //对讲
   public static final int FLAG_DIANBO = 3; //点播
   public  static  String FileBroadCycle="2";


   public static int mStrokeWidth = 2;   //地图线宽度
   public static int mColor = 80;       //颜色
   public static int mFillAlpha = 10;  //填充透明度
   public static int terminaltype[]={1,2,3,4,5,8,14,17,25,26,28,30,31,34,35,37,40,41,44};

}
