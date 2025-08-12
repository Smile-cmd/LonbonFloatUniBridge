package com.lonbon.floatunibridging;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.lb.extend.common.CallbackData;
import com.lb.extend.security.broadcast.AreaDivision;
import com.lb.extend.security.broadcast.IBroadcastService;
import com.lb.extend.security.broadcast.SpeakBroadcastState;
import com.lb.extend.security.card.CardData;
import com.lb.extend.security.card.SwingCardService;
import com.lb.extend.security.education.EducationService;
import com.lb.extend.security.education.EducationTaskStateBean;
import com.lb.extend.security.fingerprint.FingerprintCompareResult;
import com.lb.extend.security.fingerprint.FingerprintFeatureResult;
import com.lb.extend.security.fingerprint.FingerprintLeftNumResult;
import com.lb.extend.security.fingerprint.FingerprintService;
import com.lb.extend.security.intercom.DeviceInfo;
import com.lb.extend.security.intercom.DoorContact;
import com.lb.extend.security.intercom.IntercomService;
import com.lb.extend.security.intercom.LocalDeviceInfo;
import com.lb.extend.security.intercom.MasterDeviceInfo;
import com.lb.extend.security.intercom.TalkEvent;
import com.lb.extend.security.setting.SystemSettingService;
import com.lb.extend.security.sip.ISipServerService;
import com.lb.extend.security.sip.SipEvent;
import com.lb.extend.security.temperature.TemperatureData;
import com.lb.extend.security.temperature.TemperatureMeasurementService;
import com.lb.extend.service.ILonbonService;
import com.zclever.ipc.core.Config;
import com.zclever.ipc.core.IpcManager;
import com.zclever.ipc.core.Result;
import com.zclever.ipc.core.client.FrameType;
import com.zclever.ipc.core.client.IPictureCallBack;
import com.zclever.ipc.core.client.IPreviewCallBack;
import com.zclever.ipc.core.client.PictureFormat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.bridge.UniJSCallback;
import io.dcloud.feature.uniapp.common.UniModule;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

/**
 * *****************************************************************************
 * <p>
 * Copyright (C),2007-2016, LonBon Technologies Co. Ltd. All Rights Reserved.
 * <p>
 * *****************************************************************************
 * 这是来邦厂家提供的uniapp 框架SDK
 * @ProjectName: LBFloatUniDemo
 * @Package: com.lonbon.floatunibridging
 * @ClassName: FloatUniModule
 * @Author： neo
 * @Create: 2022/4/6
 * @Describe:
 */
public class FloatUniModule extends UniModule implements SettingProviderInterface,IntercomProviderInterface,EducationProviderInterface,SipProviderInterface{

    private final String TAG = "FloatUniModule";

    @UniJSMethod(uiThread = true)
    public void initIPCManager(UniJSCallback uniJsCallback) {
        Log.i(TAG, "initIPCManager" + uniJsCallback);
        //监听IPC服务断开
        IpcManager.INSTANCE.setServerDeath(new Function0<Unit>() {
            @Override
            public Unit invoke() {
                Singleton.getSingleton().setConnect(false);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("code",1);
                if (uniJsCallback != null) {
                    Log.i(TAG, "initIPCManager serverDeath uniJsCallback " + uniJsCallback);
                    uniJsCallback.invokeAndKeepAlive(jsonObject);
                } else {
                    Log.i(TAG, "initIPCManager ipcStateUniJSCallback null 3");
                }
                Log.d(TAG, "initIPCManager:serverDeath: 服务链接断开！");
                Log.d(TAG, "initIPCManager:serverDeath: 服务链接断开重连！");
                linkIpc(uniJsCallback);
                return null;
            }
        });
        if (!Singleton.getSingleton().isInitIpc()) {
            Singleton.getSingleton().setInitIpc(true);
            //首先配置开启媒体服务
            IpcManager.INSTANCE.config(Config.Companion.builder().configOpenMedia(true).build());
            //传入上下文
            IpcManager.INSTANCE.init(mUniSDKInstance.getContext());
            //连接IPC服务
            linkIpc(uniJsCallback);
        } else {
            Log.i(TAG, "initIPCManager already Init");
        }
    }

