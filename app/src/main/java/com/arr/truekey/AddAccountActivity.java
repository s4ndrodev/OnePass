package com.arr.truekey;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.arr.truekey.databinding.ActivityAddAccountBinding;
import com.arr.truekey.utils.room.AppDatabase;
import com.arr.truekey.utils.room.entity.Account;
import com.arr.truekey.utils.security.SecurityUtils;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddAccountActivity extends AppCompatActivity {

    private ActivityAddAccountBinding binding;
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private int accountId = -1;
    private Account existingAccount;

    private final ActivityResultLauncher<ScanOptions> qrScanLauncher =
            registerForActivityResult(
                    new ScanContract(),
                    result -> {
                        if (result.getContents() != null) handleScannedText(result.getContents());
                    });

    private final ActivityResultLauncher<PickVisualMediaRequest> galleryLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.PickVisualMedia(),
                    uri -> {
                        if (uri != null) decodeQrFromGallery(uri);
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        char[] passphrase = SecurityUtils.getOrCreatePassphrase(this);
        db = AppDatabase.getInstance(this, passphrase);

        accountId = getIntent().getIntExtra("account_id", -1);
        if (accountId != -1) {
            loadExistingAccount();
            //     binding.btnDelete.setVisibility(View.VISIBLE);
        }

        binding.btnSave.setOnClickListener(v -> saveAccount());
        ///   binding.btnDelete.setOnClickListener(v -> deleteAccount());

        binding.btnGeneratePassword.setVisibility(View.GONE);
        binding.etPassword.setOnFocusChangeListener(
                (v, hasFocus) ->
                        binding.btnGeneratePassword.setVisibility(
                                hasFocus ? View.VISIBLE : View.GONE));

        binding.btnGeneratePassword.setOnClickListener(
                v -> binding.etPassword.setText(generatePassword(16)));

        binding.btnScanQr.setOnClickListener(
                v -> {
                    ScanOptions options = new ScanOptions();
                    options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
                    options.setPrompt("Escanea el código QR de tu cuenta");
                    options.setBeepEnabled(false);
                    options.setOrientationLocked(false); // permite modo vertical
                    qrScanLauncher.launch(options);
                });

        binding.btnPickQr.setOnClickListener(
                v ->
                        galleryLauncher.launch(
                                new PickVisualMediaRequest.Builder()
                                        .setMediaType(
                                                ActivityResultContracts.PickVisualMedia.ImageOnly
                                                        .INSTANCE)
                                        .build()));
    }

    private void loadExistingAccount() {
        executor.execute(
                () -> {
                    existingAccount = db.accountDao().getById(accountId);
                    runOnUiThread(
                            () -> {
                                if (existingAccount == null) return;
                                binding.etLabel.setText(existingAccount.label);
                                binding.etUsername.setText(existingAccount.username);
                                binding.etPassword.setText(existingAccount.password);
                                binding.etOtpSecret.setText(existingAccount.otpSecret);
                            });
                });
    }

    private void saveAccount() {
        String label = binding.etLabel.getText().toString().trim();
        String username = binding.etUsername.getText().toString().trim();
        String password = binding.etPassword.getText().toString();
        String otpSecret = binding.etOtpSecret.getText().toString().trim().replace(" ", "");

        if (label.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, "Completa nombre, usuario y secreto OTP", Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        Account account =
                (accountId != -1 && existingAccount != null) ? existingAccount : new Account();

        account.label = label;
        account.username = username;
        account.password = password;
        account.otpSecret = otpSecret;

        executor.execute(
                () -> {
                    if (accountId != -1) {
                        db.accountDao().update(account);
                    } else {
                        db.accountDao().insert(account);
                    }
                    runOnUiThread(this::finish);
                });
    }

    private void deleteAccount() {
        if (existingAccount == null) return;
        executor.execute(
                () -> {
                    db.accountDao().delete(existingAccount);
                    runOnUiThread(this::finish);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    private String generatePassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*-_";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View focused = getCurrentFocus();
            if (focused instanceof EditText) {
                int x = (int) ev.getRawX();
                int y = (int) ev.getRawY();
                if (!isInside(focused, x, y) && !isInside(binding.btnGeneratePassword, x, y)) {
                    focused.clearFocus();
                    InputMethodManager imm =
                            (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private boolean isInside(View view, int rawX, int rawY) {
        Rect rect = new Rect();
        view.getGlobalVisibleRect(rect);
        return rect.contains(rawX, rawY);
    }

    private void decodeQrFromGallery(Uri uri) {
        executor.execute(
                () -> {
                    String text = null;
                    try {
                        Bitmap bitmap = loadScaledBitmap(uri, 1600);
                        int w = bitmap.getWidth(), h = bitmap.getHeight();
                        int[] pixels = new int[w * h];
                        bitmap.getPixels(pixels, 0, w, 0, 0, w, h);

                        BinaryBitmap binary =
                                new BinaryBitmap(
                                        new HybridBinarizer(new RGBLuminanceSource(w, h, pixels)));
                        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
                        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
                        hints.put(
                                DecodeHintType.POSSIBLE_FORMATS,
                                Collections.singletonList(BarcodeFormat.QR_CODE));

                        text = new QRCodeReader().decode(binary, hints).getText();
                    } catch (Exception e) {
                        // no se encontró un QR legible: text queda en null
                    }
                    String result = text;
                    runOnUiThread(
                            () -> {
                                if (result != null) handleScannedText(result);
                                else
                                    Toast.makeText(
                                                    this,
                                                    "No se encontró un QR en la imagen",
                                                    Toast.LENGTH_SHORT)
                                            .show();
                            });
                });
    }

    // Reduce imágenes grandes para no quedarte sin memoria
    private Bitmap loadScaledBitmap(Uri uri, int maxSide) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, bounds);
        }
        int sample = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / sample > maxSide) sample *= 2;

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            return BitmapFactory.decodeStream(in, null, opts);
        }
    }

    private void handleScannedText(String text) {
        Uri uri = Uri.parse(text.trim());
        if (!"otpauth".equalsIgnoreCase(uri.getScheme())) {
            toast("El QR no es un código OTP válido");
            return;
        }
        if (!"totp".equalsIgnoreCase(uri.getHost())) {
            toast("Solo se admiten códigos TOTP");
            return;
        }
        String secret = uri.getQueryParameter("secret");
        if (secret == null || secret.trim().isEmpty()) {
            toast("El QR no contiene un secreto");
            return;
        }

        // La app genera códigos con SHA1 / 6 dígitos / 30 s
        String algorithm = uri.getQueryParameter("algorithm");
        String digits = uri.getQueryParameter("digits");
        String period = uri.getQueryParameter("period");
        boolean standard =
                (algorithm == null || "SHA1".equalsIgnoreCase(algorithm))
                        && (digits == null || "6".equals(digits))
                        && (period == null || "30".equals(period));
        if (!standard) {
            toast("Este QR usa parámetros que la app aún no soporta");
            return;
        }

        String path = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");
        String issuer = uri.getQueryParameter("issuer");
        String account = path;
        int colon = path.indexOf(':');
        if (colon >= 0) {
            if (issuer == null || issuer.isEmpty()) issuer = path.substring(0, colon);
            account = path.substring(colon + 1);
        }

        binding.etOtpSecret.setText(secret.replace(" ", "").toUpperCase());
        if (binding.etLabel.getText().toString().trim().isEmpty() && issuer != null)
            binding.etLabel.setText(issuer.trim());
        if (binding.etUsername.getText().toString().trim().isEmpty())
            binding.etUsername.setText(account.trim());
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
