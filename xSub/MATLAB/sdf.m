function [S, I, SP, SI] = sdf(Y,X,Ws,Wi,plot)
%Speech Detection filters out non-speech features with GMM.
if(nargin < 5)
    plot = false;
end

D = [Y(:,1) sum(Y(:,2:end), 2)];
[GMM,r] = gmm(D, 2, 300);
[~, I] = max(r,[],2);
[~, s] = max(GMM.MU(:,1));
I = (I==s);
S = Y(I==1,:);

% assign speech samples and silence samples
padding = (size(Y,1)-1)*Wi + Ws;
X = [X; zeros(padding - length(X), 1)];
indices = repmat(1:Ws, size(Y,1), 1) + repmat((0:size(Y,1)-1)' * Wi, 1, Ws);
silence = unique(indices(I==0,:));
speech = setdiff(unique(indices(I==1,:)), silence);
SP = X(speech);
SI = X(silence);

% fprintf('speech:silence = %d:%d are detected, total %d=%d samples\n',...
%     length(speech), length(silence), length(speech)+length(silence), length(X));

% play raw audio, detected speech and silence
% disp('playing recording clip...');
% playblocking(audioplayer(X, 16000));
% disp('playing detected speech...');
% playblocking(audioplayer(SP, 16000));
% disp('playing detected silence...');
% playblocking(audioplayer(SI, 16000));
if(~plot)
    return;
end

% scatter plot to visualize the clusters
figure;
subplot(3,1,1);
g = cell(size(D,1),1);
g(I==0) = {'silence'};
g(I==1) = {'speech'};
gscatter(D(:,1), D(:,2), g);
hold on;
plot(GMM.MU(:,1), GMM.MU(:,2), 'kx', 'LineWidth', 2);
title(sprintf('Speech Detection with GMM/EM'));
hold off;

% comparison of spectra of raw audio and detected speech
subplot(3,1,2);
spectrogram(X, Ws, Ws-Wi, 512, 16000, 'yaxis');
title('Raw Audio Spectrum');
subplot(3,1,3);
X(silence) = 0;
spectrogram(X, Ws, Ws-Wi, 512, 16000, 'yaxis');
title('Detected Speech Spectrum');

% speech is marked as red in raw audio signal
% figure;
% plot(X);
% hold on;
% plot(speech, X(speech), 'r');
% title('Speech Detection Marked on Raw Audio');
end

