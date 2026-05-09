package   com.htgd.radiocontrol.aeroradiocontrol.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import androidx.viewpager.widget.PagerAdapter;

/**
 * Created by wzq on 2017-07-24.
 */
public class ViewPagerXunhuAdapter extends PagerAdapter {
    private ArrayList<View> viewList;
    private Context mContext;

    public ViewPagerXunhuAdapter(Context context,ArrayList<View> viewList){
        this.mContext = context;
        this.viewList = viewList;
    }

    @Override
    public int getCount() {
        return viewList==null?0:viewList.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        if (view == object) return true;
        else return false;
    }
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view =viewList.get(position);
        container.addView(view);
        return view;
    }
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView(viewList.get(position));
    }
}
