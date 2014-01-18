package edu.uiowa.cs.xll;

import android.text.Spanned;

/**
 * Created by farleylai on 1/17/14.
 */
public class Subtitle {
    public static class TimeCode {
        private int hh;
        private int mm;
        private int ss;
        private int ms;
        private int time;

        public TimeCode(int hh, int mm, int ss, int ms) {
            this.hh = hh;
            this.mm = mm;
            this.ss = ss;
            this.ms = ms;
            this.time = (60 * 60 * hh + 60 * mm + ss) * 1000 + ms;
        }

        public int time() {
            return time;
        }
    }

    private int _idx;
    private TimeCode _beginning;
    private TimeCode _ending;
    private Spanned _text;

    public Subtitle(int idx, TimeCode beginning, TimeCode ending, Spanned text) {
        _idx = idx;
        _beginning = beginning;
        _ending = ending;
        _text = text;
    }

    public int getIndex() { return _idx; }

    public TimeCode beginning() { return _beginning; }

    public TimeCode ending() { return _ending; }

    public Spanned getText() { return _text; }

    public void debug() {
        System.out.println(_idx);
        System.out.printf("%02d:%02d:%02d.%03d->%02d:%02d:%02d.%03d\n",
                _beginning.hh, _beginning.mm, _beginning.ss, _beginning.ms,
                _ending.hh, _ending.mm, _ending.ss, _ending.ms);
        System.out.println(_text);
    }
}
