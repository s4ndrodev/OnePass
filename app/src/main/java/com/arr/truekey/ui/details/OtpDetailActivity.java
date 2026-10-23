package com.arr.truekey.ui.details;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.arr.truekey.AddAccountActivity;
import com.arr.truekey.databinding.ActivityOtpDetailBinding;
import com.arr.truekey.utils.room.entity.Account;
import com.arr.truekey.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class OtpDetailActivity extends AppCompatActivity {

    private ActivityOtpDetailBinding binding;
    private OtpDetailViewModel viewModel;
    private int accountId;
    private Account currentAccount;
    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOtpDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        accountId = getIntent().getIntExtra("account_id", -1);

        OtpDetailViewModelFactory factory =
                new OtpDetailViewModelFactory(getApplication(), accountId);
        viewModel = new ViewModelProvider(this, factory).get(OtpDetailViewModel.class);

        viewModel
                .getAccount()
                .observe(
                        this,
                        account -> {
                            if (account == null) return;
                            currentAccount = account;
                            binding.title.setText(account.label);
                            binding.tvPassword.setText(account.password);
                            binding.tvAliasDetail.setText(account.username);
                            updatePasswordText();
                            boolean hasOtp =
                                    account.otpSecret != null
                                            && !account.otpSecret.trim().isEmpty();
                            binding.copyOTP.setVisibility(hasOtp ? View.VISIBLE : View.GONE);
                            binding.dividerOtp.setVisibility(hasOtp ? View.VISIBLE : View.GONE);
                        });

        viewModel.getCode().observe(this, binding.tvCode::setText);
        binding.progressTimer.setMax(30);
        viewModel.getSecondsRemaining().observe(this, binding.progressTimer::setProgress);

        binding.btnShowPassword.setOnClickListener(v -> togglePasswordVisibility());
        binding.copyOTP.setOnClickListener(
                v ->
                        copyToClipboard(
                                binding.tvCode.getText().toString(),
                                "Copiado al portapapeles",
                                "Código OTP"));
        binding.copyPassword.setOnClickListener(
                v ->
                        copyToClipboard(
                                currentAccount.password,
                                "Copiado al portapapeles",
                                "Contraseña"));
        binding.copyAlias.setOnClickListener(
                v ->
                        copyToClipboard(
                                binding.tvAliasDetail.getText().toString(),
                                "Copiado al portapapeles",
                                "Usuario"));

        binding.btnEdit.setOnClickListener(
                v -> {
                    Intent intent = new Intent(this, AddAccountActivity.class);
                    intent.putExtra("account_id", accountId);
                    startActivity(intent);
                });

        binding.btnDelete.setOnClickListener(this::confirmDelete);
    }

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        updatePasswordText();
        binding.btnShowPassword.setIconResource(
                passwordVisible ? R.drawable.ic_hide_eye : R.drawable.ic_show_eye);
    }

    private void updatePasswordText() {
        if (currentAccount == null) return;
        binding.tvPassword.setText(
                passwordVisible ? currentAccount.password : maskPassword(currentAccount.password));
    }

    private String maskPassword(String password) {
        int length = (password == null || password.isEmpty()) ? 6 : password.length();
        StringBuilder dots = new StringBuilder();
        for (int i = 0; i < length; i++) dots.append("•");
        return dots.toString();
    }

    private void copyToClipboard(String displayedCode, String toastText, String plainText) {
        String cleanCode = displayedCode.replace(" ", "");
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(plainText, cleanCode));
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
            Toast.makeText(this, toastText, Toast.LENGTH_SHORT).show();
    }

    private void confirmDelete(View view) {
        if (currentAccount == null) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle("Eliminar cuenta")
                .setMessage("¿Seguro que quieres eliminar " + currentAccount.label + "?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton(
                        "Eliminar",
                        (dialog, which) -> viewModel.deleteAccount(currentAccount, this::finish))
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
