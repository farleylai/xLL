/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/

package com.qualcomm.vuforia.samples.VuforiaSamples.app.TextRecognition;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.qualcomm.vuforia.CameraDevice;
import com.qualcomm.vuforia.RectangleInt;
import com.qualcomm.vuforia.Renderer;
import com.qualcomm.vuforia.State;
import com.qualcomm.vuforia.TextTracker;
import com.qualcomm.vuforia.Tracker;
import com.qualcomm.vuforia.TrackerManager;
import com.qualcomm.vuforia.VideoBackgroundConfig;
import com.qualcomm.vuforia.VideoMode;
import com.qualcomm.vuforia.Vuforia;
import com.qualcomm.vuforia.WordList;
import com.qualcomm.vuforia.samples.SampleApplication.SampleApplicationControl;
import com.qualcomm.vuforia.samples.SampleApplication.SampleApplicationException;
import com.qualcomm.vuforia.samples.SampleApplication.SampleApplicationSession;
import com.qualcomm.vuforia.samples.SampleApplication.utils.LoadingDialogHandler;
import com.qualcomm.vuforia.samples.SampleApplication.utils.SampleApplicationGLView;
import com.qualcomm.vuforia.samples.SampleApplication.utils.SampleUtils;
import com.qualcomm.vuforia.samples.VuforiaSamples.R;
import com.qualcomm.vuforia.samples.VuforiaSamples.app.TextRecognition.TextRecoRenderer.WordDesc;
import com.qualcomm.vuforia.samples.VuforiaSamples.ui.SampleAppMenu.SampleAppMenu;
import com.qualcomm.vuforia.samples.VuforiaSamples.ui.SampleAppMenu.SampleAppMenuGroup;
import com.qualcomm.vuforia.samples.VuforiaSamples.ui.SampleAppMenu.SampleAppMenuInterface;


