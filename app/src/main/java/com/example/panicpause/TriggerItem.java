package com.example.panicpause;

// Model class to represent each trigger item in the hierarchy
public class TriggerItem {
    private String imgTag;      // trigger name (cat, dog, bird, etc)
    private boolean isParent;   // is this trigger also a name of a group of triggers (eg bird - true)
    private String parentTag;   // name of the parent trigger; if the highest in the hierarchy then ""
    //private String strRes;      // name of the string in the strings.xml (for display)
    private String nameRus;

    /*
    // Use @PropertyName to map Firestore field names to Java field names
    @PropertyName("img_tag")
    private String imgTag;  // trigger name (cat, dog, bird, etc)
    @PropertyName("is_parent")
    private boolean isParent;   // is this trigger also a name of a group of triggers (eg bird - true)
    @PropertyName("parent_tag")
    private String parentTag;   // name of the parent trigger; if the highest in the hierarchy then ""
    @PropertyName("str_res")
    private String strRes;      // name of the string in the strings.xml (for display)
*/

    private boolean isExpanded;    // track if category is expanded
    private int level;     // Hierarchy level (0=root, 1=subcategory, 2=sub-subcategory)

    public TriggerItem(){
        // Default constructor for Firestore
        // (не используется напрямую, но требуется для совместимости)
    }

    /**
     * Устаревший конструктор для обратной совместимости.
     * Используется при загрузке старых данных без поля name_rus.
     * Для новых данных предпочтительно использовать конструктор с 5 параметрами.
     */
    /*public TriggerItem(String imgTag, boolean isParent,
                        String parentTag, String strRes){
        // nameRus = пустая строка → будет использоваться strRes как fallback
        this.imgTag=imgTag;
        this.isParent=isParent;
        this.parentTag = parentTag;
        this.strRes = strRes;
        this.nameRus = "";
        this.isExpanded = false;
        this.level = -1;
    }*/

    /**
     * Основной конструктор с поддержкой нового поля nameRus.
     *
     * @param imgTag техническое имя тега
     * @param isParent флаг родительского тега
     * @param parentTag имя родительского тега
     * strRes имя строкового ресурса (для обратной совместимости) (убрано)
     * @param nameRus реальное название тега из БД (приоритет для отображения)
     */
    public TriggerItem(String imgTag, boolean isParent, String parentTag, String nameRus){
        this.imgTag = imgTag;
        this.isParent = isParent;
        this.parentTag = parentTag;
        //this.strRes = strRes != null ? strRes : "";
        this.nameRus = nameRus != null ? nameRus : "";
        this.isExpanded = false;
        this.level = -1; // Will be calculated based on parent hierarchy
    }


    /*
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
    }*/

    public String getImgTag() { return imgTag; }
    public void setImgTag(String imgTag) { this.imgTag = imgTag; }

    public boolean isParent() { return isParent; }
    public void setParent(boolean parent) { isParent = parent; }

    public String getParentTag() { return parentTag; }
    public void setParentTag(String parentTag) { this.parentTag = parentTag; }

    //public String getStrRes() { return strRes; }
    //public void setStrRes(String strRes) { this.strRes = strRes; }

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

/**
     * Возвращает название тега для отображения пользователю.
     * Приоритетная логика:
     * 1. Если nameRus не пустой → возвращаем его напрямую (новое поведение)
     * 2. Иначе → загружаем строку из ресурсов по strRes (старое поведение)
     *
     * РЕКОМЕНДАЦИЯ: Во всех UI-компонентах использовать этот метод вместо прямого
     * обращения к getStrRes() + context.getString().
     *
     * @param context необходим для доступа к строковым ресурсам (только при fallback)
     * @return локализованное название тега
     */
/*
    public String getDisplayName(Context context) {
        // Приоритет: реальное название из БД
        if (nameRus != null && !nameRus.trim().isEmpty()) {
            return nameRus.trim();
        }
        return imgTag != null ? imgTag : "unknown";
        // Fallback: строковый ресурс (для старых данных или при ошибке)
        if (strRes != null && !strRes.trim().isEmpty() && context != null) {
            try {
                int resId = context.getResources().getIdentifier(strRes, "string", context.getPackageName());
                if (resId != 0) {
                    return context.getString(resId);
                }
            } catch (Exception e) {
                // Игнорируем ошибки доступа к ресурсам — вернём strRes как есть
            }
        }
        // Крайний fallback: возвращаем техническое имя imgTag
        return imgTag != null ? imgTag : "unknown";
    }*/

}
