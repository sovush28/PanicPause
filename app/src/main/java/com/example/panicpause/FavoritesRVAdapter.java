package com.example.panicpause;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.List;

public class FavoritesRVAdapter extends RecyclerView.Adapter<FavoritesRVAdapter.FavoriteViewHolder> {
    private final List<DataManager.PhotoData> photos;
    private final Context context;
    private final DataManager dataManager;

    public FavoritesRVAdapter(List<DataManager.PhotoData> photos, Context context, DataManager dataManager) {
        this.photos = photos;
        this.context = context;
        this.dataManager = dataManager;
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.favorites_rv_item, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        DataManager.PhotoData photo = photos.get(position);

        // Загружаем фото
        loadPhoto(photo, holder.photoIV);

        // Обработка клика на info
        holder.photoIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FavsPhotoDetailsDialogFragment dialog = FavsPhotoDetailsDialogFragment.newInstance(
                        photo,
                        dataManager,
                        deletedPhoto -> {
                            // Найдём позицию удалённого элемента
                            int position = photos.indexOf(deletedPhoto);
                            if (position != -1) {
                                photos.remove(position);
                                notifyItemRemoved(position);
                            }
                        }
                        // лямбда-выражение реализует интерфейс OnFavDeletedListener
                );
                dialog.show(((AppCompatActivity) context).getSupportFragmentManager(), "favs_photo_details");
            }
        });

        // Обработка клика на сердечко (удаление из избранных)
        holder.heartIB.setOnClickListener(v -> {
            showDeleteFaveConfirmationDialog(photo, position);
        });
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    // ViewHolder и loadPhoto аналогичны существующим

    // Загружает фото в изображение с помощью Glide.
    // Сначала пытаемся загрузить локальный файл, если его нет — загружаем по URL.
    private void loadPhoto(DataManager.PhotoData photo, com.google.android.material.imageview.ShapeableImageView imageView) {
        // Получаем имя файла из URL
        String filename = DataManager.getFilenameFromUrl(photo.imgUrl);
        if (filename == null) {
            return;
        }

        // Проверяем наличие локального файла
        File photoFile = new File(context.getFilesDir(), "photos/" + filename);

        if (photoFile.exists()) {
            // Загружаем локальный файл
            Glide.with(context)
                    .load(photoFile)
                    //.placeholder(R.drawable.placeholder_image) // Заглушка при загрузке
                    //.error(R.drawable.error_image) // Изображение при ошибке
                    .into(imageView);
        } else {
            // Загружаем по интернету (если есть)
            Glide.with(context)
                    .load(photo.imgUrl)
                    //.placeholder(R.drawable.placeholder_image)
                    //.error(R.drawable.error_image)
                    .into(imageView);
        }
    }


    static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        com.google.android.material.imageview.ShapeableImageView photoIV;
        //ImageView infoIV;
        ImageButton heartIB;

        FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            photoIV=itemView.findViewById(R.id.favorites_rv_iv);
            //infoIV=itemView.findViewById(R.id.favs_photo_info_iv);
            heartIB=itemView.findViewById(R.id.favs_heart_ib);
        }
    }

    private void showDeleteFaveConfirmationDialog(DataManager.PhotoData photo, int position) {
        try{
            DeleteFromFavesDialogFragment dialog = new DeleteFromFavesDialogFragment();
            dialog.setOnDeleteFaveListener(new DeleteFromFavesDialogFragment.OnDeleteFaveListener() {
                @Override
                public void onDeleteFaveConfirmed() { // Пользователь подтвердил удаление из избр
                    List<String> faves = dataManager.getFaves();
                    faves.remove(photo.imgUrl);
                    dataManager.saveFaves(faves);

                    int actualPosition = photos.indexOf(photo);
                    if (actualPosition != -1) {
                        // Удаляем из списка и обновляем
                        photos.remove(actualPosition);
                        notifyItemRemoved(actualPosition);
                        if(photos.isEmpty())
                            Toast.makeText(context, context.getString(R.string.no_favs), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onDeleteFaveCancelled() {
                    // Пользователь отменил удаление
                    dialog.dismiss();
                }
            });

            dialog.show(((AppCompatActivity) context).getSupportFragmentManager(), "delete_fave_dialog");
        }
        catch(IllegalStateException ex){
            // Обработка случая, когда Activity уничтожается
            Log.e("Dialog", "Cannot show dialog - activity state invalid");
        }
    }


}