    private void linkIpc(UniJSCallback uniJsCallback) {
        Log.i(TAG, "initIPCManager linkIpc");
        if (!Singleton.getSingleton().isConnect()) {
            Log.i(TAG, "initIPCManager open");
            IpcManager.INSTANCE.open("com.lonbon.lonbon_app", null, new Function1<Boolean, Unit>() {
                @Override
                public Unit invoke(Boolean aBoolean) {
                    if (aBoolean) {
                        //todo:IPC连接成功不代表LonbonApp启动完成，目前暂无启动完成标志，暂时延迟发送重连成功标识
                        Log.i(TAG, "initIPCManager currentThread" + Thread.currentThread());
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(8000);
                                    Singleton.getSingleton().setConnect(true);
                                    mUniSDKInstance.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            JSONObject jsonObject = new JSONObject();
                                            jsonObject.put("code", 0);
                                            if (uniJsCallback != null) {
                                                Log.i(TAG, "initIPCManager open uniJsCallback " + uniJsCallback);
                                                uniJsCallback.invokeAndKeepAlive(jsonObject);
                                            } else {
                                                Log.i(TAG, "initIPCManager ipcStateUniJSCallback null 1");
                                            }
                                            Log.d(TAG, "initIPCManager:linkIpc: 服务链接成功！");
                                        }
                                    });
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                    } else {
                        Singleton.getSingleton().setConnect(false);
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("code", 1);
                        if (uniJsCallback != null) {
                            uniJsCallback.invokeAndKeepAlive(jsonObject);
                        } else {
                            Log.i(TAG, "initIPCManager ipcStateUniJSCallback null 2");
                        }
                        Log.d(TAG, "initIPCManager:linkIpc: 服务链接失败！");
                        Log.d(TAG, "initIPCManager:linkIpc: 服务链接失败重连！");
                        linkIpc(uniJsCallback);
                    }
                    return null;
                }
            });
        }
    }

    //run ui thread
    @UniJSMethod(uiThread = true)
    public void printDeviceInfoTest(UniJSCallback uniJsCallback){
        Log.d(TAG, "printDeviceInfoTest");
        Toast.makeText(mUniSDKInstance.getContext(), TAG, Toast.LENGTH_SHORT).show();
        IpcManager.INSTANCE.getService(IntercomService.class).getCurrentDeviceInfo(new Result<LocalDeviceInfo>() {
            @Override
            public void onData(LocalDeviceInfo localDeviceInfo) {
                Toast.makeText(mUniSDKInstance.getContext(), localDeviceInfo.toString(), Toast.LENGTH_SHORT).show();
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("deviceName",localDeviceInfo.getDeviceName());
                jsonObject.put("deviceModel",localDeviceInfo.getDeviceModel());
                jsonObject.put("customizedModel",localDeviceInfo.getCustomizedModel());
                jsonObject.put("hardwareVersion",localDeviceInfo.getHardwareVersion());
                jsonObject.put("NKVersion",localDeviceInfo.getNKVersion());
                jsonObject.put("modelCode",localDeviceInfo.getModelCode());
                jsonObject.put("platform",localDeviceInfo.getPlatform());
                jsonObject.put("account",localDeviceInfo.getAccount());
                jsonObject.put("password",localDeviceInfo.getPassword());
                jsonObject.put("encPassword",localDeviceInfo.getEncPassword());
                jsonObject.put("sipPort",localDeviceInfo.getSipPort());
                jsonObject.put("sn",localDeviceInfo.getSn());
                jsonObject.put("mac",localDeviceInfo.getMac());
                jsonObject.put("ip",localDeviceInfo.getIp());

                jsonObject.put("gateway",localDeviceInfo.getGateway());
                jsonObject.put("netmask",localDeviceInfo.getNetmask());
                jsonObject.put("isAllowSDRecording",localDeviceInfo.isAllowSDRecording());
                jsonObject.put("manufactoryType",localDeviceInfo.getManufactoryType());
                jsonObject.put("paymentTermCode",localDeviceInfo.getPaymentTermCode());
                jsonObject.put("produceTime",localDeviceInfo.getProduceTime());
                jsonObject.put("displayNum",localDeviceInfo.getDisplayNum());
                jsonObject.put("masterNum",localDeviceInfo.getMasterNum());
                jsonObject.put("slaveNum",localDeviceInfo.getSlaveNum());
                uniJsCallback.invokeAndKeepAlive(jsonObject);
            }
        });
    }

    //run JS thread
    @UniJSMethod(uiThread = true)
    public void printInputTest(String methodName,UniJSCallback uniJsCallback){
        Log.d(TAG, "printInputTest: "+methodName);
        Toast.makeText(mUniSDKInstance.getContext(), methodName, Toast.LENGTH_SHORT).show();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("printInputTest",methodName);
        uniJsCallback.invoke(jsonObject);
    }
    /*********************************************/


    @UniJSMethod(uiThread = true)
    @Override
    public void setTalkViewPosition(int left, int top, int width, int height) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        if (IpcManager.INSTANCE.getService(IntercomService.class) == null){
            Log.d(TAG, "setTalkViewPosition: IpcManager.INSTANCE.getService(IntercomService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(IntercomService.class).setTalkViewPosition(left,top,width,height);
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void extDoorLampCtrl(int color, int open) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "extDoorLampCtrl: ");
        if (IpcManager.INSTANCE.getService(IntercomService.class) == null){
            Log.d(TAG, "extDoorLampCtrl: IpcManager.INSTANCE.getService(IntercomService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(IntercomService.class).extDoorLampCtrl(color,open);
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void onDoorContactValue(UniJSCallback uniJsCallback) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "onDoorContactValue: ");
        if (IpcManager.INSTANCE.getService(IntercomService.class) == null){
            Log.d(TAG, "onDoorContactValue: IpcManager.INSTANCE.getService(IntercomService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(IntercomService.class).onDoorContactValue(new Result<DoorContact>() {
            @Override
            public void onData(DoorContact doorContact) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("doorNum",doorContact.getNum());
                jsonObject.put("isOpen",doorContact.getOpen());
                uniJsCallback.invokeAndKeepAlive(jsonObject);
            }
        });
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void asyncGetDeviceListInfo(int areaId, int masterNum, int slaveNum, int devRegType, UniJSCallback uniJsCallback) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "asyncGetDeviceListInfo: ");
        if (IpcManager.INSTANCE.getService(IntercomService.class) == null){
            Log.d(TAG, "asyncGetDeviceListInfo: IpcManager.INSTANCE.getService(IntercomService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(IntercomService.class).asyncGetDeviceListInfo(areaId,masterNum,slaveNum,devRegType, new Result<ArrayList<DeviceInfo>>() {
            @Override
            public void onData(ArrayList<DeviceInfo> deviceInfos) {
                String gsonString = new Gson().toJson(deviceInfos);
                Log.d(TAG, "onData: "+gsonString);
                uniJsCallback.invokeAndKeepAlive(gsonString);
            }
        });
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void updateDeviceTalkState(UniJSCallback uniJsCallback) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "updateDeviceTalkState: ");
        if (IpcManager.INSTANCE.getService(IntercomService.class) == null){
            Log.d(TAG, "updateDeviceTalkState: IpcManager.INSTANCE.getService(IntercomService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(IntercomService.class).updateDeviceTalkState(new Result<DeviceInfo>() {
            @Override
            public void onData(DeviceInfo deviceInfo) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("areaID",deviceInfo.getAreaID());
                jsonObject.put("masterNum",deviceInfo.getMasterNum());
                jsonObject.put("slaveNum",deviceInfo.getSlaveNum());
                jsonObject.put("childNum",deviceInfo.getChildNum());
                jsonObject.put("devRegType",deviceInfo.getDevRegType());
                jsonObject.put("ip",deviceInfo.getIp());
                jsonObject.put("description",deviceInfo.getDescription());
                jsonObject.put("talkState",deviceInfo.getTalkState());
                jsonObject.put("door1",deviceInfo.getDoorState().size() >= 1);
                jsonObject.put("door2",deviceInfo.getDoorState().size() >= 2);
                uniJsCallback.invokeAndKeepAlive(jsonObject);
            }
        });
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void deviceClick(int areaId, int masterNum, int slaveNum, int devRegType) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "deviceClick: ");
        if (IpcManager.INSTANCE.getService(IntercomService.class) == null){
            Log.d(TAG, "deviceClick: IpcManager.INSTANCE.getService(IntercomService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(IntercomService.class).masterClickItem(masterNum,slaveNum,areaId,devRegType);
    }

    /**
     * 呼叫对讲设备
     * @param areaId 区号ID 最多三位
     * @param masterNum 主机号 最多三位
     * @param slaveNum 分机号 最多三位
     * @param devRegType 设备注册类型 0，主机或这分机，8门口机
     */
    @UniJSMethod(uiThread = true)
    @Override
    public void nativeCall(int areaId , int masterNum ,int slaveNum ,int devRegType){
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "nativeCall: ");
        if (IpcManager.INSTANCE.getService(IntercomService.class) == null){
            Log.d(TAG, "nativeCall: IpcManager.INSTANCE.getService(IntercomService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(IntercomService.class).call(masterNum,slaveNum,areaId,devRegType);

    }

    /**
     * 接听对讲设备
     * @param areaId 区号ID 最多三位
     * @param masterNum 主机号 最多三位
     * @param slaveNum 分机号 最多三位
     * @param devRegType 设备注册类型 0，主机或这分机，8门口机
     */
    @UniJSMethod(uiThread = true)
    @Override
    public void nativeAnswer(int areaId , int masterNum ,int slaveNum ,int devRegType){
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "nativeAnswer: ");
        if (IpcManager.INSTANCE.getService(IntercomService.class) == null){
            Log.d(TAG, "nativeAnswer: IpcManager.INSTANCE.getService(IntercomService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(IntercomService.class).answer(masterNum,slaveNum,areaId,devRegType);

    }

    /**
     * 挂断对讲设备
     * @param areaId 区号ID 最多三位
     * @param masterNum 主机号 最多三位
     * @param slaveNum 分机号 最多三位
     * @param devRegType 设备注册类型 0，主机或这分机，8门口机
     */
    @UniJSMethod(uiThread = true)
    @Override
    public void nativeHangup(int areaId , int masterNum ,int slaveNum ,int devRegType){
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "nativeHangup: ");
        if (IpcManager.INSTANCE.getService(IntercomService.class) == null){
            Log.d(TAG, "nativeHangup: IpcManager.INSTANCE.getService(IntercomService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(IntercomService.class).hangup(masterNum,slaveNum,areaId,devRegType);

    }
    @UniJSMethod(uiThread = true)
    @Override
    public void openLockCtrl(int num, int open) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        if (IpcManager.INSTANCE.getService(IntercomService.class) == null){
            Log.d(TAG, "openLockCtrl: IpcManager.INSTANCE.getService(IntercomService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(IntercomService.class).openLockCtrl(num,open);

    }
    @UniJSMethod(uiThread = true)
    @Override
    public void getCurrentDeviceInfo(UniJSCallback uniJsCallback) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        if (IpcManager.INSTANCE.getService(IntercomService.class) == null){
            Log.d(TAG, "getCurrentDeviceInfo: IpcManager.INSTANCE.getService(IntercomService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(IntercomService.class).getCurrentDeviceInfo(new Result<LocalDeviceInfo>() {
            @Override
            public void onData(LocalDeviceInfo localDeviceInfo) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("deviceName",localDeviceInfo.getDeviceName());
                jsonObject.put("deviceModel",localDeviceInfo.getDeviceModel());
                jsonObject.put("customizedModel",localDeviceInfo.getCustomizedModel());
                jsonObject.put("hardwareVersion",localDeviceInfo.getHardwareVersion());
                jsonObject.put("NKVersion",localDeviceInfo.getNKVersion());
                jsonObject.put("modelCode",localDeviceInfo.getModelCode());
                jsonObject.put("platform",localDeviceInfo.getPlatform());
                jsonObject.put("account",localDeviceInfo.getAccount());
                jsonObject.put("password",localDeviceInfo.getPassword());
                jsonObject.put("encPassword",localDeviceInfo.getEncPassword());
                jsonObject.put("sipPort",localDeviceInfo.getSipPort());
                jsonObject.put("sn",localDeviceInfo.getSn());
                jsonObject.put("mac",localDeviceInfo.getMac());
                jsonObject.put("ip",localDeviceInfo.getIp());

                jsonObject.put("gateway",localDeviceInfo.getGateway());
                jsonObject.put("netmask",localDeviceInfo.getNetmask());
                jsonObject.put("isAllowSDRecording",localDeviceInfo.isAllowSDRecording());
                jsonObject.put("manufactoryType",localDeviceInfo.getManufactoryType());
                jsonObject.put("paymentTermCode",localDeviceInfo.getPaymentTermCode());
                jsonObject.put("produceTime",localDeviceInfo.getProduceTime());
                jsonObject.put("displayNum",localDeviceInfo.getDisplayNum());
                jsonObject.put("masterNum",localDeviceInfo.getMasterNum());
                jsonObject.put("slaveNum",localDeviceInfo.getSlaveNum());
                uniJsCallback.invokeAndKeepAlive(jsonObject);
            }
        });

    }
    @UniJSMethod(uiThread = true)
    @Override
    public void talkEventCallback(UniJSCallback uniJsCallback) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        if (IpcManager.INSTANCE.getService(IntercomService.class) == null){
            Log.d(TAG, "talkEventCallback: IpcManager.INSTANCE.getService(IntercomService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(IntercomService.class).talkEventCallback(new Result<TalkEvent>() {
            @Override
            public void onData(TalkEvent talkEvent) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("eventID",talkEvent.getEventID());

                jsonObject.put("areaID",talkEvent.getDeviceInfo().getAreaID());
                jsonObject.put("masterNum",talkEvent.getDeviceInfo().getMasterNum());
                jsonObject.put("slaveNum",talkEvent.getDeviceInfo().getSlaveNum());
                jsonObject.put("childNum",talkEvent.getDeviceInfo().getChildNum());
                jsonObject.put("devRegType",talkEvent.getDeviceInfo().getDevRegType());
                jsonObject.put("ip",talkEvent.getDeviceInfo().getIp());
                jsonObject.put("description",talkEvent.getDeviceInfo().getDescription());
                jsonObject.put("talkState",talkEvent.getDeviceInfo().getTalkState());
                jsonObject.put("door1",talkEvent.getDeviceInfo().getDoorState().size() >= 1);
                jsonObject.put("door2",talkEvent.getDeviceInfo().getDoorState().size() >= 2);
                uniJsCallback.invokeAndKeepAlive(jsonObject);
            }
        });

    }
    @UniJSMethod(uiThread = true)
    @Override
    public void onDeviceOnLine(UniJSCallback uniJsCallback) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        if (IpcManager.INSTANCE.getService(IntercomService.class) == null){
            Log.d(TAG, "onDeviceOnLine: IpcManager.INSTANCE.getService(IntercomService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(IntercomService.class).onDeviceOnLine(new Result<DeviceInfo>() {
            @Override
            public void onData(DeviceInfo deviceInfo) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("areaID",deviceInfo.getAreaID());
                jsonObject.put("masterNum",deviceInfo.getMasterNum());
                jsonObject.put("slaveNum",deviceInfo.getSlaveNum());
                jsonObject.put("childNum",deviceInfo.getChildNum());
                jsonObject.put("devRegType",deviceInfo.getDevRegType());
                jsonObject.put("ip",deviceInfo.getIp());
                jsonObject.put("description",deviceInfo.getDescription());
                jsonObject.put("talkState",deviceInfo.getTalkState());
                jsonObject.put("door1",deviceInfo.getDoorState().size() >= 1);
                jsonObject.put("door2",deviceInfo.getDoorState().size() >= 2);
                uniJsCallback.invokeAndKeepAlive(jsonObject);
            }
        });
    }
    @UniJSMethod(uiThread = true)
    @Override
    public void onDeviceOffLine(UniJSCallback uniJsCallback) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        if (IpcManager.INSTANCE.getService(IntercomService.class) == null){
            Log.d(TAG, "onDeviceOffLine: IpcManager.INSTANCE.getService(IntercomService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(IntercomService.class).onDeviceOffLine(new Result<DeviceInfo>() {
            @Override
            public void onData(DeviceInfo deviceInfo) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("areaID",deviceInfo.getAreaID());
                jsonObject.put("masterNum",deviceInfo.getMasterNum());
                jsonObject.put("slaveNum",deviceInfo.getSlaveNum());
                jsonObject.put("childNum",deviceInfo.getChildNum());
                jsonObject.put("devRegType",deviceInfo.getDevRegType());
                jsonObject.put("ip",deviceInfo.getIp());
                jsonObject.put("description",deviceInfo.getDescription());
                jsonObject.put("talkState",deviceInfo.getTalkState());
                jsonObject.put("door1",deviceInfo.getDoorState().size() >= 1);
                jsonObject.put("door2",deviceInfo.getDoorState().size() >= 2);
                uniJsCallback.invokeAndKeepAlive(jsonObject);
            }
        });

    }
    @UniJSMethod(uiThread = true)
    @Override
    public void listenToTalk() {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        if (IpcManager.INSTANCE.getService(IntercomService.class) == null){
            Log.d(TAG, "listenToTalk: IpcManager.INSTANCE.getService(IntercomService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(IntercomService.class).listenToTalk();
    }
    @UniJSMethod(uiThread = true)
    @Override
    public void hideTalkView(Boolean hide) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        if (IpcManager.INSTANCE.getService(IntercomService.class) == null){
            Log.d(TAG, "hideTalkView: IpcManager.INSTANCE.getService(IntercomService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(IntercomService.class).hideTalkView(hide);

    }
    @UniJSMethod(uiThread = true)
    @Override
    public void oneKeyCall() {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        if (IpcManager.INSTANCE.getService(IntercomService.class) == null){
            Log.d(TAG, "oneKeyCall: IpcManager.INSTANCE.getService(IntercomService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(IntercomService.class).oneKeyCall();

    }
    @UniJSMethod(uiThread = true)
    @Override
    public void setLocalVideoViewPosition(int left, int top, int width, int height) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        if (IpcManager.INSTANCE.getService(IntercomService.class) == null){
            Log.d(TAG, "setLocalVideoViewPosition: IpcManager.INSTANCE.getService(IntercomService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(IntercomService.class).setPreViewPosition(left,top,width,height);
    }
    @UniJSMethod(uiThread = true)
    @Override
    public void hideLocalPreView(Boolean hide) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        if (IpcManager.INSTANCE.getService(IntercomService.class) == null){
            Log.d(TAG, "hideLocalPreView: IpcManager.INSTANCE.getService(IntercomService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(IntercomService.class).hidePreView(hide);
    }
    @UniJSMethod(uiThread = true)
    @Override
    public void setExtMicEna(Boolean enable) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        if (IpcManager.INSTANCE.getService(IntercomService.class) == null){
            Log.d(TAG, "setExtMicEna: IpcManager.INSTANCE.getService(IntercomService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(IntercomService.class).setMicEna(enable);
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void openLocalCamera(Boolean isOpen) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        if (IpcManager.INSTANCE.getService(IntercomService.class) == null){
            Log.d(TAG, "openLocalCamera: IpcManager.INSTANCE.getService(IntercomService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(IntercomService.class).openLocalCamera(isOpen);
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void initFrame() {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        if (IpcManager.INSTANCE.getService(IntercomService.class) == null){
            Log.d(TAG, "openLocalCamera: IpcManager.INSTANCE.getService(IntercomService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(IntercomService.class).initFrame();
    }

    private int width = 0;
    private int height = 0;
    @UniJSMethod(uiThread = true)
    @Override
    public void setViewWidthHeight(int width, int height) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        if (IpcManager.INSTANCE.getService(IntercomService.class) == null){
            Log.d(TAG, "openLocalCamera: IpcManager.INSTANCE.getService(IntercomService.class) is null !");
            return;
        }
        this.width = width;
        this.height = height;
        IpcManager.INSTANCE.getService(IntercomService.class).setViewWidthHeight(width,height);
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void startTakeFrame() {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        if (IpcManager.INSTANCE.getService(IntercomService.class) == null){
            Log.d(TAG, "startTakeFrame: IpcManager.INSTANCE.getService(IntercomService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(IntercomService.class).startTakeFrame();
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void stopTakeFrame() {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        IpcManager.INSTANCE.getMediaService().stopTakeFrame();
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void takePicture() {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        IpcManager.INSTANCE.getMediaService().takePicture(PictureFormat.JPEG);
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void takeFrame() {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }

        IpcManager.INSTANCE.getMediaService().takeFrame(FrameType.NV21);
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void takePictureCallBack(UniJSCallback uniJsCallback) {
        IpcManager.INSTANCE.getMediaService().setPictureCallBack(new IPictureCallBack() {
            @Override
            public void onPictureTaken(@Nullable byte[] bytes, int i, int i1, @NonNull PictureFormat pictureFormat) {
                Log.i(TAG, "takePictureCallBack: "+Arrays.toString(bytes));
                Log.i(TAG, "takePictureCallBack: "+pictureFormat);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("bytes", Base64.encodeToString(bytes, Base64.DEFAULT));
                jsonObject.put("pictureFormat",pictureFormat);
                uniJsCallback.invokeAndKeepAlive(jsonObject);
            }
        });

    }

    @UniJSMethod(uiThread = true)
    @Override
    public void takeFrameCallBack(UniJSCallback uniJsCallback) {
        IpcManager.INSTANCE.getMediaService().setPreviewCallBack(new IPreviewCallBack() {
            @Override
            public void onPreviewFrame(@Nullable byte[] bytes, int i, int i1, @NonNull FrameType frameType) {
                Log.i(TAG, "takeFrameCallBack: "+ Arrays.toString(bytes));
                Log.i(TAG, "takeFrameCallBack: "+frameType);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("bytes",getBitmapBase64(bytes,width,height));
                jsonObject.put("frameType",frameType);
                uniJsCallback.invokeAndKeepAlive(jsonObject);
            }
        });
    }

    /**
     * 设置通话记录文件存储路径
     * @param path
     * @param uniJsCallback
     */
    @UniJSMethod(uiThread = false)
    @Override
    public void setRecordPath(String path, UniJSCallback uniJsCallback) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        if (IpcManager.INSTANCE.getService(IntercomService.class) == null){
            Log.d(TAG, "setRecordPath: IpcManager.INSTANCE.getService(IntercomService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(IntercomService.class).setRecordPath(path, new Result<String>() {
            @Override
            public void onData(String s) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("url",s);
                uniJsCallback.invoke(jsonObject);
            }
        });
    }

    /**
     * 获取该路径下的文件
     * @param path
     * @param uniJsCallback
     */
    @UniJSMethod(uiThread = false)
    @Override
    public void getFileList(String path, UniJSCallback uniJsCallback) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        if (IpcManager.INSTANCE.getService(IntercomService.class) == null){
            Log.d(TAG, "getFileList: IpcManager.INSTANCE.getService(IntercomService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(IntercomService.class).getFileList(path, new Result<ArrayList<File>>() {
            @Override
            public void onData(ArrayList<File> files) {
                List<JsonFile> jsonList = new ArrayList<>();
                for(File file:files){
                    boolean hasChildFile = file.listFiles() != null && file.listFiles().length > 0;
                    jsonList.add(new JsonFile(
                            file.getPath(),file.getName(),file.length(),file.isDirectory(),hasChildFile
                    ));
                }
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("filesJson",new Gson().toJson(jsonList));
                uniJsCallback.invoke(jsonObject);
            }
        });
    }

    /**
     * 删除文件
     * @param path
     * @param uniJsCallback
     */
    @UniJSMethod(uiThread = false)
    @Override
    public void deleteFile(String path, UniJSCallback uniJsCallback) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        if (IpcManager.INSTANCE.getService(IntercomService.class) == null){
            Log.d(TAG, "deleteFile: IpcManager.INSTANCE.getService(IntercomService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(IntercomService.class).deleteFile(path, new Result<Boolean>() {
            @Override
            public void onData(Boolean isSuccess) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("isSuccess",isSuccess);
                uniJsCallback.invoke(jsonObject);
            }
        });

    }

    /**
     * 主机控制分机通话音量
     * @param volume - 范围 0-5
     */
    @UniJSMethod(uiThread = true)
    @Override
    public void setSlaveVolume(int volume) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "setSlaveVolume: " + volume);
        if (IpcManager.INSTANCE.getService(IntercomService.class) == null){
            Log.d(TAG, "setSlaveVolume: IpcManager.INSTANCE.getService(IntercomService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(IntercomService.class).setSlaveTalkVolume(volume);
    }

    /**
     * 主机获取分机通话音量（同步方法）
     * @return 0：成功，其它值失败
     */
    @UniJSMethod(uiThread = true)
    @Override
    public void syncGetSlaveVolume(UniJSCallback uniJSCallback) {
        Log.d(TAG, "syncGetSlaveVolume: ");
        JSONObject jsonObject = new JSONObject();
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            jsonObject.put("slaveVolume", 3);
        }else {
            if (IpcManager.INSTANCE.getService(IntercomService.class) == null){
                Log.d(TAG, "syncGetSlaveVolume: IpcManager.INSTANCE.getService(IntercomService.class) is null !");
                jsonObject.put("slaveVolume", 3);
                return;
            }
            jsonObject.put("slaveVolume", IpcManager.INSTANCE.getService(IntercomService.class).getSlaveTalkVolume());
        }
        uniJSCallback.invoke(jsonObject);
    }

    /**
     * 图片转换为Base64字符串
     * @param curFaceNV21Data
     * @param width
     * @param height
     * @return
     */
    private String getBitmapBase64(byte[] curFaceNV21Data, int width, int height){
        //encode image to base64 string
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Bitmap bitmap = getBitmapFromYuv(curFaceNV21Data,width,height);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    /**
     * @param curFaceNV21Data
     * @param width
     * @param height
     * @return
     */
    public Bitmap getBitmapFromYuv(byte[] curFaceNV21Data, int width, int height) {

        Bitmap bmp = null;

        try {
            YuvImage image = new YuvImage(curFaceNV21Data, ImageFormat.NV21, width, height, null);
            if (image != null) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                image.compressToJpeg(new Rect(0, 0, width, height), 80, stream);

                bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                //TODO：此处可以对位图进行处理，如显示，保存等

                stream.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return bmp;
    }


    /*********************************************/


    /**
     * 启动刷卡
     */
    @UniJSMethod(uiThread = true)
    @Override
    public void syncStartCard(UniJSCallback uniJSCallback){
        Log.d(TAG, "syncStartCard: ");
        JSONObject jsonObject = new JSONObject();
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            jsonObject.put("code",-1);
        }else {
            if (IpcManager.INSTANCE.getService(SwingCardService.class) == null){
                Log.d(TAG, "syncStartCard: IpcManager.INSTANCE.getService(SwingCardService.class) is null !");
                jsonObject.put("code",-1);
                return;
            }
            jsonObject.put("code",0);
            IpcManager.INSTANCE.getService(SwingCardService.class).start();
        }
        uniJSCallback.invoke(jsonObject);
    }

    /**
     * 关闭刷卡
     */
    @UniJSMethod(uiThread = true)
    @Override
    public void syncStopCard(UniJSCallback uniJSCallback){
        Log.d(TAG, "syncStopCard: ");
        JSONObject jsonObject = new JSONObject();
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            jsonObject.put("code",-1);
        }else {

            if (IpcManager.INSTANCE.getService(SwingCardService.class) == null){
                Log.d(TAG, "syncStopCard: IpcManager.INSTANCE.getService(SwingCardService.class) is null !");
                jsonObject.put("code",-1);
                return;
            }
            jsonObject.put("code",0);
            IpcManager.INSTANCE.getService(SwingCardService.class).stop();
        }
        uniJSCallback.invoke(jsonObject);
    }

    /**
     * 刷卡回调
     * @param uniJSCallback
     */
    @UniJSMethod(uiThread = true)
    @Override
    public void setCardDataCallBack(UniJSCallback uniJSCallback){
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "setCardDataCallBack: ");
        if (IpcManager.INSTANCE.getService(SwingCardService.class) == null){
            Log.d(TAG, "setCardDataCallBack: IpcManager.INSTANCE.getService(SwingCardService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(SwingCardService.class).setCardDataCallBack(new Result<CallbackData<CardData>>() {
            @Override
            public void onData(CallbackData<CardData> cardDataCallbackData) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("code",String.valueOf(cardDataCallbackData.getCode()));
                jsonObject.put("msg",cardDataCallbackData.getMsg());
                jsonObject.put("cardNum",cardDataCallbackData.getData().getCardNum());
                uniJSCallback.invokeAndKeepAlive(jsonObject);
            }
        });
    }
    /**********************************************************************************/

    @UniJSMethod(uiThread = true)
    @Override
    public void syncStartFinger(UniJSCallback uniJSCallback) {
        Log.d(TAG, "syncStartFinger: ");
        JSONObject jsonObject = new JSONObject();
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            jsonObject.put("code",-1);
        }else {
            if (IpcManager.INSTANCE.getService(FingerprintService.class) == null){
                Log.d(TAG, "syncStartFinger: IpcManager.INSTANCE.getService(FingerprintService.class) is null !");
                jsonObject.put("code",-1);
                return;
            }
            jsonObject.put("code",0);
            IpcManager.INSTANCE.getService(FingerprintService.class).init();
        }
        uniJSCallback.invoke(jsonObject);

    }

    @UniJSMethod(uiThread = true)
    @Override
    public void syncStopFinger(UniJSCallback uniJSCallback) {
        Log.d(TAG, "syncStopFinger: ");
        JSONObject jsonObject = new JSONObject();
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            jsonObject.put("code",-1);
        }else {
            if (IpcManager.INSTANCE.getService(FingerprintService.class) == null){
                Log.d(TAG, "syncStopFinger: IpcManager.INSTANCE.getService(FingerprintService.class) is null !");
                jsonObject.put("code",-1);
                return;
            }
            jsonObject.put("code",0);
            IpcManager.INSTANCE.getService(FingerprintService.class).stop();
        }
        uniJSCallback.invoke(jsonObject);
    }
    @UniJSMethod(uiThread = true)
    @Override
    public void fingerModuleStop() {
        Log.d(TAG, "fingerModuleStop: ");
        if (IpcManager.INSTANCE.getService(FingerprintService.class) == null){
            Log.d(TAG, "fingerModuleStop: IpcManager.INSTANCE.getService(FingerprintService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(FingerprintService.class).destroy();
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void fingerprintCollect(String id) {
        Log.d(TAG, "fingerprintCollect: ");
        if (IpcManager.INSTANCE.getService(FingerprintService.class) == null){
            Log.d(TAG, "fingerprintCollect: IpcManager.INSTANCE.getService(FingerprintService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(FingerprintService.class).fingerprintCollect(id);

    }
    @UniJSMethod(uiThread = true)
    @Override
    public void fingerprintRecognition() {
        Log.d(TAG, "fingerprintRecognition: ");
        if (IpcManager.INSTANCE.getService(FingerprintService.class) == null){
            Log.d(TAG, "fingerprintRecognition: IpcManager.INSTANCE.getService(FingerprintService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(FingerprintService.class).fingerprintRecognition();

    }


    @UniJSMethod(uiThread = true)
    @Override
    public void fingerprintFeatureInput(String id, String feature) {
        Log.d(TAG, "fingerprintFeatureInput: ");
        if (IpcManager.INSTANCE.getService(FingerprintService.class) == null){
            Log.d(TAG, "fingerprintFeatureInput: IpcManager.INSTANCE.getService(FingerprintService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(FingerprintService.class).fingerprintFeatureInput(id,feature);
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void setFingerprintFeatureCallBack(UniJSCallback uniJSCallback) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "setGetFingerprintFeatureCallBack: ");
        if (IpcManager.INSTANCE.getService(FingerprintService.class) == null){
            Log.d(TAG, "setFingerprintFeatureCallBack: IpcManager.INSTANCE.getService(FingerprintService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(FingerprintService.class).setFingerprintFeatureCallBack(new Result<CallbackData<FingerprintFeatureResult>>() {
            @Override
            public void onData(CallbackData<FingerprintFeatureResult> callbackData) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("code",String.valueOf(callbackData.getCode()));
                jsonObject.put("msg",callbackData.getMsg());
                jsonObject.put("id", callbackData.getData().getId());
                jsonObject.put("feature",callbackData.getData().getFeature());
                uniJSCallback.invokeAndKeepAlive(jsonObject);
            }
        });
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void setFingerprintFeatureLeftNumCallBack(UniJSCallback uniJSCallback) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "setGetFingerprintFeatureLeftNumCallBack: ");
        if (IpcManager.INSTANCE.getService(FingerprintService.class) == null){
            Log.d(TAG, "setFingerprintFeatureLeftNumCallBack: IpcManager.INSTANCE.getService(FingerprintService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(FingerprintService.class).setFingerprintLeftNumCallBack(new Result<CallbackData<FingerprintLeftNumResult>>() {
            @Override
            public void onData(CallbackData<FingerprintLeftNumResult> callbackData) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("code",String.valueOf(callbackData.getCode()));
                jsonObject.put("msg",callbackData.getMsg());
                jsonObject.put("leftCounts",String.valueOf(callbackData.getData().getLeftCounts()));
                jsonObject.put("fingerprintBase64Str",callbackData.getData().getFingerprintBase64Str());
                uniJSCallback.invokeAndKeepAlive(jsonObject);
            }
        });
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void setCompareFingerprintCallBack(UniJSCallback uniJSCallback) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "setGetCompareFingerprintCallBack: ");
        if (IpcManager.INSTANCE.getService(FingerprintService.class) == null){
            Log.d(TAG, "setCompareFingerprintCallBack: IpcManager.INSTANCE.getService(FingerprintService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(FingerprintService.class).setFingerprintCompareCallBack(new Result<CallbackData<FingerprintCompareResult>>() {
            @Override
            public void onData(CallbackData<FingerprintCompareResult> callbackData) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("code",String.valueOf(callbackData.getCode()));
                jsonObject.put("msg",callbackData.getMsg());
                jsonObject.put("id",callbackData.getData().getId());
                jsonObject.put("feature",callbackData.getData().getFeature());
                jsonObject.put("fingerprintBase64Str",callbackData.getData().getFingerprintBase64Str());
                uniJSCallback.invokeAndKeepAlive(jsonObject);
            }
        });
    }

    /**
     * 根据人员id清除本地指纹存储信息
     * @param id String
     */

    @UniJSMethod(uiThread = true)
    @Override
    public void clearFingerprintById(String id) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "setGetCompareFingerprintCallBack: ");
        if (IpcManager.INSTANCE.getService(FingerprintService.class) == null){
            Log.d(TAG, "setCompareFingerprintCallBack: IpcManager.INSTANCE.getService(FingerprintService.class) is null !");
            return;
        }

        IpcManager.INSTANCE.getService(FingerprintService.class).clearFingerprintById(id);
    }

    /**
     * 根据指纹特征值清除本地指纹存储信息
     * @param feature String
     */
    @UniJSMethod(uiThread = true)
    @Override
    public void clearFingerprintByFeature(String feature) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "setGetCompareFingerprintCallBack: ");
        if (IpcManager.INSTANCE.getService(FingerprintService.class) == null){
            Log.d(TAG, "setCompareFingerprintCallBack: IpcManager.INSTANCE.getService(FingerprintService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(FingerprintService.class).clearFingerprintByFeature(feature);
    }

    /**
     * 清空本地所有指纹存储信息
     */
    @UniJSMethod(uiThread = true)
    @Override
    public void clearAllFingerprint() {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "setGetCompareFingerprintCallBack: ");
        if (IpcManager.INSTANCE.getService(FingerprintService.class) == null){
            Log.d(TAG, "setCompareFingerprintCallBack: IpcManager.INSTANCE.getService(FingerprintService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(FingerprintService.class).clearAllFingerprint();
    }
    /**********************************************************************************/


    @UniJSMethod(uiThread = true)
    @Override
    public void syncStartTemperature(UniJSCallback uniJSCallback) {
        Log.d(TAG, "syncStartTemperature: ");
        JSONObject jsonObject = new JSONObject();
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            jsonObject.put("code",-1);
        }else {

            if (IpcManager.INSTANCE.getService(TemperatureMeasurementService.class) == null){
                Log.d(TAG, "syncStartTemperature: IpcManager.INSTANCE.getService(TemperatureMeasurementService.class) is null !");
                jsonObject.put("code",-1);
                return;
            }
            jsonObject.put("code",0);
            IpcManager.INSTANCE.getService(TemperatureMeasurementService.class).start();
        }
        uniJSCallback.invoke(jsonObject);

    }

    @UniJSMethod(uiThread = true)
    @Override
    public void syncStopTemperature(UniJSCallback uniJSCallback) {
        Log.d(TAG, "syncStopTemperature: ");
        JSONObject jsonObject = new JSONObject();
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            jsonObject.put("code",-1);
        }else {

            if (IpcManager.INSTANCE.getService(TemperatureMeasurementService.class) == null){
                Log.d(TAG, "syncStopTemperature: IpcManager.INSTANCE.getService(TemperatureMeasurementService.class) is null !");
                jsonObject.put("code",-1);
                return;
            }
            jsonObject.put("code",0);
            IpcManager.INSTANCE.getService(TemperatureMeasurementService.class).stop();
        }
        uniJSCallback.invoke(jsonObject);

    }

    @UniJSMethod(uiThread = true)
    @Override
    public void setTemperatureDataCallBack(UniJSCallback uniJSCallback) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "setTemperatureDataCallBack: ");
        if (IpcManager.INSTANCE.getService(TemperatureMeasurementService.class) == null){
            Log.d(TAG, "setTemperatureDataCallBack: IpcManager.INSTANCE.getService(TemperatureMeasurementService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(TemperatureMeasurementService.class).setTemperatureDataCallBack(new Result<CallbackData<TemperatureData>>() {
            @Override
            public void onData(CallbackData<TemperatureData> callbackData) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("code",String.valueOf(callbackData.getCode()));
                jsonObject.put("msg",callbackData.getMsg());
                jsonObject.put("temperature",String.valueOf(callbackData.getData().getTemperature()));
                Log.d(TAG, "setTemperatureDataCallBack: "+ jsonObject);
                uniJSCallback.invokeAndKeepAlive(jsonObject);
            }
        });
    }
    /**********************************************************************************/

    @UniJSMethod(uiThread = true)
    @Override
    public void setSystemTime(long time) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "setSystemTime: "+time);
        if (IpcManager.INSTANCE.getService(SystemSettingService.class) == null){
            Log.d(TAG, "setSystemTime: IpcManager.INSTANCE.getService(SystemSettingService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(SystemSettingService.class).setSystemTime(time);
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void setStreamVolumeTypeMusic(int value) {
        AudioManagerHelper audioManagerHelper = new AudioManagerHelper(mUniSDKInstance.getContext());
        audioManagerHelper.setAudioType(AudioManagerHelper.TYPE_MUSIC);
        audioManagerHelper.setVoice100(value);
        Log.d(TAG, "setValue: "+value);
    }
    @UniJSMethod(uiThread = true)
    @Override
    public void getStreamVolumeTypeMusic(UniJSCallback uniJSCallback) {
        AudioManagerHelper audioManagerHelper = new AudioManagerHelper(mUniSDKInstance.getContext());
        audioManagerHelper.setAudioType(AudioManagerHelper.TYPE_MUSIC);
        int value = audioManagerHelper.get100CurrentVolume();
        Log.d(TAG, "value: "+value);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("value",value);
        uniJSCallback.invoke(jsonObject);

    }

    @UniJSMethod(uiThread = true)
    @Override
    public void setStreamVolumeTypeAlarm(int value) {
        AudioManagerHelper audioManagerHelper = new AudioManagerHelper(mUniSDKInstance.getContext());
        audioManagerHelper.setAudioType(AudioManagerHelper.TYPE_ALARM);
        audioManagerHelper.setVoice100(value);
        Log.d(TAG, "setValue: "+value);

    }
    @UniJSMethod(uiThread = true)
    @Override
    public void getStreamVolumeTypeAlarm(UniJSCallback uniJSCallback) {
        AudioManagerHelper audioManagerHelper = new AudioManagerHelper(mUniSDKInstance.getContext());
        audioManagerHelper.setAudioType(AudioManagerHelper.TYPE_ALARM);
        int value = audioManagerHelper.get100CurrentVolume();
        Log.d(TAG, "value: "+value);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("value",value);
        uniJSCallback.invoke(jsonObject);
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void setStreamVolumeTypeRing(int value) {
        AudioManagerHelper audioManagerHelper = new AudioManagerHelper(mUniSDKInstance.getContext());
        audioManagerHelper.setAudioType(AudioManagerHelper.TYPE_RING);
        audioManagerHelper.setVoice100(value);
        Log.d(TAG, "setValue: "+value);
    }
    @UniJSMethod(uiThread = true)
    @Override
    public void getStreamVolumeTypeRing(UniJSCallback uniJSCallback) {
        AudioManagerHelper audioManagerHelper = new AudioManagerHelper(mUniSDKInstance.getContext());
        audioManagerHelper.setAudioType(AudioManagerHelper.TYPE_RING);
        int value = audioManagerHelper.get100CurrentVolume();
        Log.d(TAG, "value: "+value);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("value",value);
        uniJSCallback.invoke(jsonObject);
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void setStreamVolumeTypeSystem(int value) {
        AudioManagerHelper audioManagerHelper = new AudioManagerHelper(mUniSDKInstance.getContext());
        audioManagerHelper.setAudioType(AudioManagerHelper.TYPE_SYSTEM);
        audioManagerHelper.setVoice100(value);
        Log.d(TAG, "setValue: "+value);
    }
    @UniJSMethod(uiThread = true)
    @Override
    public void getStreamVolumeTypeSystem(UniJSCallback uniJSCallback) {
        AudioManagerHelper audioManagerHelper = new AudioManagerHelper(mUniSDKInstance.getContext());
        audioManagerHelper.setAudioType(AudioManagerHelper.TYPE_SYSTEM);
        int value = audioManagerHelper.get100CurrentVolume();
        Log.d(TAG, "value: "+value);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("value",value);
        uniJSCallback.invoke(jsonObject);
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void setStreamVolumeTypeVoiceCall(int value) {
        AudioManagerHelper audioManagerHelper = new AudioManagerHelper(mUniSDKInstance.getContext());
        audioManagerHelper.setAudioType(AudioManagerHelper.TYPE_VOICE_CALL);
        audioManagerHelper.setVoice100(value);
        Log.d(TAG, "setValue: "+value);
    }
    @UniJSMethod(uiThread = true)
    @Override
    public void getStreamVolumeTypeVoiceCall(UniJSCallback uniJSCallback) {
        AudioManagerHelper audioManagerHelper = new AudioManagerHelper(mUniSDKInstance.getContext());
        audioManagerHelper.setAudioType(AudioManagerHelper.TYPE_VOICE_CALL);
        int value = audioManagerHelper.get100CurrentVolume();
        Log.d(TAG, "value: "+value);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("value",value);
        uniJSCallback.invoke(jsonObject);
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void setStreamMute(int volumeType, boolean isMute){
        AudioManagerHelper audioManagerHelper = new AudioManagerHelper(mUniSDKInstance.getContext());
        audioManagerHelper.setStreamMute(volumeType,isMute);
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void rebootSystem() {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "rebootSystem: ");
        if (IpcManager.INSTANCE.getService(SystemSettingService.class) == null){
            Log.d(TAG, "rebootSystem: IpcManager.INSTANCE.getService(SystemSettingService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(SystemSettingService.class).rebootSystem();
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void openGuard(int isOpen) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "openGuard: isOpen："+isOpen);
        IpcManager.INSTANCE.getService(ILonbonService.class).openGuard(isOpen == 1);

    }

    /***********************************电教相关***********************************************/

    @UniJSMethod(uiThread = true)
    @Override
    public void initEducation() {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "initEducation: ");
        if (IpcManager.INSTANCE.getService(EducationService.class) == null){
            Log.d(TAG, "initEducation: IpcManager.INSTANCE.getService(EducationService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(EducationService.class).init();
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void enterLiveShow() {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "enterLiveShow: ");
        if (IpcManager.INSTANCE.getService(EducationService.class) == null){
            Log.d(TAG, "enterLiveShow: IpcManager.INSTANCE.getService(EducationService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(EducationService.class).showEducation();
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void exitLiveShow() {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "exitLiveShow: ");
        if (IpcManager.INSTANCE.getService(EducationService.class) == null){
            Log.d(TAG, "exitLiveShow: IpcManager.INSTANCE.getService(EducationService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(EducationService.class).exitEducation();
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void syncGetEducationTaskList(UniJSCallback uniJSCallback) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "syncGetEducationTaskList: ");
        if (IpcManager.INSTANCE.getService(EducationService.class) == null){
            Log.d(TAG, "syncGetEducationTaskList: IpcManager.INSTANCE.getService(EducationService.class) is null !");
            return;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("educationTaskList", new Gson().toJson(IpcManager.INSTANCE.getService(EducationService.class).getEducationTask()));
        uniJSCallback.invoke(jsonObject);
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void enterEducationTask() {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "enterEducationTask: ");
        if (IpcManager.INSTANCE.getService(EducationService.class) == null){
            Log.d(TAG, "enterEducationTask: IpcManager.INSTANCE.getService(EducationService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(EducationService.class).showEducationTask();
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void exitEducationTask() {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "exitEducationTask: ");
        if (IpcManager.INSTANCE.getService(EducationService.class) == null){
            Log.d(TAG, "exitEducationTask: IpcManager.INSTANCE.getService(EducationService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(EducationService.class).exitEducationTask();
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void setEducationStateListener(UniJSCallback uniJSCallback) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "setEducationStateListener: ");
        if (IpcManager.INSTANCE.getService(EducationService.class) == null){
            Log.d(TAG, "setEducationStateListener: IpcManager.INSTANCE.getService(EducationService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(EducationService.class).setEducationTaskStateListener(new Result<EducationTaskStateBean>() {
            @Override
            public void onData(EducationTaskStateBean educationTaskStateBean) {
                Log.d(TAG, "educationTaskStateBean: " + educationTaskStateBean);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("educationTaskStateBean", new Gson().toJson(educationTaskStateBean));
                uniJSCallback.invokeAndKeepAlive(jsonObject);
            }
        });
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void hdmiOpen(int outputConfigure) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "hdmiOpen: " + outputConfigure);
        if (IpcManager.INSTANCE.getService(EducationService.class) == null){
            Log.d(TAG, "hdmiOpen: IpcManager.INSTANCE.getService(EducationService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(EducationService.class).setHDMIConfigure(outputConfigure);
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void audioSyncOutput(int enable) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "audioSyncOutput: " + enable);
        if (IpcManager.INSTANCE.getService(EducationService.class) == null){
            Log.d(TAG, "audioSyncOutput: IpcManager.INSTANCE.getService(EducationService.class) is null  !");
            return;
        }
        IpcManager.INSTANCE.getService(EducationService.class).setAudioSyncOutput(enable);
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void setHdmiStatusListener(UniJSCallback uniJSCallback) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "setHdmiStatusListener: ");
        if (IpcManager.INSTANCE.getService(EducationService.class) == null){
            Log.d(TAG, "setHdmiStatusListener: IpcManager.INSTANCE.getService(EducationService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(EducationService.class).setHdmiStatusListener(new Result<Boolean>() {
            @Override
            public void onData(Boolean aBoolean) {
                Log.d(TAG, "hdmiStatusListener: " + aBoolean);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("hdmiStatus", aBoolean);
                uniJSCallback.invokeAndKeepAlive(jsonObject);
            }
        });
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void syncGetHdmiStatus(UniJSCallback uniJSCallback) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "syncGetHdmiStatus: ");
        if (IpcManager.INSTANCE.getService(EducationService.class) == null){
            Log.d(TAG, "syncGetHdmiStatus: IpcManager.INSTANCE.getService(EducationService.class) is null !");
            return;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("hdmiStatus", IpcManager.INSTANCE.getService(EducationService.class).getHdmiStatus());
        uniJSCallback.invoke(jsonObject);
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void hornControlSwitch(boolean isOpen) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "hornControlSwitch: " + isOpen);
        if (IpcManager.INSTANCE.getService(EducationService.class) == null){
            Log.d(TAG, "hornControlSwitch: IpcManager.INSTANCE.getService(EducationService.class) is null  !");
            return;
        }
        IpcManager.INSTANCE.getService(EducationService.class).setHornControlSwitch(isOpen);
    }

    /***********************************广播相关***********************************************/

    @UniJSMethod(uiThread = true)
    @Override
    public void initBroadcast() {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "initBroadcast: ");
        if (IpcManager.INSTANCE.getService(IBroadcastService.class) == null){
            Log.d(TAG, "initBroadcast: IpcManager.INSTANCE.getService(IBroadcastService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(IBroadcastService.class).initSpeakBroadcast();
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void setOnIONotifyListener(UniJSCallback uniJSCallback) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "setOnIONotifyListener: ");
        if (IpcManager.INSTANCE.getService(IBroadcastService.class) == null){
            Log.d(TAG, "setOnIONotifyListener: IpcManager.INSTANCE.getService(IBroadcastService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(IBroadcastService.class).onIONotifyListener(new Result<Integer>() {
            @Override
            public void onData(Integer data) {
                Log.d(TAG, "onData: " + data);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("ioState", data);
                uniJSCallback.invokeAndKeepAlive(jsonObject);
            }
        });
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void setOnSpeakBroadcastListener(UniJSCallback uniJSCallback) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "setOnSpeakBroadcastListener: ");
        if (IpcManager.INSTANCE.getService(IBroadcastService.class) == null){
            Log.d(TAG, "setOnSpeakBroadcastListener: IpcManager.INSTANCE.getService(IBroadcastService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(IBroadcastService.class).onSpeakBroadcastListener(new Result<SpeakBroadcastState>() {
            @Override
            public void onData(SpeakBroadcastState data) {
                Log.d(TAG, "onData: " + data);
                int event = data.getEvent();
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("event", event);
                uniJSCallback.invokeAndKeepAlive(jsonObject);
            }
        });
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void setOnToastListener(UniJSCallback uniJSCallback) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "setOnToastListener: ");
        if (IpcManager.INSTANCE.getService(IBroadcastService.class) == null){
            Log.d(TAG, "setOnToastListener: IpcManager.INSTANCE.getService(IBroadcastService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(IBroadcastService.class).onToastListener(new Result<String>() {
            @Override
            public void onData(String s) {
                Log.d(TAG, "onToastListener" + s);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("toast", s);
                uniJSCallback.invokeAndKeepAlive(jsonObject);
            }
        });
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void addBroadcastObj(int num, UniJSCallback uniJSCallback) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "addBroadcastObj: ");
        if (IpcManager.INSTANCE.getService(IBroadcastService.class) == null){
            Log.d(TAG, "addBroadcastObj: IpcManager.INSTANCE.getService(IBroadcastService.class) is null !");
            return;
        }

        if (num < 1000){
            Log.d(TAG, "addBroadcastObj: num must bigger than 1000 !");
            return;
        }
        AreaDivision areaDivision = new AreaDivision();
        areaDivision.setDisplayNum(num);
        areaDivision.setMasterNum(num / 1000);
        if (!Singleton.getSingleton().getAreaDivisionArrayList().contains(areaDivision)) {
            Singleton.getSingleton().getAreaDivisionArrayList().add(areaDivision);
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("broadcastDevices", new Gson().toJson(Singleton.getSingleton().getAreaDivisionArrayList()));
        uniJSCallback.invoke(jsonObject);
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void clearBroadcastObj() {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "addBroadcastObj: ");
        if (IpcManager.INSTANCE.getService(IBroadcastService.class) == null){
            Log.d(TAG, "addBroadcastObj: IpcManager.INSTANCE.getService(IBroadcastService.class) is null !");
            return;
        }
        Singleton.getSingleton().getAreaDivisionArrayList().clear();
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void setSpeakBroadcastDevice() {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "setSpeakBroadcastDevice: ");
        if (IpcManager.INSTANCE.getService(IBroadcastService.class) == null){
            Log.d(TAG, "setSpeakBroadcastDevice: IpcManager.INSTANCE.getService(IBroadcastService.class) is null !");
            return;
        }
        for (int i = 0; i < Singleton.getSingleton().getAreaDivisionArrayList().size(); i++) {
            Log.d(TAG, "setSpeakBroadcastDevice: " + Singleton.getSingleton().getAreaDivisionArrayList().get(i));
        }
        IpcManager.INSTANCE.getService(IBroadcastService.class).setSpeakBroadcastDevice(Singleton.getSingleton().getAreaDivisionArrayList());
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void startSpeakBroadcast(int data) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "startSpeakBroadcast: ");
        if (IpcManager.INSTANCE.getService(IBroadcastService.class) == null){
            Log.d(TAG, "startSpeakBroadcast: IpcManager.INSTANCE.getService(IBroadcastService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(IBroadcastService.class).startSpeakBroadcast(data);
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void stopSpeakBroadcast(int data) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "stopSpeakBroadcast: ");
        if (IpcManager.INSTANCE.getService(IBroadcastService.class) == null){
            Log.d(TAG, "stopSpeakBroadcast: IpcManager.INSTANCE.getService(IBroadcastService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(IBroadcastService.class).stopSpeakBroadcast(data);
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void getMasterDeviceListInfo(UniJSCallback uniJsCallback) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "getMasterDeviceListInfo: ");
        if (IpcManager.INSTANCE.getService(IntercomService.class) == null){
            Log.d(TAG, "getMasterDeviceListInfo: IpcManager.INSTANCE.getService(IntercomService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(IntercomService.class).getSubMasterList(new Result<ArrayList<MasterDeviceInfo>>() {
            @Override
            public void onData(ArrayList<MasterDeviceInfo> masterDeviceInfos) {
                String gsonString = new Gson().toJson(masterDeviceInfos);
                Log.d(TAG, "getMasterDeviceListInfo onData: "+gsonString);
                uniJsCallback.invoke(gsonString);
            }
        });
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void getSipUsername(UniJSCallback uniJSCallback) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "getSipUsername");
        if (IpcManager.INSTANCE.getService(ISipServerService.class) == null){
            Log.d(TAG, "getSipUsername: IpcManager.INSTANCE.getService(ISipServerService.class) is null !");
            return;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sipUsername", IpcManager.INSTANCE.getService(ISipServerService.class).getSipUsername());
        uniJSCallback.invoke(jsonObject);
    }
    @UniJSMethod(uiThread = true)
    @Override
    public void getSipDisplayName(UniJSCallback uniJSCallback) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "getSipDisplayName");
        if (IpcManager.INSTANCE.getService(ISipServerService.class) == null){
            Log.d(TAG, "getSipDisplayName: IpcManager.INSTANCE.getService(ISipServerService.class) is null !");
            return;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sipDisplayName", IpcManager.INSTANCE.getService(ISipServerService.class).getSipDisplayName());
        uniJSCallback.invoke(jsonObject);
    }
    @UniJSMethod(uiThread = true)
    @Override
    public void isSipRegisterState(UniJSCallback uniJSCallback){
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "isSipRegisterState");
        if (IpcManager.INSTANCE.getService(ISipServerService.class) == null){
            Log.d(TAG, "getSipDisplayName: IpcManager.INSTANCE.getService(ISipServerService.class) is null !");
            return;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sipRegisterState", IpcManager.INSTANCE.getService(ISipServerService.class).isSipRegisterState());
        uniJSCallback.invoke(jsonObject);
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void onSipEvent(UniJSCallback uniJSCallback) {
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "onSipEvent");
        if (IpcManager.INSTANCE.getService(ISipServerService.class) == null){
            Log.d(TAG, "onSipEvent: IpcManager.INSTANCE.getService(ISipServerService.class) is null !");
            return;
        }
        IpcManager.INSTANCE.getService(ISipServerService.class).onSipEvent(new Result<SipEvent>() {
            @Override
            public void onData(SipEvent sipEvent) {
                Log.d(TAG, "SipEvent: " + sipEvent);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("sipEvent", new Gson().toJson(sipEvent));
                uniJSCallback.invokeAndKeepAlive(jsonObject);
            }
        });
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void sipCall(String sipNum, int dataType, UniJSCallback uniJSCallback){
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "sipCall: ");
        if (IpcManager.INSTANCE.getService(ISipServerService.class) == null){
            Log.d(TAG, "sipCall: IpcManager.INSTANCE.getService(ISipServerService.class) is null !");
            return;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("tip", IpcManager.INSTANCE.getService(ISipServerService.class).sipCall(sipNum,dataType));
        uniJSCallback.invoke(jsonObject);
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void sipAnswer(String sipNum, int dataType, UniJSCallback uniJSCallback){
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "sipAnswer: ");
        if (IpcManager.INSTANCE.getService(ISipServerService.class) == null){
            Log.d(TAG, "sipAnswer: IpcManager.INSTANCE.getService(ISipServerService.class) is null !");
            return;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("tip", IpcManager.INSTANCE.getService(ISipServerService.class).sipAnswer(sipNum,dataType));
        uniJSCallback.invoke(jsonObject);
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void sipHangup(String sipNum, int dataType, UniJSCallback uniJSCallback){
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "sipHangup: ");
        if (IpcManager.INSTANCE.getService(ISipServerService.class) == null){
            Log.d(TAG, "sipHangup: IpcManager.INSTANCE.getService(ISipServerService.class) is null !");
            return;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("tip", IpcManager.INSTANCE.getService(ISipServerService.class).sipHangup(sipNum,dataType));
        uniJSCallback.invoke(jsonObject);
    }

    @UniJSMethod(uiThread = true)
    @Override
    public void sipAudioCall(String sipNum, int dataType, UniJSCallback uniJSCallback){
        if (!Singleton.getSingleton().isConnect()){
            showToast();
            return ;
        }
        Log.d(TAG, "sipAudioCall: ");
        if (IpcManager.INSTANCE.getService(ISipServerService.class) == null){
            Log.d(TAG, "sipAudioCall: IpcManager.INSTANCE.getService(ISipServerService.class) is null !");
            return;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("tip", IpcManager.INSTANCE.getService(ISipServerService.class).sipAudioCall(sipNum,dataType));
        uniJSCallback.invoke(jsonObject);
    }
    /**********************************************************************************/

    private void showToast(){
        Toast.makeText(mUniSDKInstance.getContext(), "连接服务中，请稍后！", Toast.LENGTH_LONG).show();
    }
    /**********************************************************************************/
}