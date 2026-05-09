package com.htgd.radiocontrol.aeroradiocontrol.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.NfcAdapter;
import android.os.Environment;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.regex.Pattern;



/***
 * 工具类
 *
 * @author nxp71465
 */
public class Utils {


    public static String TAG = "Utils";
    public static boolean isNumber(String str) {
        Pattern p = Pattern.compile("[0-9]*");
        return p.matcher(str).matches();
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale+0.5f);
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }
    //获取日期时间
    public static String getDate() {
        SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date = sDateFormat.format(new java.util.Date());
        return date.toString();
    }
    //获取时间
    public static String getDateTime() {
        Calendar c = Calendar.getInstance();
        String hour = String.valueOf(c.get(Calendar.HOUR));
        String mins = String.valueOf(c.get(Calendar.MINUTE));
        String seconds = String.valueOf(c.get(Calendar.SECOND));
        StringBuffer sbBuffer = new StringBuffer();
        sbBuffer.append(hour +
                mins + seconds);
        return sbBuffer.toString();
    }
    public static String getJson(String fileName,Context context) {
        //将json数据变成字符串
        StringBuilder stringBuilder = new StringBuilder();
        try {
            //获取assets资源管理器
            AssetManager assetManager = context.getAssets();
            //通过管理器打开文件并读取

            BufferedReader bf = new BufferedReader(new InputStreamReader(
                    assetManager.open(fileName),"GBK") );
            String line;
            while ((line = bf.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }
    /***
     * 检查网络，有toast提示
     *
     * @param context
     * @return
     */
    public static boolean checkNetworkEnable(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        if (info != null && info.isConnected() && info.getState() == NetworkInfo.State.CONNECTED) {
            return true;
        }
        return false;
    }
    /**
     * 获取屏幕高度(px)
     */
    public static int getScreenHeight(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }
    /**
     * 获取屏幕宽度(px)
     */
    public static int getScreenWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }



    static final String DOWNLOAD_ROOT_PATH = Environment.getExternalStorageDirectory() + "/wallet/apks/";

    static {
        File file = new File(DOWNLOAD_ROOT_PATH);
        if (!file.exists()) {
            file.mkdirs();
        }
    }


    /***
     * 检查NFC是否可用
     *
     * @param context
     * @return
     */
    @SuppressLint("NewApi")
    public static boolean checkNFCDisable(Context context) {
        NfcAdapter nfcAdapter = null;
        try {
            nfcAdapter = NfcAdapter.getDefaultAdapter(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            return false;
        } else {
            return true;
        }
    }

//    /**
//     * 进入设置网络界面
//     *
//     * @param activity
//     */
//    public static void gotoSetNetWork(Context activity) {
//        Intent intent = new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS);
//        ((Activity) activity).startActivity(intent);
//    }


//    /***
//     * 通过包名得到 intent
//     *
//     * @param context
//     * @param packageName
//     * @param pi
//     * @return
//     */
//    private static Intent getIntentFromPackageName(Context context, String packageName, PackageInfo pi) {
//        try {
//            PackageManager manager = context.getPackageManager();
//            if (pi == null) {
//                pi = manager.getPackageInfo(packageName, 0);
//            }
//            Intent resolveIntent = new Intent(Intent.ACTION_MAIN, null);
//            resolveIntent.setPackage(pi.packageName);
//            List<ResolveInfo> apps = manager.queryIntentActivities(resolveIntent, 0);
//            if (apps != null & apps.size() != 0) {
//                ResolveInfo ri = apps.iterator().next();
//                if (ri != null) {
//                    packageName = ri.activityInfo.packageName;
//                    String className = ri.activityInfo.name;
//                    Intent intent = new Intent(Intent.ACTION_MAIN);
//
//                    ComponentName cn = new ComponentName(packageName, className);
//                    intent.setComponent(cn);
//                    return intent;
//                }
//            }
//        } catch (NameNotFoundException e) {
//        }
//        return null;
//    }

//    /***
//     * 启动应用
//     *
//     * @param context
//     * @param packageName
//     * @throws Exception
//     */
//    public static void loadApk(Activity context, String packageName) throws Exception {
//        if (packageName.length() > 0) {
//            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
//            if (launchIntent == null) {
//                launchIntent = getIntentFromPackageName(context, packageName, null);
//            }
//            if (launchIntent != null) {
//                context.startActivity(launchIntent);
////				context.startActivityForResult(launchIntent, 104);
//                return;
//            }
//        }
//        throw new Exception("packageName " + packageName + " not found");
//    }
//
//    public static void showNotification(Context context, NotificationCompat.Builder builder, int notificationId) {
//        NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//        mNotifyMgr.notify(notificationId, builder.build());
//    }
//
//    public static void cacleNotification(Context context, int notificationId) {
//        NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//        mNotifyMgr.cancel(notificationId);
//    }

//    public static String pAmountToTvPAmount(float pAmount) {
//        if (pAmount > 9999999999.99f || pAmount < 0) {
//            return "";
//        }
//        int integerPart = (int) pAmount;
//        String result = String.format(String.format("%010d", integerPart));
//        String strValue = String.valueOf(pAmount);
//        int index = strValue.indexOf('.');
//        if (index == -1) {
//            result += "00";
//            return result;
//        }
//        int count = 0;
//        String fractionPart = "";
//        while (count <= 1) {
//            count++;
//            index++;
//            if (index > strValue.length() - 1) {
//                break;
//            }
//            char c = strValue.charAt(index);
//            fractionPart += c;
//        }
//        if (fractionPart.length() == 0) {
//            return "";
//        } else if (fractionPart.length() == 1) {
//            fractionPart += "0";
//        }
//        result += fractionPart;
//        return result;
//    }

//    /**
//     * 每四个字符以空格分开
//     */
//    public static String formatToBankNumber(String number) {
//        int lenth = number.length();
//        int sections = lenth / 4;
//        StringBuffer buffer = new StringBuffer();
//        for (int i = 0; i < sections; i++) {
//            buffer.append(number.substring(i * 4, i * 4 + 4));
//            buffer.append(" ");
//        }
//        buffer.append(number.substring(sections * 4, lenth));
//        return buffer.toString();
//    }

    @SuppressLint("DefaultLocale")
    public static String toAmountString(float amount) {
        return String.format("%.2f", amount);
    }

//    /**
//     * 充值圈存等使用的aid
//     */
//    public static final String INSTANCE_ID = "A000000333010106";
//
//    /**
//     * 访问服务端的token常量
//     */
//    public static final String AUTH_TOKEN = "123456789abcdefghi";

//	public static String postRequest(Context context, Map<String, String> params) {
//		/**
//		 * 向服务端发请求
//		 */
//		Set<String> set = params.keySet();
//		String method = null;
//		StringBuffer postDatas = new StringBuffer();
//
//		for (String key : set) {
//			if (key.equals("method")) {
//				method = params.get(key);
//			} else {
//				try {
//					postDatas.append(URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(params.get(key), "UTF-8") + "&");
//				} catch (UnsupportedEncodingException e) {
//					e.printStackTrace();
//				}
//			}
//		}
//		postDatas.deleteCharAt(postDatas.length() - 1);
//		String data = postDatas.toString();
//		String response = HttpRequest.request_https(context, method, data);
//
//		return response;
//	}

	/*public static IccData setIccDataBeforeScriptExecution(GacCommand gacCommand, GacCommandResponse gacCommandResponse, GpoCommandResponse gpoCommandResponse) {
        // 先检查传入参数是否为空
		if (gacCommand == null || gacCommandResponse == null || gpoCommandResponse == null) {
			return null;
		}
		*/

    /**
     * 组装IccData数据
     *//*
        IccData iccData = new IccData();
		iccData.setTAG_9F02(gacCommand.getTAG_9F02());
		iccData.setTAG_9F03(gacCommand.getTAG_9F03());
		iccData.setTAG_9F1A(gacCommand.getTAG_9F1A());
		iccData.setTAG_95(gacCommand.getTAG_95());
		iccData.setTAG_5F2A(gacCommand.getTAG_5F2A());
		iccData.setTAG_9A(gacCommand.getTAG_9A());
		iccData.setTAG_9C(gacCommand.getTAG_9C());
		iccData.setTAG_9F37(gacCommand.getTAG_9F37());
		iccData.setTAG_82(gpoCommandResponse.getTAG_82());
		iccData.setTAG_9F27(gacCommandResponse.getTAG_9F27());
		iccData.setTAG_9F36(gacCommandResponse.getTAG_9F36());
		iccData.setTAG_9F26(gacCommandResponse.getTAG_9F26());
		iccData.setTAG_9F10(gacCommandResponse.getTAG_9F10());
		iccData.setTAG_9F33("E0E1C8");

		return iccData;
	}*/

	/*public static IccData setIccDataAfterScriptExecution(GacCommand gacCommand, GacCommandResponse gacCommandResponse, GpoCommandResponse gpoCommandResponse, CommandResponse scriptCommandResponse) {
        // 先检查传入参数是否为空
		if (gacCommand == null || gacCommandResponse == null || gpoCommandResponse == null || scriptCommandResponse == null) {
			return null;
		}
		// 构造ICC_DATA数据
		IccData iccData = new IccData();
		// 终端性能,9F33字段，文档中说明3个字节0的字节
		iccData.setTAG_9F33("000000");
		// 终端验证结果，95字段，文档中说明五个全0的字节
		iccData.setTAG_95(gacCommand.getTAG_95());
		// 不可预知数，9F37，Apk生成，随即生成的四字节
		iccData.setTAG_9F37(gacCommand.getTAG_9F37());
		// 接口设备序列号，9F1E，Apk生成,八个字符
		iccData.setTAG_9F1E("00000000");
		// 发卡行应用数据，9F10，Gac返回，需解析，返回apdu从第十五字节后的字节（除掉最后两个字节的状态字）
		iccData.setTAG_9F10(gacCommandResponse.getTAG_9F10());
		// 应用密文，9F26，Gac返回，需解析，返回apdu的第六到14字节
		iccData.setTAG_9F26(gacCommandResponse.getTAG_9F26());
		// 应用交易计数器，9F36，Gac返回，需解析，返回apdu的第四第五字节
		iccData.setTAG_9F36(gacCommandResponse.getTAG_9F36());
		// 应用交互特征，82，Gpo响应数据，需解析，第三和第四字节
		iccData.setTAG_82(gpoCommandResponse.getTAG_82());
		// 发卡方脚本结果，DF31，卡片执行完脚本返回的结果
		if (scriptCommandResponse.isSucceed()) {
			iccData.setTAG_DF31("2000000000");
		} else {
			iccData.setTAG_DF31("0000000000");
		}
		// 终端国家代码，9F1A，0156
		iccData.setTAG_9F1A(gacCommand.getTAG_9F1A());
		// 交易日期，9A
		iccData.setTAG_9A(gacCommand.getTAG_9A());

		return iccData;
	}*/

//    public static final class ClientErrorIds {
//        // 获取的SEConnection对象为空
//        public static final int SE_SERVICE_IS_NULL = 1;
//        // 从SEConnection获取的selectResponse为空
//        public static final int SE_SEL_RESP_IS_NULL = 2;
//        // 执行GPO指令失败
//        public static final int SE_GPO_FAILED = 3;
//        // 现金圈存时执行首次gac指令失败
//        public static final int SE_XJQC_FIRST_GAC_FAILED = 4;
//        // 现金圈存前客户端组装ICC_DATA并将之序列化出错
//        public static final int SE_XJQC_ICC_DATA_ENC_ERROR = 5;
//        // 现金圈存时解析服务端返回的RechargeResponse出错
//        public static final int PARSE_RECHARGE_RESP_ERROR = 6;
//        // 现金圈存时解析服务端返回的ICC_DATA出错
//        public static final int XJQC_DECODE_ICC_DATA_FROM_SERV_ERROR = 7;
//    }

//    /**
//     * 判断网络是否可用
//     *
//     * @param context
//     * @return
//     */
//    public static boolean isConnect(Context context) {
//        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
//        if (connectivity != null) {
//            NetworkInfo info = connectivity.getActiveNetworkInfo();
//            if (info != null && info.isConnected()) {
//                if (info.getState() == NetworkInfo.State.CONNECTED) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }

//	/**
//	 * http请求
//	 * @param reqparam
//	 * @param handler
//	 */
//	public static void GetDataFormServer(final String reqparam, final Handler handler) {
//		new Thread(new Runnable() {
//			@Override
//			public void run() {
//				try {
//					String url = "http://officeserver.snowballtech.com:6060/sample/KMSServlet";
//					if(url.contains("http://")){
//						sendHttpForHttpClient(url, reqparam,handler);
//					}else if(url.contains("https://")){
//						sendHttpsRequestForHttpClient(url, reqparam,handler);
//					}else{
//						handler.obtainMessage(1, "url地址错误").sendToTarget();
//					}
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		}).start();
//	}

//	public static void sendHttpForHttpClient(String urlStr, String request,Handler handler){
//		try {
//			HttpClient httpClient = new DefaultHttpClient();
//			httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 15000);
//			httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 15000);
//			HttpPost httpPost;
//			httpPost = new HttpPost(urlStr);
//			List<NameValuePair> params = new ArrayList<NameValuePair>();
//			params.add(new BasicNameValuePair("requestParam", request));
//			httpPost.setEntity(new UrlEncodedFormEntity(params, "utf-8"));
//			HttpResponse httpResponse = httpClient.execute(httpPost);
//			int resultCode = httpResponse.getStatusLine().getStatusCode();
//			Log.i(TAG,"access url__ :" + urlStr + " ,  status :" + resultCode);
//			if (resultCode== 200) {
//				String json = EntityUtils.toString(httpResponse.getEntity());
//				sendHandString(json, 0, handler);
//				Log.i(TAG,"access url__ :" + urlStr + " ,  result :" + json);
//			} else {
//				sendHandString("获取服务端数据失败！", 1, handler);
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//			sendHandString("访问服务端异常！", 1, handler);
//
//		}
//		Log.e(TAG, "end sendHttpForHttpClient");
//	}

//    static class SSLSocketFactoryEx extends SSLSocketFactory {
//        SSLContext sslContext = SSLContext.getInstance("TLS");
//
//        public SSLSocketFactoryEx(KeyStore truststore) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
//            super(truststore);
//            sslContext.init(null, new TrustManager[]{new TrustAnyTrustManager()}, null);
//        }
//
//        @Override
//        public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
//            return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
//        }
//
//        @Override
//        public Socket createSocket() throws IOException {
//            return sslContext.getSocketFactory().createSocket();
//        }
//    }

//    private static class TrustAnyTrustManager implements X509TrustManager {
//        @Override
//        public X509Certificate[] getAcceptedIssuers() {
//            return null;
//        }
//
//        @Override
//        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
//        }
//
//        @Override
//        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
//        }
//    }

//	public static String sendHttpsRequestForHttpClient(String urlStr, String request,Handler handler) {
//		Log.i(TAG," access url sendHttpsRequestForHttpClient begin,url="+urlStr+"_param="+request);
//		String respononse = "";
//		try {
//			HttpParams httpParameters = new BasicHttpParams();
//			HttpConnectionParams.setConnectionTimeout(httpParameters, 5000);
//			HttpConnectionParams.setSoTimeout(httpParameters, 5000);
//			HttpClient httpclient = new DefaultHttpClient(httpParameters);
//			KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
//			trustStore.load(null, null);
//			SSLSocketFactory sf = new SSLSocketFactoryEx(trustStore);
//			sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
//			ClientConnectionManager ccm = httpclient.getConnectionManager();
//			SchemeRegistry sr = ccm.getSchemeRegistry();
//			sr.register(new Scheme("https", sf, 443));
//			HttpPost httpPost = new HttpPost(urlStr);
//
//			List<NameValuePair> params = new ArrayList<NameValuePair>();
//			params.add(new BasicNameValuePair("requestParam", request));
//			httpPost.setEntity(new UrlEncodedFormEntity(params, "utf-8"));
//
//			HttpResponse responseObj = httpclient.execute(httpPost);
//			if (responseObj != null) {
//				HttpEntity entity = responseObj.getEntity();
//				if (null != entity) {
//					int status = responseObj.getStatusLine().getStatusCode();
//					Log.i(TAG,"access url :" + urlStr + " ,  status :" + status);
//					if (status == 200) {
//						String json = EntityUtils.toString(responseObj.getEntity());
//						Log.i(TAG," costtime access url :" + urlStr + " result:" + json);
//						sendHandString(json, 0, handler);
//					}else {
//						sendHandString("获取服务端数据失败！", 1, handler);
//
//					}
//					entity.consumeContent();
//				} else {
//					Log.i(TAG,"  access url :" + urlStr + " , HttpEntity is null ");
//				}
//			} else {
//				Log.i(TAG,"  access url :" + urlStr + " , HttpResponse is null ");
//			}
//
//		} catch (Exception e) {
//			Log.i(TAG,"exception="+e.getMessage());
//			sendHandString("访问服务端异常！", 1, handler);
//			e.printStackTrace();
//		}
//		Log.i(TAG, " access url sendHttpsRequestForHttpClient end ");
//		return respononse;
//	}

//    public static void sendHandString(String result, int what, Handler handler) {
//        Message mes = new Message();
//        mes.what = what;
//        mes.obj = result;
//        handler.sendMessage(mes);
//    }
}
