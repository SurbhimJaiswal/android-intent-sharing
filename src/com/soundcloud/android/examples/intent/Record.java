package com.soundcloud.android.examples.intent;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class Record extends Activity {
    private boolean mStarted;
    private MediaRecorder mRecorder;


    private static final File PATH = new File("/sdcard/test.mp3");

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.record);

        final Button record_btn = (Button) findViewById(R.id.record_btn);
        final Button share_btn = (Button) findViewById(R.id.share_btn);
        final Button play_btn = (Button) findViewById(R.id.play_btn);

        Record last = getLastNonConfigurationInstance();
        if (last != null) {
            mStarted = last.mStarted;
            mRecorder = last.mRecorder;
            record_btn.setText(mStarted ? R.string.stop : R.string.record);
        }

        record_btn.setOnClickListener(new View.OnClickListener() {
            public synchronized void onClick(View v) {
                if (!mStarted) {
                    Toast.makeText(Record.this, R.string.recording, Toast.LENGTH_SHORT).show();

                    mStarted = true;
                    mRecorder = getRecorder(PATH);
                    mRecorder.start();
                    record_btn.setText(R.string.stop);
                } else {
                    Toast.makeText(Record.this, R.string.recording_stopped, Toast.LENGTH_SHORT).show();

                    mRecorder.stop();
                    mRecorder.release();
                    mRecorder = null;
                    mStarted = false;

                    record_btn.setText(R.string.record);
                    share_btn.setEnabled(true);
                    play_btn.setEnabled(true);
                }
            }
        });


        play_btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                play_btn.setEnabled(false);
                record_btn.setEnabled(false);
                share_btn.setEnabled(false);
                play(new MediaPlayer.OnCompletionListener() {
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        play_btn.setEnabled(true);
                        record_btn.setEnabled(true);
                        share_btn.setEnabled(true);
                    }
                });
            }
        });

        share_btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                share();
            }
        });
    }

    private void play(MediaPlayer.OnCompletionListener onCompletion) {
        MediaPlayer player = new MediaPlayer();
        try {
            player.setDataSource(PATH.getAbsolutePath());
            player.prepare();
            player.setOnCompletionListener(onCompletion);
            player.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void share() {
        Intent intent = new Intent("com.soundcloud.android.SHARE")
                .putExtra(Intent.EXTRA_STREAM, Uri.fromFile(PATH))
                // here you can set metadata for the track to be uploaded
                .putExtra("com.soundcloud.android.extra.title", "SoundCloud Demo upload")
                .putExtra("com.soundcloud.android.extra.where", "Somewhere")
                .putExtra("com.soundcloud.android.extra.public", false);

        startActivityForResult(intent, 0);
    }

    @Override public Record getLastNonConfigurationInstance() {
        return (Record) super.getLastNonConfigurationInstance();
    }

    @Override public Record onRetainNonConfigurationInstance() {
        return this;
    }

    // callback gets executed when the SoundCloud app returns
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Toast.makeText(this, "Shared", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show();
        }
    }

    private MediaRecorder getRecorder(File path) {
        MediaRecorder recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);

         if (Build.VERSION.SDK_INT >= 8) {
             recorder.setAudioSamplingRate(44100);
             recorder.setAudioEncodingBitRate(96000);
             recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
             recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
         } else {
             // older version of Android, use crappy sounding voice codec
             recorder.setAudioSamplingRate(8000);
             recorder.setAudioEncodingBitRate(12200);
             recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
             recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
         }

        recorder.setOutputFile(path.getAbsolutePath());

        try {
            recorder.prepare();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return recorder;
    }
}
