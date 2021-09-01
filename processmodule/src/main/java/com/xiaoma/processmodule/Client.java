package com.xiaoma.processmodule;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import androidx.annotation.NonNull;

import com.xiaoma.processmodule.interfaces.IHandleMessage;
import com.xiaoma.processmodule.ipc.ProcessService;
import com.xiaoma.processmodule.utils.ProcessUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Client implements IHandleMessage, ServiceConnection, IBinder.DeathRecipient {
    private Messenger messenger;
    private ClientHandler handler;
    private Context context;
    private IProcessInterfaces stub;
    private volatile boolean isBind = false;
    private Bundle bundle;
    private volatile long version = 0;
    private volatile Integer pid;
    private Boolean isMainProcess;

    private Map<Long, IHandleMessage> callbackMap = new ConcurrentHashMap<>();

    private Client() {
        handler = new ClientHandler(this, Looper.getMainLooper());
        messenger = new Messenger(handler);
    }


    public void bindService(Context context, Bundle bundle) {
        if (context == null) return;
        if (isBind()) return;
        this.context = context.getApplicationContext();
        this.bundle = bundle;
        Intent intent = new Intent(context, ProcessService.class);
        context.bindService(intent, this, Context.BIND_AUTO_CREATE);

    }

    public void sendMessage(Message message) {
        sendMessage(message, null);
    }

    public synchronized void sendMessage(Message message, IHandleMessage callback) {
        message.replyTo = messenger;
        Bundle data = message.getData();
        if (data == null) {
            data = new Bundle();
        }
        final long version = getVersion();
        data.putLong(Const.VERSION, version);
        if (pid == null || pid <= 0) {
            pid = android.os.Process.myPid();
        }
        if (isMainProcess == null) {
            isMainProcess = ProcessUtils.isMainProcess(context);
        }
        data.putInt(Const.PID_KEY, pid);
        data.putBoolean(Const.MAIN_PROCESS, isMainProcess);
        message.setData(data);

        if (callback != null) {
            callbackMap.put(version, callback);
        }

        if (stub != null) {
            try {
                stub.handleMessage(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isBind() {
        return stub != null && isBind;
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        stub = IProcessInterfaces.Stub.asInterface(iBinder);
        isBind = true;
        try {
            stub.asBinder().linkToDeath(Client.this, 0);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        Message msg = Message.obtain();
        msg.what = Const.INIT_CODE;
        msg.setData(bundle);
        sendMessage(msg);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        stub = null;
        isBind = false;
    }

    @Override
    public void binderDied() {
        if (stub != null) {
            stub.asBinder().unlinkToDeath(this, 0);
            stub = null;
            isBind = false;
            bindService(context, bundle);
        }
    }

    private static class INNER {
        private static final Client INSTANCE = new Client();
    }

    public static Client getInstance() {
        return INNER.INSTANCE;
    }

    public Messenger getMessenger() {
        return messenger;
    }

    public Handler getHandler() {
        return handler;
    }


    @Override
    public Message handleMessage(Message msg) {
        List<IHandleMessage> messagesCallbacks = ProcessApi.getInstance().getClientMessagesCallbacks();
        if (msg != null && msg.getData() != null) {
            final long version = msg.getData().getLong(Const.VERSION);
            final IHandleMessage callback = callbackMap.get(version);
            if (callback != null) {
                callback.handleMessage(msg);
            }
        }
        for (IHandleMessage iHandleMessage : messagesCallbacks) {
            if (iHandleMessage != null) {
                iHandleMessage.handleMessage(msg);
            }
        }
        return null;
    }

    public synchronized long getVersion() {
        version++;
        return version;
    }

    private static class ClientHandler extends Handler {
        private final Client client;

        private ClientHandler(Client client, Looper looper) {
            super(looper);
            this.client = client;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (client != null) {
                client.handleMessage(msg);
            }
        }
    }
}
