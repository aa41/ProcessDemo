package com.xiaoma.processmodule.interfaces;

import android.os.Message;
import android.os.Messenger;

public interface IHandleMessage {
    Message handleMessage(Message msg);
}
