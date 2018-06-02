package com.simon.a61915.monyluck;

import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedModule implements IXposedHookLoadPackage {
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XposedBridge.log("Loaded app: " + lpparam.packageName);
        XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ClassLoader cl = ((Context)param.args[0]).getClassLoader();
                XposedBridge.log("finding process:"+lpparam.processName+",context:"+param.args[0]);
//                hookRecivingTime(cl, lpparam.processName);
                hookSendingTime(cl, lpparam.processName);
                super.afterHookedMethod(param);
            }
        });
    }
    public static void hookSendingTime(final ClassLoader cl, String processName)throws Throwable{
        XposedBridge.log("finding sdk.e in " + processName);
        try {
            XposedHelpers.findAndHookMethod("com.tencent.mm.bu.h", cl, "insert",
                    String.class, String.class, ContentValues.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object tableName = param.args[0];
                    Object fieldName = param.args[1];
                    ContentValues cv = (ContentValues) param.args[2];
                    XposedBridge.log("DBInfo-" + "table name: " + tableName + "\n");
                    XposedBridge.log("DBInfo-" + "field name: " + fieldName + "\n");
                    for (String key : cv.keySet()) {
                        XposedBridge.log("DBInfo-" + "key=" + key + ", value=" + cv.get(key) + "\n");
                    }
                    XposedBridge.log("------table name: " + tableName + "----" + "field name: " + fieldName + "--end-----\n");
                    super.afterHookedMethod(param);
                }
            });
        }catch (Exception e){
            XposedBridge.log("finding sdk.e errors: "+ Log.getStackTraceString(e));
        }
    }

    public static void hookRecivingTime(final ClassLoader cl, String processName) throws Throwable{
        Class<?> hookclass = null;
        try{
            hookclass = cl.loadClass("com.tencent.mm.plugin.luckymoney.ui.i");
            XposedBridge.log("finded in " + processName);
            XposedHelpers.findAndHookMethod(hookclass, "sJ", int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object obj = param.getResult();
                    Object owJ = obj.getClass().getField("owJ").get(obj);
                    Object oxe = obj.getClass().getField("oxe").get(obj);
                    Object oxf = obj.getClass().getField("oxf").get(obj);
                    Object oxs = obj.getClass().getField("oxs").get(obj);
                    Object oxt = obj.getClass().getField("oxt").get(obj);
                    Object oxu = obj.getClass().getField("oxu").get(obj);
                    Object userName = obj.getClass().getField("userName").get(obj);

                    XposedBridge.log("WXField-" + "owJ:" + owJ.toString());
                    XposedBridge.log("WXField-" + "oxe:" + oxe.toString());
                    XposedBridge.log("WXField-" + "oxf:" + oxf.toString());
                    XposedBridge.log("WXField-" + "oxs:" + oxs.toString());
                    XposedBridge.log("WXField-" + "oxt:" + oxt.toString());
                    XposedBridge.log("WXField-" + "oxu:" + oxu.toString());
                    XposedBridge.log("WXField-" + "userName:" + userName.toString());
                    super.afterHookedMethod(param);
                }
            });
        }catch (Exception e){
            XposedBridge.log("finding ui.i errors: "+ Log.getStackTraceString(e));
        }
    }
}
