package vn.edu.mku.ecoscan.ui.history;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import vn.edu.mku.ecoscan.R;
import vn.edu.mku.ecoscan.data.AppDatabase;
import vn.edu.mku.ecoscan.data.DocumentDao;
import vn.edu.mku.ecoscan.data.DocumentEntity;
import vn.edu.mku.ecoscan.ui.detail.DocumentDetailActivity;
import vn.edu.mku.ecoscan.ui.vault.DocumentAdapter;

public class HistoryFragment extends Fragment {

    private RecyclerView recyclerHistory;
    private View layoutEmpty;
    private DocumentAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_history, container, false);

        recyclerHistory = root.findViewById(R.id.recyclerHistory);
        layoutEmpty = root.findViewById(R.id.layoutEmpty);

        setupRecyclerView();
        loadHistory();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHistory();
    }

    private void setupRecyclerView() {
        adapter = new DocumentAdapter(
                document -> {
                    Intent intent = new Intent(requireContext(), DocumentDetailActivity.class);
                    intent.putExtra("document_id", document.id);
                    startActivity(intent);
                },
                document -> {}
        );
        recyclerHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerHistory.setAdapter(adapter);
    }

    private void loadHistory() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            DocumentDao dao = db.documentDao();
            List<DocumentEntity> list = dao.getAll();

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    List<DocumentEntity> docs = list != null ? list : new ArrayList<>();
                    adapter.setData(docs);
                    if (docs.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        recyclerHistory.setVisibility(View.GONE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        recyclerHistory.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }
}
