package com.htgd.radiocontrol.aeroradiocontrol.constant;

import android.os.Environment;

/**
 * Created by wzq on 2019-07-28.
 */
public class Constant {
    public static final String header = "Authorization";
    public static final String token_tag = "Bearer ";
    // 分配给sdk的端口号
    public static int SDK_SERVER_NOMBER;
    //音频
    public static int VOICE_NOMBER_INIT = 80;
    //sdk的版本号
    public static int SDK_VERSION;

    public static String serveraddress;
    //获取服务器认证
    public static final String getAuthorization = "/authorizations";
    //获取服务器时间
    public static final String getServerTime = "";
    //获取所有设备
    public static final String getMahcinelistAll = "/terminal/terminalinfo";
    //获取所有设备
    public static final String getMahcinelist = "/terminal/terminaldo/";
    //获取设备经纬度
    public static final String getMahcineLatitude = "/terminal/gitude/";
    //保存设备经纬度
    public static final String saveMahcineLatitude = "/terminal/savegitude";
    //获取单个设备信息
    public static final String getMachineInfo = "";
    //获取分区列表
    public static final String getPartMachines = "";
    //获取任务列表
    public static final String getTaskList = "/task/taskinfo";
    //获取作息任务列表
    public static final String getsecheList = "/task/sechinfo";
    //获取单个任务信息
    public static final String getTaskInfo = "";

    //获取文字语音任务内容信息
    public static final String getTtsTaskContent = "/task/ttstaskcontent";
    //删除任务记录
    public static final String deleteTask = "/task/taskinfo";
    //删除任务媒体
    public static final String deleteTaskMedia = "/task/taskmusic";
    //上传单个任务信息
    public static final String postTtsTaskInfo = "/task/ttstaskinfo";
    //上传单个任务信息
    public static final String putTaskInfo = "/task/taskinfo";
    //删除任务终端
    public static final String deleteTaskTerminal = "/task/taskterminal";
    //创建作息方案任务
    public static final String postSchemeTask = "/task/sechetask";
    //更新作息方案任务
    public static final String putSchemeTask = "/task/sechetask";
    //创建作息方案
    public static final String postSchemeInfo = "/task/sechetaskinfo";
    //启用或停用作息任务
    public static final String postChangeTaskStatu = "/task/sechenableordisable";
    //根据分区获取终端
    public static final String getGroupTerminal = "/terminal/zoneterminal";
    //删除终端分区
    public static final String deleteZone = "/terminal/terzone";
    //根据分区提交终端
    public static final String postGroupTerminal = "/terminal/zoneterminal";
    //根据任务获取设备
    public static final String getTaskMachines = "/task/taskterminal";
    //根据任务获取媒体
    public static final String getTaskMusics = "/task/taskmusic";
    //获取指定类型的媒体
    public static final String getMusicInfo = "/terminal/mediainfo/";
    //获取所有媒体
    public static final String getAllMusicInfo = "/terminal/mediainfo";
    //获取媒体文件夹
    public static final String getFolderInfo = "/terminal/mediafolderinfo";
    //获取作息方案的任务列表
    public static final String postTaskListInfo = "/task/sechetaskinfo";
    //运行或停止广播方案
    public static final String postRunOrStopTask = "/task/taskenordis";
    //启用或停止方案
    public static final String postUserOrStopTask = "/task/taskdoorno";
    //上传单个任务信息
    public static final String postTaskInfo = "/task/taskinfo";
    //调节任务声音
    public static final String postSetTaskVoice = "/task/taskvolume";
    //获取快捷任务接口
    public static final String postGetShortcutTask = "/terminal/terminalquicktask";
    //获取设备快捷键
    public static final String getTerminalShortcutkey = "/terminal/shortcutkey";
    //获取临时任务记录
    public static final String getTempTtsTask = "/task/gettempttstask";
    //删除临时任务记录
    public static final String deleteTempTtsTask = "/task/deltemptts";
    //获取服务器状态
    public static final String getServerState = "/server/serverstate";
    //查找分区
    public static final String SearchZone = "/terminal/terzone";
    //上传终端分区
    public static final String postZone = "/terminal/terzone";
    //上传文件
    public static final String postFile = "/terminal/mediainfo";
    //任务绑定音乐
    public static final String setTaskMusic = "/task/taskmusic";
    //添加临时任务
    public static final String addTempTask = "/task/addtempttstask";
    //添加临时任务的终端
    public static final String postTaskTerminal = "/task/taskterminal";

    //启动紧急任务
    public static final String postUrgentPLAY = "/terminal/urgentplay";

    //上传临时文件
    public static final String postTempMediaFile = "/task/addtempttstaskmedia";
    //固定数据的KEY
    public static final String BUNDLE_KEY_TYPE = "budle_key";  //key

    public static final String BUNDLE_KEY_MODEL = "bulde_model";

    public static final int keyZuoxi = 1;  //作息方案标识

    public static final int keyGuangbo = 2; //文件广播标识

    public static final int keyCaibo = 3; //采播管理

    public static final int keyGongfang = 5; // 终端功放

    public static final int keyDiantai = 10; // 网络电台
    public static final int keyWenZiYuYin = 17; // 文字语音

    public static final String BUNDLE_KEY_MACHINE_ID_LIST = "budle_machine_id_list";

    //网络请求时的筛选
    public static final int FLAG_XUNHU = 1;  //寻呼
    public static final int FLAG_DUIJIANG = 2; //对讲
    public static final int FLAG_DIANBO = 3; //点播

    public static final int FALG_MUSIC = 3; // 点播媒体
    //获取tts状态
    public static final String getTtsState = "get_tts_state";//请求tts状态
    //缓存时候的key
    public static final String key_tokenModel = "get_token_request"; // 请求token 实体
    public static final String key_terminalName = "get_terminal_name";

    public static final String key_tokenString = "sever_token"; //服务器获取到的token
    public static final String key_user_priority = "user_priority";


    public static final String LOCAL_DIR = Environment.getExternalStorageDirectory().toString() + "/" + "htgd" + "/";
    public static final String LOCAL_DIR_LOG = Environment.getExternalStorageDirectory().toString() + "/" + "htgd_LOG" + "/";
    public static final String APK_DIR = Environment.getExternalStorageDirectory().toString() + "/" + "htgd" + "/apk";

    public static final String SD_PATH = "/mnt/usb_storage/USB_DISK2/udisk0/htgd";



}
