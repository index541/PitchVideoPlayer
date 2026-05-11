package com.kento.pitchvideoplayer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import java.util.Locale;

/**
 * Pitch Video Player
 *
 * ローカル動画/音声・直接URLをExoPlayerで再生し、速度を保ったままピッチ変更できます。
 * YouTube/ニコニコはWebView再生モードとして分離しています。
 *
 * 注意：YouTube/ニコニコのWebView再生音声は、規約・技術制限によりこの画面のピッチ変更対象外です。
 */
public class MainActivity extends Activity {

    private static final int REQ_OPEN_MEDIA = 1001;

    // ===== 調整しやすい設定値 =====
    private static final float PITCH_MIN_SEMITONE = -12.0f; // ← ピッチ変更の最小値（半音）
    private static final float PITCH_MAX_SEMITONE = 12.0f;  // ← ピッチ変更の最大値（半音）
    private static final float SPEED_MIN = 0.50f;           // ← 再生速度の最小値
    private static final float SPEED_MAX = 2.00f;           // ← 再生速度の最大値
    private static final int SLIDER_SCALE = 10;             // ← ピッチスライダー精度。10なら0.1半音単位

    // ===== UI配色 =====
    private static final int COLOR_BG = Color.rgb(15, 23, 42);
    private static final int COLOR_PANEL = Color.rgb(30, 41, 59);
    private static final int COLOR_PANEL_2 = Color.rgb(17, 24, 39);
    private static final int COLOR_TEXT = Color.WHITE;
    private static final int COLOR_SUB_TEXT = Color.rgb(203, 213, 225);
    private static final int COLOR_ACCENT = Color.rgb(56, 189, 248);
    private static final int COLOR_WARNING = Color.rgb(251, 191, 36);

    private ExoPlayer player;
    private PlayerView playerView;
    private WebView webView;

    private LinearLayout playerArea;
    private LinearLayout webArea;
    private TextView statusText;
    private TextView pitchText;
    private TextView speedText;
    private TextView volumeText;
    private TextView currentSourceText;
    private SeekBar pitchSeekBar;
    private SeekBar speedSeekBar;
    private SeekBar volumeSeekBar;

    private float currentSemitone = 0.0f;
    private float currentSpeed = 1.0f;
    private float currentVolume = 1.0f;

