package com.htgd.radiocontrol.aeroradiocontrol.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2016/8/13.
 */
public class Node {

    public String isfile() {
        return isfile;
    }

    public void setIsfile(String isfile) {
        this.isfile = isfile;
    }

    public    String isfile;

    public Node() {}

    public Node(int id, int pId, String name,String isfile) {
        this.id = id;
        this.pId = pId;
        this.name = name;
        this.isfile=isfile;
    }
    /**
     * 自身Id
     */
    private int id;
    /**
     * 父节点id
     */
    private int pId = 0;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 树的层级
     */
    private int level;

    /**
     * 节点图标
     */
    private int icon;

    /**
     * 是否展开
     */
    private boolean isExpand = false;

    /**
     * 父节点
     */
    private Node parent;

    /**
     * 子节点
     */

    private List<Node> childrens = new ArrayList<>();


    public boolean isHide() {
        return isHide;
    }

    public void setHide(boolean hide) {
        isHide = hide;
    }

    public boolean ischecked() {
        return ischecked;
    }

    public void setIschecked(boolean ischecked) {
        this.ischecked = ischecked;
    }

    //父节点是否隐藏checkbox
    private boolean isHide;
    //是否选中checkbox
    private boolean  ischecked;
    public int getId() {
        return id;
    }

    public int getpId() {
        return pId;
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return parent == null ? 0 : parent.getLevel() + 1;
    }

    public int getIcon() {
        return icon;
    }

    public boolean isExpand() {
        return isExpand;
    }

    public Node getParent() {
        return parent;
    }

    public List<Node> getChildrens() {
        return childrens;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setpId(int pId) {
        this.pId = pId;
    }

    public void setName(String name) {
        this.name = name;
    }


    public void setLevel(int level) {
        this.level = level;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    /**
     * 如果当前节点有孩子，当改变当前节点的展开状态，则孩子也要做相应的改变
     * @param expand
     */
    public void setExpand(boolean expand) {

        isExpand = expand;

        if (!isExpand) {
            //为收缩，递归收缩孩子节点
            for (Node node : childrens) {
                node.setExpand(false);
            }
        }
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public void setChildrens(List<Node> childrens) {
        this.childrens = childrens;
    }

    /**
     * 判断当前节点是否是根节点
     * @return
     */
    public boolean isRoot() {
        //当没有父节点时则为根节点，返回true
        return  parent == null;
    }

    /**
     * 判断当前节点是否是叶子节点
     * @return
     */


    public boolean isLeaf() {
        return childrens.size() == 0;
    }

    /**
     * 叶子节点没有展开状态，是否显示要根据父节点是否是展开的
     * @return
     */
    public boolean isParentExpand() {
        if (parent == null) {
            return false;
        }
        return parent.isExpand();
    }


}
