# Sharing sounds to SoundCloud via intents

This is a small sample app to demonstrate how you could integrate SoundCloud in your
own Android application. You can record some audio and then upload it to
SoundCloud with the official Android app (which must be installed).

If it is not installed the user will be prompted to install it.

## How sharing works

The integration works with the standard [Android intent model][]:


    File myAudiofile = new File("/path/to/audio.mp3");
    Intent intent = new Intent("com.soundcloud.android.SHARE")
        .putExtra(Intent.EXTRA_STREAM, Uri.fromFile(myAudiofile))
        .putExtra("com.soundcloud.android.extra.title", "Demo")
        .putExtra("com.soundcloud.android.extra.description", "Testing");

    try {
       startActivityForResult(intent, 0);
    } catch (ActivityNotFoundException e) {
      // SoundCloud Android app not installed, show a dialog etc.
    }


## Intent extras

The following extras can be set on the intent to set the metadata of the
uploaded track (all except the first need to be prefixed with `com.soundcloud.android.extra`)

  * android.intent.extra.STREAM (`android.net.Uri`) - the audio data to be
  uploaded (needs to have the `file` schema)
  * title (`String`) the title of the track
  * where (`String`) the location of the recording
  * description (`String`)
  * public (`boolean`) if the track should be public or not
  * location (`android.location.Location`) the location
  * tags (`String[]`) tags for the track
  * genre (`String`)
  * artwork (`android.net.Uri`) artwork to use for this track (needs to be
  `file` schema)

## I need more control over how the file is uploaded!

The share intent tries to cover the basic use cases but sometimes you just want
to do things differently, or you need access to other API features.

In this case check out the [Android token sharing][] sample project which
demonstrates how to obtain a token and talk to the SoundCloud API directly.

[Android intent model]: http://developer.android.com/reference/android/content/Intent.html
[Android token sharing]: https://github.com/soundcloud/android-token-sharing
