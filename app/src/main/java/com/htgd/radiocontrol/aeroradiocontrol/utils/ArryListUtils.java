package com.htgd.radiocontrol.aeroradiocontrol.utils;

import android.content.Context;
import android.widget.Toast;

import com.example.htapplib.HTIntf;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.MachineInfo;
import com.htgd.radiocontrol.aeroradiocontrol.model.responseModel.TaskZuoxiModel;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by wzq on 2017-08-15.
 */
public class ArryListUtils {
    private  static  ArryListUtils inStance;
    public static  ArryListUtils getInStance(){
        if(inStance == null){
            synchronized (ArryListUtils.class){
                inStance = new ArryListUtils();
            }
        }
        return  inStance;
    }
    //去除重复的对象
    public ArrayList<TaskZuoxiModel>  cutTheSameModel(ArrayList<TaskZuoxiModel> listold){
        ArrayList<TaskZuoxiModel> listNew = new ArrayList<>();
        HashMap<String, TaskZuoxiModel> map = new HashMap<String, TaskZuoxiModel>();
        if(listold !=null && listold.size()>0){
            for (TaskZuoxiModel model:listold){
                 LogUtils.setLog("the a is" + model.getName());
                map.put(model.getName(), model);
            }
        }
        for(String a : map.keySet()){
            listNew.add(map.get(a));
             LogUtils.setLog("the model is" + a);
        }
        return  listNew;
    }


    //将选中的设备全部取消选中状态
    public ArrayList<MachineInfo> cleanChoose(ArrayList<MachineInfo> machineList){
        if(machineList != null && machineList.size()>0){
            for(int i=0;i<machineList.size();i++){
                if(machineList.get(i).isChoose()){
                    machineList.get(i).setChoose(false);
                }
            }
        }
        return  machineList;
    }

    // 将选中的设备重新提取出来组成一个新的数组
    public  ArrayList<MachineInfo> setChooseData( ArrayList<MachineInfo> machineList){
        ArrayList<MachineInfo> newList = new ArrayList<>();
        if(machineList!=null &&machineList.size()>0){
            for(MachineInfo info:machineList){
                if(info.isChoose()){
                    newList.add(info);
                }
            }
        }
        return  newList;
    }

    public ArrayList<MachineInfo> cutTheSameMachine(ArrayList<MachineInfo> machineList , Context context){
        ArrayList<MachineInfo> machineInfos =  new ArrayList<>();
      if(machineList!=null && machineList.size()>0  && context!=null){
          for(MachineInfo info :machineList){
              if(info.getId() != HTIntf.getterminalid()){
                  LogUtils.setLog("the not same name model is" +info.getName());
                  machineInfos.add(info);
              }else {
                   LogUtils.setLog("the same name model is" +info.getName());
              }
          }
      }
      return  machineInfos;
    }

    public ArrayList<MachineInfo> getTheOnNetMachine(ArrayList<MachineInfo>machineList,Context context){
        ArrayList<MachineInfo> machineInfos =  new ArrayList<>();
        for(int i=0;i<machineList.size();i++){
            if(machineList.get(i).getNetstate()==1){
                machineInfos.add(machineList.get(i));
            }
        }
        return   machineInfos;
    }
    public ArrayList<MachineInfo> getTheOnFreeMachine(ArrayList<MachineInfo>machineList,Context context){
        ArrayList<MachineInfo> machineInfos =  new ArrayList<>();
        for(int i=0;i<machineList.size();i++){
            if(machineList.get(i).getState()==1){
                machineInfos.add(machineList.get(i));
            }
        }
        return   machineInfos;
    }

}
