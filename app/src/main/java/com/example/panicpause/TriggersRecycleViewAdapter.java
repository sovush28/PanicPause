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

    // helps RecyclerView identify which layout to use
    private static int TYPE_CATEGORY=0;
    private static int TYPE_CHILD=1;

    private List<TriggerItem> allItems; // list from Firestore
    private List<TriggerItem> displayedItems; // depends on expand/collapse

    private OnTriggerClickListener listener; // Click listener for user interactions


    // Track which triggers are selected by the user
    private Set<String> userSelectedTriggers = new HashSet<>();

    // Interface to handle click events in the activity
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
        buildDisplayedItemsList(); //initial display list with only root items
    }

    // Set the user's selected triggers and update UI
    public void setUserSelectedTriggers(Set<String> triggers) {
        this.userSelectedTriggers = triggers != null ? triggers : new HashSet<>();
        notifyDataSetChanged(); // Refresh all items to update plus/checkmark states
    }

    // Check if a trigger is currently selected
    private boolean isTriggerSelected(String imgTag) {
        return userSelectedTriggers.contains(imgTag);
    }

    /*
    // Add a trigger to selected set
    public void addSelectedTrigger(String imgTag) {
        userSelectedTriggers.add(imgTag);
        // Find and update the specific item if possible, otherwise refresh all
        notifyDataSetChanged();
    }

    // Remove a trigger from selected set
    public void removeSelectedTrigger(String imgTag) {
        userSelectedTriggers.remove(imgTag);
        // Find and update the specific item if possible, otherwise refresh all
        notifyDataSetChanged();
    }
*/

    // Build initial display list with only root items (level 0)
    // Only show items without parents (root categories)
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
        // determine view type based on whether item is a parent category
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

    // Toggle category expand/collapse and update ui
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

    // Add child items when category is expanded
    private void expandTriggerCategory(int position, TriggerItem triggerCategory) {
        List<TriggerItem> triggerChildren = getChildren(triggerCategory.getImgTag());
        if (!triggerChildren.isEmpty()) {
            displayedItems.addAll(position+1,triggerChildren);
            notifyItemRangeInserted(position+1,triggerChildren.size());
            notifyItemChanged(position);    // Update triangle icon
        }
    }

    // Remove child items when category is collapsed
    private void collapseTriggerCategory(int position, TriggerItem triggerCategory){
        // Calculate how many items to remove
        int removeCount = calculateVisibleChildrenCount(position, triggerCategory.getImgTag());

        // Ensure we don't exceed list bounds
        int startIndex = position + 1;
        int endIndex = position + 1 + removeCount;

        if (removeCount > 0 && endIndex <= displayedItems.size()) {
            displayedItems.subList(startIndex, endIndex).clear();
            notifyItemRangeRemoved(startIndex, removeCount);
        } else if (removeCount > 0) {
            // Fallback: remove from startIndex to end of list
            int actualRemoveCount = displayedItems.size() - startIndex;
            if (actualRemoveCount > 0) {
                displayedItems.subList(startIndex, displayedItems.size()).clear();
                notifyItemRangeRemoved(startIndex, actualRemoveCount);
            }
        }
        notifyItemChanged(position);

    }

    // Calculate visible children count based on current displayed items
    private int calculateVisibleChildrenCount(int parentPosition, String parentTag) {
        if (parentTag == null)
            return 0;

        int count = 0;
        int currentPosition = parentPosition + 1;

        // Count consecutive items that are children of this parent
        while (currentPosition < displayedItems.size()) {
            TriggerItem currentItem = displayedItems.get(currentPosition);
            if (currentItem == null)
                break;

            // Check if this item is a direct or indirect child of our parent
            if (isChildOfParent(currentItem, parentTag)) {
                count++;
                currentPosition++;
            } else {
                // We've reached an item that's not a child, stop counting
                break;
            }
        }
        return count;
    }

    // Check if an item is a child (direct or indirect) of a parent
    private boolean isChildOfParent(TriggerItem item, String parentTag) {
        if (item == null || parentTag == null)
            return false;

        // Direct child
        if (parentTag.equals(item.getParentTag())) {
            return true;
        }

        // Indirect child - check if it's a child of a child
        String currentParentTag = item.getParentTag();
        while (currentParentTag != null && !currentParentTag.isEmpty()) {
            // Find the parent item
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

    // find item by imgTag
    private TriggerItem findItemByTag(String tag) {
        for (TriggerItem item : allItems) {
            if (tag.equals(item.getImgTag())) {
                return item;
            }
        }
        return null;
    }

    // Get all direct children of a category
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

    // Update all items (e.g., when loading from Firestore)
    public void updateItems(List<TriggerItem> newItems) {
        this.allItems = newItems;
        if(newItems==null)
            this.allItems=new ArrayList<>();
        //this.allItems = newItems != null ? newItems : new ArrayList<>();

        calculateLevels(); // Calculate hierarchy levels for all items
        buildDisplayedItemsList();
        notifyDataSetChanged(); // Refresh entire list
    }

    // Calculate hierarchy levels for proper indentation
    private void calculateLevels() {
        for (TriggerItem item : allItems) {
            if(item!=null)
                item.setLevel(calculateLevel(item, 0));
        }
    }

    // Recursively calculate level based on parent hierarchy
    private int calculateLevel(TriggerItem item, int currentLevel) {
        if (item == null)
            return currentLevel;

        // Root items have no parent (null or empty)
        String parentTag = item.getParentTag();
        if (parentTag == null || parentTag.isEmpty()) {
            return currentLevel;
        }

        if (Objects.equals(item.getParentTag(), "") || item.getParentTag()==null) {
            return currentLevel; // Root level
        }

        // Find parent and calculate its level recursively
        for (TriggerItem parent : allItems) {
            if (parent != null && parent.getImgTag() != null &&
                    parent.getImgTag().equals(parentTag)) {
                return calculateLevel(parent, currentLevel + 1);
            }
        }
        return currentLevel; // Fallback if parent not found
    }


    // ViewHolder for category items (with expand/collapse triangle)
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

            // Set text from string resource
            try {
                int strResID = itemView.getContext().getResources()
                        .getIdentifier(triggerItem.getStrRes(), "string",
                                itemView.getContext().getPackageName());
                titleTV.setText(strResID);
            }
            catch (Exception e) {
                // Fallback: use the string resource name directly
                titleTV.setText(triggerItem.getStrRes());
            }

            // Set triangle rotation based on expanded state
            if(triggerItem.isExpanded()){
                triangleIV.setRotation(90);
            }
            else{
                triangleIV.setRotation(0);
            }

            // Update button state based on whether trigger is selected
            updatePlusButtonSrc(triggerItem.getImgTag());

            // Apply margins based on hierarchy level for visual indentation
            ViewGroup.MarginLayoutParams params=(ViewGroup.MarginLayoutParams) outerLayout.getLayoutParams();
            int horizMargin=28+(triggerItem.getLevel()*18);
            params.setMargins(dpToPx(horizMargin), dpToPx(18), 0, 0);
            outerLayout.setLayoutParams(params);

            // Set click listener for expand/collapse
            categoryLayout.setOnClickListener(v -> {
                if(listener!=null){
                    listener.onCategoryClick(triggerItem, position);
                }
            });
            plusBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        // Pass whether the trigger is currently selected
                        boolean isCurrentlySelected = isTriggerSelected(triggerItem.getImgTag());
                        listener.onTriggerClick(triggerItem, plusBtn, isCurrentlySelected);
                    }
                }
            });
        }

        private int dpToPx(int dp){
            return (int)(dp*itemView.getContext().getResources().getDisplayMetrics().density);
        }

        //Update the plus button to show plus or checkmark based on selection state
        private void updatePlusButtonSrc(String imgTag) {
            if (isTriggerSelected(imgTag)) {
                // Show checkmark for selected triggers
                //plusBtn.setBackground(ContextCompat.getDrawable(itemView.getContext(), R.drawable.check_mark));
                plusBtn.setImageResource(R.drawable.check_mark);
            } else {
                // Show plus for unselected triggers
                //plusBtn.setBackground(ContextCompat.getDrawable(itemView.getContext(), R.drawable.plus));
                plusBtn.setImageResource(R.drawable.plus);
            }
        }

    }

    // ViewHolder for trigger child items (with plus/checkmark button)
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

            // Set text from string resource
            try {
                int strResID = itemView.getContext().getResources()
                        .getIdentifier(triggerItem.getStrRes(), "string",
                                itemView.getContext().getPackageName());
                titleTV.setText(strResID);
            } catch (Exception e) {
                // Fallback: use the string resource name directly
                titleTV.setText(triggerItem.getStrRes());
            }

            // Update button state based on whether trigger is selected
            updatePlusButtonSrc(triggerItem.getImgTag());

            // Apply margins based on hierarchy level for visual indentation
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) triggerLayout.getLayoutParams();
            int margin = 46 + ((triggerItem.getLevel() - 1) * 18); // Deeper indentation for triggers
            params.setMargins(dpToPx(margin), dpToPx(18), dpToPx(28), 0);
            triggerLayout.setLayoutParams(params);

            // Set click listener for adding/removing trigger
            plusBtn.setOnClickListener(v -> {
                if (listener != null) {
                    // Pass whether the trigger is currently selected
                    boolean isCurrentlySelected = isTriggerSelected(triggerItem.getImgTag());
                    listener.onTriggerClick(triggerItem, plusBtn, isCurrentlySelected);
                }
            });
        }

        private int dpToPx(int dp) {
            return (int) (dp * itemView.getContext().getResources().getDisplayMetrics().density);
        }

        //Update the plus button to show plus or checkmark based on selection state
        private void updatePlusButtonSrc(String imgTag) {
            if (isTriggerSelected(imgTag)) {
                // Show checkmark for selected triggers
                //plusBtn.setBackground(ContextCompat.getDrawable(itemView.getContext(), R.drawable.check_mark));
                plusBtn.setImageResource(R.drawable.check_mark);
            } else {
                // Show plus for unselected triggers
                //plusBtn.setBackground(ContextCompat.getDrawable(itemView.getContext(), R.drawable.plus));
                plusBtn.setImageResource(R.drawable.plus);
            }
        }

    }

}
