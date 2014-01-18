package edu.uiowa.cs.xll;

import java.io.IOException;
import java.util.List;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ActionBar;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v13.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v13.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);

        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                        getActionBar().setSelectedNavigationItem(position);
                    }
                });

        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        ActionBar.TabListener tabListener = new ActionBar.TabListener() {
            public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
                // show the given tab
                mViewPager.setCurrentItem(tab.getPosition());
            }

            public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
                // hide the given tab
            }

            public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
                // probably ignore this event
            }
        };

        actionBar.addTab(actionBar.newTab().setText("Listening").setTabListener(tabListener));
        actionBar.addTab(actionBar.newTab().setText("Vocabulary").setTabListener(tabListener));
        actionBar.addTab(actionBar.newTab().setText("Speaking").setTabListener(tabListener));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.listening, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            switch (position) {
                case 0:
                    return ListeningFragment.newInstance(position + 1);
                case 1:
                case 2:
                default:
                    return PlaceholderFragment.newInstance(position + 1);
            }
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
                case 2:
                    return getString(R.string.title_section3).toUpperCase(l);
            }
            return null;
        }
    }

    /**
     * A listening fragment containing a player view.
     */
    public static class ListeningFragment extends Fragment implements MediaPlayer.OnCompletionListener, SeekBar.OnSeekBarChangeListener {
        private static final String ARG_SECTION_NUMBER = "section_number";
        private ImageButton btnPlay;
        private ImageButton btnForward;
        private ImageButton btnBackward;
        private ImageButton btnNext;
        private ImageButton btnPrevious;
        private ImageButton btnPlaylist;
        private ImageButton btnRepeat;
        private ImageButton btnShuffle;
        private SeekBar songProgressBar;
        private TextView songTitleLabel;
        private TextView songSubtitle;
        private TextView songCurrentDurationLabel;
        private TextView songTotalDurationLabel;
        // Media Player
        private  MediaPlayer mp;
        // Handler to update UI timer, progress bar etc,.
        private Handler mHandler = new Handler();;
        private SongsManager songManager;
        private Utilities utils;
        private int seekForwardTime = 5000; // 5000 milliseconds
        private int seekBackwardTime = 5000; // 5000 milliseconds
        private int currentSongIndex = 0;
        private int currentSubtitleIndex = 1;
        private boolean isShuffle = false;
        private boolean isRepeat = false;
        private List<HashMap<String, String>> songsList;// = new ArrayList<HashMap<String, String>>();
        private List<Subtitle> subtitleList;// = new ArrayList<Subtitle>();

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static ListeningFragment newInstance(int sectionNumber) {
            ListeningFragment fragment = new ListeningFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public ListeningFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
//            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
//            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
//            textView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));

            final View rootView = inflater.inflate(R.layout.player, container, false);
            // All player buttons
            btnPlay = (ImageButton) rootView.findViewById(R.id.btnPlay);
            btnForward = (ImageButton) rootView.findViewById(R.id.btnForward);
            btnBackward = (ImageButton) rootView.findViewById(R.id.btnBackward);
            btnNext = (ImageButton) rootView.findViewById(R.id.btnNext);
            btnPrevious = (ImageButton) rootView.findViewById(R.id.btnPrevious);
            btnPlaylist = (ImageButton) rootView.findViewById(R.id.btnPlaylist);
            btnRepeat = (ImageButton) rootView.findViewById(R.id.btnRepeat);
            btnShuffle = (ImageButton) rootView.findViewById(R.id.btnShuffle);
            songProgressBar = (SeekBar) rootView.findViewById(R.id.songProgressBar);
            songTitleLabel = (TextView) rootView.findViewById(R.id.songTitle);
            songSubtitle = (TextView) rootView.findViewById(R.id.Subtitle);
            songCurrentDurationLabel = (TextView) rootView.findViewById(R.id.songCurrentDurationLabel);
            songTotalDurationLabel = (TextView) rootView.findViewById(R.id.songTotalDurationLabel);

            // Mediaplayer
            songManager = new SongsManager();
            mp = new MediaPlayer();
            utils = new Utilities();

            // Listeners
            songProgressBar.setOnSeekBarChangeListener(this); // Important
            mp.setOnCompletionListener(this); // Important

            // Getting all songs list
            songsList = songManager.getPlayList();

            // By default play first song
            if(!songsList.isEmpty()) playSong(0);

            /**
             * Play button click event
             * plays a song and changes button to pause image
             * pauses a song and changes button to play image
             * */
            btnPlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    // check for already playing
                    if(mp.isPlaying()){
                        if(mp!=null){
                            mp.pause();
                            btnPlay.setImageResource(R.drawable.btn_play);
                        }
                    }else{
                        // Resume song
                        if(mp!=null){
                            mp.start();
                            btnPlay.setImageResource(R.drawable.btn_pause);
                        }
                    }

                }
            });

            /**
             * Forward button click event
             * Forwards song specified seconds
             * */
            btnForward.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    // get current song position
                    int currentPosition = mp.getCurrentPosition();
                    Subtitle subtitle = songManager.nextSubtitle(currentPosition);
                    if(subtitle != null)
                        seekForwardTime = subtitle.beginning().time() - currentPosition;
                    else
                        seekForwardTime = 5000;

                    if(currentPosition + seekForwardTime <= mp.getDuration()){
//                        songSubtitle.setText(subtitle.getText());
                        mp.seekTo(currentPosition + seekForwardTime);
                    }else{
                        mp.seekTo(mp.getDuration());
                    }
                }
            });

            /**
             * Backward button click event
             * Backward song to specified seconds
             * */
            btnBackward.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    // get current song position
                    int currentPosition = mp.getCurrentPosition();
                    Subtitle subtitle = songManager.prevSubtitle(currentPosition);
                    if(subtitle != null)
                        seekBackwardTime = currentPosition - subtitle.beginning().time();
                    else
                        seekBackwardTime = 5000;

                    // check if seekBackward time is greater than 0 sec
                    if(currentPosition - seekBackwardTime >= 0){
//                        songSubtitle.setText(subtitle.getText());
                        mp.seekTo(currentPosition - seekBackwardTime);
                    }else{
                        mp.seekTo(0);
                    }

                }
            });

            /**
             * Next button click event
             * Plays next song by taking currentSongIndex + 1
             * */
            btnNext.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    // check if next song is there or not
                    if(currentSongIndex < (songsList.size() - 1)){
                        playSong(currentSongIndex + 1);
                        currentSongIndex = currentSongIndex + 1;
                    }else{
                        // play first song
                        playSong(0);
                        currentSongIndex = 0;
                    }

                }
            });

            /**
             * Back button click event
             * Plays previous song by currentSongIndex - 1
             * */
            btnPrevious.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    if(currentSongIndex > 0){
                        playSong(currentSongIndex - 1);
                        currentSongIndex = currentSongIndex - 1;
                    }else{
                        // play last song
                        playSong(songsList.size() - 1);
                        currentSongIndex = songsList.size() - 1;
                    }

                }
            });

            /**
             * Button Click event for Repeat button
             * Enables repeat flag to true
             * */
            btnRepeat.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    if(isRepeat){
                        isRepeat = false;
                        Toast.makeText(rootView.getContext(), "Repeat is OFF", Toast.LENGTH_SHORT).show();
                        btnRepeat.setImageResource(R.drawable.btn_repeat);
                    }else{
                        // make repeat to true
                        isRepeat = true;
                        Toast.makeText(rootView.getContext(), "Repeat is ON", Toast.LENGTH_SHORT).show();
                        // make shuffle to false
                        isShuffle = false;
                        btnRepeat.setImageResource(R.drawable.btn_repeat_focused);
                        btnShuffle.setImageResource(R.drawable.btn_shuffle);
                    }
                }
            });

            /**
             * Button Click event for Shuffle button
             * Enables shuffle flag to true
             * */
            btnShuffle.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    if(isShuffle){
                        isShuffle = false;
                        Toast.makeText(rootView.getContext(), "Shuffle is OFF", Toast.LENGTH_SHORT).show();
                        btnShuffle.setImageResource(R.drawable.btn_shuffle);
                    }else{
                        // make repeat to true
                        isShuffle= true;
                        Toast.makeText(rootView.getContext(), "Shuffle is ON", Toast.LENGTH_SHORT).show();
                        // make shuffle to false
                        isRepeat = false;
                        btnShuffle.setImageResource(R.drawable.btn_shuffle_focused);
                        btnRepeat.setImageResource(R.drawable.btn_repeat);
                    }
                }
            });

            /**
             * Button Click event for Play list click event
             * Launches list activity which displays list of songs
             * */
            btnPlaylist.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    Intent i = new Intent(rootView.getContext(), PlayListActivity.class);
                    startActivityForResult(i, 100);
                }
            });

            return rootView;
        }

        /**
         * Receiving song index from playlist view
         * and play the song
         * */
        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if(resultCode == 100){
                currentSongIndex = data.getExtras().getInt("songIndex");
                playSong(currentSongIndex);
            }

        }

        /**
         * Function to play a song
         * @param songIndex - index of song
         * */
        public void  playSong(int songIndex){
            // Play song
            try {
                mp.reset();
                mp.setDataSource(songsList.get(songIndex).get("songPath"));
                mp.prepare();
                mp.start();

                String songTitle = songsList.get(songIndex).get("songTitle");
                songTitleLabel.setText(songTitle);
                songManager.loadSubtitle(songIndex);

                // Changing Button Image to pause image
                btnPlay.setImageResource(R.drawable.btn_pause);

                // set Progress bar values
                songProgressBar.setProgress(0);
                songProgressBar.setMax(100);

                // Updating progress bar
                updateProgressBar();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Update timer on seekbar
         * */
        public void updateProgressBar() {
            mHandler.postDelayed(mUpdateTimeTask, 100);
        }

        /**
         * Background Runnable thread
         * */
        private Runnable mUpdateTimeTask = new Runnable() {
            Subtitle prev = null;
            public void run() {
                int totalDuration = mp.getDuration();
                int currentPosition = mp.getCurrentPosition();

                // Refresh subtitle if necessary
                Subtitle sub = songManager.seekSubtitle(currentPosition);
                if(sub == null)
                    songSubtitle.setText("");
                else if(sub != prev)
                    songSubtitle.setText(sub.getText());

                prev = sub;

                // Displaying Total Duration time
                songTotalDurationLabel.setText(""+utils.milliSecondsToTimer(totalDuration));
                // Displaying time completed playing
                songCurrentDurationLabel.setText(""+utils.milliSecondsToTimer(currentPosition));

                // Updating progress bar
                int progress = (int)(utils.getProgressPercentage(currentPosition, totalDuration));
                //Log.d("Progress", ""+progress);
                songProgressBar.setProgress(progress);

                // Running this thread after 100 milliseconds
                mHandler.postDelayed(this, 100);
            }
        };

        /**
         *
         * */
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {

        }

        /**
         * When user starts moving the progress handler
         * */
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // remove message Handler from updating progress bar
            mHandler.removeCallbacks(mUpdateTimeTask);
        }

        /**
         * When user stops moving the progress hanlder
         * */
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mHandler.removeCallbacks(mUpdateTimeTask);
            int totalDuration = mp.getDuration();
            int currentPosition = utils.progressToTimer(seekBar.getProgress(), totalDuration);

            // forward or backward to certain seconds
            mp.seekTo(currentPosition);

            // update timer progress again
            updateProgressBar();
        }

        /**
         * On Song Playing completed
         * if repeat is ON play same song again
         * if shuffle is ON play random song
         * */
        @Override
        public void onCompletion(MediaPlayer arg0) {

            // check for repeat is ON or OFF
            if(isRepeat){
                // repeat is on play same song again
                playSong(currentSongIndex);
            } else if(isShuffle){
                // shuffle is on - play a random song
                Random rand = new Random();
                currentSongIndex = rand.nextInt((songsList.size() - 1) - 0 + 1) + 0;
                playSong(currentSongIndex);
            } else{
                // no repeat or shuffle ON - play next song
                if(currentSongIndex < (songsList.size() - 1)){
                    playSong(currentSongIndex + 1);
                    currentSongIndex = currentSongIndex + 1;
                } else {
                    btnPlay.setImageResource(R.drawable.btn_play);
                    // play first song
//                    playSong(0);
//                    currentSongIndex = 0;
                    return;
                }
            }
        }

        @Override
        public void onDestroy(){
            super.onDestroy();
            mHandler.removeCallbacks(mUpdateTimeTask);
            mHandler = null;
            mp.release();
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            textView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
            return rootView;
        }
    }
}
