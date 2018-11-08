package com.example.musicplayer1;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;
    private TextView musicStatus, musicTime, musicTotal, musicName;
    private SeekBar seekBar;

    private Button btnPlayOrPause, btnStop, btnQuit, btnPrevious, btnNext, btnMode;
    private SimpleDateFormat time = new SimpleDateFormat("mm:ss");

    private boolean tag1 = false;
    private boolean tag2 = false;
    private static MusicService musicService;

    public static List<MusicInfo> musicList;

    //播放模式
    public static final int SINGLE_CYCLE = 1;     // 单曲循环
    public static final int ALL_CYCLE = 2;        //顺序循环

    public static int currentMode = SINGLE_CYCLE;
    public static int position = 0;


    //  在Activity中调用 bindService 保持与 Service 的通信
    private void bindServiceConnection() {
        Intent intent = new Intent(MainActivity.this, MusicService.class);
        startService(intent);
        bindService(intent, serviceConnection, this.BIND_AUTO_CREATE);
    }

    //  回调onServiceConnected 函数，通过IBinder 获取 Service对象，实现Activity与 Service的绑定
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            musicService = ((MusicService.MyBinder) (service)).getService();
            Log.i("musicService", musicService + "");
            musicTotal.setText(time.format(musicService.mediaPlayer.getDuration()));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
        }
    };

    private void findViewById() {
        musicTime = (TextView) findViewById(R.id.MusicTime);
        musicTotal = (TextView) findViewById(R.id.MusicTotal);
        musicName = (TextView) findViewById(R.id.musicName);
        seekBar = (SeekBar) findViewById(R.id.MusicSeekBar);
        btnPlayOrPause = (Button) findViewById(R.id.BtnPlayorPause);
        btnStop = (Button) findViewById(R.id.BtnStop);
        btnQuit = (Button) findViewById(R.id.BtnQuit);
        btnPrevious = (Button) findViewById(R.id.BtnPrevious);
        btnNext = (Button) findViewById(R.id.BtnNext);
        btnMode = (Button) findViewById(R.id.BtnMode);
        musicStatus = (TextView) findViewById(R.id.MusicStatus);
    }

    private void initMusic() {
        ContentResolver contentResolver = MainActivity.this.getContentResolver();

        Cursor mCursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,   //路径
                new String[]{MediaStore.Audio.Media._ID,    //写入我想要获得的信息（列）
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.ALBUM,
                        MediaStore.Audio.Media.DISPLAY_NAME,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.DURATION,
                        MediaStore.Audio.Media.DATA}, null, null, null);

        musicList = new ArrayList<>();

        for (int i = 0; i < mCursor.getCount(); ++i) {
            MusicInfo musicInfo = new MusicInfo();  //MusicInfo类是数据储存单元
            mCursor.moveToNext();   //读取下一行，moveToNext()有boolean返回值，执行成功返回ture,反之false，可用于判断是否读取完毕。
            musicInfo.id = mCursor.getLong(0);
            musicInfo.title = mCursor.getString(1);
            musicInfo.album = mCursor.getString(2);
            musicInfo.displayName = mCursor.getString(3);
            musicInfo.artist = mCursor.getString(4);
            musicInfo.duration = mCursor.getLong(5);
            musicInfo.url = mCursor.getString(6);   //转存数据

            musicList.add(musicInfo);  //添加到List
        }

        musicName.setText(musicList.get(0).title);
    }

    //  通过 Handler 更新 UI 上的组件状态
    public Handler handler = new Handler();
    public Runnable runnable = new Runnable() {
        @Override
        public void run() {
            musicTime.setText(time.format(musicService.mediaPlayer.getCurrentPosition()));
            musicName.setText(musicList.get(position).title);
            seekBar.setProgress(musicService.mediaPlayer.getCurrentPosition());
            seekBar.setMax(musicService.mediaPlayer.getDuration());
            musicTotal.setText(time.format(musicService.mediaPlayer.getDuration()));
            handler.postDelayed(runnable, 100);

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);  //保存实例
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            int hasWriteContactsPermission = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_ASK_PERMISSIONS);
            }else {
                findViewById();
                initMusic();
                bindServiceConnection();
                myListener();
                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser == true) {
                            musicService.mediaPlayer.seekTo(progress);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
            }
        }

    }

    private void myListener() {
        ImageView imageView = (ImageView) findViewById(R.id.Image);
        btnPlayOrPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (musicService.mediaPlayer != null) {
                    seekBar.setProgress(musicService.mediaPlayer.getCurrentPosition());
                    seekBar.setMax(musicService.mediaPlayer.getDuration());
                }
                //  由tag的变换来控制事件的调用
                if (musicService.tag != true) {
                    btnPlayOrPause.setText("PAUSE");
                    musicStatus.setText("Playing");
                    musicService.playOrPause();
                    musicService.tag = true;

                    if (tag1 == false) {
                        tag1 = true;
                    } else {
                    }
                } else {
                    btnPlayOrPause.setText("PLAY");
                    musicStatus.setText("Paused");
                    musicService.playOrPause();
                    musicService.tag = false;
                }
                if (tag2 == false) {
                    handler.post(runnable);
                    tag2 = true;
                }
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                musicStatus.setText("Stopped");
                btnPlayOrPause.setText("PLAY");
                musicService.stop();
                musicService.tag = false;
            }
        });

        //  停止服务时，必须解除绑定，写入btnQuit按钮中
        btnQuit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handler.removeCallbacks(runnable);
                unbindService(serviceConnection);
                Intent intent = new Intent(MainActivity.this, MusicService.class);
                stopService(intent);
                try {
                    MainActivity.this.finish();
                } catch (Exception e) {

                }
            }
        });

        btnPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playPreviousSong();
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playNextSong();
            }
        });

        btnMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeMode();
            }
        });
    }

    /**
     * 更换播放模式
     * <p>
     * 单曲循环 → 顺序播放 → 单曲循环
     */
    private void changeMode() {
        switch (currentMode) {
            case SINGLE_CYCLE:
                currentMode = ALL_CYCLE;
                btnMode.setText("顺序播放");
                break;
            case ALL_CYCLE:
                currentMode = SINGLE_CYCLE;
                btnMode.setText("单曲循环");
                break;
        }

    }

    //  获取并设置返回键的点击事件
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(false);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public static void playPreviousSong() {
        if (position == 0) {
            position = musicList.size() - 1;
        } else {
            position--;
        }
        String songUrl = musicList.get(position).url;
        try {
            musicService.mediaPlayer.reset();
            musicService.mediaPlayer.setDataSource(songUrl);
            musicService.mediaPlayer.prepare();
            musicService.mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void playNextSong() {
        if (position == musicList.size() - 1) {
            position = 0;
        } else {
            position++;
        }
        String songUrl = musicList.get(position).url;
        try {
            musicService.mediaPlayer.reset();
            musicService.mediaPlayer.setDataSource(songUrl);
            musicService.mediaPlayer.prepare();
            musicService.mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_ASK_PERMISSIONS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 用户成功授予权限
                findViewById();
                initMusic();
                bindServiceConnection();
                myListener();
                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser == true) {
                            musicService.mediaPlayer.seekTo(progress);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
            } else {
                Toast.makeText(this, "你拒绝了此应用对读取内容的申请！", Toast.LENGTH_SHORT).show();
            }
        }
    }
}