public class TextReco extends Activity implements SampleApplicationControl,
    SampleAppMenuInterface
{
    private static final String TAG = "DictEye";
    private static final int COLOR_OPAQUE = Color.argb(178, 0, 0, 0);
    private static final int WORDLIST_MARGIN = 10;
    private SampleApplicationSession vuforiaAppSession;    
    
    // Our OpenGL view:
    private SampleApplicationGLView mGlView;
    
    // Our renderer:
    private TextRecoRenderer mRenderer;
    
    private SampleAppMenu mSampleAppMenu;
    
    private ArrayList<View> mSettingsAdditionalViews;
    
    private RelativeLayout mUILayout;
    
    private LoadingDialogHandler loadingDialogHandler = new LoadingDialogHandler(
        this);
    private boolean mIsTablet = false;
    
    private boolean mIsVuforiaStarted = false;
    
    private GestureDetector mGestureDetector;
    
    private boolean mAdvanced = true;
    private boolean mFlash = false;

    private View mLevelOptionView;
    private View mFlashOptionView;
    
    private boolean mIsDroidDevice = false;
    
    private Map<String, String> mDictionary = new HashMap<String, String>();
    private Map<String, Integer> mDB = new TreeMap<String, Integer>();
    

    
    
    // Called when the activity first starts or the user navigates back to an
    // activity.
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        vuforiaAppSession = new SampleApplicationSession(this);        
        startLoadingAnimation();
        
        // xLL: landscape mode
//        vuforiaAppSession.initAR(this, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        vuforiaAppSession.initAR(this, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);       
        mGestureDetector = new GestureDetector(this, new GestureListener());        
        mIsDroidDevice = android.os.Build.MODEL.toLowerCase().startsWith("droid");
        loadDictionary(this, mDictionary);
        loadDB(this, mDB);
    }
    
    // Process Single Tap event to trigger autofocus
    private class GestureListener extends
        GestureDetector.SimpleOnGestureListener
    {
        // Used to set autofocus one second after a manual focus is triggered
        private final Handler autofocusHandler = new Handler();
        
        
        @Override
        public boolean onDown(MotionEvent e)
        {
            return true;
        }
        
        
        @Override
        public boolean onSingleTapUp(MotionEvent e)
        {
            // Generates a Handler to trigger autofocus
            // after 1 second
            autofocusHandler.postDelayed(new Runnable()
            {
                public void run()
                {
                    boolean result = CameraDevice.getInstance().setFocusMode(
                        CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO);
                    
                    if (!result)
                        Log.e("SingleTapUp", "Unable to trigger focus");
                }
            }, 1000L);
            
            return true;
        }
    }
    
    
    // Called when the activity will start interacting with the user.
    @Override
    protected void onResume()
    {
        Log.d(TAG, "onResume");
        super.onResume();
        
        // This is needed for some Droid devices to force portrait
        if (mIsDroidDevice)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        
        try
        {
            vuforiaAppSession.resumeAR();
        } catch (SampleApplicationException e)
        {
            Log.e(TAG, e.getString());
        }
        
        if (mIsVuforiaStarted)
            postStartCamera();
        
        // Resume the GL view:
        if (mGlView != null)
        {
            mGlView.setVisibility(View.VISIBLE);
            mGlView.onResume();
        }
        
    }
    
    
    // Callback for configuration changes the activity handles itself
    @Override
    public void onConfigurationChanged(Configuration config)
    {
        Log.d(TAG, "onConfigurationChanged");
        super.onConfigurationChanged(config);
        
        // xLL: avoid unexpected triggering
        if(mRenderer == null) return;
        
        vuforiaAppSession.onConfigurationChanged();
        configureVideoBackgroundROI();
    }
    
    
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        // Process the Gestures
        if (mSampleAppMenu != null && mSampleAppMenu.processEvent(event))
            return true;
        
        return mGestureDetector.onTouchEvent(event);
    }
    
    public static void loadDictionary(Context ctx, Map<String, String> dict) {
    	AssetManager assetManager = ctx.getAssets();
        try {
			InputStream in = assetManager.open("TextReco/dict.lst");
			Scanner scanner = new Scanner(in, "UTF-8");
			scanner.useDelimiter(":\\s|\n");
			int entries = 0;
			while(scanner.hasNext()) {
				String word = scanner.next();
				if(word.isEmpty() || !scanner.hasNext()) break;
				String definition = scanner.next();
				dict.put(word, definition);
				entries++;
				Log.d(TAG, String.format("fetch %s: %s", word, definition));
			}
			scanner.close();
			Log.d(TAG, String.format("%d definition entries are loaded", entries));
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    private String lookup(String word) {
    	word = word.toLowerCase();
    	String definition = mDictionary.get(word);
    	if(definition == null)
    		return "";
    	else {
    		int count = 1;
    		if(mDB.get(word) != null)
    			count = mDB.get(word) + 1;
    		mDB.put(word, count);
    		return definition;
    	}
    }
    
    public static void loadDB(Context ctx, Map<String, Integer> DB) {
		try {
			Scanner db = new Scanner(ctx.openFileInput("DictEye.db"), "UTF-8");
			db.useDelimiter("\t|\n");
	    	DB.clear();
	    	while(db.hasNext()) {
	    		String word = db.next();
	    		if(word.isEmpty() || !db.hasNext()) break;
	    		int count = db.nextInt();
	    		DB.put(word, count);
//	    		Log.d(TAG, String.format("read %s\t%d", word, count));
	    	}
	    	db.close();
    		Log.d(TAG, String.format("loaded %d word stats entries from DB", DB.size()));
		} catch (FileNotFoundException e) {
			Log.d(TAG, e.getMessage());
		}
    	
    }
    
    public static void saveDB(Context ctx, Map<String, Integer> DB) throws IOException {
    	OutputStreamWriter db = new OutputStreamWriter(ctx.openFileOutput("DictEye.db", MODE_PRIVATE), "UTF-8");
    	for(String word: DB.keySet()) {
    		int count = DB.get(word);
    		db.append(String.format("%s\t%d\n", word, count));
//    		Log.d(TAG, String.format("%s: lookup for %d times", word, count));
    	}
    	db.close();
    	Log.d(TAG, String.format("saved %d word stats entries to DB", DB.size()));
    }
    
    // Called when the system is about to start resuming a previous activity.
	@Override
    protected void onPause()
    {
        Log.d(TAG, "onPause");
        try {
        	saveDB(this, mDB);
        } catch(IOException e) {
        	e.printStackTrace();
        }
        super.onPause();
        
        if (mGlView != null)
        {
            mGlView.setVisibility(View.INVISIBLE);
            mGlView.onPause();
        }
        
        // Turn off the flash
        if (mFlashOptionView != null && mFlash) {
            // OnCheckedChangeListener is called upon changing the checked state
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)           
                ((Switch) mFlashOptionView).setChecked(false);
            else            
                ((CheckBox) mFlashOptionView).setChecked(false);
        }
        
        try {
            vuforiaAppSession.pauseAR();
        } catch (SampleApplicationException e) {
            Log.e(TAG, e.getString());
        }
        
        stopCamera();
    }
       
    // The final call you receive before your activity is destroyed.
    @Override
    protected void onDestroy()
    {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        
        try {
            vuforiaAppSession.stopAR();
        } catch (SampleApplicationException e) {
            Log.e(TAG, e.getString());
        }
        
        System.gc();
    }    
    
    private void startLoadingAnimation()
    {
        LayoutInflater inflater = LayoutInflater.from(this);
        mUILayout = (RelativeLayout) inflater.inflate(
            R.layout.camera_overlay_textreco, null, false);
        
        mUILayout.setVisibility(View.VISIBLE);
        mUILayout.setBackgroundColor(Color.BLACK);
        
        // Gets a reference to the loading dialog
        loadingDialogHandler.mLoadingDialogContainer = mUILayout
            .findViewById(R.id.loading_indicator);
        
        // Shows the loading indicator at start
        loadingDialogHandler
            .sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);
        
        // Adds the inflated layout to the view
        addContentView(mUILayout, new LayoutParams(LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT));
        
    }
    
    
    // Initializes AR application components.
    private void initApplicationAR()
    {
        // Create OpenGL ES view:
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = Vuforia.requiresAlpha();
        
        mGlView = new SampleApplicationGLView(this);
        mGlView.init(translucent, depthSize, stencilSize);
        
        mRenderer = new TextRecoRenderer(this, vuforiaAppSession);
        mGlView.setRenderer(mRenderer);
        
//        mGlView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
//        mGlView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        showLoupe(false);
        
    }
    
    
    private void postStartCamera()
    {
        // Sets the layout background to transparent
        mUILayout.setBackgroundColor(Color.TRANSPARENT);
        
        // start the image tracker now that the camera is started
        Tracker t = TrackerManager.getInstance().getTracker(
            TextTracker.getClassType());
        if (t != null)
            t.start();
        
        configureVideoBackgroundROI();
    }
    
    private DisplayMetrics mMetrics;
    void configureVideoBackgroundROI()
    {
        VideoMode vm = CameraDevice.getInstance().getVideoMode(
            CameraDevice.MODE.MODE_DEFAULT);
        VideoBackgroundConfig config = Renderer.getInstance()
            .getVideoBackgroundConfig();
        
//        DisplayMetrics metrics = new DisplayMetrics();
        mMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
        int screenWidth = mMetrics.widthPixels;
        int screenHeight = mMetrics.heightPixels;
        
        {
            // calc ROI
            // width of margin is :
            // 5% of the width of the screen for a phone
            // 20% of the width of the screen for a tablet
            int marginWidth = mIsTablet ? (screenWidth * 20) / 100
                : (screenWidth * 5) / 100;
            
            // loupe height is :
            // 15% of the screen height for a phone
            // 10% of the screen height for a tablet
            int loupeHeight = mIsTablet ? (screenHeight * 20) / 100
                : (screenHeight * 30) / 100;
            
            // lupue width takes the width of the screen minus 2 margins
            int loupeWidth = screenWidth - (2 * marginWidth);
            
            // definition of the region of interest
            mRenderer.setROI(
            		screenWidth / 2, 
            		marginWidth + (loupeHeight / 2),
            		loupeWidth, 
            		loupeHeight);
        }
        
        // convert into camera coords
        int[] loupeCenterX = { 0 };
        int[] loupeCenterY = { 0 };
        int[] loupeWidth = { 0 };
        int[] loupeHeight = { 0 };
        SampleUtils.screenCoordToCameraCoord(
        		(int) mRenderer.ROICenterX, (int) mRenderer.ROICenterY, 
        		(int) mRenderer.ROIWidth, (int) mRenderer.ROIHeight, 
        		screenWidth, screenHeight,
        		vm.getWidth(), vm.getHeight(), 
        		loupeCenterX, loupeCenterY,
        		loupeWidth, loupeHeight);
        
        RectangleInt detROI = new RectangleInt(
        		loupeCenterX[0] - (loupeWidth[0] / 2), 
        		loupeCenterY[0] - (loupeHeight[0] / 2),
        		loupeCenterX[0] + (loupeWidth[0] / 2), 
        		loupeCenterY[0] + (loupeHeight[0] / 2));
        
        TextTracker tt = (TextTracker) TrackerManager.getInstance().getTracker(
            TextTracker.getClassType());
        if (tt != null)
            tt.setRegionOfInterest(detROI, detROI,
//                TextTracker.UP_DIRECTION.REGIONOFINTEREST_UP_IS_9_HRS,
                TextTracker.UP_DIRECTION.REGIONOFINTEREST_UP_IS_0_HRS);
        
        int[] size = config.getSize().getData();
        int[] pos = config.getPosition().getData();
        int offx = ((screenWidth - size[0]) / 2) + pos[0];
        int offy = ((screenHeight - size[1]) / 2) + pos[1];
        mRenderer.setViewport(offx, offy, size[0], size[1]);
    }
    
    
    private void stopCamera()
    {
        doStopTrackers();
        
        CameraDevice.getInstance().stop();
        CameraDevice.getInstance().deinit();
    }
        
    void updateWordListUI(final List<WordDesc> words)
    {
//    	Log.d(LOGTAG, words.size() + " words detected");
        runOnUiThread(new Runnable()
        {          
            public void run()
            {
                RelativeLayout wordListLayout = (RelativeLayout) mUILayout
                    .findViewById(R.id.wordList);
                wordListLayout.removeAllViews();
                
                if (words.size() > 0)
                {
                    LayoutParams params = wordListLayout.getLayoutParams();
                    // Changes the height and width to the specified *pixels*
                    int maxTextHeight = params.height - (2 * WORDLIST_MARGIN);
                    
                    int[] textInfo = fontSizeForTextHeight(maxTextHeight, words.size(), params.width, 32, 8);
                    
                    int count = -1;
                    int nbWords = textInfo[2]; // number of words we can display
                    TextView previousView = null;
                    TextView tv;
                    for (WordDesc word : words)
                    {
                        count++;
                        if (count == nbWords)
                        {
                            break;
                        }
                        tv = new TextView(TextReco.this);
                        
                        // look up definitions
                       
                        StringBuilder builder = new StringBuilder();
                        tv.setText(builder.append(word.text).append(": ").append(lookup(word.text)).toString());
                        RelativeLayout.LayoutParams txtParams = new RelativeLayout.LayoutParams(
                            LayoutParams.WRAP_CONTENT,
                            LayoutParams.WRAP_CONTENT);
                        
                        if (previousView != null)
                            txtParams.addRule(RelativeLayout.BELOW,
                                previousView.getId());
                        
                        int screenWidth = mMetrics.widthPixels;
                        int marginWidth = mIsTablet ? (screenWidth * 20) / 100 : (screenWidth * 5) / 100;
                        txtParams.setMargins(
//                        		0, 
                        		marginWidth,
                        		(count == 0) ? WORDLIST_MARGIN : 0, 
                        		0, 
                        		(count == (nbWords - 1)) ? WORDLIST_MARGIN : 0
                        		);
                        tv.setLayoutParams(txtParams);
                        tv.setGravity(
//                        		Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL
                        		Gravity.LEFT | Gravity.CENTER
                        		);
                        tv.setTextSize(textInfo[0]/2);
                        tv.setTextColor(Color.WHITE);
                        tv.setHeight(textInfo[1]);
                        tv.setId(count + 100);                       
                        wordListLayout.addView(tv);
                        previousView = tv;
                    }
                }
            }
        });
    }
    
    
    private void showLoupe(boolean isActive)
    {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        
        // width of margin is :
        // 5% of the width of the screen for a phone
        // 20% of the width of the screen for a tablet
        int marginWidth = mIsTablet ? (width * 20) / 100 : (width * 5) / 100;
        
        // loupe height is :
        // 33% of the screen height for a phone
        // 20% of the screen height for a tablet
        int loupeHeight = mIsTablet ? (height * 20) / 100 : (height * 30) / 100;
        
        // lupue width takes the width of the screen minus 2 margins
        int loupeWidth = width - (2 * marginWidth);
        
        int wordListHeight = height - (loupeHeight + marginWidth);
        
        // definition of the region of interest
        mRenderer.setROI(width / 2, marginWidth + (loupeHeight / 2),
            loupeWidth, loupeHeight);
        
        // Gets a reference to the loading dialog
        View loadingIndicator = mUILayout.findViewById(R.id.loading_indicator);
        
        RelativeLayout loupeLayout = (RelativeLayout) mUILayout
            .findViewById(R.id.loupeLayout);
        
        ImageView topMargin = (ImageView) mUILayout
            .findViewById(R.id.topMargin);
        
        ImageView leftMargin = (ImageView) mUILayout
            .findViewById(R.id.leftMargin);
        
        ImageView rightMargin = (ImageView) mUILayout
            .findViewById(R.id.rightMargin);
        
        ImageView loupeArea = (ImageView) mUILayout.findViewById(R.id.loupe);
        
        RelativeLayout wordListLayout = (RelativeLayout) mUILayout
            .findViewById(R.id.wordList);
        
        wordListLayout.setBackgroundColor(COLOR_OPAQUE);
        
        if (isActive)
        {
            topMargin.getLayoutParams().height = marginWidth;
            topMargin.getLayoutParams().width = width;
            
            leftMargin.getLayoutParams().width = marginWidth;
            leftMargin.getLayoutParams().height = loupeHeight;
            
            rightMargin.getLayoutParams().width = marginWidth;
            rightMargin.getLayoutParams().height = loupeHeight;
            
            RelativeLayout.LayoutParams params;
            
            params = (RelativeLayout.LayoutParams) loupeLayout
                .getLayoutParams();
            params.height = loupeHeight;
            loupeLayout.setLayoutParams(params);
            
            loupeArea.getLayoutParams().width = loupeWidth;
            loupeArea.getLayoutParams().height = loupeHeight;
            loupeArea.setVisibility(View.VISIBLE);
            
            params = (RelativeLayout.LayoutParams) wordListLayout
                .getLayoutParams();
            params.height = wordListHeight;
            params.width = width;
            wordListLayout.setLayoutParams(params);
            
            loadingIndicator.setVisibility(View.GONE);
            loupeArea.setVisibility(View.VISIBLE);
            topMargin.setVisibility(View.VISIBLE);
            loupeLayout.setVisibility(View.VISIBLE);
            wordListLayout.setVisibility(View.VISIBLE);
            
        } else
        {
            loadingIndicator.setVisibility(View.VISIBLE);
            loupeArea.setVisibility(View.GONE);
            topMargin.setVisibility(View.GONE);
            loupeLayout.setVisibility(View.GONE);
            wordListLayout.setVisibility(View.GONE);
        }
        
    }
    
    
    // the funtions returns 3 values in an array of ints
    // [0] : the text size
    // [1] : the text component height
    // [2] : the number of words we can display
    private int[] fontSizeForTextHeight(int totalTextHeight, int nbWords,
        int textWidth, int textSizeMax, int textSizeMin)
    {
        
        int[] result = new int[3];
        String text = "Agj";
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT));
        tv.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        // tv.setTextSize(30);
        // tv.setHeight(textHeight);
        int textSize = 0;
        int layoutHeight = 0;
        
        final float densityMultiplier = getResources().getDisplayMetrics().density;
        
        for (textSize = textSizeMax; textSize >= textSizeMin; textSize -= 2)
        {
            // Get the font size setting
            float fontScale = Settings.System.getFloat(getContentResolver(),
                Settings.System.FONT_SCALE, 1.0f);
            // Text view line spacing multiplier
            float spacingMult = 1.0f * fontScale;
            // Text view additional line spacing
            float spacingAdd = 0.0f;
            TextPaint paint = new TextPaint(tv.getPaint());
            paint.setTextSize(textSize * densityMultiplier);
            // Measure using a static layout
            StaticLayout layout = new StaticLayout(text, paint, textWidth,
                Alignment.ALIGN_NORMAL, spacingMult, spacingAdd, true);
            layoutHeight = layout.getHeight();
            if ((layoutHeight * nbWords) < totalTextHeight)
            {
                result[0] = textSize;
                result[1] = layoutHeight;
                result[2] = nbWords;
                return result;
            }
        }
        
        // we won't be able to display all the fonts
        result[0] = textSize;
        result[1] = layoutHeight;
        result[2] = totalTextHeight / layoutHeight;
        return result;
    }
    
    
    @Override
    public void onInitARDone(SampleApplicationException exception)
    {
        
        if (exception == null)
        {
            initApplicationAR();
            
            // Hint to the virtual machine that it would be a good time to
            // run the garbage collector:
            //
            // NOTE: This is only a hint. There is no guarantee that the
            // garbage collector will actually be run.
            System.gc();
            
            // Activate the renderer:
            mRenderer.mIsActive = true;
            
            // Now add the GL surface view. It is important
            // that the OpenGL ES surface view gets added
            // BEFORE the camera is started and video
            // background is configured.
            addContentView(mGlView, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
            
            // Hides the Loading Dialog
            loadingDialogHandler
                .sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);
            showLoupe(true);
            
            // Sets the UILayout to be drawn in front of the camera
            mUILayout.bringToFront();
            
            try
            {
                vuforiaAppSession.startAR(CameraDevice.CAMERA.CAMERA_DEFAULT);
            } catch (SampleApplicationException e)
            {
                Log.e(TAG, e.getString());
            }
            
            mIsVuforiaStarted = true;
            
            postStartCamera();
            
            setSampleAppMenuAdditionalViews();
            mSampleAppMenu = new SampleAppMenu(this, this, "xLL",
                mGlView, mUILayout, mSettingsAdditionalViews);
            setSampleAppMenuSettings();
            
        } else
        {
            Log.e(TAG, exception.getString());
            finish();
        }
    }
    
    
    // Functions to load and destroy tracking data.
    @Override
    public boolean doLoadTrackersData()
    {
        TrackerManager tm = TrackerManager.getInstance();
        TextTracker tt = (TextTracker) tm.getTracker(TextTracker.getClassType());
        WordList wl = tt.getWordList();
        
        // xLL: filter words of interest
        boolean ret = wl.loadWordList("TextReco/Vuforia-English-word.vwl", WordList.STORAGE_TYPE.STORAGE_APPRESOURCE);
        wl.addWordsFromFile("TextReco/AdditionalWords.lst", WordList.STORAGE_TYPE.STORAGE_APPRESOURCE);
        wl.loadFilterList(mAdvanced ? "TextReco/FilterListAdvanced.lst" : "TextReco/FilterList.lst", WordList.STORAGE_TYPE.STORAGE_APPRESOURCE);
        wl.setFilterMode(WordList.FILTER_MODE.FILTER_MODE_WHITE_LIST);
        Log.i(TAG, mAdvanced ? "advanced filter list applied" : "filter list applied");
        return ret;
    }
    
    
    @Override
    public boolean doUnloadTrackersData()
    {
        // Indicate if the trackers were unloaded correctly
        boolean result = true;
        TrackerManager tm = TrackerManager.getInstance();
        TextTracker tt = (TextTracker) tm.getTracker(TextTracker.getClassType());
        WordList wl = tt.getWordList();
        wl.unloadAllLists();        
        return result;
    }
    
    
    @Override
    public void onQCARUpdate(State state)
    {
    }
    
    
    @Override
    public boolean doInitTrackers()
    {
        TrackerManager tManager = TrackerManager.getInstance();
        Tracker tracker;
        
        // Indicate if the trackers were initialized correctly
        boolean result = true;
        
        tracker = tManager.initTracker(TextTracker.getClassType());
        if (tracker == null)
        {
            Log.e(
                TAG,
                "Tracker not initialized. Tracker already initialized or the camera is already started");
            result = false;
        } else
        {
            Log.i(TAG, "Tracker successfully initialized");
        }
        
        return result;
    }
    
    
    @Override
    public boolean doStartTrackers()
    {
        // Indicate if the trackers were started correctly
        boolean result = true;
        
        Tracker textTracker = TrackerManager.getInstance().getTracker(
            TextTracker.getClassType());
        if (textTracker != null)
            textTracker.start();
        
        return result;
    }
    
    
    @Override
    public boolean doStopTrackers()
    {
        // Indicate if the trackers were stopped correctly
        boolean result = true;      
        Tracker textTracker = TrackerManager.getInstance().getTracker(
            TextTracker.getClassType());
        if (textTracker != null)
            textTracker.stop();
        
        return result;
    }
    
    
    @Override
    public boolean doDeinitTrackers()
    {
        // Indicate if the trackers were deinitialized correctly
        boolean result = true;          
        TrackerManager tManager = TrackerManager.getInstance();
        tManager.deinitTracker(TextTracker.getClassType());
        Log.e(TAG, "UnloadTrackersData");   
        return result;
    }
    
    final public static int CMD_BACK = -1;
    final public static int CMD_LEVEL = 0;
    final public static int CMD_FLASH = 1;
    
    
    // This method sets the additional views to be moved along with the GLView
    private void setSampleAppMenuAdditionalViews()
    {
        mSettingsAdditionalViews = new ArrayList<View>();
        mSettingsAdditionalViews.add(mUILayout.findViewById(R.id.topMargin));
        mSettingsAdditionalViews.add(mUILayout.findViewById(R.id.loupeLayout));
        mSettingsAdditionalViews.add(mUILayout.findViewById(R.id.wordList));
    }
    
    
    // This method sets the menu's settings
    private void setSampleAppMenuSettings()
    {
        SampleAppMenuGroup group;       
        group = mSampleAppMenu.addGroup("", false);
        group.addTextItem(getString(R.string.menu_back), -1);   
        group = mSampleAppMenu.addGroup("", true);
        mLevelOptionView = group.addSelectionItem("Advanced Level", CMD_LEVEL, true);
        group = mSampleAppMenu.addGroup("", true);
        mFlashOptionView = group.addSelectionItem(getString(R.string.menu_flash), CMD_FLASH, false);       
        mSampleAppMenu.attachMenu();
    }
    
    
    @Override
    public boolean menuProcess(int command)
    {
        boolean result = true;       
        switch (command)
        {
            case CMD_BACK:
                finish(); break;
            case CMD_LEVEL: {
                mAdvanced = !mAdvanced;
                doStopTrackers();
                doUnloadTrackersData();
                doLoadTrackersData();
                doStartTrackers();
            	break;
            } case CMD_FLASH:
                result = CameraDevice.getInstance().setFlashTorchMode(!mFlash);              
                if (result)
                    mFlash = !mFlash;
                else {
                    showToast(getString(mFlash ? R.string.menu_flash_error_off
                        : R.string.menu_flash_error_on));
                    Log.e(TAG,
                        getString(mFlash ? R.string.menu_flash_error_off
                            : R.string.menu_flash_error_on));
                }
                break;
            
        }
        
        return result;
    }
    
    
    private void showToast(String text)
    {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
    
}