    private boolean youtubeNoticeShown = false;
    private boolean nicoNoticeShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializePlayer();
        setContentView(createContentView());
        applyPlaybackParameters();
        setStatus("準備完了：ローカルファイル、または直接URLを開いてください。", false);
    }

    private void initializePlayer() {
        player = new ExoPlayer.Builder(this).build();
        player.setRepeatMode(Player.REPEAT_MODE_OFF);
        player.setVolume(currentVolume);
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {
                setStatus("再生エラー：" + safeMessage(error), true);
            }
        });
    }

    private String safeMessage(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null) {
            return "詳細不明";
        }
        return throwable.getMessage();
    }

    private View createContentView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);
        root.setPadding(dp(14), dp(14), dp(14), dp(14));

        root.addView(createHeader());
        root.addView(createTabBar());
        root.addView(createMainContainer());
        root.addView(createStatusPanel());

        return root;
    }

    private View createHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(16), dp(14), dp(16), dp(14));
        header.setBackground(rounded(COLOR_PANEL, 20));

        TextView title = new TextView(this);
        title.setText("Pitch Video Player");
        title.setTextColor(COLOR_TEXT);
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);

        TextView subtitle = new TextView(this);
        subtitle.setText("ローカル動画/音声・直接URLを、速度を保ったままピッチ変更再生します。");
        subtitle.setTextColor(COLOR_SUB_TEXT);
        subtitle.setTextSize(13);
        subtitle.setPadding(0, dp(4), 0, 0);

        header.addView(title);
        header.addView(subtitle);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, dp(10));
        header.setLayoutParams(lp);
        return header;
    }

    private View createTabBar() {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        row.addView(makeButton("プレイヤー", v -> showPlayerMode()));
        row.addView(makeButton("ファイルを開く", v -> openLocalMediaPicker()));
        row.addView(makeButton("URLを開く", v -> showUrlDialog()));
        row.addView(makeButton("YouTube", v -> showYouTubeMode()));
        row.addView(makeButton("ニコニコ", v -> showNicoMode()));
        row.addView(makeButton("±0に戻す", v -> resetPitchAndSpeed()));

        scroll.addView(row);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, dp(10));
        scroll.setLayoutParams(lp);
        return scroll;
    }

    private Button makeButton(String text, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(COLOR_TEXT);
        button.setTextSize(13);
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setBackground(rounded(COLOR_PANEL, 999));
        button.setOnClickListener(listener);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(42)
        );
        lp.setMargins(0, 0, dp(8), 0);
        button.setLayoutParams(lp);
        return button;
    }

    private View createMainContainer() {
        FrameLayout container = new FrameLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        );
        lp.setMargins(0, 0, 0, dp(10));
        container.setLayoutParams(lp);

        playerArea = createPlayerArea();
        webArea = createWebArea();
        webArea.setVisibility(View.GONE);

        container.addView(playerArea);
        container.addView(webArea);
        return container;
    }

    private LinearLayout createPlayerArea() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(rounded(COLOR_PANEL_2, 20));
        root.setPadding(dp(12), dp(12), dp(12), dp(12));

        playerView = new PlayerView(this);
        playerView.setPlayer(player);
        playerView.setUseController(true);
        playerView.setControllerShowTimeoutMs(3000);
        playerView.setBackgroundColor(Color.BLACK);

        LinearLayout.LayoutParams playerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        );
        playerLp.setMargins(0, 0, 0, dp(10));
        playerView.setLayoutParams(playerLp);
        root.addView(playerView);

        currentSourceText = new TextView(this);
        currentSourceText.setText("再生元：未選択");
        currentSourceText.setTextColor(COLOR_SUB_TEXT);
        currentSourceText.setTextSize(12);
        currentSourceText.setSingleLine(false);
        currentSourceText.setPadding(dp(4), 0, dp(4), dp(8));
        root.addView(currentSourceText);

        root.addView(createControlPanel());
        return root;
    }

    private View createControlPanel() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(12), dp(10), dp(12), dp(10));
        panel.setBackground(rounded(COLOR_PANEL, 18));

        pitchText = makeControlLabel("ピッチ：±0.0 半音");
        panel.addView(pitchText);
        panel.addView(makePitchSeekBar());

        speedText = makeControlLabel("速度：1.00x");
        panel.addView(speedText);
        panel.addView(makeSpeedSeekBar());

        volumeText = makeControlLabel("音量：100%");
        panel.addView(volumeText);
        panel.addView(makeVolumeSeekBar());

        TextView note = new TextView(this);
        note.setText("※ このピッチ変更は、アプリ内プレイヤーで再生している音声にのみ適用されます。YouTube/ニコニコのWebView再生音には適用されません。");
        note.setTextColor(COLOR_WARNING);
        note.setTextSize(12);
        note.setPadding(0, dp(8), 0, 0);
        panel.addView(note);

        scroll.addView(panel);
        return scroll;
    }

    private TextView makeControlLabel(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(COLOR_TEXT);
        label.setTextSize(15);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setPadding(0, dp(6), 0, dp(2));
        return label;
    }

    private SeekBar makePitchSeekBar() {
        SeekBar seekBar = new SeekBar(this);
        pitchSeekBar = seekBar;
        int min = Math.round(PITCH_MIN_SEMITONE * SLIDER_SCALE);
        int max = Math.round(PITCH_MAX_SEMITONE * SLIDER_SCALE);
        seekBar.setMax(max - min);
        seekBar.setProgress(Math.round((currentSemitone - PITCH_MIN_SEMITONE) * SLIDER_SCALE));
        seekBar.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentSemitone = PITCH_MIN_SEMITONE + (progress / (float) SLIDER_SCALE);
                applyPlaybackParameters();
            }
        });
        return seekBar;
    }

    private SeekBar makeSpeedSeekBar() {
        SeekBar seekBar = new SeekBar(this);
        speedSeekBar = seekBar;
        seekBar.setMax(Math.round((SPEED_MAX - SPEED_MIN) * 100));
        seekBar.setProgress(Math.round((currentSpeed - SPEED_MIN) * 100));
        seekBar.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentSpeed = SPEED_MIN + (progress / 100.0f);
                applyPlaybackParameters();
            }
        });
        return seekBar;
    }

    private SeekBar makeVolumeSeekBar() {
        SeekBar seekBar = new SeekBar(this);
        volumeSeekBar = seekBar;
        seekBar.setMax(100);
        seekBar.setProgress(100);
        seekBar.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentVolume = progress / 100.0f;
                if (player != null) {
                    player.setVolume(currentVolume);
                }
                volumeText.setText(String.format(Locale.JAPAN, "音量：%d%%", progress));
            }
        });
        return seekBar;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private LinearLayout createWebArea() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(rounded(COLOR_PANEL_2, 20));
        root.setPadding(dp(10), dp(10), dp(10), dp(10));

        TextView label = new TextView(this);
        label.setText("サービスWebViewモード");
        label.setTextColor(COLOR_TEXT);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextSize(16);
        label.setPadding(dp(4), 0, dp(4), dp(8));
        root.addView(label);

        TextView warning = new TextView(this);
        warning.setText("この画面は公式ページを表示するモードです。動画サービス側の仕様・規約により、このWebView内の音声はアプリ内ピッチ変更の対象外です。");
        warning.setTextColor(COLOR_WARNING);
        warning.setTextSize(12);
        warning.setPadding(dp(4), 0, dp(4), dp(8));
        root.addView(warning);

        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        webView.setWebViewClient(new WebViewClient());
        webView.setBackgroundColor(Color.BLACK);

        LinearLayout.LayoutParams webLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        );
        webView.setLayoutParams(webLp);
        root.addView(webView);
        return root;
    }

    private View createStatusPanel() {
        statusText = new TextView(this);
        statusText.setTextColor(COLOR_SUB_TEXT);
        statusText.setTextSize(12);
        statusText.setSingleLine(false);
        statusText.setPadding(dp(12), dp(10), dp(12), dp(10));
        statusText.setBackground(rounded(COLOR_PANEL, 16));
        return statusText;
    }

    private void openLocalMediaPicker() {
        showPlayerMode();
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"video/*", "audio/*"});
        startActivityForResult(intent, REQ_OPEN_MEDIA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_OPEN_MEDIA && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            final int flags = data.getFlags()
                    & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            try {
                getContentResolver().takePersistableUriPermission(uri, flags & Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {
                // 一部のファイルプロバイダでは永続権限が取れないため、その場合は一時権限で再生します。
            }
            playMediaUri(uri, "ローカルファイル：" + uri);
        }
    }

    private void showUrlDialog() {
        showPlayerMode();
        final EditText input = new EditText(this);
        input.setHint("https://example.com/video.mp4 または .m3u8 など");
        input.setSingleLine(false);
        input.setMinLines(2);
        input.setTextColor(Color.BLACK);

        new AlertDialog.Builder(this)
                .setTitle("URLを開く")
                .setMessage("直接再生可能なMP4/HLS/音声URLはピッチ変更できます。YouTube/ニコニコURLはWebViewモードで開きます。")
                .setView(input)
                .setPositiveButton("開く", (dialog, which) -> {
                    String url = input.getText().toString().trim();
                    if (url.isEmpty()) {
                        setStatus("URLが空です。", true);
                        return;
                    }
                    openUrl(url);
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private void openUrl(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.contains("youtube.com") || lower.contains("youtu.be")) {
            showWebMode("YouTube", url);
            return;
        }
        if (lower.contains("nicovideo.jp") || lower.contains("nico.ms")) {
            showWebMode("ニコニコ動画", url);
            return;
        }
        playMediaUri(Uri.parse(url), "URL：" + url);
    }

    private void playMediaUri(Uri uri, String sourceLabel) {
        showPlayerMode();
        if (player == null) {
            initializePlayer();
            if (playerView != null) {
                playerView.setPlayer(player);
            }
        }
        try {
            player.setMediaItem(MediaItem.fromUri(uri));
            player.prepare();
            player.play();
            applyPlaybackParameters();
            currentSourceText.setText("再生元：" + sourceLabel);
            setStatus("再生開始：ピッチ・速度スライダーで調整できます。", false);
        } catch (Exception ex) {
            setStatus("再生開始に失敗しました：" + safeMessage(ex), true);
        }
    }

    private void showPlayerMode() {
        playerArea.setVisibility(View.VISIBLE);
        webArea.setVisibility(View.GONE);
        setStatus("プレイヤーモード：アプリ内で再生している音声にピッチ変更が適用されます。", false);
    }

    private void showYouTubeMode() {
        if (!youtubeNoticeShown) {
            youtubeNoticeShown = true;
            showServiceNotice("YouTube");
        }
        showWebMode("YouTube", "https://m.youtube.com/");
    }

    private void showNicoMode() {
        if (!nicoNoticeShown) {
            nicoNoticeShown = true;
            showServiceNotice("ニコニコ動画");
        }
        showWebMode("ニコニコ動画", "https://www.nicovideo.jp/");
    }

    private void showWebMode(String serviceName, String url) {
        playerArea.setVisibility(View.GONE);
        webArea.setVisibility(View.VISIBLE);
        if (player != null && player.isPlaying()) {
            player.pause();
        }
        webView.loadUrl(url);
        setStatus(serviceName + " WebViewモード：公式ページを表示しています。この音声はピッチ変更対象外です。", true);
    }

    private void showServiceNotice(String serviceName) {
        new AlertDialog.Builder(this)
                .setTitle(serviceName + "モードについて")
                .setMessage(serviceName + "は公式ページをWebViewで表示します。規約・技術制限により、このWebView内の音声をアプリ側で直接ピッチ変更する処理は入れていません。\n\nピッチ変更したい場合は、ローカル動画/音声、または直接再生可能なURLをプレイヤーモードで開いてください。")
                .setPositiveButton("OK", null)
                .show();
    }

    private void resetPitchAndSpeed() {
        currentSemitone = 0.0f;
        currentSpeed = 1.0f;
        currentVolume = 1.0f;

        if (pitchSeekBar != null) {
            pitchSeekBar.setProgress(Math.round((currentSemitone - PITCH_MIN_SEMITONE) * SLIDER_SCALE));
        }
        if (speedSeekBar != null) {
            speedSeekBar.setProgress(Math.round((currentSpeed - SPEED_MIN) * 100));
        }
        if (volumeSeekBar != null) {
            volumeSeekBar.setProgress(100);
        }
        if (player != null) {
            player.setVolume(currentVolume);
        }
        applyPlaybackParameters();
        Toast.makeText(this, "ピッチ・速度・音量を初期値に戻しました。", Toast.LENGTH_SHORT).show();
    }

    private void applyPlaybackParameters() {
        float pitchRatio = semitoneToPitchRatio(currentSemitone);
        if (player != null) {
            player.setPlaybackParameters(new PlaybackParameters(currentSpeed, pitchRatio));
        }
        if (pitchText != null) {
            String sign = currentSemitone > 0 ? "+" : "";
            pitchText.setText(String.format(Locale.JAPAN, "ピッチ：%s%.1f 半音 / 倍率 %.3f", sign, currentSemitone, pitchRatio));
        }
        if (speedText != null) {
            speedText.setText(String.format(Locale.JAPAN, "速度：%.2fx", currentSpeed));
        }
    }

    /**
     * 半音をピッチ倍率に変換する。
     * 0半音=1.0、+12半音=2.0、-12半音=0.5。
     */
    private float semitoneToPitchRatio(float semitone) {
        return (float) Math.pow(2.0, semitone / 12.0);
    }

    private void setStatus(String message, boolean warning) {
        if (statusText == null) {
            return;
        }
        statusText.setText(message);
        statusText.setTextColor(warning ? COLOR_WARNING : COLOR_SUB_TEXT);
    }

    @Override
    public void onBackPressed() {
        if (webArea != null && webArea.getVisibility() == View.VISIBLE && webView != null && webView.canGoBack()) {
            webView.goBack();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (playerView != null) {
            playerView.setPlayer(null);
        }
        if (player != null) {
            player.release();
            player = null;
        }
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private abstract static class SimpleSeekBarListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // 未使用
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // 未使用
        }
    }
}
