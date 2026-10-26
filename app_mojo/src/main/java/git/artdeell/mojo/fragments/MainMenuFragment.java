package git.artdeell.mojo.fragments;

import static git.artdeell.mojo.Tools.openPath;
import static git.artdeell.mojo.Tools.shareLog;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import androidx.activity.result.contract.ActivityResultContracts;
import android.app.ActivityManager;
import android.content.Intent;
import android.text.format.Formatter;
import android.widget.SeekBar;
import android.widget.TextView;
import git.artdeell.mojo.prefs.LauncherPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kdt.mcgui.mcVersionSpinner;

import git.artdeell.mojo.CustomControlsActivity;
import git.artdeell.mojo.R;

import git.artdeell.mojo.Architecture;
import git.artdeell.mojo.Tools;
import git.artdeell.mojo.contracts.OpenDocumentWithExtension;
import git.artdeell.mojo.extra.ExtraConstants;
import git.artdeell.mojo.extra.ExtraCore;
import git.artdeell.mojo.instances.Instance;
import git.artdeell.mojo.instances.Instances;
import git.artdeell.mojo.progresskeeper.ProgressKeeper;
import git.artdeell.mojo.utils.FileUtils;

import java.io.File;

public class MainMenuFragment extends Fragment {
    public static final String TAG = "MainMenuFragment";

    private mcVersionSpinner mVersionSpinner;

    private final ActivityResultLauncher<Object> mModInstallerLauncher =
            registerForActivityResult(new OpenDocumentWithExtension("jar"), (data)->{
                if(data != null) Tools.launchModInstaller(requireContext(), data);
            });

