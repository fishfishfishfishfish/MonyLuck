package com.simon.a61915.monyluck;

import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static android.content.Context.MODE_PRIVATE;

public class XposedModule implements IXposedHookLoadPackage {
    private static int MODE = MODE_PRIVATE;
    private static String SENDTIME_SP = "saveSendTime";
    private static SharedPreferences SP;
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XposedBridge.log("Loaded app: " + lpparam.packageName);
        XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Context ctx = (Context)param.args[0];
                ClassLoader cl = ctx.getClassLoader();
                SP = ctx.getSharedPreferences(SENDTIME_SP, MODE);
                XposedBridge.log("finding process:"+lpparam.processName+",context:"+param.args[0]);
                hookRecivingTime(cl, lpparam.processName);
                hookSendingTime(cl, lpparam.processName);
                super.afterHookedMethod(param);
            }
        });
    }
    private static void hookSendingTime(final ClassLoader cl, String processName)throws Throwable{
        XposedBridge.log("finding sdk.e in " + processName);
        try {
            XposedHelpers.findAndHookMethod("com.tencent.mm.bu.h", cl, "insert",
                    String.class, String.class, ContentValues.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object tableName = param.args[0];
                    Object fieldName = param.args[1];
                    ContentValues cv = (ContentValues) param.args[2];
                    if("AppMessage".equals(tableName) || "msgId".equals(fieldName)){
                        XposedBridge.log("WXField-" + "table name: " + tableName + "\n");
                        XposedBridge.log("WXField-" + "field name: " + fieldName + "\n");
                        String content = cv.get("xml").toString();
                        String sendId = parseSendId(content).substring(13);
                        String sendTime = Long.toString(System.currentTimeMillis()/1000);
                        saveCreateTime(SP, sendId, sendTime);
                        XposedBridge.log("WXField-content:" + sendId);
                        XposedBridge.log("WXField-createTime:" + sendTime);
                    }
                    super.afterHookedMethod(param);
                }
            });
        }catch (Exception e){
            XposedBridge.log("finding sdk.e errors: "+ Log.getStackTraceString(e));
        }
    }

    private static void hookRecivingTime(final ClassLoader cl, String processName) throws Throwable{
        Class<?> hookclass = null;
        try{
            hookclass = cl.loadClass("com.tencent.mm.plugin.luckymoney.ui.i");
            XposedBridge.log("finded in " + processName);
            XposedHelpers.findAndHookMethod(hookclass, "getView", int.class, View.class, ViewGroup.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    //反射调用sJ方法
                    int index = (Integer)param.args[0];
                    Object obj = param.thisObject;
                    Method method = obj.getClass().getDeclaredMethod("sJ", int.class);
                    method.setAccessible(true);
                    Object itemObj = method.invoke(obj, index);
                    Object owJ = itemObj.getClass().getField("owJ").get(itemObj);
                    Object oxf = itemObj.getClass().getField("oxf").get(itemObj);
                    Object userName = itemObj.getClass().getField("userName").get(itemObj);

                    String sendId = owJ.toString().substring(13);
                    String revTime = oxf.toString();
                    String sendTime = SP.getString(sendId, "error");
                    long costTime = Long.parseLong(revTime)-Long.parseLong(sendTime);

                    XposedBridge.log("WXField-" + "sendId:" + sendId); // sendId
                    XposedBridge.log("WXField-" + "revTime:" + revTime); // 领取时间
                    XposedBridge.log("WXField-" + "sendTime:" + sendTime); //发红包时间
                    XposedBridge.log("WXField-" + "costTime:" + Long.toString(costTime)); //花费时间

                    //修改view
                    Object result = param.getResult();
                    if(result instanceof LinearLayout){
                        LinearLayout itemView = (LinearLayout)result;
                        LinearLayout leftLayout = (LinearLayout)itemView.getChildAt(1);
                        TextView timetext = (TextView)leftLayout.getChildAt(3);
                        timetext.setTextColor(0xFFFF0000);
                    }
                    super.afterHookedMethod(param);
                }
            });
        }catch (Exception e){
            XposedBridge.log("finding ui.i errors: "+ Log.getStackTraceString(e));
        }
    }

    private static String parseSendId(String xmlData) throws Exception{
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(new StringReader(xmlData));
        int eventType = parser.getEventType();
        String res = "";
        while (eventType != XmlPullParser.END_DOCUMENT){
            String nodeName = parser.getName();
            switch (eventType){
                case XmlPullParser.START_TAG:{
                    if("paymsgid".equals(nodeName)){
                        res = parser.nextText();
                    }
                    break;
                }
            }
            if(!"".equals(res)){
                break;
            }
            parser.next();
            eventType = parser.getEventType();
        }
        return res;
    }
    private static void saveCreateTime(SharedPreferences SP, String sendId, String createTime){
        SharedPreferences.Editor SPEdit = SP.edit();
        SPEdit.putString(sendId, createTime);
        SPEdit.apply();
    }
}
