package com.xiaoma.processmodule.ipc;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import androidx.annotation.Nullable;

import com.xiaoma.processmodule.Const;
import com.xiaoma.processmodule.IProcessInterfaces;
import com.xiaoma.processmodule.ProcessApi;
import com.xiaoma.processmodule.interfaces.IHandleMessage;
import com.xiaoma.processmodule.interfaces.IServiceInit;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ProcessService extends Service implements IBinder.DeathRecipient {
    private Messenger clientMessenger;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return stub.asBinder();
    }


    IProcessInterfaces stub = new IProcessInterfaces.Stub() {
        @Override
        public void handleMessage(Message message) throws RemoteException {
            if (clientMessenger == null) {
                clientMessenger = message.replyTo;
                clientMessenger.getBinder().linkToDeath(ProcessService.this, 0);
            }
            switch (message.what) {
                case Const.INIT_CODE:
                    Bundle data = message.getData();
                    IServiceInit serviceInit = ProcessApi.getInstance().getServiceInit();
                    if (serviceInit != null) {
                        serviceInit.init(data);
                    }
                    break;
                default:
                    List<IHandleMessage> messagesCallbacks = ProcessApi.getInstance().getServiceMessagesCallbacks();
                    for (IHandleMessage iHandleMessage : messagesCallbacks) {
                        if (iHandleMessage != null) {
                            iHandleMessage.handleMessage(clientMessenger,message);
                        }
                    }
            }

//            if (clientMessenger != null) {
//                clientMessenger.send(Message.obtain(message));
//            }
        }
    };

    @Override
    public void binderDied() {
        clientMessenger.getBinder().unlinkToDeath(this, 0);
        System.exit(0);
    }
}