    private final ActivityResultLauncher<String> mBackgroundLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if(uri != null) saveBackground(uri);
            });

    public MainMenuFragment(){
        super(R.layout.fragment_launcher);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Button mNewsButton = view.findViewById(R.id.news_button);
        Button mDiscordButton = view.findViewById(R.id.social_media_button);
        Button mCustomControlButton = view.findViewById(R.id.custom_control_button);
        Button mInstallJarButton = view.findViewById(R.id.install_jar_button);
        Button mShareLogsButton = view.findViewById(R.id.share_logs_button);
        Button mOpenDirectoryButton = view.findViewById(R.id.open_files_button);

        ImageButton mEditProfileButton = view.findViewById(R.id.edit_profile_button);
        Button mPlayButton = view.findViewById(R.id.play_button);
        mVersionSpinner = view.findViewById(R.id.mc_version_spinner);

        view.findViewById(R.id.versions_button).setOnClickListener(v ->
                Tools.swapFragment(requireActivity(), ProfileTypeSelectFragment.class, ProfileTypeSelectFragment.TAG, null));
        view.findViewById(R.id.modpacks_button).setOnClickListener(v ->
                Tools.swapFragment(requireActivity(), SearchModFragment.class, SearchModFragment.TAG, null));
        showSystemInfo((TextView) view.findViewById(R.id.system_info_text));
        setupPerformanceCard(view);
        view.findViewById(R.id.hero_card).setOnClickListener(v -> mBackgroundLauncher.launch("image/*"));
        applyCustomBackground(view);

        mNewsButton.setOnClickListener(v -> Tools.openURL(requireActivity(), Tools.URL_HOME));
        mDiscordButton.setOnClickListener(v -> Tools.openURL(requireActivity(), getString(R.string.social_media_invite)));
        mCustomControlButton.setOnClickListener(v -> startActivity(new Intent(requireContext(), CustomControlsActivity.class)));
        mInstallJarButton.setOnClickListener(v -> runInstallerWithConfirmation());
        mEditProfileButton.setOnClickListener(v -> mVersionSpinner.openProfileEditor(requireActivity()));

        mPlayButton.setOnClickListener(v -> ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true));

        mShareLogsButton.setOnClickListener((v) -> shareLog(requireContext()));

        mOpenDirectoryButton.setOnClickListener((v)-> openGameDirectory(v.getContext()));


        mNewsButton.setOnLongClickListener((v)->{
            Tools.swapFragment(requireActivity(), GamepadMapperFragment.class, GamepadMapperFragment.TAG, null);
            return true;
        });
    }

    private File backgroundFile() {
        return new File(requireContext().getFilesDir(), "coba_background.jpg");
    }

    private void saveBackground(Uri uri) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (java.io.InputStream in = requireContext().getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(in, null, bounds);
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = Math.max(1, Math.max(bounds.outWidth, bounds.outHeight) / 1600);
            Bitmap bitmap;
            try (java.io.InputStream in = requireContext().getContentResolver().openInputStream(uri)) {
                bitmap = BitmapFactory.decodeStream(in, null, options);
            }
            if(bitmap == null) return;
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(backgroundFile())) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out);
            }
            applyCustomBackground(getView());
        } catch (java.io.IOException e) {
            Tools.showError(requireContext(), e);
        }
    }

    private void applyCustomBackground(View view) {
        if(view == null) return;
        File file = backgroundFile();
        if(!file.exists()) return;
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        if(bitmap == null) return;
        Drawable image = new BitmapDrawable(getResources(), bitmap);
        Drawable shade = new ColorDrawable(Color.argb(150, 7, 10, 31));
        view.findViewById(R.id.fragment_menu_main).setBackground(new LayerDrawable(new Drawable[]{image, shade}));
    }

    private int maxRamMb() {
        int deviceRam = Tools.getTotalDeviceMemory(requireContext());
        if(Architecture.is32BitsDevice() || deviceRam < 2048) return Math.min(1024, deviceRam);
        return deviceRam - (deviceRam < 3064 ? 800 : 1024);
    }

    private void applyPreset(float ramFraction, int resolution, boolean aggressive) {
        int ram = Math.max(512, (int) (maxRamMb() * ramFraction));
        LauncherPreferences.DEFAULT_PREF.edit()
                .putInt("allocation", ram)
                .putInt("resolutionRatio", resolution)
                .putBoolean("sustainedPerformance", aggressive)
                .putBoolean("alternate_surface", true)
                .putBoolean("bigCoreAffinity", aggressive)
                .putBoolean("force_vsync", !aggressive)
                .apply();
        LauncherPreferences.loadPreferences(requireContext());
        refreshPerformanceViews(getView());
        Toast.makeText(requireContext(), R.string.coba_preset_applied, Toast.LENGTH_SHORT).show();
    }

    private void refreshPerformanceViews(View view) {
        if(view == null) return;
        SeekBar ram = view.findViewById(R.id.ram_seekbar);
        SeekBar res = view.findViewById(R.id.res_seekbar);
        ram.setProgress(LauncherPreferences.PREF_RAM_ALLOCATION - 256);
        res.setProgress(Math.round(LauncherPreferences.PREF_SCALE_FACTOR * 100) - 25);
        ((TextView) view.findViewById(R.id.ram_label)).setText(getString(R.string.coba_ram_label, LauncherPreferences.PREF_RAM_ALLOCATION));
        ((TextView) view.findViewById(R.id.res_label)).setText(getString(R.string.coba_res_label, Math.round(LauncherPreferences.PREF_SCALE_FACTOR * 100)));
    }

    private void setupPerformanceCard(View view) {
        SeekBar ram = view.findViewById(R.id.ram_seekbar);
        SeekBar res = view.findViewById(R.id.res_seekbar);
        ram.setMax(Math.max(1, maxRamMb() - 256));
        res.setMax(175);
        refreshPerformanceViews(view);
        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if(!fromUser) return;
                boolean isRam = bar.getId() == R.id.ram_seekbar;
                int value = isRam ? progress + 256 : progress + 25;
                ((TextView) view.findViewById(isRam ? R.id.ram_label : R.id.res_label)).setText(
                        isRam ? getString(R.string.coba_ram_label, value) : getString(R.string.coba_res_label, value));
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {
                LauncherPreferences.DEFAULT_PREF.edit()
                        .putInt(bar.getId() == R.id.ram_seekbar ? "allocation" : "resolutionRatio",
                                bar.getId() == R.id.ram_seekbar ? bar.getProgress() + 256 : bar.getProgress() + 25)
                        .apply();
                LauncherPreferences.loadPreferences(requireContext());
            }
        };
        ram.setOnSeekBarChangeListener(listener);
        res.setOnSeekBarChangeListener(listener);
        view.findViewById(R.id.preset_balanced).setOnClickListener(v -> applyPreset(0.5f, 100, false));
        view.findViewById(R.id.preset_performance).setOnClickListener(v -> applyPreset(0.7f, 85, true));
        view.findViewById(R.id.preset_extreme).setOnClickListener(v -> applyPreset(0.9f, 70, true));
    }

    private void showSystemInfo(TextView target) {
        ActivityManager am = (ActivityManager) requireContext().getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(info);
        target.setText(getString(R.string.coba_system_info,
                Formatter.formatFileSize(requireContext(), info.availMem),
                Formatter.formatFileSize(requireContext(), info.totalMem),
                android.os.Build.VERSION.RELEASE));
    }

    private void openGameDirectory(Context context) {
        Instance instance = Instances.loadSelectedInstance();
        if(instance == null) {
            Toast.makeText(context, R.string.no_instance, Toast.LENGTH_LONG).show();
            return;
        }
        File gameDirectory = instance.getGameDirectory();
        if(FileUtils.ensureDirectorySilently(gameDirectory)) {
            openPath(context, gameDirectory, false);
        }else {
            Toast.makeText(context, R.string.gamedir_open_failed, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ExtraCore.setValue(ExtraConstants.REFRESH_ACCOUNT_SPINNER, true);
    }

    private void runInstallerWithConfirmation() {
        if (ProgressKeeper.getTaskCount() == 0) {
            mModInstallerLauncher.launch(null);
        } else Toast.makeText(requireContext(), R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
    }
}
