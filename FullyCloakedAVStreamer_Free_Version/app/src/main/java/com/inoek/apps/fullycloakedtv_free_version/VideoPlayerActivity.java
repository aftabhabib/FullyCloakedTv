package com.inoek.apps.fullycloakedtv_free_version;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class VideoPlayerActivity extends Activity implements SurfaceHolder.Callback, IVLCVout.Callback {
    public final static String TAG = "Playing Video";
    private String mFilePath;
    private String mName;
    private SurfaceView mSurface;
    private SurfaceHolder holder;
    private org.videolan.libvlc.LibVLC libvlc;
    private org.videolan.libvlc.MediaPlayer mMediaPlayer = null;
    private int mVideoWidth;
    private int mVideoHeight;
    // Display Surface
    private LinearLayout vlcContainer;
    // Overlay / Controls

    private FrameLayout vlcOverlay;
    private ImageView vlcButtonPlayPause;
    private Handler handlerOverlay;
    private Runnable            runnableOverlay;
    private Handler             handlerSeekbar;
    private Runnable            runnableSeekbar;
    private SeekBar vlcSeekbar;
    private TextView vlcDuration, bufferPercent;
    private TextView            overlayTitle;
    private boolean isPaused = false, autoPlay = true;
    private ProgressBar progressBar;
    SharedPreferences preferences;
    SharedPreferences.Editor preferencesEditor;
    Switch autoPlaySwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_video_player);
        preferences = getApplicationContext().getSharedPreferences("stream", 0);
        preferencesEditor = preferences.edit();
        String videourl = (String) getIntent().getSerializableExtra("video");
        String name = (String) getIntent().getSerializableExtra("video_name");
        mName = name;
        mFilePath = videourl;
        mSurface = (SurfaceView) findViewById(R.id.surface);
        holder = mSurface.getHolder();
        // VLC
        vlcContainer = (LinearLayout) findViewById(R.id.vlc_container);
        mSurface = (SurfaceView) findViewById(R.id.surface);
        bufferPercent = (TextView) findViewById(R.id.bufferPercent);
        autoPlaySwitch = (Switch) findViewById(R.id.autoPlaySwitch);
        autoPlay = preferences.getBoolean("autoPlay",autoPlay);
        if(autoPlay)
        {
            autoPlaySwitch.setChecked(true);
        }
        else
        {
            autoPlaySwitch.setChecked(false);
        }

        // OVERLAY / CONTROLS
        vlcOverlay = (FrameLayout) findViewById(R.id.vlc_overlay);
        vlcButtonPlayPause = (ImageView) findViewById(R.id.vlc_button_play_pause);
        vlcSeekbar = (SeekBar) findViewById(R.id.vlc_seekbar);
        vlcDuration = (TextView) findViewById(R.id.vlc_duration);

        overlayTitle = (TextView) findViewById(R.id.vlc_overlay_title);
        overlayTitle.setText(mName);
        progressBar = (ProgressBar) findViewById(R.id.progressbar);
        progressBar.setVisibility(View.VISIBLE);
        mSurface.setBackgroundResource(R.drawable.videobg);
        // AUTOSTART
        playMovie();
    }

    @Override
    public void onBackPressed() {
        preferencesEditor.putBoolean("isPlaying", false);
        preferencesEditor.apply();
        setResult(RESULT_OK);
        finish();
        super.onBackPressed();
    }

    private org.videolan.libvlc.MediaPlayer.EventListener mPlayerListener = new MyPlayerListener(this);

    /**
     * Creates MediaPlayer and plays video
     *
     * @param media
     */
    private void createPlayer(String media) {
        releasePlayer();
        setupControls();
        try {
            // Create LibVLC
            // TODO: make this more robust, and sync with audio demo
            ArrayList<String> options = new ArrayList<String>();
            //options.add("--subsdec-encoding <encoding>");
            options.add("--aout=opensles");
            options.add("--audio-time-stretch"); // time stretching
            options.add("-vvv"); // verbosity
            libvlc = new LibVLC(this, options);
            holder.setKeepScreenOn(true);

            // Creating media player
            mMediaPlayer = new org.videolan.libvlc.MediaPlayer(libvlc);
            mMediaPlayer.setEventListener(mPlayerListener);

            // Seting up video output
            final IVLCVout vout = mMediaPlayer.getVLCVout();
            vout.setVideoView(mSurface);
            //vout.setSubtitlesView(mSurfaceSubtitles);
            vout.addCallback(this);
            vout.attachViews();

            Media m = new Media(libvlc, Uri.parse(media));
            m.setHWDecoderEnabled(true,true);
            m.addOption(":network-caching=9000");
            m.parseAsync();
            mMediaPlayer.setMedia(m);
            mMediaPlayer.play();
        } catch (Exception e) {
            Toast.makeText(this, "Error in creating player!", Toast
                    .LENGTH_LONG).show();
        }
    }

    private void showOverlay() {
        vlcOverlay.setVisibility(View.VISIBLE);
    }

    private void hideOverlay() {
        if(progressBar.getVisibility()==View.GONE) {
            vlcOverlay.setVisibility(View.GONE);
        }
    }

    private void setupControls() {

        // Auto Play control
        autoPlaySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    autoPlay = true;
                }
                else {
                    autoPlay = false;
                }
                preferencesEditor.putBoolean("autoPlay",autoPlay);
                preferencesEditor.apply();
            }
        });

        // PLAY PAUSE
        vlcButtonPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.pause();
                    isPaused = true;
                    vlcButtonPlayPause.setImageDrawable(ResourcesCompat.getDrawable(getResources(),R.drawable.ic_media_play,null));
                } else {
                    mMediaPlayer.play();
                    isPaused = false;
                    vlcButtonPlayPause.setImageDrawable(ResourcesCompat.getDrawable(getResources(),R.drawable.ic_media_pause,null));
                }
            }
        });

        // SEEKBAR
        handlerSeekbar = new Handler();
        runnableSeekbar = new Runnable() {
            @Override
            public void run() {
                if (libvlc != null) {
                    long curTime = mMediaPlayer.getTime();
                    long totalTime = (long) (curTime / mMediaPlayer.getPosition());
                    int minutes = (int) (curTime / (60 * 1000));
                    int seconds = (int) ((curTime / 1000) % 60);
                    int endMinutes = (int) (totalTime / (60 * 1000));
                    int endSeconds = (int) ((totalTime / 1000) % 60);
                    String duration = String.format("%02d:%02d / %02d:%02d", minutes, seconds, endMinutes, endSeconds);
                    vlcSeekbar.setProgress((int) (mMediaPlayer.getPosition() * 100));
                    vlcDuration.setText(duration);
                }
                handlerSeekbar.postDelayed(runnableSeekbar, 1000);
            }
        };

        runnableSeekbar.run();
        vlcSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                Log.v("NEW POS", "pos is : " + i);
                // if change was initiated by user
                long totalLength = mMediaPlayer.getLength();
                if(b && totalLength > 0) {
                    mMediaPlayer.setPosition(((float) i / 100.0f));
                }
                //if (i != 0)
                //    libvlc.setPosition(((float) i / 100.0f));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        // OVERLAY
        handlerOverlay = new Handler();
        runnableOverlay = new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
            @Override
            public void run() {
                hideOverlay();
                toggleFullscreen(true);
            }
        };
        final long timeToDisappear = 3000;
        handlerOverlay.postDelayed(runnableOverlay, timeToDisappear);
        vlcContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showOverlay();
                handlerOverlay.removeCallbacks(runnableOverlay);
                handlerOverlay.postDelayed(runnableOverlay, timeToDisappear);
            }
        });
    }

    public void playMovie() {
        if (libvlc != null && mMediaPlayer.isPlaying())
            return ;
        vlcContainer.setVisibility(View.VISIBLE);
        holder = mSurface.getHolder();
        holder.addCallback(this);
        createPlayer(mFilePath);
    }

    private void releasePlayer() {
        if (libvlc == null)
            return;
        mMediaPlayer.stop();
        mMediaPlayer.getMedia().release();
        final IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.removeCallback(this);
        vout.detachViews();
        holder = null;
        libvlc.release();
        libvlc = null;

        mVideoWidth = 0;
        mVideoHeight = 0;
    }

    private void toggleFullscreen(boolean fullscreen)
    {
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        if (fullscreen)
        {
            attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            vlcContainer.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
        else
        {
            attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
        }
        getWindow().setAttributes(attrs);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setSize(mVideoWidth, mVideoHeight);
    }

    @Override
    protected void onResume() {
        if(!mMediaPlayer.getVLCVout().areViewsAttached()) {
            mMediaPlayer.getVLCVout().setVideoView(mSurface);
            mMediaPlayer.getVLCVout().attachViews();
            mMediaPlayer.getVLCVout().addCallback(this);
        }
        if(!isPaused)
            mMediaPlayer.play();
        super.onResume();
    }

    @Override
    protected void onPause() {
        mMediaPlayer.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSurface.getBackground().setAlpha(255);
        releasePlayer();
    }


    /**
     * Used to set size for SurfaceView
     *
     * @param width
     * @param height
     */
    private void setSize(int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;
        if (mVideoWidth * mVideoHeight <= 1)
            return;

        if (holder == null || mSurface == null)
            return;

        int w = getWindow().getDecorView().getWidth();
        int h = getWindow().getDecorView().getHeight();
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        if (w > h && isPortrait || w < h && !isPortrait) {
            int i = w;
            w = h;
            h = i;
        }

        float videoAR = (float) mVideoWidth / (float) mVideoHeight;
        float screenAR = (float) w / (float) h;

        if (screenAR < videoAR)
            h = (int) (w / videoAR);
        else
            w = (int) (h * videoAR);

        holder.setFixedSize(mVideoWidth, mVideoHeight);
        ViewGroup.LayoutParams lp = mSurface.getLayoutParams();
        lp.width = w;
        lp.height = h;
        mSurface.setLayoutParams(lp);
        mSurface.invalidate();
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onNewLayout(IVLCVout vlcVout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
        progressBar.setVisibility(View.GONE);
        mSurface.getBackground().setAlpha(0);
        hideOverlay();
        if (width * height == 0)
            return;
        // store video size
        mVideoWidth = width;
        mVideoHeight = height;
        setSize(mVideoWidth, mVideoHeight);
    }

    @Override
    public void onSurfacesCreated(IVLCVout vlcVout) {

    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vlcVout) {

    }

    @Override
    public void onHardwareAccelerationError(IVLCVout vlcVout) {
        Log.e(TAG, "Error with hardware acceleration");
        this.releasePlayer();
    }

    private static class MyPlayerListener implements org.videolan.libvlc.MediaPlayer.EventListener {
        private WeakReference<VideoPlayerActivity> mOwner;

        public MyPlayerListener(VideoPlayerActivity owner) {
            mOwner = new WeakReference<VideoPlayerActivity>(owner);
        }

        @Override
        public void onEvent(org.videolan.libvlc.MediaPlayer.Event event) {
            final VideoPlayerActivity player = mOwner.get();

            switch (event.type) {
                case org.videolan.libvlc.MediaPlayer.Event.EndReached:
                    Log.d(TAG, "MediaPlayerEndReached");
                    new CountDownTimer(5000, 1000) {
                        @Override
                        public void onTick(long millisUntilFinished) {

                        }

                        @Override
                        public void onFinish() {
                            player.preferencesEditor.putBoolean("isPlaying", false);
                            player.preferencesEditor.apply();
                            player.releasePlayer();
                            player.setResult(RESULT_OK);
                            player.finish();
                        }
                    }.start();
                    break;
                case org.videolan.libvlc.MediaPlayer.Event.Buffering:
                    player.bufferPercent.setText("Buffering : "+event.getBuffering());
                    if(event.getBuffering() == 100)
                    {
                        player.bufferPercent.setText("");
                    }
                    Log.d("BUFFERING", ""+event.getBuffering());
                    break;
                case org.videolan.libvlc.MediaPlayer.Event.Playing:
                    break;
                case org.videolan.libvlc.MediaPlayer.Event.Opening:
                    player.bufferPercent.setText("Opening......");
                    break;
                case org.videolan.libvlc.MediaPlayer.Event.Paused:
                case org.videolan.libvlc.MediaPlayer.Event.Stopped:
                default:
                    break;
            }
        }
    }
}
