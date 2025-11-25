package com.example.safespace;

import com.google.firebase.firestore.PropertyName;

// Model class to represent each trigger item in the hierarchy
public class TriggerItem {
    // Use @PropertyName to map Firestore field names to Java field names
    @PropertyName("img_tag")
    private String imgTag;  // trigger name (cat, dog, bird, etc)
    @PropertyName("is_parent")
    private boolean isParent;   // is this trigger also a name of a group of triggers (eg bird - true)
    @PropertyName("parent_tag")
    private String parentTag;   // name of the parent trigger; if the highest in the hierarchy then ""
    @PropertyName("str_res")
    private String strRes;      // name of the string in the strings.xml (for display)

    private boolean isExpanded;    // track if category is expanded
    private int level;     // Hierarchy level (0=root, 1=subcategory, 2=sub-subcategory)

    public TriggerItem(){
        // Default constructor for Firestore
    }

    public TriggerItem(String imgTag, boolean isParent,
                        String parentTag, String strRes){
        this.imgTag=imgTag;
        this.isParent=isParent;
        this.parentTag = parentTag;
        this.strRes = strRes;
        this.isExpanded = false;
        this.level = -1; // Will be calculated based on parent hierarchy
    }
    @PropertyName("img_tag")
    public String getImgTag() {
        return imgTag;
    }
    @PropertyName("img_tag")
    public void setImgTag(String imgTag) {
        this.imgTag = imgTag;
    }

    @PropertyName("is_parent")
    public boolean isParent() {
        return isParent;
    }
    @PropertyName("is_parent")
    public void setParent(boolean parent) {
        isParent = parent;
    }

    @PropertyName("parent_tag")
    public String getParentTag(){
        return parentTag;
    }
    @PropertyName("parent_tag")
    public void setParentTag(String parentTag) {
        this.parentTag = parentTag;
    }

    @PropertyName("str_res")
    public String getStrRes() {
        return strRes;
    }
    @PropertyName("str_res")
    public void setStrRes(String strRes) {
        this.strRes = strRes;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

    public int getLevel() {
        return level;
    }
    public void setLevel(int level) {
        this.level = level;
    }

}
