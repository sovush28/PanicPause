package com.example.panicpause;

/**
 * TriggerItem - класс для представления каждого тега/триггера
 */
public class TriggerItem {
    private String imgTag;      // cat, dog, bird, etc
    private boolean isParent;   // является ли также названием группы тегов (напр. bird - true)
    private String parentTag;   // название тега-родителя ("" если высший в иерархии)
    private String nameRus;

    private boolean isExpanded;    // раскрыта ли группа тегов
    private int level;     // уровень иерархии (0=root, 1=subcategory, 2=sub-subcategory)

    public TriggerItem(){
        // Default constructor for Firestore
        // (не используется напрямую, но требуется для совместимости)
    }

    public TriggerItem(String imgTag, boolean isParent, String parentTag, String nameRus){
        this.imgTag = imgTag;
        this.isParent = isParent;
        this.parentTag = parentTag;
        this.nameRus = nameRus != null ? nameRus : "";
        this.isExpanded = false;
        this.level = -1; // будет вычисляться на основе иерархии родителя
    }

    public String getImgTag() { return imgTag; }
    public void setImgTag(String imgTag) { this.imgTag = imgTag; }

    public boolean isParent() { return isParent; }
    public void setParent(boolean parent) { isParent = parent; }

    public String getParentTag() { return parentTag; }
    public void setParentTag(String parentTag) { this.parentTag = parentTag; }

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

    public String getNameRus() {
        return nameRus != null ? nameRus : "";
    }

}
