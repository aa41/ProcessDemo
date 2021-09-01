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
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;

import com.xiaoma.processmodule.interfaces.IHandleMessage;
import com.xiaoma.processmodule.ipc.ProcessService;

import java.util.List;

public class Client implements IHandleMessage, ServiceConnection, IBinder.DeathRecipient {
    private Messenger messenger;
    private ClientHandler handler;
    private Context context;
    private IProcessInterfaces stub;
    private volatile boolean isBind = false;
    private Bundle bundle;


    private Client() {
        handler = new ClientHandler(this, Looper.getMainLooper());
        messenger = new Messenger(handler);
    }

    public void attachContext(Context context) {
        this.context = context.getApplicationContext();
    }

    public void bindService(Bundle bundle) {
        if (context == null) return;
        if (isBind()) return;
        this.bundle = bundle;
        Intent intent = new Intent(context, ProcessService.class);
        context.bindService(intent, this, Context.BIND_AUTO_CREATE);

    }

    public void sendMessage(Message message) {
        message.replyTo = messenger;
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
        Log.e("1111111111111111", "onServiceConnected---pid:" + Process.myPid());

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
        Log.e("1111111111111111", "binderDied");
        if (stub != null) {
            stub.asBinder().unlinkToDeath(this, 0);
            stub = null;
            isBind = false;
            bindService(bundle);
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
    public void handleMessage(Messenger messenger,Message msg) {
        Log.e("1111111111111111", "client:" + msg.what);
        List<IHandleMessage> messagesCallbacks = ProcessApi.getInstance().getClientMessagesCallbacks();
        for (IHandleMessage iHandleMessage : messagesCallbacks) {
            if (iHandleMessage != null) {
                iHandleMessage.handleMessage(messenger,msg);
            }
        }
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
                client.handleMessage(client.messenger,msg);
            }
        }
    }
}
