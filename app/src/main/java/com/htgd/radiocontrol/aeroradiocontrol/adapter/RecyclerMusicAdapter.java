package com.htgd.radiocontrol.aeroradiocontrol.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MusicInfoModel;
import com.htgd.radiocontrol.aeroradiocontrol.utils.LogUtils;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


/**
 * 作者：wzq
 * 时间：2018/8/28:11:40
 * 邮箱：535708929
 * 说明：可伸缩选中的列表适配器
 */
public class RecyclerMusicAdapter extends RecyclerView.Adapter<RecyclerMusicAdapter.RecyclerMusicViewHolder>{
    private   String mTag="RecyclerMusicAdapter";
    private ArrayList<MusicInfoModel> choosesList;
    private Context context;
    public interface DeleteListener {
        public void delete(int i);
    }
    public void setListener( DeleteListener listener) {
        this.listener = listener;
    }
    private DeleteListener listener;
    public RecyclerMusicAdapter(Context context, ArrayList<MusicInfoModel> list) {
        this.choosesList=list;
        this.context=context;
        LogUtils.setLog(mTag+"recycleadapter"+choosesList.size());
    }

    @Override
    public   RecyclerMusicViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new  RecyclerMusicViewHolder(LayoutInflater.from(context).inflate(R.layout.item_music_selected, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerMusicViewHolder holder, int position) {
        LogUtils.setLog("recycleadapter"+choosesList.get(position));
        holder.name.setText(choosesList.get(position).getName() );
       final int poss=position;
        holder.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.delete(poss);
            }
        });
    }
    @Override
    public int getItemCount() {
        return choosesList.size();
    }


    // 列表ViewHolder
    public class RecyclerMusicViewHolder extends RecyclerView.ViewHolder {
        private TextView name;
        private TextView delete;

        public RecyclerMusicViewHolder(View itemView) {
            super(itemView);
           name = (TextView) itemView.findViewById(R.id.select_name);
          delete = (TextView) itemView.findViewById(R.id.operation);
        }
    }
}
