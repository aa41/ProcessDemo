package com.xiaoma.processmodule;

import android.content.Context;
import android.os.Bundle;

import com.xiaoma.processmodule.interfaces.IHandleMessage;
import com.xiaoma.processmodule.interfaces.IServiceInit;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ProcessApi {

    private IServiceInit iServiceInit;
    private List<IHandleMessage> ServiceMessagesCallbacks = new CopyOnWriteArrayList<>();
    private List<IHandleMessage> clientMessagesCallbacks = new CopyOnWriteArrayList<>();



    public static ProcessApi getInstance() {
        return INNER.INSTANCE;
    }

    private static class INNER {
        private static final ProcessApi INSTANCE = new ProcessApi();
    }


    public void clientInit(Context context, Bundle bundle) {
        Client.getInstance().attachContext(context);
        Client.getInstance().bindService(bundle);
    }

    public void clientInit(Context context) {
        clientInit(context, null);
    }

    public void addServiceMessageCallback(IHandleMessage iHandleMessage) {
        if (!ServiceMessagesCallbacks.contains(iHandleMessage)) {
            ServiceMessagesCallbacks.add(iHandleMessage);
        }
    }

    public void removeServiceMessageCallback(IHandleMessage iHandleMessage) {
        ServiceMessagesCallbacks.remove(iHandleMessage);
    }

    public void addClientMessageCallback(IHandleMessage iHandleMessage) {
        if (!clientMessagesCallbacks.contains(iHandleMessage)) {
            clientMessagesCallbacks.add(iHandleMessage);
        }
    }

    public void removeClientMessageCallback(IHandleMessage iHandleMessage) {
        clientMessagesCallbacks.remove(iHandleMessage);
    }

    public void serviceInit(IServiceInit iServiceInit){
        this.iServiceInit =iServiceInit;
    }

    public IServiceInit getServiceInit(){
        return iServiceInit;
    }


    public List<IHandleMessage> getServiceMessagesCallbacks(){
        return ServiceMessagesCallbacks;
    }

    public List<IHandleMessage> getClientMessagesCallbacks(){
        return clientMessagesCallbacks;
    }





}
