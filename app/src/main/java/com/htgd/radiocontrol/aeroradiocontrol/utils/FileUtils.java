package com.htgd.radiocontrol.aeroradiocontrol.utils;

import android.os.Environment;

import com.htgd.radiocontrol.aeroradiocontrol.model.MachineListModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;


/**
 * Created by wzq on 2018/3/17.
 */

public class FileUtils {
    private static   String projectPath="";

    public FileUtils() {
    }

    /**
     * 数据存放在本地
     *
     * @param tArrayList
     */
    public static void saveStorage2SDCard(ArrayList tArrayList, String fileName) {
        FileOutputStream fileOutputStream = null;
        ObjectOutputStream objectOutputStream = null;
        FileInputStream fileInputStream = null;
        try {
            File file = FileUtils.getFile(File.separator +  projectPath + File.separator + fileName);
            fileOutputStream = new FileOutputStream(file.toString());  //新建一个内容为空的文件
            objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(tArrayList);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (objectOutputStream != null) {
            try {
                objectOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (fileOutputStream != null) {
            try {
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 获取本地的List数据
     *
     * @return
     */
    public  ArrayList<MachineListModel> getStorageEntities(String fileName) {
        ObjectInputStream objectInputStream = null;
        FileInputStream fileInputStream = null;
        ArrayList<MachineListModel> savedArrayList = new ArrayList<>();
        try {
            File file = FileUtils.getFile(File.separator + FileUtils.projectPath + File.separator + fileName);
            fileInputStream = new FileInputStream(file.toString());
            objectInputStream = new ObjectInputStream(fileInputStream);
            savedArrayList = (ArrayList<MachineListModel>) objectInputStream.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return savedArrayList;
    }


    /**
     * 创建文件
     *
     * @param filePath 文件存放路径
     */
    public static File getFile(String filePath) {
        //获取SDCard根目录
        String sdCardPath = Environment.getExternalStorageState();
        File file = new File(sdCardPath + filePath);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }
    /**
     * 删除文件夹
     *
     * @param folder 文件存放路径
     */
    public void deleteDirectory(File folder) {
        if (folder.exists()) {
            File[] files = folder.listFiles();
            if (files == null) {
                return;
            }
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        folder.delete();
    }
}
