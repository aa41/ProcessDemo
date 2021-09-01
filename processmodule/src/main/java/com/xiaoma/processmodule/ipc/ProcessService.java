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
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ProcessService extends Service {

    private WeakHashMap<Messenger, Long> messengerCache = new WeakHashMap<>();


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return stub.asBinder();
    }


    IProcessInterfaces stub = new IProcessInterfaces.Stub() {
        @Override
        public void handleMessage(Message message) throws RemoteException {
            final Messenger messenger = message.replyTo;
            final Bundle data = message.getData();
            final long pid = data.getLong(Const.PID_KEY);
            final boolean mainProcess = data.getBoolean(Const.MAIN_PROCESS, false);
            if (messengerCache.get(messenger) == null) {
                messengerCache.put(messenger, pid);
                messenger.getBinder().linkToDeath(new DeathRecipient() {
                    @Override
                    public void binderDied() {
                        messenger.getBinder().unlinkToDeath(this, 0);
                        messengerCache.remove(messenger);
                        if (mainProcess) {
                            System.exit(0);
                        }
                    }
                }, 0);
            }

            switch (message.what) {
                case Const.INIT_CODE:
                    IServiceInit serviceInit = ProcessApi.getInstance().getServiceInit();
                    if (serviceInit != null) {
                        serviceInit.init(data);
                    }
                    break;
                default:
                    List<IHandleMessage> messagesCallbacks = ProcessApi.getInstance().getServiceMessagesCallbacks();
                    for (IHandleMessage iHandleMessage : messagesCallbacks) {
                        if (iHandleMessage != null) {
                            final Message msg = iHandleMessage.handleMessage(message);
                            if(messenger != null){
                                messenger.send(msg);
                            }
                        }
                    }
            }

//            if (clientMessenger != null) {
//                clientMessenger.send(Message.obtain(message));
//            }
        }
    };

}
