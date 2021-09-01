package com.xiaoma.processmodule.interfaces;

import android.os.Message;
import android.os.Messenger;

public interface IHandleMessage {
    void handleMessage(Messenger messenger,Message msg);
}
