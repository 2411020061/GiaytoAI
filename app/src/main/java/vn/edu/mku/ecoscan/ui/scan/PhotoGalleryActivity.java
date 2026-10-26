package vn.edu.mku.ecoscan.ui.scan;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import vn.edu.mku.ecoscan.R;

public class PhotoGalleryActivity extends AppCompatActivity {

    public static final String EXTRA_SELECTED_IMAGE_PATH = "selected_image_path";

    private RecyclerView rvPhotos;
    private View layoutEmpty, btnBack, btnOpenSystemGallery;
    private TextView tvGallerySubtitle;

    private PhotoAdapter adapter;
    private final List<File> photoFiles = new ArrayList<>();

    private final ActivityResultLauncher<Intent> systemGalleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        Intent data = new Intent();
                        data.setData(uri);
                        setResult(RESULT_OK, data);
                        finish();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_gallery);

        findViews();
        setupListeners();
        setupRecyclerView();
        loadLocalPhotos();
    }

    private void findViews() {
        rvPhotos = findViewById(R.id.rvPhotos);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        btnBack = findViewById(R.id.btnBack);
        btnOpenSystemGallery = findViewById(R.id.btnOpenSystemGallery);
        tvGallerySubtitle = findViewById(R.id.tvGallerySubtitle);
    }

    private void setupListeners() {
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (btnOpenSystemGallery != null) {
            btnOpenSystemGallery.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                systemGalleryLauncher.launch(intent);
            });
        }
    }

    private void setupRecyclerView() {
        adapter = new PhotoAdapter(
                photoFile -> {
                    Intent data = new Intent();
                    data.putExtra(EXTRA_SELECTED_IMAGE_PATH, photoFile.getAbsolutePath());
                    data.setData(Uri.fromFile(photoFile));
                    setResult(RESULT_OK, data);
                    finish();
                },
                photoFile -> confirmAndDeletePhoto(photoFile)
        );
        rvPhotos.setLayoutManager(new GridLayoutManager(this, 3));
        rvPhotos.setAdapter(adapter);
    }

    private void loadLocalPhotos() {
        photoFiles.clear();

        // 1. Thư mục lưu ảnh quét GiayToAI
        File giayToAiDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "GiayToAI"
        );
        if (giayToAiDir.exists() && giayToAiDir.isDirectory()) {
            File[] files = giayToAiDir.listFiles((dir, name) ->
                    name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpeg")
            );
            if (files != null) {
                photoFiles.addAll(Arrays.asList(files));
            }
        }

        // 2. Thư mục EcoScan
        File ecoScanDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "EcoScan"
        );
        if (ecoScanDir.exists() && ecoScanDir.isDirectory()) {
            File[] files = ecoScanDir.listFiles((dir, name) ->
                    name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpeg")
            );
            if (files != null) {
                photoFiles.addAll(Arrays.asList(files));
            }
        }

        // Sắp xếp mới nhất lên đầu
        Collections.sort(photoFiles, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

        adapter.setData(photoFiles);

        if (photoFiles.isEmpty()) {
            rvPhotos.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
            tvGallerySubtitle.setText("Chưa có ảnh chụp");
        } else {
            rvPhotos.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
            tvGallerySubtitle.setText(photoFiles.size() + " ảnh • Bấm 🗑️ để xóa ảnh");
        }
    }

    private void confirmAndDeletePhoto(File photoFile) {
        new AlertDialog.Builder(this)
                .setTitle("🗑️ Xóa ảnh khỏi máy")
                .setMessage("Bạn có chắc chắn muốn xóa tệp ảnh \"" + photoFile.getName() + "\" vĩnh viễn khỏi thiết bị?")
                .setPositiveButton("Xóa ngay", (dialog, which) -> deletePhoto(photoFile))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deletePhoto(File photoFile) {
        try {
            boolean deleted = photoFile.delete();
            if (deleted || !photoFile.exists()) {
                // Thông báo cho Android MediaStore để cập nhật lại hệ thống
                MediaScannerConnection.scanFile(
                        this,
                        new String[]{photoFile.getAbsolutePath()},
                        null,
                        null
                );

                Toast.makeText(this, "🗑️ Đã xóa ảnh thành công!", Toast.LENGTH_SHORT).show();
                loadLocalPhotos();
            } else {
                Toast.makeText(this, "Không thể xóa tệp ảnh", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi khi xóa tệp: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Inner Adapter Class for Photo Grid
    private static class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.ViewHolder> {

        interface OnPhotoClickListener {
            void onPhotoClick(File photoFile);
        }

        interface OnPhotoDeleteListener {
            void onPhotoDelete(File photoFile);
        }

        private List<File> list = new ArrayList<>();
        private final OnPhotoClickListener clickListener;
        private final OnPhotoDeleteListener deleteListener;

        public PhotoAdapter(OnPhotoClickListener clickListener, OnPhotoDeleteListener deleteListener) {
            this.clickListener = clickListener;
            this.deleteListener = deleteListener;
        }

        public void setData(List<File> newList) {
            this.list = newList != null ? newList : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_photo_grid, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            File photoFile = list.get(position);
            holder.tvPhotoName.setText(photoFile.getName());

            Picasso.get()
                    .load(photoFile)
                    .resize(200, 200)
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.imgThumbnail);

            holder.itemView.setOnClickListener(v -> {
                if (clickListener != null) clickListener.onPhotoClick(photoFile);
            });

            if (holder.btnDeletePhoto != null) {
                holder.btnDeletePhoto.setOnClickListener(v -> {
                    if (deleteListener != null) deleteListener.onPhotoDelete(photoFile);
                });
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imgThumbnail;
            TextView tvPhotoName;
            View btnDeletePhoto;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                imgThumbnail = itemView.findViewById(R.id.imgThumbnail);
                tvPhotoName = itemView.findViewById(R.id.tvPhotoName);
                btnDeletePhoto = itemView.findViewById(R.id.btnDeletePhoto);
            }
        }
    }
}
