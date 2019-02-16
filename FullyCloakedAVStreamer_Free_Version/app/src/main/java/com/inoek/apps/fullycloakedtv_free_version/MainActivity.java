package com.inoek.apps.fullycloakedtv_free_version;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.inoek.apps.fullycloakedtv_free_version.M3UClassLoader.M3UFile;
import com.inoek.apps.fullycloakedtv_free_version.M3UClassLoader.M3UItem;
import com.inoek.apps.fullycloakedtv_free_version.M3UClassLoader.M3UToolSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static android.os.Build.VERSION_CODES.M;

public class MainActivity extends AppCompatActivity implements IVLCVout.Callback,
        UrlUpdaterInterface<String>, InternetChecker<MainActivity.ReturnValueForAction>,
        UrlDecoder<MainActivity.UrlDecoderData>, UrlValidator<Boolean> {

    public final static String TAG = "MainActivity";
    Button _play;
    ImageButton browseVideo, moveNext, movePrevious, resetVideo,audio_play, search_button, epg_button;
    EditText txtVideo, searchText;
    private SurfaceView mSurface;
    private SurfaceHolder holder;
    private LibVLC libvlc;
    private org.videolan.libvlc.MediaPlayer mMediaPlayer = null;
    VideoView /*_vView,*/ _aView;
    ProgressBar pbar;
    InterstitialAd interAd;
    final private int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;
    public static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-6847751695888548/3408096519";
    SharedPreferences preferences;
    SharedPreferences.Editor preferencesEditor;
    String videoUrl,audioUrl, playListURL, defaultPlayListURL;
    Handler handler;
    Animation animation;
    AVInterpolator avInterpolator;
    ArrayList<LinkedList<M3UItem>> playListEntries;
    M3UFile m3ufile;
    TextView channelName, channelNum;
    int channelIndex = 0;
    ArrayList<String> channelNamesList;
    boolean isVideo = false, isFreeVersion = false, isError = false, autoPlay = true, isPlaying = false, isNative = false, isTouched = false;
    static boolean isPaused;
    Spinner channelSelector;
    Switch autoPlaySwitch;
    private int mVideoWidth;
    private int mVideoHeight;
    private String mFilePath;
    CountDownTimer timer;
    private static ProgressDialog progressDialog;
    private boolean calledFromMain = false;

    class ReturnValueForAction
    {
        public Integer updateId;
        public Boolean bIsConnected;
        public ReturnValueForAction()
        {
            updateId = 0;
            bIsConnected = false;
        }
    }

    class UrlDecoderData
    {
        public Integer updateId;
        public String decodedURL;
        public UrlDecoderData()
        {
            updateId = 0;
            decodedURL = "";
        }
    }

    private void showMessage() {
        AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
        dlgAlert.setMessage("No Internet Connection");
        dlgAlert.setTitle("Information");
        dlgAlert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
            }
        });
        dlgAlert.setCancelable(true);
        dlgAlert.create().show();
    }

    private void showMessageMistake() {
        AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
        dlgAlert.setMessage("Playlist failed to load, try after sometime");
        dlgAlert.setTitle("Information");
        dlgAlert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
            }
        });
        dlgAlert.setCancelable(true);
        dlgAlert.create().show();
    }

    private void showMessageError(String channelName) {
        AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
        dlgAlert.setMessage(channelName+" doesn't exist");
        dlgAlert.setTitle("Information");
        dlgAlert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
            }
        });
        dlgAlert.setCancelable(true);
        dlgAlert.create().show();
    }

    private void setUpPermissions()
    {
        if (Build.VERSION.SDK_INT >= 23) {
            List<String> permissionsNeeded = new ArrayList<String>();
            final List<String> permissionsList = new ArrayList<String>();
            if (!addPermission(permissionsList,
                    Manifest.permission.WAKE_LOCK))
                permissionsNeeded.add("Wake Lock");
            if (!addPermission(permissionsList, Manifest.permission.DISABLE_KEYGUARD))
                permissionsNeeded.add("Disable Keyguard");
            if (!addPermission(permissionsList, Manifest.permission.INTERNET))
                permissionsNeeded.add("Internet Connection");
            if (!addPermission(permissionsList,
                    Manifest.permission.ACCESS_NETWORK_STATE))
                permissionsNeeded.add("Network State");
            if (permissionsList.size() > 0) {
                requestPermissions(
                        permissionsList.toArray(new String[permissionsList
                                .size()]),
                        REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
                return;
            }
        }
    }

    /**
     * Registering callbacks
     */
    private org.videolan.libvlc.MediaPlayer.EventListener mPlayerListener = new MyPlayerListener(this);

    private void releasePlayer() {
        if (libvlc == null)
            return;
        mMediaPlayer.stop();
        final IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.removeCallback(this);
        vout.detachViews();
        holder = null;
        libvlc.release();
        libvlc = null;
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

    private void fillChannelNames()
    {
        channelNamesList.clear();
        for(M3UItem item:playListEntries.get(0))
        {
            if(item.getChannelName()!=null) {
                channelNamesList.add(item.getChannelName());
            }
            else {
                channelNamesList.add(" ");
            }
        }
        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, channelNamesList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        channelSelector.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    private boolean isMediaAudio(int lChannelIndex)
    {
        if(channelIndex < playListEntries.get(0).size()) {
            String strGroupTitle = playListEntries.get(0).get(lChannelIndex).getGroupTitle();
            if(strGroupTitle != null) {
                if (strGroupTitle.contains("radio") || strGroupTitle.contains("audio")) {
                    return true;
                }
            }
        }
        return false;
    }

    private void init()
    {
        channelIndex = preferences.getInt("channelIndex",channelIndex);
        channelSelector.setSelection(channelIndex);
        autoPlay = preferences.getBoolean("autoPlay",autoPlay);
        isFreeVersion = true;
        handler = new Handler();
        autoPlaySwitch = (Switch) findViewById(R.id.autoPlaySwitch);
        if(autoPlay)
        {
            autoPlaySwitch.setChecked(true);
        }
        else
        {
            autoPlaySwitch.setChecked(false);
        }
        channelName = (TextView) findViewById(R.id.channelName);
        channelNum = (TextView) findViewById(R.id.channelNum);
        _play = (Button) findViewById(R.id.play);
        epg_button = (ImageButton) findViewById(R.id.epg_button);
        search_button = (ImageButton) findViewById(R.id.search);
        browseVideo = (ImageButton) findViewById(R.id.browseVideo);
        moveNext  = (ImageButton) findViewById(R.id.btn_move_next);
        resetVideo = (ImageButton) findViewById(R.id.resetVideo);
        movePrevious  = (ImageButton) findViewById(R.id.btn_move_previous);
        txtVideo = (EditText) findViewById(R.id.videoUrl);
        searchText = (EditText) findViewById(R.id.selected_channel);
        audio_play = (ImageButton)findViewById(R.id.audio_play);
        mSurface = (SurfaceView) findViewById(R.id.myVideo);
        holder = mSurface.getHolder();
        _play.setVisibility(View.VISIBLE);
        interAd = new InterstitialAd(this);
        interAd.setAdUnitId(INTERSTITIAL_AD_UNIT_ID);
        AdRequest.Builder builder = new AdRequest.Builder();
        AdRequest ad = builder.build();
        interAd.loadAd(ad);
        videoUrl = preferences.getString("videoUrl",playListURL);
        audioUrl = preferences.getString("audioUrl",getString(R.string.audio_url));
        _aView = (VideoView) findViewById(R.id.myAudio);
        pbar = (ProgressBar) findViewById(R.id.progress);
        pbar.setVisibility(View.GONE);
        if(videoUrl.contains(".m3u"))
        {
            txtVideo.setSelected(false);
            channelName.setText(playListEntries.get(0).get(channelIndex).getChannelName());
            channelNum.setText(String.valueOf((channelIndex+1+"/"+playListEntries.get(0).size())));
            new DownloadImageFromInternet(mSurface, progressDialog)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, playListEntries.get(0).get(channelIndex).getLogoURL());
        }
        else {
            txtVideo.setSelected(false);
            preferencesEditor.putString("videoUrl", txtVideo.getText().toString());
            preferencesEditor.apply();
        }
        _aView.setVideoURI(Uri.parse(audioUrl));

        txtVideo.setEnabled(true);
        txtVideo.setHint("Custom playlist url..");
        browseVideo.setEnabled(true);
        browseVideo.setImageResource(R.mipmap.ic_launcher);

        // if app is free version
        if(isFreeVersion)
        {
            txtVideo.setEnabled(false);
            txtVideo.setText("Upgrade to PRO");
            browseVideo.setEnabled(false);
            browseVideo.setImageResource(R.mipmap.ic_download_disabled);
        }
    }

    private void performPlayNext()
    {
        String vUrl = preferences.getString("videoUrl", playListURL);
        if (vUrl.contains(".m3u")) {
            channelIndex = (channelIndex + 1);
            if (channelIndex >= playListEntries.get(0).size())
                channelIndex = 0;
            preferencesEditor.putInt("channelIndex", channelIndex);
            preferencesEditor.apply();
            isVideo = false;
            final IVLCVout vout = mMediaPlayer.getVLCVout();
            if(!vout.areViewsAttached()) {
                vout.setVideoView(mSurface);
                vout.addCallback(this);
                vout.attachViews();
            }
            isPlaying = false;
            channelName.setText(playListEntries.get(0).get(channelIndex).getChannelName());
            channelNum.setText(String.valueOf(channelIndex + 1) + "/" + String.valueOf(playListEntries.get(0).size()));
            new DownloadImageFromInternet(mSurface, progressDialog)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, playListEntries.get(0).get(channelIndex).getLogoURL());
            System.out.println("Line 396 progress bar visible");
            pbar.setVisibility(View.VISIBLE);
            _play.setVisibility(View.INVISIBLE);
            playVideo();
        }
    }

    private void playNext()
    {
        autoPlay = preferences.getBoolean("autoPlay",autoPlay);
        if(autoPlay) {
            mMediaPlayer.stop();
            if(mMediaPlayer.getMedia() != null) {
                mMediaPlayer.getMedia().release();
            }
           checkWifiAndUpdate(7);
        }
        else
        {
            System.out.println("Line 415 progress bar gone");
            pbar.setVisibility(View.INVISIBLE);
            _play.setBackgroundResource(R.drawable.ic_media_play);
            _play.setVisibility(View.VISIBLE);
        }
    }

    private void performPlayNextOnError()
    {
        String vUrl = preferences.getString("videoUrl", playListURL);
        if (vUrl.contains(".m3u")) {
            channelIndex = (channelIndex + 1);
            if (channelIndex >= playListEntries.get(0).size())
                channelIndex = 0;
            preferencesEditor.putInt("channelIndex", channelIndex);
            preferencesEditor.apply();
            isVideo = false;
            final IVLCVout vout = mMediaPlayer.getVLCVout();
            if(!vout.areViewsAttached()) {
                vout.setVideoView(mSurface);
                vout.addCallback(this);
                vout.attachViews();
            }
            isPlaying = false;
            channelName.setText(playListEntries.get(0).get(channelIndex).getChannelName());
            channelNum.setText(String.valueOf(channelIndex + 1) + "/" + String.valueOf(playListEntries.get(0).size()));
            new DownloadImageFromInternet(mSurface, progressDialog)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, playListEntries.get(0).get(channelIndex).getLogoURL());
            System.out.println("Line 443 progress bar visible");
            pbar.setVisibility(View.VISIBLE);
            _play.setVisibility(View.INVISIBLE);
            playVideo();
        }
    }

    private void playNextOnError()
    {
        mMediaPlayer.stop();
        if(mMediaPlayer.getMedia() != null) {
            mMediaPlayer.getMedia().release();
        }
        checkWifiAndUpdate(8);
    }

    private void performPlayVideoAfterDecode(String url)
    {
        mFilePath = url;
        playVideo();
    }

    private void createPlayer(String media) {
        releasePlayer();
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
            System.out.println("Decoding from create player....");
            try {
                new decodeURL(MainActivity.this, this, progressDialog, 1).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, media);
            }
            catch(Exception e)
            {

            }
        } catch (Exception e) {
            Toast.makeText(this, "Error in creating player!", Toast
                    .LENGTH_LONG).show();
        }
    }

    @Override
    public void OnCompleteUpdate(String obj) {
        if (!preferences.getBoolean("overridden", false) && obj!=null) {
            loadPlayListURL(obj);
            m3ufile = M3UToolSet.load(playListURL);
        }
        else {
            m3ufile = M3UToolSet.load(obj);
            playListURL = obj;
        }
        if (m3ufile.getItems().isEmpty()) {
            return;
        }
        playListEntries.clear();
        channelNamesList.clear();
        playListEntries.add(m3ufile.getItems());
        fillChannelNames();
        defaultPlayListURL = playListURL;
        preferencesEditor.putString("audioUrl", audioUrl);
        preferencesEditor.apply();
        preferencesEditor.putString("videoUrl", playListURL);
        preferencesEditor.apply();
        init();
        setUpOnClickListeners();
        // only when created this has to be executed
        if(calledFromMain)
        {
            calledFromMain = false;
            if(playListEntries.size() > 0) {
                System.out.println("Create player");
                createPlayer(playListEntries.get(0).get(channelIndex).getStreamURL());
            }
            else
            {
                showMessageMistake();
            }
        }
    }


    private class PlayMediaAsync extends AsyncTask<String, Void, Boolean>
    {

        @Override
        protected Boolean doInBackground(String... params) {
            mMediaPlayer.setMedia(new Media(libvlc, Uri.parse(params[0])));
            mMediaPlayer.getMedia().setHWDecoderEnabled(true,true);
            mMediaPlayer.getMedia().addOption(":network-caching=9000");
            mMediaPlayer.play();
            return null;
        }
    }

    private void performPlayVLCVideo()
    {
        if (!isError) {
            if (isVideo) {
                if (!preferences.getBoolean("isPlaying", false)) {
                    preferencesEditor.putBoolean("isPlaying", true);
                    preferencesEditor.apply();
                    String vUrl = preferences.getString("videoUrl", playListURL);
                    if (isNative) {
                        Intent intent = new Intent(getApplicationContext(), NativePlayer.class);
                        if (vUrl.contains(".m3u")) {
                            intent.putExtra("video", mFilePath);
                            intent.putExtra("video_name", playListEntries.get(0).get(channelIndex).getChannelName());
                        } else {
                            intent.putExtra("video", vUrl);
                            intent.putExtra("video_name", "Custom Video");
                        }
                        startActivityForResult(intent, 1);
                    } else {
                        Intent intent = new Intent(getApplicationContext(), VideoPlayerActivity.class);
                        if (vUrl.contains(".m3u")) {
                            intent.putExtra("video", mFilePath);
                            intent.putExtra("video_name", playListEntries.get(0).get(channelIndex).getChannelName());
                        } else {
                            intent.putExtra("video", vUrl);
                            intent.putExtra("video_name", "Custom Video");
                        }
                        startActivityForResult(intent, 1);
                    }
                }
            }
        }
    }

    private void playVLCVideo()
    {
        autoPlay = preferences.getBoolean("autoPlay", autoPlay);
        if (autoPlay && !isPaused) {
         timer = new CountDownTimer(10000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {

                }

                @Override
                public void onFinish() {
                checkWifiAndUpdate(9);
                }
            }.start();
        }
    }

    private void performURLValidation(boolean isValidVideo)
    {
        System.out.println("Line 640 progress bar gone");
        pbar.setVisibility(View.GONE);
        _play.setVisibility(View.VISIBLE);
        if (isValidVideo) {
            isError = false;
            _play.setBackground(getDrawable(R.drawable.ic_media_play));
            if (isMediaAudio(channelIndex)) {
                new PlayMediaAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mFilePath);
            } else {
                isVideo = true;
                playVLCVideo();
            }
        } else {
            isError = true;
            _play.setBackground(getDrawable(R.drawable.ic_error_outline_black_24dp));
            showMessageError(channelName.getText().toString());
            playNextOnError();
        }
    }

    private void performPlayVideoAfterDecodingURL(String url)
    {
        mFilePath = url;
        try {
            if (mFilePath != null) {
                if(!isPaused) {
                    new ValidateTheURL(MainActivity.this, this, progressDialog).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mFilePath);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playVideo()
    {
        if(!preferences.getBoolean("isPlaying",false)) {
            System.out.println("Decoding from play video....");
            if(!isPaused) {
                new decodeURL(MainActivity.this, this, progressDialog, 0).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, playListEntries.get(0).get(channelIndex).getStreamURL());
            }
        }
    }

    private void performAudioPlay()
    {
        if (!_aView.isPlaying()) {
            audio_play.setBackgroundResource(R.drawable.on);
            _aView.start();
        } else {
            audio_play.setBackgroundResource(R.drawable.off);
            _aView.pause();
        }
    }

    private void performBrowseVideo()
    {
        String enteredURL = txtVideo.getText().toString();
        if (enteredURL.length() > 5) {
            try {
                Uri newUrl = Uri.parse(txtVideo.getText().toString());
                if (enteredURL.contains(".m3u")) {
                    m3ufile = M3UToolSet.load(enteredURL);
                    if (m3ufile.getItems().isEmpty()) {
                        return;
                    }
                    playListEntries.clear();
                    playListEntries.add(m3ufile.getItems());
                    preferencesEditor.putString("videoUrl", enteredURL);
                    preferencesEditor.apply();
                    preferencesEditor.putInt("channelIndex",0);
                    preferencesEditor.apply();
                    fillChannelNames();
                    channelName.setText(playListEntries.get(0).get(0).getChannelName());
                    channelNum.setText("1/" + String.valueOf(playListEntries.get(0).size()));
                    new DownloadImageFromInternet(mSurface, progressDialog)
                            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, playListEntries.get(0).get(channelIndex).getLogoURL());
                    playVideo();
                } else {
                    txtVideo.setSelected(false);
                    preferencesEditor.putString("videoUrl", txtVideo.getText().toString());
                    preferencesEditor.apply();
                    channelName.setText("Custom Video");
                    channelNum.setText("-/-");
                    playVideo();
                }
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "This channel doesn't seem to load. Try another channel", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setUpOnClickListeners()
    {
        // EPG display
        epg_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this,"Coming Soon....", Toast.LENGTH_SHORT).show();
            }
        });

        // auto play control
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

        // search button code
        search_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!searchText.getText().toString().isEmpty()) {
                    mMediaPlayer.stop();
                    if (mMediaPlayer.getMedia() != null) {
                        mMediaPlayer.getMedia().release();
                    }
                    animation = AnimationUtils.loadAnimation(v.getContext(), R.anim.bounce_anim);
                    avInterpolator = new AVInterpolator(0.2, 20);
                    animation.setInterpolator(avInterpolator);
                    search_button.startAnimation(animation);
                    String channel_sel = searchText.getText().toString();
                    searchText.clearFocus();
                    InputMethodManager inputManager = (InputMethodManager)
                            getSystemService(Context.INPUT_METHOD_SERVICE);

                    inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                            InputMethodManager.HIDE_NOT_ALWAYS);
                    if (!channel_sel.isEmpty() && !channel_sel.equals("")) {
                        int num = Integer.parseInt(channel_sel);
                        if (num > playListEntries.get(0).size()) {
                            num = playListEntries.get(0).size();
                            searchText.setText(String.valueOf(num));
                        }
                        if (num <= 0) {
                            num = 1;
                            searchText.setText(String.valueOf(num));
                        }
                        channelIndex = num - 1;
                        String vUrl = preferences.getString("videoUrl", playListURL);
                        if (vUrl.contains(".m3u")) {
                            preferencesEditor.putInt("channelIndex", channelIndex);
                            preferencesEditor.apply();
                            isVideo = false;
                            final IVLCVout vout = mMediaPlayer.getVLCVout();
                            if (!vout.areViewsAttached()) {
                                vout.setVideoView(mSurface);
                                vout.addCallback(MainActivity.this);
                                vout.attachViews();
                            }
                            isPlaying = false;
                            channelName.setText(playListEntries.get(0).get(channelIndex).getChannelName());
                            channelNum.setText(String.valueOf(channelIndex + 1) + "/" + String.valueOf(playListEntries.get(0).size()));
                            new DownloadImageFromInternet(mSurface, progressDialog)
                                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, playListEntries.get(0).get(channelIndex).getLogoURL());
                            pbar.setVisibility(View.VISIBLE);
                            _play.setVisibility(View.INVISIBLE);
                            playVideo();
                        }
                    }
                }
            }
        });

        // Play's Video
        _play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isError) {
                    if (isVideo) {
                        // if timer has already started..
                        if (timer != null) {
                            timer.cancel();
                        }
                        preferencesEditor.putBoolean("isPlaying", true);
                        preferencesEditor.apply();
                        String vUrl = preferences.getString("videoUrl", playListURL);
                        if(isNative)
                        {
                            Intent intent = new Intent(getApplicationContext(), NativePlayer.class);
                            if (vUrl.contains(".m3u")) {
                                intent.putExtra("video", mFilePath);
                                intent.putExtra("video_name", playListEntries.get(0).get(channelIndex).getChannelName());
                            } else {
                                intent.putExtra("video", vUrl);
                                intent.putExtra("video_name", "Custom Video");
                            }
                            startActivityForResult(intent, 1);
                        }
                        else {
                            Intent intent = new Intent(getApplicationContext(), VideoPlayerActivity.class);
                            if (vUrl.contains(".m3u")) {
                                intent.putExtra("video", mFilePath);
                                intent.putExtra("video_name", playListEntries.get(0).get(channelIndex).getChannelName());
                            } else {
                                intent.putExtra("video", vUrl);
                                intent.putExtra("video_name", "Custom Video");
                            }
                            startActivityForResult(intent, 1);
                        }
                    } else {
                        if (!mMediaPlayer.isPlaying()) {
                            _play.setBackgroundResource(R.drawable.ic_media_pause);
                            if(preferences.getBoolean("isPlaying",false)) {
                                mMediaPlayer.play();
                            }
                            else
                            {
                                preferencesEditor.putBoolean("isPlaying", true);
                                preferencesEditor.apply();
                                new PlayMediaAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mFilePath);
                            }
                        } else {
                            _play.setBackgroundResource(R.drawable.ic_media_play);
                            mMediaPlayer.pause();
                        }
                    }
                } else {
                    showMessageError(channelName.getText().toString());
                }
            }
        });

        // Play's Audio
        audio_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              checkWifiAndUpdate(4);
            }
        });

        // Play's Custom Video
        browseVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animation = AnimationUtils.loadAnimation(v.getContext(), R.anim.bounce_anim);
                avInterpolator = new AVInterpolator(0.2, 20);
                animation.setInterpolator(avInterpolator);
                browseVideo.startAnimation(animation);
                preferencesEditor.putBoolean("overridden", true);
                preferencesEditor.apply();
                String enteredURL = txtVideo.getText().toString();
                txtVideo.clearFocus();
                //  _vView.requestFocus();
                InputMethodManager inputManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);

                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
               checkWifiAndUpdate(6);
            }
        });

        moveNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMediaPlayer.stop();
                preferencesEditor.putBoolean("isPlaying", false);
                preferencesEditor.apply();
                if(mMediaPlayer.getMedia() != null) {
                    mMediaPlayer.getMedia().release();
                }
                animation = AnimationUtils.loadAnimation(v.getContext(), R.anim.bounce_anim);
                avInterpolator = new AVInterpolator(0.2, 20);
                animation.setInterpolator(avInterpolator);
                moveNext.startAnimation(animation);
                String vUrl = preferences.getString("videoUrl", playListURL);
                if (vUrl.contains(".m3u")) {
                    channelIndex = (channelIndex + 1);
                    if (channelIndex >= playListEntries.get(0).size())
                        channelIndex = 0;
                    preferencesEditor.putInt("channelIndex", channelIndex);
                    preferencesEditor.apply();
                    isVideo = false;
                    final IVLCVout vout = mMediaPlayer.getVLCVout();
                    if(!vout.areViewsAttached()) {
                        vout.setVideoView(mSurface);
                        vout.addCallback(MainActivity.this);
                        vout.attachViews();
                    }
                    isPlaying = false;
                    channelName.setText(playListEntries.get(0).get(channelIndex).getChannelName());
                    channelNum.setText(String.valueOf(channelIndex + 1) + "/" + String.valueOf(playListEntries.get(0).size()));
                    new DownloadImageFromInternet(mSurface, progressDialog)
                            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, playListEntries.get(0).get(channelIndex).getLogoURL());
                    pbar.setVisibility(View.VISIBLE);
                    _play.setVisibility(View.INVISIBLE);
                    playVideo();
                }
            }
        });

        movePrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMediaPlayer.stop();
                preferencesEditor.putBoolean("isPlaying", false);
                preferencesEditor.apply();
                if(mMediaPlayer.getMedia() != null) {
                    mMediaPlayer.getMedia().release();
                }
                animation = AnimationUtils.loadAnimation(v.getContext(), R.anim.bounce_anim);
                avInterpolator = new AVInterpolator(0.2, 20);
                animation.setInterpolator(avInterpolator);
                movePrevious.startAnimation(animation);
                String vUrl = preferences.getString("videoUrl", playListURL);
                if (vUrl.contains(".m3u")) {
                    channelIndex = (channelIndex - 1);
                    if (channelIndex < 0)
                        channelIndex = playListEntries.get(0).size() - 1;
                    preferencesEditor.putInt("channelIndex", channelIndex);
                    preferencesEditor.apply();
                    isVideo = false;
                    final IVLCVout vout = mMediaPlayer.getVLCVout();
                    if(!vout.areViewsAttached()) {
                        vout.setVideoView(mSurface);
                        vout.addCallback(MainActivity.this);
                        vout.attachViews();
                    }
                    isPlaying = false;
                    channelName.setText(playListEntries.get(0).get(channelIndex).getChannelName());
                    channelNum.setText(String.valueOf(channelIndex + 1) + "/" + String.valueOf(playListEntries.get(0).size()));
                    new DownloadImageFromInternet(mSurface, progressDialog)
                            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, playListEntries.get(0).get(channelIndex).getLogoURL());
                    pbar.setVisibility(View.VISIBLE);
                    _play.setVisibility(View.INVISIBLE);
                    playVideo();
                }
            }
        });

        resetVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animation = AnimationUtils.loadAnimation(v.getContext(), R.anim.bounce_anim);
                avInterpolator = new AVInterpolator(0.2, 20);
                animation.setInterpolator(avInterpolator);
                preferencesEditor.putBoolean("overridden", false);
                preferencesEditor.apply();
                resetVideo.startAnimation(animation);
                Toast.makeText(MainActivity.this, "Getting updated url from server",
                        Toast.LENGTH_SHORT).show();
                preferencesEditor.putInt("channelIndex",0);
                preferencesEditor.apply();
                resetURL();
            }
        });
    }

    private void loadPlayListURL(String jsonString)
    {
        JSONObject obj = null;
        String url = "",aUrl="";
        try {
            obj = new JSONObject(jsonString);
            JSONArray objArray = obj.getJSONArray("result");
            JSONObject objobject = objArray.getJSONObject(0);
            url = objobject.getString("video_playlist_url").trim();
            aUrl = objobject.getString("audio_url").trim();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        playListURL = url;
        audioUrl = aUrl;
    }

    private void performResetURL()
    {
        if(!preferences.getBoolean("overridden",false)) {
            if (playListEntries != null) {
                // String url = "";
                new GetURLFromDatabase(MainActivity.this, this, progressDialog).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
        else
        {
            String vUrl = preferences.getString("videoUrl", playListURL);
            OnCompleteUpdate(vUrl);
        }
    }

    private void resetURL()
    {
       checkWifiAndUpdate(2);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode==RESULT_OK)
        {
            isPaused = false;
            playNext();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void performResetURLFromMain() {
        // if user has loaded custom list then don't reset cause user has overridden db operations
        if (!preferences.getBoolean("overridden", false)) {
            // whenever app is opened, url would be updated from the database
            calledFromMain = true;
            resetURL();
        }
        else
        {
            calledFromMain = true;
            String vUrl = preferences.getString("videoUrl", playListURL);
            OnCompleteUpdate(vUrl);
        }
    }

    private void UpdateAppBasedOnWifiStatus(Integer updateID)
    {
        switch(updateID)
        {
            case 0:
                performResetURLFromMain();
                break;
            case 1:
                performResetURLFromResume();
                break;
            case 2:
                performResetURL();
                break;
            case 3:
                performAdLoading();
                break;
            case 4:
                performAudioPlay();
                break;
            case 6:
                performBrowseVideo();
                break;
            case 7:
                performPlayNext();
                break;
            case 8:
                performPlayNextOnError();
                break;
            case 9:
                performPlayVLCVideo();
                break;
        }
    }

    @Override
    public void OnURLDecoded(UrlDecoderData obj) {
        switch(obj.updateId)
        {
            case 0:
                performPlayVideoAfterDecodingURL(obj.decodedURL);
                break;
            case 1:
                performPlayVideoAfterDecode(obj.decodedURL);
                break;
        }
    }

    @Override
    public void OnValidURL(Boolean obj) {
        performURLValidation(obj);
    }


    @Override
    public void OnCheckNetworkConnection(ReturnValueForAction obj) {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        boolean wifiConnected = (ni != null && ni.isConnected());
        if(wifiConnected && obj.bIsConnected)
        {
            UpdateAppBasedOnWifiStatus(obj.updateId);
        }
        else {
            showMessage();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);
        preferences = getApplicationContext()
                .getSharedPreferences("stream", 0);
        preferencesEditor = preferences.edit();
        isPaused = false;
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setTitle("Please wait.");
        progressDialog.setIndeterminate(false);
        progressDialog.setCancelable(false);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        if(ActivityCompat.checkSelfPermission(getApplicationContext(),Manifest.permission.WAKE_LOCK)!=PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(getApplicationContext(),Manifest.permission.DISABLE_KEYGUARD)!=PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(getApplicationContext(),Manifest.permission.INTERNET)!=PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(getApplicationContext(),Manifest.permission.ACCESS_NETWORK_STATE)!=PackageManager.PERMISSION_GRANTED)
        {
            setUpPermissions();
        }

        if (savedInstanceState == null) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WAKE_LOCK) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.DISABLE_KEYGUARD) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED) {
                channelIndex = preferences.getInt("channelIndex", 0);
                autoPlay = preferences.getBoolean("autoPlay", false);
                preferencesEditor.putInt("channelIndex", channelIndex);
                preferencesEditor.apply();
                preferencesEditor.putBoolean("autoPlay", autoPlay);
                preferencesEditor.putBoolean("isPlaying", false);
                preferencesEditor.apply();
                playListEntries = new ArrayList<>();
                channelNamesList = new ArrayList<>();
                channelSelector = (Spinner) findViewById(R.id.channelDropdown);
                channelSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (isTouched) {
                            mMediaPlayer.stop();
                            preferencesEditor.putBoolean("isPlaying", false);
                            preferencesEditor.apply();
                            if (mMediaPlayer.getMedia() != null) {
                                mMediaPlayer.getMedia().release();
                            }
                            String vUrl = preferences.getString("videoUrl", playListURL);
                            if (vUrl.contains(".m3u")) {
                                channelIndex = (position);
                                if (channelIndex < 0)
                                    channelIndex = playListEntries.get(0).size() - 1;
                                preferencesEditor.putInt("channelIndex", channelIndex);
                                preferencesEditor.apply();
                                isVideo = false;
                                final IVLCVout vout = mMediaPlayer.getVLCVout();
                                if (!vout.areViewsAttached()) {
                                    vout.setVideoView(mSurface);
                                    vout.addCallback(MainActivity.this);
                                    vout.attachViews();
                                }
                                isPlaying = false;
                                channelName.setText(playListEntries.get(0).get(channelIndex).getChannelName());
                                channelNum.setText(String.valueOf(channelIndex + 1) + "/" + String.valueOf(playListEntries.get(0).size()));
                                new DownloadImageFromInternet(mSurface, progressDialog)
                                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, playListEntries.get(0).get(channelIndex).getLogoURL());
                                System.out.println("Line 553 progress bar visible");
                                pbar.setVisibility(View.VISIBLE);
                                _play.setVisibility(View.INVISIBLE);
                                playVideo();
                            }
                        } else {
                            isTouched = true;
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
                checkWifiAndUpdate(0);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    private void performResetURLFromResume()
    {
        resetURL();
    }

    @Override
    protected void onResume() {
        isPaused = false;
        isTouched = false;
        super.onResume();
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WAKE_LOCK) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.DISABLE_KEYGUARD) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED) {
          checkWifiAndUpdate(1);
        }
    }

    @Override
    protected void onPause() {
        isPaused = true;
        if(mMediaPlayer!=null && mMediaPlayer.isPlaying()) {
            _play.setBackground(getDrawable(R.drawable.ic_media_play));
            mMediaPlayer.stop();
        }
        if(_aView!=null) {
            if (_aView.isPlaying()) {
                audio_play.setBackgroundResource(R.drawable.off);
                _aView.pause();
            }
        }
        super.onPause();
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS: {
                Map<String, Integer> perms = new HashMap<String, Integer>();
                // Initial
                perms.put(Manifest.permission.WAKE_LOCK,
                        PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.DISABLE_KEYGUARD,
                        PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.INTERNET,
                        PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.ACCESS_NETWORK_STATE,
                        PackageManager.PERMISSION_GRANTED);


                // Fill with results
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);
                // Check for ACCESS_FINE_LOCATION
                if (perms.get(Manifest.permission.WAKE_LOCK) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.DISABLE_KEYGUARD) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED) {
                    // All Permissions Granted
                    Toast.makeText(MainActivity.this, "Thanks! I'll take care of streaming from now",
                            Toast.LENGTH_SHORT).show();
                    Toast.makeText(MainActivity.this, "Just a moment, restarting the App",
                            Toast.LENGTH_SHORT).show();
                    new CountDownTimer(2000,1000) {

                        @Override
                        public void onTick(long millisUntilFinished) {

                        }

                        @Override
                        public void onFinish() {
                            finish();
                            startActivity(getIntent());
                        }
                    }.start();
                } else {
                    // Permission Denied
                    finish();
                    showMessageOKCancel("Sorry!, you denied my Permission Requests!");
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions,
                        grantResults);
        }
    }

    private boolean addPermission(List<String> permissionsList,
                                  String permission) {
        if (Build.VERSION.SDK_INT >= M) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsList.add(permission);
                // Check for Rationale Option
                if (Build.VERSION.SDK_INT >= M) {
                    if (!shouldShowRequestPermissionRationale(permission))
                        return false;
                }
            }
        }
        return true;
    }

    private void showMessageOKCancel(String message) {
        new AlertDialog.Builder(MainActivity.this).setMessage(message).create()
                .show();
    }


    public void checkWifiAndUpdate(Integer updateId) {
        // TODO Auto-generated method stub
        if(!isPaused) {
            new ConnectionToInternet(MainActivity.this, this, progressDialog, updateId).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    public void showInterstitialAd() {
        // TODO Auto-generated method stub
        if(_aView.isPlaying()) {
            audio_play.setBackgroundResource(R.drawable.off);
            _aView.pause();
        }
       checkWifiAndUpdate(3);
    }


    private void performAdLoading()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                interAd.setAdListener(new AdListener() {
                    @Override
                    public void onAdClosed() {
                        AdRequest.Builder builder = new AdRequest.Builder();
                        AdRequest ad = builder.build();
                        interAd.loadAd(ad);
                    }
                });
                interAd.show();
                System.out.println("ad shown");
            }
        });
    }

    @Override
    public void onNewLayout(IVLCVout vlcVout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {

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

    private class DownloadImageFromInternet extends AsyncTask<String, Void, Bitmap> {
        SurfaceView surfaceView;
        private ProgressDialog progressDialog;

        public DownloadImageFromInternet(SurfaceView _surfaceView, ProgressDialog pd) {
            surfaceView = _surfaceView;
            progressDialog = pd;
        }

        protected Bitmap doInBackground(String... urls) {
            Bitmap bimage = null;
            if(!URLUtil.isValidUrl(urls[0]))
            {
                return bimage;
            }
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
                httpURLConnection.setInstanceFollowRedirects(false);
                String imageURL = httpURLConnection.getHeaderField("Location");
                if(imageURL==null)
                    imageURL = url.toString();
                InputStream in = new URL(imageURL).openStream();
                byte[] buffer = new byte[1024];
                int total_size = 0, bytesRead = 0;
                while((bytesRead = in.read(buffer)) !=-1) {
                    total_size += bytesRead;
                }
                if(total_size > 100000)
                {
                    int factor = total_size / 100000;
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = factor/2;
                    InputStream is = new URL(imageURL).openStream();
                    bimage = BitmapFactory.decodeStream(is, null, options);
                }
                else {
                    InputStream is = new URL(imageURL).openStream();
                    bimage = BitmapFactory.decodeStream(is);
                }
                if(bimage!=null) {
                    int width = bimage.getWidth();
                    int height = bimage.getHeight();
                    if(width * height > 999999) {
                        float scaleWidth = ((float) (width / 5)) / width;
                        float scaleHeight = ((float) (height / 5)) / height;
                        // CREATE A MATRIX FOR THE MANIPULATION
                        Matrix matrix = new Matrix();
                        // RESIZE THE BIT MAP
                        matrix.postScale(scaleWidth, scaleHeight);
                        // RECREATE THE NEW BITMAP
                        Bitmap resizedBitmap = Bitmap.createBitmap(bimage, 0, 0, width, height, matrix, false);
                        bimage = resizedBitmap;
                    }
                }
            } catch (Exception e) {
                Log.e("Error Message", e.getMessage());
                e.printStackTrace();
            }
            return bimage;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.setMessage("Updating Album Cover...");
            progressDialog.show();
        }

        protected void onPostExecute(Bitmap result) {
            if ((progressDialog.getWindow()!=null) && (progressDialog != null) && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            if(result!=null) {
                surfaceView.setBackground(new BitmapDrawable(getResources(),result));
            }
        }
    }

    private class ConnectionToInternet extends AsyncTask<String, Void, ReturnValueForAction> {
        Context cx;
        private InternetChecker<ReturnValueForAction> callback;
        private ProgressDialog progressDialog;
        private Integer updateId;

        public ConnectionToInternet(Context c, InternetChecker<ReturnValueForAction> cb, ProgressDialog pd, Integer updateId) {
            cx = c;
            callback = cb;
            progressDialog = pd;
            this.updateId = updateId;
        }

        protected ReturnValueForAction doInBackground(String... urls) {
            boolean networkExists = false;
            try {
                SocketAddress sockaddr = new InetSocketAddress("google.com", 80);
                // Create an unbound socket
                Socket sock = new Socket();

                // This method will block no more than timeoutMs.
                // If the timeout occurs, SocketTimeoutException is thrown.
                int timeoutMs = 5000;   // 5 seconds
                sock.connect(sockaddr, timeoutMs);
                networkExists = true;
            } catch(IOException e) {
                // Handle exception
                System.out.println(e.getMessage());
            }
            ReturnValueForAction returnValueForAction = new ReturnValueForAction();
            returnValueForAction.updateId = updateId;
            returnValueForAction.bIsConnected = networkExists;
            return returnValueForAction;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.setMessage("Checking your internet connectivity...");
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(ReturnValueForAction aBoolean) {
            super.onPostExecute(aBoolean);

            if ((progressDialog.getWindow()!=null) && (progressDialog != null) && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            callback.OnCheckNetworkConnection(aBoolean);
        }
    }

    private class ValidateTheURL extends AsyncTask<String, Void, Boolean> {

        Context cx;
        private UrlValidator<Boolean> callback;
        private ProgressDialog progressDialog;

        public ValidateTheURL(Context c, UrlValidator<Boolean> cb, ProgressDialog pd) {
            cx = c;
            callback = cb;
            progressDialog = pd;
        }

        protected Boolean doInBackground(String... urls) {
            boolean networkExists = false;
            try {
                if(urls[0].contains("rtmp") || urls[0].contains("rtsp") || urls[0].contains("m3u8"))
                {
                    networkExists = true;
                    if(urls[0].contains("rtsp") || urls[0].contains("m3u8")) {
                        isNative = true;
                    }
                    else if(urls[0].contains("rtmp"))
                    {
                        isNative = false;
                    }
                }
                else {
                    HttpURLConnection con = (HttpURLConnection) new URL(urls[0]).openConnection(Proxy.NO_PROXY);
                    // stop following browser redirect
                    con.setInstanceFollowRedirects(false);
                    String type = con.getContentType();

                    if(type!=null && type.contains("application/force-download")) {
                        isNative = true;
                    }
                    else
                    {
                        isNative = false;
                    }
                    if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        networkExists = true;
                    }
                }
            } catch(IOException e) {
                // Handle exception
                System.out.println(e.getMessage());
            }
            return networkExists;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.setMessage("Loading media....");
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if ((progressDialog.getWindow()!=null) && (progressDialog != null) && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            callback.OnValidURL(aBoolean);
        }
    }


    private class decodeURL extends AsyncTask<String, Void, UrlDecoderData> {
        Context cx;
        private UrlDecoder<UrlDecoderData> callback;
        private ProgressDialog progressDialog;
        private Integer updateId;

        public decodeURL(Context c, UrlDecoder<UrlDecoderData> cb, ProgressDialog pd, Integer updateId) {
            cx = c;
            callback = cb;
            progressDialog = pd;
            this.updateId = updateId;
        }

        protected UrlDecoderData doInBackground(String... urls) {
            UrlDecoderData originalURL = new UrlDecoderData();
            originalURL.updateId = updateId;
            try
            {
                if(urls[0].contains("rtmp") || urls[0].contains("rtsp")) {
                    originalURL.decodedURL = urls[0];
                }
                else
                {
                    URL url = new URL(urls[0]);
                    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
                    httpURLConnection.setInstanceFollowRedirects(false);
                    originalURL.decodedURL = httpURLConnection.getHeaderField("Location");
                    if(originalURL.decodedURL == null)
                    {
                        originalURL.decodedURL = url.toString();
                    }
                }
            } catch(IOException e) {
                // Handle exception
                System.out.println(e.getMessage());
            }
            return originalURL;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.setMessage("Connected. Loading Channel information....");
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(UrlDecoderData s) {
            super.onPostExecute(s);
            if ((progressDialog.getWindow()!=null) && (progressDialog != null) && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            callback.OnURLDecoded(s);
        }
    }

    private static class MyPlayerListener implements org.videolan.libvlc.MediaPlayer.EventListener {
        private WeakReference<MainActivity> mOwner;

        public MyPlayerListener(MainActivity owner) {
            mOwner = new WeakReference<MainActivity>(owner);
        }

        @Override
        public void onEvent(org.videolan.libvlc.MediaPlayer.Event event) {
            MainActivity player = mOwner.get();

            switch (event.type) {
                case org.videolan.libvlc.MediaPlayer.Event.EndReached:
                    Log.d(TAG, "MediaPlayerEndReached");
                    player.preferencesEditor.putBoolean("isPlaying", false);
                    player.preferencesEditor.apply();
                    player.playNext();
                    break;
                case org.videolan.libvlc.MediaPlayer.Event.Playing:
                    if(!player.isVideo) {
                        System.out.println("Line 1593 progress bar gone");
                        player.pbar.setVisibility(View.GONE);
                        player._play.setVisibility(View.VISIBLE);
                        player._play.setBackground(player.getDrawable(R.drawable.ic_media_pause));
                    }
                    break;
                case org.videolan.libvlc.MediaPlayer.Event.EncounteredError:
                    player.preferencesEditor.putBoolean("isPlaying", false);
                    player.preferencesEditor.apply();
                    player.showMessageError(player.channelName.getText().toString());
                    player.playNextOnError();
                    break;
                case org.videolan.libvlc.MediaPlayer.Event.Paused:
                case org.videolan.libvlc.MediaPlayer.Event.Stopped:
                default:
                    break;
            }
        }
    }

}
