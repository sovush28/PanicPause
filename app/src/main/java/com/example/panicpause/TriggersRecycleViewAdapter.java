package com.example.panicpause;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class TriggersRecycleViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // помогает RecyclerView понять, какой layout использовать
    private static int TYPE_CATEGORY=0;
    private static int TYPE_CHILD=1;

    private List<TriggerItem> allItems;
    private List<TriggerItem> displayedItems; // зависит от expand/collapse

    private OnTriggerClickListener listener;

    private Set<String> userSelectedTriggers = new HashSet<>();

    public interface OnTriggerClickListener {
        void onCategoryClick(TriggerItem triggerCategory, int position);
        void onTriggerClick(TriggerItem triggerChild,
                            ImageButton plusButton,
                            boolean isCurrentlySelected);
    }

    public TriggersRecycleViewAdapter(List<TriggerItem> triggerItems, OnTriggerClickListener listener){
        this.allItems=triggerItems;
        this.listener=listener;
        this.displayedItems=new ArrayList<>();
        buildDisplayedItemsList(); //изначально отобразить список только с названиями групп (категорий)
    }

    public void setUserSelectedTriggers(Set<String> triggers) {
        this.userSelectedTriggers = triggers != null ? triggers : new HashSet<>();
        notifyDataSetChanged(); // обновить все элементы, чтобы обновить состояния плюсов/галочек
    }

    private boolean isTriggerSelected(String imgTag) {
        return userSelectedTriggers.contains(imgTag);
    }

    private void buildDisplayedItemsList() {
        displayedItems.clear();
        for (TriggerItem item : allItems) {
            if (Objects.equals(item.getParentTag(), "") || item.getParentTag()==null) {
                displayedItems.add(item);
            }
        }
    }

    @Override
    public int getItemViewType(int position){
        TriggerItem item = displayedItems.get(position);
        if(item != null && item.isParent()){
            return TYPE_CATEGORY;
        }
        else{
            return TYPE_CHILD;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if(viewType==TYPE_CATEGORY){
            View view=inflater.inflate(R.layout.tr_item_group_title_layout, parent, false);
            return new TriggerCategoryViewHolder(view);
        }
        else{
            View view=inflater.inflate(R.layout.tr_item_child_layout, parent, false);
            return new TriggerChildViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder,
                                 int position){
        TriggerItem triggerItem=displayedItems.get(position);
        if (triggerItem == null) return;

        if(holder instanceof TriggerCategoryViewHolder){
            ((TriggerCategoryViewHolder) holder).bind(triggerItem,position);
        }
        else if(holder instanceof TriggerChildViewHolder){
            ((TriggerChildViewHolder) holder).bind(triggerItem);
        }
    }

    @Override
    public int getItemCount(){
        return displayedItems.size();
    }

    /**
     * Метод toggleCategory отвечает за переключение состояния категории (раскрытие/скрытие)
     * и за обновление UI.
     * @param position позиция категории
     */
    public void toggleCategory(int position){
        if (position < 0 || position >= displayedItems.size())
            return;

        TriggerItem triggerCategory=displayedItems.get(position);
        if (triggerCategory == null || !triggerCategory.isParent())
            return;

        triggerCategory.setExpanded(!triggerCategory.isExpanded());

        if(triggerCategory.isExpanded()){
            expandTriggerCategory(position,triggerCategory);
        }
        else{
            collapseTriggerCategory(position, triggerCategory);
        }

    }

    /**
     * Метод expandTriggerCategory добавляет дочерние теги при раскрытии категории
     * @param position позиция категории
     * @param triggerCategory тип тега
     */
    private void expandTriggerCategory(int position, TriggerItem triggerCategory) {
        List<TriggerItem> triggerChildren = getChildren(triggerCategory.getImgTag());
        if (!triggerChildren.isEmpty()) {
            displayedItems.addAll(position+1,triggerChildren);
            notifyItemRangeInserted(position+1,triggerChildren.size());
            notifyItemChanged(position);
        }
    }

    /**
     * Метод collapseTriggerCategory скрывает дочерние теги при схлопывании категории
     * @param position позиция категории
     * @param triggerCategory тип тега
     */
    private void collapseTriggerCategory(int position, TriggerItem triggerCategory){
        // вычислить количество скрываемых элементов
        int removeCount = calculateVisibleChildrenCount(position, triggerCategory.getImgTag());

        int startIndex = position + 1;
        int endIndex = position + 1 + removeCount;

        if (removeCount > 0 && endIndex <= displayedItems.size()) {
            displayedItems.subList(startIndex, endIndex).clear();
            notifyItemRangeRemoved(startIndex, removeCount);
        } else if (removeCount > 0) {
            int actualRemoveCount = displayedItems.size() - startIndex;
            if (actualRemoveCount > 0) {
                displayedItems.subList(startIndex, displayedItems.size()).clear();
                notifyItemRangeRemoved(startIndex, actualRemoveCount);
            }
        }
        notifyItemChanged(position);

    }

    private int calculateVisibleChildrenCount(int parentPosition, String parentTag) {
        if (parentTag == null)
            return 0;

        int count = 0;
        int currentPosition = parentPosition + 1;

        while (currentPosition < displayedItems.size()) {
            TriggerItem currentItem = displayedItems.get(currentPosition);
            if (currentItem == null)
                break;

            if (isChildOfParent(currentItem, parentTag)) {
                count++;
                currentPosition++;
            } else {
                break;
            }
        }
        return count;
    }

    private boolean isChildOfParent(TriggerItem item, String parentTag) {
        if (item == null || parentTag == null)
            return false;

        if (parentTag.equals(item.getParentTag())) {
            return true;
        }

        String currentParentTag = item.getParentTag();
        while (currentParentTag != null && !currentParentTag.isEmpty()) {
            TriggerItem parentItem = findItemByTag(currentParentTag);
            if (parentItem == null)
                break;
            if (parentTag.equals(parentItem.getParentTag())) {
                return true;
            }
            currentParentTag = parentItem.getParentTag();
        }
        return false;
    }

    private TriggerItem findItemByTag(String tag) {
        for (TriggerItem item : allItems) {
            if (tag.equals(item.getImgTag())) {
                return item;
            }
        }
        return null;
    }

    private List<TriggerItem> getChildren(String parentTag) {
        List<TriggerItem> children = new ArrayList<>();
        if(parentTag==null)
            return children;

        for (TriggerItem triggerItem : allItems) {
            String itemParent = triggerItem.getParentTag();
            if (itemParent != null && itemParent.equals(parentTag)) {
                children.add(triggerItem);
            }
        }
        return children;
    }

    public void updateItems(List<TriggerItem> newItems) {
        this.allItems = newItems;
        if(newItems==null)
            this.allItems=new ArrayList<>();

        calculateLevels();
        buildDisplayedItemsList();
        notifyDataSetChanged();
    }

    private void calculateLevels() {
        for (TriggerItem item : allItems) {
            if(item!=null)
                item.setLevel(calculateLevel(item, 0));
        }
    }

    private int calculateLevel(TriggerItem item, int currentLevel) {
        if (item == null)
            return currentLevel;

        String parentTag = item.getParentTag();
        if (parentTag == null || parentTag.isEmpty()) {
            return currentLevel;
        }

        if (Objects.equals(item.getParentTag(), "") || item.getParentTag()==null) {
            return currentLevel;
        }

        for (TriggerItem parent : allItems) {
            if (parent != null && parent.getImgTag() != null &&
                    parent.getImgTag().equals(parentTag)) {
                return calculateLevel(parent, currentLevel + 1);
            }
        }
        return currentLevel;
    }

    public class TriggerCategoryViewHolder extends RecyclerView.ViewHolder{
        private TextView titleTV;
        private ImageView triangleIV;
        private View categoryLayout, outerLayout;
        private ImageButton plusBtn;

        public TriggerCategoryViewHolder(@NonNull View itemView){
            super(itemView);
            titleTV=itemView.findViewById(R.id.tr_category_title_tv);
            triangleIV=itemView.findViewById(R.id.tr_category_title_triangle_iv);
            categoryLayout=itemView.findViewById(R.id.tr_category_title_layout);
            outerLayout=itemView.findViewById(R.id.tr_category_title_layout_outer);
            plusBtn=itemView.findViewById(R.id.tr_plus_ib);
        }

        public void bind(TriggerItem triggerItem, int position){
            if (triggerItem == null)
                return;

            if(triggerItem.getNameRus()!=null)
                titleTV.setText(triggerItem.getNameRus());

            if(triggerItem.isExpanded()){
                triangleIV.setRotation(90);
            }
            else{
                triangleIV.setRotation(0);
            }

            updatePlusButtonSrc(triggerItem.getImgTag());

            ViewGroup.MarginLayoutParams params=(ViewGroup.MarginLayoutParams) outerLayout.getLayoutParams();
            int horizMargin=28+(triggerItem.getLevel()*18);
            params.setMargins(dpToPx(horizMargin), dpToPx(18), 0, 0);
            outerLayout.setLayoutParams(params);

            categoryLayout.setOnClickListener(v -> {
                if(listener!=null){
                    listener.onCategoryClick(triggerItem, position);
                }
            });
            plusBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        boolean isCurrentlySelected = isTriggerSelected(triggerItem.getImgTag());
                        listener.onTriggerClick(triggerItem, plusBtn, isCurrentlySelected);
                    }
                }
            });
        }

        private int dpToPx(int dp){
            return (int)(dp*itemView.getContext().getResources().getDisplayMetrics().density);
        }

        private void updatePlusButtonSrc(String imgTag) {
            if (isTriggerSelected(imgTag)) {
                plusBtn.setImageResource(R.drawable.check_mark);
            } else {
                plusBtn.setImageResource(R.drawable.plus);
            }
        }
    }

    public class TriggerChildViewHolder extends RecyclerView.ViewHolder{
        private TextView titleTV;
        private ImageButton plusBtn;
        private View triggerLayout;

        public TriggerChildViewHolder(@NonNull View itemView){
            super(itemView);
            titleTV=itemView.findViewById(R.id.tr_child_title_tv);
            plusBtn=itemView.findViewById(R.id.tr_plus_ib);
            triggerLayout=itemView.findViewById(R.id.tr_child_layout);
        }

        public void bind(TriggerItem triggerItem){
            if (triggerItem == null)
                return;

            if(triggerItem.getNameRus()!=null)
                titleTV.setText(triggerItem.getNameRus());

            updatePlusButtonSrc(triggerItem.getImgTag());

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) triggerLayout.getLayoutParams();
            int margin = 46 + ((triggerItem.getLevel() - 1) * 18); // больший отступ слева
            params.setMargins(dpToPx(margin), dpToPx(18), dpToPx(28), 0);
            triggerLayout.setLayoutParams(params);

            plusBtn.setOnClickListener(v -> {
                if (listener != null) {
                    boolean isCurrentlySelected = isTriggerSelected(triggerItem.getImgTag());
                    listener.onTriggerClick(triggerItem, plusBtn, isCurrentlySelected);
                }
            });
        }

        private int dpToPx(int dp) {
            return (int) (dp * itemView.getContext().getResources().getDisplayMetrics().density);
        }

        private void updatePlusButtonSrc(String imgTag) {
            if (isTriggerSelected(imgTag)) {
                plusBtn.setImageResource(R.drawable.check_mark);
            } else {
                plusBtn.setImageResource(R.drawable.plus);
            }
        }
    }

}
