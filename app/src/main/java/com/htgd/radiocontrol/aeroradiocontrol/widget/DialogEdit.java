package radiocontrol.htgd.com.aeroradiocontrol.widget;

import android.content.Context;

/**
 * Created by wzw on 2018/4/2.
 */

public class DialogEdit {
    private   String content;
    private   Context mContext;

    public DialogEdit(Context context, String content ) {//a=0为文字，1为bar，2为日期，3为时间
        super();
        this.mContext = context;

        this.content = content;
    }

}
