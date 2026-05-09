package com.htgd.radiocontrol.aeroradiocontrol.model.eventmodel;


import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TempTTSModel;

/**
 * 作者：wzq
 * 时间：2019/3/29:10:33
 * 邮箱：535708929
 * 说明：上传完刷新list
 */
public class EventBusUpDataNewUI {
    private TempTTSModel message;

    public EventBusUpDataNewUI(TempTTSModel str) {
        this.message = str;
    }

    public TempTTSModel getMessage() {
        return this.message;
    }

    public void setMessage(TempTTSModel message) {
        this.message = message;
    }
}
