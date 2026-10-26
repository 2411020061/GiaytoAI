package vn.edu.mku.ecoscan.ui.vault;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import vn.edu.mku.ecoscan.R;
import vn.edu.mku.ecoscan.data.AppDatabase;
import vn.edu.mku.ecoscan.data.DocumentDao;
import vn.edu.mku.ecoscan.data.DocumentEntity;
import vn.edu.mku.ecoscan.ui.MainActivity;
import vn.edu.mku.ecoscan.ui.detail.DocumentDetailActivity;

public class VaultFragment extends Fragment {

    private RecyclerView rvVaultDocuments;
    private DocumentAdapter adapter;
    private EditText etSearch;
    private TextView chipAll, chipActive, chipExpiring;
    private TextView tvDocumentCountLabel, tvVaultSubtitle;
    private TextView tvCountCccd;
    private View btnAddDocument, cardCategoryCccd;

    private List<DocumentEntity> allDocuments = new ArrayList<>();
    private String currentFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_vault, container, false);

        findViews(root);
        setupRecyclerView();
        setupListeners();
        loadDocuments();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDocuments();
    }

    private void findViews(View root) {
        rvVaultDocuments = root.findViewById(R.id.rvVaultDocuments);
        etSearch = root.findViewById(R.id.etSearch);

        chipAll = root.findViewById(R.id.chipAll);
        chipActive = root.findViewById(R.id.chipActive);
        chipExpiring = root.findViewById(R.id.chipExpiring);

        tvDocumentCountLabel = root.findViewById(R.id.tvDocumentCountLabel);
        tvVaultSubtitle = root.findViewById(R.id.tvVaultSubtitle);

        tvCountCccd = root.findViewById(R.id.tvCountCccd);
        btnAddDocument = root.findViewById(R.id.btnAddDocument);
        cardCategoryCccd = root.findViewById(R.id.cardCategoryCccd);
    }

    private void setupRecyclerView() {
        adapter = new DocumentAdapter(
                document -> {
                    Intent intent = new Intent(requireContext(), DocumentDetailActivity.class);
                    intent.putExtra("document_id", document.id);
                    startActivity(intent);
                },
                this::confirmAndDeleteDocument
        );
        rvVaultDocuments.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvVaultDocuments.setAdapter(adapter);
    }

    private void confirmAndDeleteDocument(DocumentEntity doc) {
        new AlertDialog.Builder(requireContext())
                .setTitle("🗑️ Xóa CCCD")
                .setMessage("Bạn có chắc chắn muốn xóa Căn cước công dân của \"" + doc.holderName + "\" khỏi kho lưu trữ?")
                .setPositiveButton("Xóa ngay", (dialog, which) -> deleteDocument(doc))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteDocument(DocumentEntity doc) {
        Executors.newSingleThreadExecutor().execute(() -> {
            if (doc.imagePath != null && !doc.imagePath.isEmpty()) {
                try {
                    File imgFile = new File(doc.imagePath);
                    if (imgFile.exists()) {
                        imgFile.delete();
                    }
                } catch (Exception e) {
                    // Ignore file delete errors
                }
            }

            DocumentDao dao = AppDatabase.getInstance(requireContext()).documentDao();
            dao.deleteById(doc.id);

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "🗑️ Đã xóa CCCD khỏi kho lưu trữ!", Toast.LENGTH_SHORT).show();
                    loadDocuments();
                });
            }
        });
    }

    private void setupListeners() {
        if (btnAddDocument != null) {
            btnAddDocument.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).switchToTab(R.id.nav_scan);
                }
            });
        }

        chipAll.setOnClickListener(v -> applyFilter("ALL"));
        chipActive.setOnClickListener(v -> applyFilter("ACTIVE"));
        chipExpiring.setOnClickListener(v -> applyFilter("EXPIRING"));

        if (cardCategoryCccd != null) {
            cardCategoryCccd.setOnClickListener(v -> applyFilter("ALL"));
        }

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void applyFilter(String filter) {
        this.currentFilter = filter;
        updateChipStyles();
        filterList(etSearch.getText().toString());
    }

    private void updateChipStyles() {
        setChipStyle(chipAll, "ALL".equals(currentFilter));
        setChipStyle(chipActive, "ACTIVE".equals(currentFilter));
        setChipStyle(chipExpiring, "EXPIRING".equals(currentFilter));
    }

    private void setChipStyle(TextView chip, boolean isActive) {
        if (chip == null) return;
        chip.setBackgroundResource(isActive ? R.drawable.badge_rounded_active : R.drawable.badge_rounded_outline);
        chip.setTextColor(isActive ? getResources().getColor(R.color.badge_active_text, null) : getResources().getColor(R.color.text_secondary, null));
    }

    private void loadDocuments() {
        Executors.newSingleThreadExecutor().execute(() -> {
            DocumentDao dao = AppDatabase.getInstance(requireContext()).documentDao();
            List<DocumentEntity> list = dao.getAll();

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    allDocuments = list != null ? list : new ArrayList<>();
                    if (tvCountCccd != null) {
                        tvCountCccd.setText(allDocuments.size() + " thẻ CCCD");
                    }
                    if (tvVaultSubtitle != null) {
                        tvVaultSubtitle.setText(allDocuments.size() + " Căn cước công dân • bảo mật mã hóa");
                    }
                    filterList(etSearch.getText().toString());
                });
            }
        });
    }

    private void filterList(String query) {
        List<DocumentEntity> filtered = new ArrayList<>();
        String queryLower = query != null ? query.toLowerCase().trim() : "";

        for (DocumentEntity doc : allDocuments) {
            boolean matchesFilter = true;

            if ("ACTIVE".equals(currentFilter)) {
                matchesFilter = "ACTIVE".equalsIgnoreCase(doc.status) && doc.daysLeft > 30;
            } else if ("EXPIRING".equals(currentFilter)) {
                matchesFilter = "EXPIRING_SOON".equalsIgnoreCase(doc.status) || doc.daysLeft <= 30;
            }

            boolean matchesSearch = queryLower.isEmpty()
                    || (doc.title != null && doc.title.toLowerCase().contains(queryLower))
                    || (doc.documentNumber != null && doc.documentNumber.toLowerCase().contains(queryLower))
                    || (doc.holderName != null && doc.holderName.toLowerCase().contains(queryLower));

            if (matchesFilter && matchesSearch) {
                filtered.add(doc);
            }
        }

        adapter.setData(filtered);
        tvDocumentCountLabel.setText(filtered.size() + " mục");
    }
}
