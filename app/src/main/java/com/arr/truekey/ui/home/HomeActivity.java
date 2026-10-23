package com.arr.truekey.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.arr.truekey.AddAccountActivity;

import com.arr.truekey.adapters.OtpAdapter;
import com.arr.truekey.databinding.ActivityMainBinding;
import com.arr.truekey.ui.details.OtpDetailActivity;
import java.util.ArrayList;

public class HomeActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private OtpAdapter adapter;
    private HomeViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding.rvAccounts.setLayoutManager(new LinearLayoutManager(this));

        adapter =
                new OtpAdapter(
                        new ArrayList<>(),
                        account -> {
                            Intent intent = new Intent(HomeActivity.this, OtpDetailActivity.class);
                            intent.putExtra("account_id", account.id);
                            startActivity(intent);
                        });
        binding.rvAccounts.setAdapter(adapter);

        binding.fabAdd.setOnClickListener(
                v -> startActivity(new Intent(HomeActivity.this, AddAccountActivity.class)));

        // Se actualiza sola cada vez que la tabla cambia, sin necesidad de
        // volver a consultar manualmente en onResume().
        viewModel
                .getAccounts()
                .observe(
                        this,
                        accounts -> {
                            adapter.updateData(accounts);
                            binding.tvEmpty.setVisibility(
                                    accounts.isEmpty() ? View.VISIBLE : View.GONE);
                        });

        viewModel.getSecondsRemaining().observe(this, adapter::updateTick);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    @Override
    protected void onStart() {
        super.onStart();
        viewModel.startTicking();
    }

    @Override
    protected void onStop() {
        super.onStop();
        viewModel.stopTicking();
    }
}
