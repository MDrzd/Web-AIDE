package org.xedox.webaide.activity;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.AnimatorSet;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Window;
import android.view.WindowInsets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import java.io.File;

import androidx.activity.EdgeToEdge;
import androidx.core.content.FileProvider;
import android.webkit.MimeTypeMap;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatDelegate;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.Toolbar;
import java.io.IOException;
import android.database.Cursor;
import android.provider.OpenableColumns;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.xedox.webaide.IDE;
import org.xedox.webaide.R;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

public abstract class BaseActivity extends AppCompatActivity {

    protected static final String TAG = "BaseActivity";
    protected Toolbar toolbar;
    public View root;
    private SelectListener mSelectListener;

    private final ActivityResultLauncher<String[]> filePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.OpenDocument(),
                    uri -> {
                        if (uri != null) {
                            onFileSelected(uri);
                        }
                    });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        root = findViewById(android.R.id.content);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        if (isEdgeToEdgeEnabled()) {
            EdgeToEdge.enable(this);
            setupEdgeToEdge();
        } else {
            WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
            root.setPadding(0, 0, 0, 0);
        }

        IDE.init(this);
    }

    private void setupEdgeToEdge() {
        Window window = getWindow();
        View decorView = window.getDecorView();

        window.setNavigationBarColor(Color.TRANSPARENT);

        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.setNavigationBarContrastEnforced(false);
        }

        WindowCompat.setDecorFitsSystemWindows(window, false);

        // Only apply insets listener for edge-to-edge mode
        ViewCompat.setOnApplyWindowInsetsListener(
                root,
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(
                            systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                    return insets;
                });
    }

    private boolean isEdgeToEdgeEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return Settings.Global.getInt(getContentResolver(), "force_fsg_nav_bar", 0) == 1;
        }
        return false;
    }

    public static void removeSystemInsets(View view, OnSystemInsetsChangedListener listener) {
        ViewCompat.setOnApplyWindowInsetsListener(
                view,
                (v, insets) -> {
                    int desiredBottomInset =
                            calculateDesiredBottomInset(
                                    view,
                                    insets.getSystemWindowInsetTop(),
                                    insets.getSystemWindowInsetBottom(),
                                    listener);
                    return ViewCompat.onApplyWindowInsets(
                            view, insets.replaceSystemWindowInsets(0, 0, 0, desiredBottomInset));
                });
    }

    public static int calculateDesiredBottomInset(
            View view, int topInset, int bottomInset, OnSystemInsetsChangedListener listener) {
        boolean hasKeyboard = isKeyboardAppeared(view, bottomInset);
        int desiredBottomInset = hasKeyboard ? bottomInset : 0;
        listener.onSystemInsetsChanged(topInset, hasKeyboard ? 0 : bottomInset);
        return desiredBottomInset;
    }

    public interface OnSystemInsetsChangedListener {
        void onSystemInsetsChanged(int statusBarSize, int navigationBarSize);
    }

    private static boolean isKeyboardAppeared(View view, int bottomInset) {
        DisplayMetrics displayMetrics = view.getResources().getDisplayMetrics();
        double heightRatio = (double) bottomInset / displayMetrics.heightPixels;
        return heightRatio > 0.25;
    }

    private void setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(
                root,
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(
                            systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                    return insets;
                });
    }

    protected void loadToolbar() {
        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) setSupportActionBar(toolbar);
    }

    public void showSnackbar(int message) {
        Snackbar.make(root, message, Snackbar.LENGTH_SHORT)
                .setAction(R.string.ok, (v) -> {})
                .show();
    }

    public void showSnackbar(String message) {
        Snackbar.make(root, message, Snackbar.LENGTH_SHORT)
                .setAction(R.string.ok, (v) -> {})
                .show();
    }

    public void showSnackbar(Throwable message) {
        Snackbar.make(root, message.getLocalizedMessage(), Snackbar.LENGTH_SHORT)
                .setAction(R.string.ok, (v) -> {})
                .show();
    }

    public void showDialog(String message) {
        new MaterialAlertDialogBuilder(this).setMessage(message).create().show();
    }

    protected void setToolbarTitle(String newTitle) {
        if (toolbar.getTitle() != null && toolbar.getTitle().equals(newTitle)) return;
        View titleView = toolbar.getChildAt(0);
        if (titleView == null) {
            toolbar.setTitle(newTitle);
            return;
        }
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(titleView, "alpha", 1f, 0f);
        ObjectAnimator slideOut =
                ObjectAnimator.ofFloat(titleView, "translationY", 0f, titleView.getHeight() / 2f);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(titleView, "alpha", 0f, 1f);
        ObjectAnimator slideIn =
                ObjectAnimator.ofFloat(titleView, "translationY", -titleView.getHeight() / 2f, 0f);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(fadeOut).with(slideOut);
        animatorSet.play(fadeIn).with(slideIn).after(fadeOut);
        fadeOut.addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        toolbar.setTitle(newTitle);
                    }
                });
        animatorSet.setDuration(300).start();
    }

    public void setSubtitle(String title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(title);
        }
    }

    public void showFilePicker(SelectListener listener) {
        filePickerLauncher.launch(new String[] {"*/*"});
        mSelectListener = listener;
    }

    protected void onFileSelected(Uri uri) {
        if (mSelectListener != null) {
            try {
                String fileName = getFileName(uri);
                String fileContent = readFileContent(uri);
                mSelectListener.onSelect(uri, fileName, fileContent);
            } catch (Exception e) {
                mSelectListener.onSelect();
                showSnackbar("Error reading file: " + e.getMessage());
            }
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private String readFileContent(Uri uri) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
        }
        return stringBuilder.toString();
    }

    public interface SelectListener {
        void onSelect(Object... options);
    }

    public void openLinkInBrowser(String link) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(link));
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void openFileInExternalApp(File file) {
        try {
            String authority = getPackageName() + ".provider";
            Uri uri = FileProvider.getUriForFile(this, authority, file);

            String mime = getMimeType(file.getPath());
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mime);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getMimeType(String url) {
        String ext = MimeTypeMap.getFileExtensionFromUrl(url);
        if (ext == null) {
            return "*/*";
        }
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getMimeTypeFromExtension(ext.toLowerCase());
    }
}
