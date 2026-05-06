package com.example.panicpause;

// Model class to represent each trigger item in the hierarchy
public class TriggerItem {
    private String imgTag;      // trigger name (cat, dog, bird, etc)
    private boolean isParent;   // is this trigger also a name of a group of triggers (eg bird - true)
    private String parentTag;   // name of the parent trigger; if the highest in the hierarchy then ""
    private String nameRus;

    private boolean isExpanded;    // track if category is expanded
    private int level;     // Hierarchy level (0=root, 1=subcategory, 2=sub-subcategory)

    public TriggerItem(){
        // Default constructor for Firestore
        // (не используется напрямую, но требуется для совместимости)
    }

    /**
     * Основной конструктор с поддержкой нового поля nameRus.
     *
     * @param imgTag техническое имя тега
     * @param isParent флаг родительского тега
     * @param parentTag имя родительского тега
     * @param nameRus реальное название тега из БД (приоритет для отображения)
     */
    public TriggerItem(String imgTag, boolean isParent, String parentTag, String nameRus){
        this.imgTag = imgTag;
        this.isParent = isParent;
        this.parentTag = parentTag;
        this.nameRus = nameRus != null ? nameRus : "";
        this.isExpanded = false;
        this.level = -1; // Will be calculated based on parent hierarchy
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

    // МЕТОДЫ ДЛЯ РАБОТЫ С nameRus
    /**
     * Возвращает название тега из БД (поле name_rus).
     * Может быть пустой строкой для старых данных.
     */
    public String getNameRus() {
        return nameRus != null ? nameRus : "";
    }

}
