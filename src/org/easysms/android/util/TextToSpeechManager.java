package org.easysms.android.util;

import java.util.Locale;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

public class TextToSpeechManager implements TextToSpeech.OnInitListener {
	private static TextToSpeechManager sInstance;
	private static final String TAG = "TextToSpeechManager";

	public static TextToSpeechManager getInstance() {
		if (sInstance == null) {
			throw new IllegalStateException("Uninitialized.");
		}
		return sInstance;
	}

	public static void init(Context context) {

		if (sInstance != null) {
			Log.w(TAG, "Already initialized.");
		}
		sInstance = new TextToSpeechManager(context);
	}

	private Context mContext;
	private Locale mLocale;
	private TextToSpeech mTts;

	/**
	 * Creates a new TextToSpeechManager instance.
	 * 
	 * @param context
	 *            current application context.
	 */
	private TextToSpeechManager(Context context) {
		mContext = context;
		mLocale = Locale.FRENCH;
		mTts = new TextToSpeech(mContext, this);
	}

	public Locale getLocale() {
		return mLocale;
	}

	// Implements TextToSpeech.OnInitListener.
	public void onInit(int status) {
		// status can be either TextToSpeech.SUCCESS or TextToSpeech.ERROR.
		if (status == TextToSpeech.SUCCESS) {
			// Set preferred language to US English.
			// Note that a language may not be available, and the result will
			// indicate this.
			int result = mTts.setLanguage(mLocale);

			if (result == TextToSpeech.LANG_MISSING_DATA
					|| result == TextToSpeech.LANG_NOT_SUPPORTED) {
				// language data is missing or the language is not supported.
				Log.e(TAG, "Language is not available.");
			} else {
				// Check the documentation for other possible result codes.
				// For example, the language may be available for the locale,
				// but not for the specified country and variant.

				// The TTS engine has been successfully initialized.
				// Allow the user to press the button for the app to speak
				// again.
				// Greet the user.
				// sayHello();
			}
		} else {
			// Initialization failed.
			Log.e(TAG, "Could not initialize TextToSpeech.");
		}
	}

	public void say(String sentence) {
		mTts.setLanguage(mLocale);
		// drop all pending entries in the playback queue.
		mTts.speak(sentence, TextToSpeech.QUEUE_FLUSH, null);
	}

	public void setLocale(Locale value) {
		mLocale = value;
	}
}
