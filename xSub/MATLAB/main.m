function [] = main(wav, threshold)
%% BoilerMake: Project Hawk
% Farley Lai
% Computer Science
% University of Iowa
% poyuan-lai@uiowa.edu

if(nargin < 2)
    threshold = 0.5;
end

if(nargin < 1)
    wav = 'F4M4';
end

segmentation = sprintf('%s.mat', wav);
if(exist(segmentation, 'file'))
    load(segmentation, 'sentences', 'lengths');
else
    % Speech sentence segmentation based on speech detection.
    fprintf('Speech Sentence Segmentation on %s.wav\n', wav);
    [sentences, lengths] = segments(wav, threshold);
    fprintf('%d sentences segmented\n', length(sentences));
    save(segmentation, 'sentences', 'lengths');
    savesrt(wav, sentences, lengths)
end

    function [hh, mm, ss, ms] = sample2time(sample, Fs)
        secs = sample / Fs;
        hh = floor(secs/60/60);
        secs = secs - hh * 60 * 60;
        mm = floor(secs/60);
        secs = secs - mm * 60;
        ss = floor(secs);
        ms = floor((secs - ss)*1000);
    end

    function savesrt(wav, sentences, lengths)        
        f = fopen(sprintf('media/%s.srt', wav), 'w');
        info = audioinfo(sprintf('media/%s.wav', wav));
        Fs = info.SampleRate;
        for i = 1:length(sentences)
            beginning = sentences(i);
            ending = beginning + lengths(i) - 1;
            [hh1, mm1, ss1, ms1] = sample2time(beginning-1, Fs);
            [hh2, mm2, ss2, ms2] = sample2time(ending-1, Fs);
            fprintf(f, '%d\n', i);
            fprintf(f, '%02d:%02d:%02d.%03d->%02d:%02d:%02d.%03d\n',...
                hh1, mm1, ss1, ms1, hh2, mm2, ss2, ms2);
            fprintf(f, 'sentence %d\n\n', i);
        end
        fclose(f);
    end

    function playSentence(player, sentence, samples)
        playblocking(player, [sentence sentence+samples-1]);
    end

    function playSentences(player, sentences, lengths)
        for i = 1:length(sentences)
            fprintf('playing sentence %d\n', i);
            playSentence(player, sentences(i), lengths(i));
        end
    end

[X, Fs] = audioread(sprintf('media/%s.wav', wav));
player = audioplayer(X, Fs);
prompt = sprintf('What sentence to play(%d-%d)?', 1, length(sentences));
options = 'a: all, p: prev, n: next and q: quit\n';
prompt = sprintf('%s\n%s', prompt, options);
while(true)
   choice = input(prompt, 's');
   switch choice;
       case 'q'
           break;
       case 'a'
           playSentences(player, sentences, lengths);
       case 'p'
           if(~isnumeric(idx) || idx - 1 < 1)
               continue;
           end
           idx = idx-1;
           playSentence(player, sentences(idx), lengths(idx));
       case 'n'
           if(~isnumeric(idx) || idx + 1 > length(sentences))
               continue;
           end
           idx = idx+1;
           playSentence(player, sentences(idx), lengths(idx));
       otherwise
           idx = str2double(choice);
           if(isnan(idx) || idx < 1 || idx > length(sentences))
               continue;
           end
           playSentence(player, sentences(idx), lengths(idx));
   end   
end

% F4TS = [
%     0       24; 
%     32.25   50.9;
%     55.2    69;
%     100.95  110.6;
%     113.5   120];
%     
% M4TS = [
%     24.1    30.7;
%     51.3    54.4;
%     69      100.25;
%     111.05  112];
end
