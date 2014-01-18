package edu.uiowa.cs.xll;

import android.text.Html;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SongsManager {
	// SDCard Path
	final String MEDIA_PATH = new String("/sdcard/Music/XLL");
	private List<HashMap<String, String>> songsList = new ArrayList<HashMap<String, String>>();
    private List<Subtitle> subtitleList = new ArrayList<Subtitle>();
	
	// Constructor
	public SongsManager(){
	}
	
	/**
	 * Function to read all mp3 files from sdcard
	 * and store the details in ArrayList
	 * */
	public List<HashMap<String, String>> getPlayList(){
		File home = new File(MEDIA_PATH);
        if(!home.exists()) {
            home.mkdir();
            return songsList;
        }
		if (home.listFiles(new FileExtensionFilter()).length > 0) {
            songsList.clear();;
			for (File file : home.listFiles(new FileExtensionFilter())) {
				HashMap<String, String> song = new HashMap<String, String>();
				song.put("songTitle", file.getName().substring(0, (file.getName().length() - 4)));
				song.put("songPath", file.getPath());
                song.put("metaPath", file.getPath().substring(0, (file.getPath().length() - 4)) + ".srt");
				// Adding each song to SongList
				songsList.add(song);
			}
		}
		// return songs list array
		return songsList;
	}

    public void loadSubtitle(int songIdx) {
        File meta = new File(songsList.get(songIdx).get("metaPath"));
        if(!meta.exists())
            subtitleList.clear();

        BufferedReader srt = null;
        try {
            srt = new BufferedReader(new InputStreamReader(new FileInputStream(meta), "UTF8"));
            String line = null;
            do {
                line = srt.readLine();
                if(line == null || line.length() == 0) break;
                int idx = Integer.parseInt(line);
                line = srt.readLine();
                if(line == null || line.length() == 0) break;
                String[] fmts = line.split("->");
                if(fmts.length != 2) break;
                Subtitle.TimeCode[] timecodes = new Subtitle.TimeCode[2];
                for(int i = 0; i < fmts.length; i++) {
                    String[] fields = fmts[i].split(":");
                    int hh = Integer.parseInt(fields[0]);
                    int mm = Integer.parseInt(fields[1]);
                    double secs = Double.parseDouble(fields[2]);
                    int ss = (int)secs;
                    int ms = (int)(secs - ss);
                    timecodes[i] = new Subtitle.TimeCode(hh, mm, ss, ms);
                }

                StringBuffer text = new StringBuffer();
                while((line = srt.readLine()) != null && line.length() > 0)
                    text.append(line).append("\n");

                Subtitle sub = new Subtitle(idx, timecodes[0], timecodes[1], Html.fromHtml(text.toString()));
                subtitleList.add(sub);
                sub.debug();
            } while(line != null);
            srt.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Subtitle getSubtitle(int idx) {
        if(idx < 1 || idx > subtitleList.size()) return null;
        return subtitleList.get(idx-1);
    }

    public Subtitle seekSubtitle(int position) {
        if(subtitleList.isEmpty()) return null;
        for(Subtitle sub: subtitleList) {
            if(position >= sub.beginning().time() && position < sub.ending().time())
                return sub;
        }

        return null;
    }

    public Subtitle prevSubtitle (long currentPosition) {
        if(subtitleList.isEmpty() || subtitleList.size() == 1) return null;
        for(int i = 0; i < subtitleList.size()-1; i++) {
            Subtitle sub = subtitleList.get(i);
            Subtitle next = subtitleList.get(i+1);
            if(currentPosition >= sub.beginning().time() && currentPosition < next.beginning().time())
                if(i > 0) return subtitleList.get(i-1);
        }

        return null;
    }

    public Subtitle nextSubtitle (long currentPosition) {
        if(subtitleList.isEmpty() || subtitleList.size() == 1) return null;
        for(int i = 0; i < subtitleList.size()-1; i++) {
            Subtitle sub = subtitleList.get(i);
            Subtitle next = subtitleList.get(i+1);
            if(currentPosition >= sub.beginning().time() && currentPosition < next.beginning().time())
                return next;
        }

        return null;
    }

	/**
	 * Class to filter files which are having .mp3 extension
	 * */
	class FileExtensionFilter implements FilenameFilter {
		public boolean accept(File dir, String name) {
			return (name.endsWith(".wav") || name.endsWith(".WAV"));
		}
	}
}
