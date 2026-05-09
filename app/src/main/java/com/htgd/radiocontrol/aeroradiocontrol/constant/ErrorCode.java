package com.htgd.radiocontrol.aeroradiocontrol.constant;

/**
 * Created by wzq on 2017-07-31.
 */
public class ErrorCode {
    public static final int SUCESS = 200;
    public static final  String SucessMsg = "sucesss ";

    public static final int TOKEN_EXPIRED = 401;
    public static final String ToeknExpiredMsg = "Token has expired";

    public static final int Failed = 9001;
    public static final String FailedMsg = "failed";

    /**
     * 手机环境引起的问题
     */
    public static final int NetWork_disable  = 1001;
    public static final String NetWork_disableMsg ="network disbale";

    public static final int NO_Permission=1002;
    public static final String NO_PermissionMsg = "系统没有给于这项权限";
    /**
     * 数据问题引起的错误
     */
    public static final int Data_Eorro= 2000;
    public static final String Data_EorroMsg ="数据格式错误";

    public static final int InputParmNull = 2001;
    public static final String InputParmNullMsg = "参数为空";

    public static final int BackDataEorro =2002;
    public static final String BackDataEorroMsg = "返回数据出错";

    public static final int DataTypeEorro = 2003;
    public static final String DataTypeEorroMsg = "数据类型不正确";

    /**
     *  业务方面引起的问题
     */
    public static final int Connect_ServerFailed =4000;
    public static final String Connect_ServerFailedMsg ="连接服务器失败";

    public static final int NoDataInPrefernce = 4001;
    public static final String NoDataInPrefernceMsg = "缓存中无数据";


    public static final String theStateIsTheSame = "当前状态已经同步";

    public static final String GET_SERVER_NOMBER_FAILED = "获取服务器端口失败";


    /**
     *   抛出异常
     */
    public static final int  Exception = 9999;
    public static final String ExceptionMsg ="system exception";

    public static final int ChangeSucess = 0;
    public static final int TheStateIsSame = 15;

}
