package com.htgd.radiocontrol.aeroradiocontrol.model.eventmodel;


import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TempTTSModel;

/**
 * 作者：wzq
 * 时间：2019/3/6:16:38
 * 邮箱：535708929
 * 说明：点击列表的执行开始上传临时任务
 */
public class EventBusPostTempTask {
    private TempTTSModel message;

    public EventBusPostTempTask(TempTTSModel str) {
        this.message = str;
    }

    public TempTTSModel getMessage() {
        return this.message;
    }

    public void setMessage(TempTTSModel message) {
        this.message = message;
    }
}
