package com.soundcloud.android.examples.intent;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class Record extends Activity {
    public static final String TAG = "soundcloud-intent-sharing-example";

    private boolean mStarted;
    private MediaRecorder mRecorder;
    private File mArtwork;

    private static boolean AAC_SUPPORTED  = Build.VERSION.SDK_INT >= 10;
    private static final int PICK_ARTWORK = 1;
    private static final int SHARE_SOUND  = 2;

    private static final File FILES_PATH = new File(
        Environment.getExternalStorageDirectory(),
        "Android/data/com.soundcloud.android.examples/files");

    private static final File RECORDING = new File(
            FILES_PATH,
            "demo-recording" + (AAC_SUPPORTED ? ".mp4" : "3gp"));

    private static final Uri MARKET_URI = Uri.parse("market://details?id=com.soundcloud.android");
    private static final int DIALOG_NOT_INSTALLED = 0;

    // Replace with the client id of your registered app!
    // see http://soundcloud.com/you/apps/
    private static final String CLIENT_ID = "fecfc092de134a960dc48e53c044ee91";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Environment.MEDIA_MOUNTED.equals(
            Environment.getExternalStorageState())) {
            if (!FILES_PATH.mkdirs()) {
                Log.w(TAG, "Could not create " + FILES_PATH);
            }
        } else {
            Toast.makeText(this, R.string.need_external_storage, Toast.LENGTH_LONG).show();
            finish();
        }

        setContentView(R.layout.record);

        final Button record_btn = (Button) findViewById(R.id.record_btn);
        final Button share_btn = (Button) findViewById(R.id.share_btn);
        final Button play_btn = (Button) findViewById(R.id.play_btn);
        final Button artwork_btn = (Button) findViewById(R.id.artwork_btn);

        Record last = getLastNonConfigurationInstance();
        if (last != null) {
            mStarted  = last.mStarted;
            mRecorder = last.mRecorder;
            mArtwork  = last.mArtwork;
            record_btn.setText(mStarted ? R.string.stop : R.string.record);
        }

        record_btn.setOnClickListener(new View.OnClickListener() {
            public synchronized void onClick(View v) {
                if (!mStarted) {
                    Toast.makeText(Record.this, R.string.recording, Toast.LENGTH_SHORT).show();

                    mStarted = true;
                    mRecorder = getRecorder(RECORDING, AAC_SUPPORTED);
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
                    artwork_btn.setEnabled(true);
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
                shareSound();
            }
        });

        artwork_btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("image/*"), PICK_ARTWORK);
            }
        });
    }

    private void play(MediaPlayer.OnCompletionListener onCompletion) {
        MediaPlayer player = new MediaPlayer();
        try {
            player.setDataSource(RECORDING.getAbsolutePath());
            player.prepare();
            player.setOnCompletionListener(onCompletion);
            player.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    // the actual sharing happens here
    private void shareSound() {
        Intent intent = new Intent("com.soundcloud.android.SHARE")
                .putExtra(Intent.EXTRA_STREAM, Uri.fromFile(RECORDING))
                // here you can set metadata for the track to be uploaded
                .putExtra("com.soundcloud.android.extra.title", "SoundCloud Android Intent Demo upload")
                .putExtra("com.soundcloud.android.extra.where", "Somewhere")
                .putExtra("com.soundcloud.android.extra.description", "This is a demo track.")
                .putExtra("com.soundcloud.android.extra.public", true)
                .putExtra("com.soundcloud.android.extra.tags", new String[] {
                    "demo",
                    "post lolcat bluez",
                    "soundcloud:created-with-client-id="+CLIENT_ID
                 })
                .putExtra("com.soundcloud.android.extra.genre", "Easy Listening")
                .putExtra("com.soundcloud.android.extra.location", getLocation());

        // attach artwork if user has picked one
        if (mArtwork != null) {
            intent.putExtra("com.soundcloud.android.extra.artwork", Uri.fromFile(mArtwork));
        }

        try {
            startActivityForResult(intent, SHARE_SOUND);
        } catch (ActivityNotFoundException notFound) {
            // use doesn't have SoundCloud app installed, show a dialog box
            showDialog(DIALOG_NOT_INSTALLED);
        }
    }


    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SHARE_SOUND:
                // callback gets executed when the SoundCloud app returns
                if (resultCode == RESULT_OK) {
                    Toast.makeText(this, R.string.shared_ok, Toast.LENGTH_SHORT).show();
                } else {
                    // canceled
                    Toast.makeText(this, R.string.shared_canceled, Toast.LENGTH_SHORT).show();
                }
                break;

            case PICK_ARTWORK:
                if (resultCode == RESULT_OK) {
                    mArtwork = getFromMediaUri(getContentResolver(), data.getData());
                }
                break;
        }
    }

    private MediaRecorder getRecorder(File path, boolean useAAC) {
        MediaRecorder recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);

         if (useAAC) {
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

    // just get the last known location from the passive provider - not terribly
    // accurate but it's a demo app.
    private Location getLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
    }

    @Override public Record getLastNonConfigurationInstance() {
        return (Record) super.getLastNonConfigurationInstance();
    }

    @Override public Record onRetainNonConfigurationInstance() {
        return this;
    }

    // Helper method to get file from a content uri
    private static File getFromMediaUri(ContentResolver resolver, Uri uri) {
        if ("file".equals(uri.getScheme())) {
            return new File(uri.getPath());
        } else if ("content".equals(uri.getScheme())) {
            String[] filePathColumn = {MediaStore.MediaColumns.DATA};
            Cursor cursor = resolver.query(uri, filePathColumn, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                        String filePath = cursor.getString(columnIndex);
                        return new File(filePath);
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        return null;
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle data) {
        if (DIALOG_NOT_INSTALLED == id) {
            return new AlertDialog.Builder(this)
                    .setTitle(R.string.sc_app_not_found)
                    .setMessage(R.string.sc_app_not_found_message)
                    .setPositiveButton(android.R.string.yes, new Dialog.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent market = new Intent(Intent.ACTION_VIEW, MARKET_URI);
                            startActivity(market);
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    }).create();
        } else {
            return null;
        }
    }
}
