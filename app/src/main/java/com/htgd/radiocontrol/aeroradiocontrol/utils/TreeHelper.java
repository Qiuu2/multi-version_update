package com.htgd.radiocontrol.aeroradiocontrol.utils;

import android.util.Log;

import com.htgd.radiocontrol.aeroradiocontrol.R;
import com.htgd.radiocontrol.aeroradiocontrol.annotation.TreeNodeId;
import com.htgd.radiocontrol.aeroradiocontrol.annotation.TreeNodeIsfile;
import com.htgd.radiocontrol.aeroradiocontrol.annotation.TreeNodeName;
import com.htgd.radiocontrol.aeroradiocontrol.annotation.TreeNodePid;
import com.htgd.radiocontrol.aeroradiocontrol.model.Node;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by Administrator on 2016/8/13.
 */
public class TreeHelper {

    /**
     * 根据数据拼装出node
     *
     * @param datas
     * @param <T>
     * @return
     * @throws IllegalAccessException
     */
    public static <T> List<Node> convertDatasToNodes(List<T> datas) throws IllegalAccessException {

        List<Node> nodeList = new ArrayList<>();
        for (T t : datas) {
            //进行反射得到变量域
            Class clazz = t.getClass();
            Field[] fields = clazz.getDeclaredFields();
            int id = -1;
            int pId = -1;
            String name = null;
            String isfile = null;
            for (Field field : fields) {
                if (field.getAnnotation(TreeNodeId.class) != null) {
                    field.setAccessible(true);
                    id = field.getInt(t);
                }

                if (field.getAnnotation(TreeNodePid.class) != null) {
                    field.setAccessible(true);
                    pId = field.getInt(t);
                }

                if (field.getAnnotation(TreeNodeName.class) != null) {
                    field.setAccessible(true);
                    name = (String) field.get(t);
                }
                if (field.getAnnotation(TreeNodeIsfile.class) != null) {
                    field.setAccessible(true);
                    isfile = (String) field.get(t);
                }
            }

            Node node = new Node(id, pId, name, isfile);
            nodeList.add(node);

        }

        for (Node node : nodeList) {
            Log.d("TAG", "id = " + node.getId() + "pId = " + node.getpId() + "name = " + node.getName() + "\n");
        }


        //设置节点间的关联关系
        for (int i = 0; i < nodeList.size(); i++) {
            //取出节点
            Node n = nodeList.get(i);
            for (int j = i + 1; j < nodeList.size(); j++) {
                Node m = nodeList.get(j);
                //n为m的父节点
                if (n.isfile() == "true"&&m.isfile() == "true") {
                    if (n.getId() == m.getpId()) {
                        //m设置父节点
                        m.setParent(n);
                        //n添加孩子节点
                        n.getChildrens().add(m);
                    } else if (n.getpId() == m.getId()) { // m为n的父节点
                        //n设置父节点
                        n.setParent(m);
                        //m添加孩子节点
                        m.getChildrens().add(n);
                    }
                } else if (n.isfile() == "false"&&m.isfile() == "true") {
                    if (n.getpId() == m.getId()) {
                        //n设置父节点
                        n.setParent(m);
                        //m添加孩子节点
                        m.getChildrens().add(n);
                    }
                }else if (n.isfile() == "true"&&m.isfile() == "false") {
                    if (n.getId() == m.getpId()) {
                        //n设置父节点
                        m.setParent(n);
                        //m添加孩子节点
                        n.getChildrens().add(m);
                    }
                }
            }
        }
        for (Node node : nodeList) {
            Log.d("TAG", "id = " + node.getId() + "pId = " + node.getpId() + "name = " + node.getName() + "children = " + node.getChildrens().size() + "\n");
        }

        //设置节点的图标
        for (Node node : nodeList) {
            setNodeIcon(node);
        }


        return nodeList;
    }

    /**
     * 设置node节点的图标
     *
     * @param
     */
    private static void setNodeIcon(Node node) {
        //有孩子节点并且是展开状态
        if (node.getChildrens().size() > 0 && node.isExpand()) {
            //node.setIcon(R.mipmap.tree_ec);
        } else if (node.getChildrens().size() > 0 && !node.isExpand()) {
            //有孩子节点并且是关闭状态
            //node.setIcon(R.mipmap.tree_ex);
        } else {
            //没有孩子节点的
            node.setIcon(-1);
        }
        if (node.isfile != "true") {
            node.setIcon(-1);
        }
    }

    /**
     * 对节点排序
     *
     * @param datas
     * @param defaultExpandLevel
     * @param <T>
     * @return
     * @throws IllegalAccessException
     */
    public static <T> List<Node> sortedNode(List<T> datas, int defaultExpandLevel) throws IllegalAccessException {
        List<Node> result = new ArrayList<>();
        List<Node> sourceNodes = convertDatasToNodes(datas);
        //找出节点中的根节点
        List<Node> rootNodes = new ArrayList<>();
        for (Node node : sourceNodes) {
            if (node.isRoot()) {
                rootNodes.add(node);
            }
        }

        //依次取出根节点按顺序排列，即先把一个分支排完再排下一个分支
        for (Node node : rootNodes) {
            addNodeBySort(result, node, defaultExpandLevel, 1);
        }
        return result;
    }

    /**
     * 按顺序添加节点
     *
     * @param result
     * @param node
     * @param defaultExpandLevel
     * @param currentExpandLevel
     */
    private static void addNodeBySort(List<Node> result, Node node, int defaultExpandLevel, int currentExpandLevel) {
        result.add(node);
        if (defaultExpandLevel >= currentExpandLevel) {
            node.setExpand(true);
        }

        //已经到叶子节点，返回
        if (node.isLeaf()) {
            return;
        }

        //递归
        for (int i = 0; i < node.getChildrens().size(); i++) {
            addNodeBySort(result, node.getChildrens().get(i), defaultExpandLevel, currentExpandLevel + 1);
        }
    }

    /**
     * 得到可见的节点集
     *
     * @param nodes
     * @return
     */
    public static List<Node> filterVisibleNode(List<Node> nodes) {
        List<Node> result = new ArrayList<>();
        for (Node node : nodes) {
            if (node.isRoot() || node.isParentExpand()) {
                //根节点或父节点为展开状态时可见
                setNodeIcon(node);
                result.add(node);
            }
        }
        return result;
    }


}
