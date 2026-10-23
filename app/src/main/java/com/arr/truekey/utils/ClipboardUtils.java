package com.arr.truekey.utils;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.PersistableBundle;
import android.widget.Toast;

public class ClipboardUtils {

    public static void copyOtp(Context context, String displayedCode) {
        String clean = displayedCode.replace(" ", "");
        if (clean.isEmpty() || clean.contains("-")) return;

        ClipData clip = ClipData.newPlainText("Código OTP", clean);

        // Android 13+: oculta el contenido en la vista previa del portapapeles
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PersistableBundle extras = new PersistableBundle();
            extras.putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true);
            clip.getDescription().setExtras(extras);
        }

        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(clip);

        // Desde Android 13 el sistema ya muestra su propia confirmación
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, "Código copiado", Toast.LENGTH_SHORT).show();
        }
    }
}
