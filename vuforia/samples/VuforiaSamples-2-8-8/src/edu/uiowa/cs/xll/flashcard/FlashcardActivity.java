package edu.uiowa.cs.xll.flashcard;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import android.app.Activity;
import android.os.Bundle;

import com.qualcomm.vuforia.samples.VuforiaSamples.app.TextRecognition.TextReco;

public class FlashcardActivity extends Activity {
	Map<String, Integer> mDB = new TreeMap<String, Integer>();;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		TextReco.loadDB(this, mDB);
		
	}

	@Override
	protected void onDestroy() {
		try {
			TextReco.saveDB(this, mDB);
		} catch (IOException e) {
			e.printStackTrace();
		}
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

}
