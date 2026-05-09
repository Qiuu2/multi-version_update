package radiocontrol.htgd.com.aeroradiocontrol.widget;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.DatePicker;

import java.util.Calendar;

/**
 * Created by wzq on 2017-08-26.
 */
public class DialogDateChoose {
    int mYear, mMonth, mDay;
    final Calendar ca = Calendar.getInstance();
    private Context mContext;
    private DatePickerDialog datePickerDialog;
    public DialogDateChoose(Context context,OnClickListener listener){
        mYear = ca.get(Calendar.YEAR);
        mMonth = ca.get(Calendar.MONTH);
        mDay = ca.get(Calendar.DAY_OF_MONTH);
        this.mContext = context;
        OnDateSetListenerNew  mdateListener = new OnDateSetListenerNew(listener);
        datePickerDialog = new DatePickerDialog(mContext,mdateListener, mYear, mMonth, mDay);
    }



    public class OnDateSetListenerNew implements DatePickerDialog.OnDateSetListener{

        OnClickListener  listener;
        public  OnDateSetListenerNew(OnClickListener  listener){
            this.listener = listener;
        }

        @Override
        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
            mYear = year;
            mMonth = month;
            mDay = dayOfMonth;
            listener.onConfirmClick(getDate());
        }
    }

    public void showDialog(){
        datePickerDialog.show();
    }
    public interface OnClickListener {

        public void onConfirmClick(String data);

        /***
         * 对话框消失回调
         */
        public void dialogDismiss();
    }

    public String getDate(){
        StringBuffer  stringBuffer = new StringBuffer();
        return String.valueOf(stringBuffer.append(mYear).append("-").append(mMonth + 1).append("-").append(mDay));
    }
    public void setCanleLister(DialogInterface.OnCancelListener lister){
        datePickerDialog.setOnCancelListener(lister);
    }
}
