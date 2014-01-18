function [sentences, lengths] = segments(wav, threshold, m, Ws, Wi)
if(nargin < 5)
    Wi = 160;   % default sliding window step to compute FFT
end
if(nargin < 4)
    m  = 26;    % default Mel filter bank size and number of coefficients
end
if(nargin < 3)
    Ws = 400;   % default sliding window size to compute FFT
end

recording = sprintf('media/%s.wav', wav);
[X, Fs] = audioread(recording);
Y = mfcc(X,Fs,m,Ws,Wi);
[~, I] = sdf(Y,X,Ws,Wi);

% timestamp silence segments
threshold = 1+floor((threshold*Fs-Ws)/Wi);
[ts, run] = segment(I,0,threshold);
tse = ts + run - 1;
silences = ts + round(run/2) - 1;

% sample indices of feature vectors
padding = (size(Y,1)-1)*Wi + Ws;
X = [X; zeros(padding - length(X), 1)];
indices = repmat(1:Ws, size(Y,1), 1) + repmat((0:size(Y,1)-1)' * Wi, 1, Ws);

% figure out sentence starts and lengths by silences
sentences = [1; tse];
frames = [ts; size(Y,1)] - sentences;
sentences = indices(sentences,1);
lengths = (frames-1)*Wi + Ws;

% remove too short sentences < 0.2s
marks = lengths > 0.2 * Fs;
sentences = sentences(marks);
lengths = lengths(marks);
end

