function Y = mfcc(X,Fs,m,Ws,Wi)
% MFCC feature extraction
if(nargin < 5)
    Wi = 160;               % window step: 160/16000 = 10ms
end
if(nargin < 4)
    Ws = 400;               % window size: 400/16000 = 25ms
end
    
    function Y = preemphasize(X, a)
        % an optional filter to enhance the high frequency of the spectrum
        Bm = [1 -a];
        Y = filter(Bm,1,X);        
    end

    function mel = hz2mel(f)
        % convert linear frequency to mel scales
        mel = 1127 * log(1+f/700);
    end

    function f = mel2hz(mel)
        % convert mel scales to linear frequency
        f = 700 * (exp(mel/1127)-1);
    end

    function B = melbank(m, N, Fs, fmin, fmax)
        % Filter banks are triangular windows spaced in Mel scales.
        % Each filter starts and ends at adjacent filter centers.
        n = N/2 + 1;    % 1/2 FFT points + DC
        if(nargin < 5)
            fmax = Fs / 2;  % Nyquist frequency of the sampling rate
        end
        if(nargin < 4)
            fmin = 300;     % minimum human voice frequency: 300Hz
        end  
        mels = linspace(hz2mel(fmin), hz2mel(fmax), m+2);
        freqs= mel2hz(mels);
        mel2hzBins = floor((N+1)*freqs/Fs);        
        fftBins = 0:256;        
        B = zeros(m,n);
        for k = 1:m
            lo   = mel2hzBins(k);
            bank = mel2hzBins(k+1);
            hi   = mel2hzBins(k+2);
            B(k,:) = (fftBins-lo) / (bank-lo) .* (fftBins >= lo & fftBins < bank);
            B(k,:) = B(k,:) + (hi-fftBins) / (hi-bank) .* (fftBins >= bank & fftBins <= hi);
        end
    end

    function Y = lift(cc, L)
        % apply a cepstral lifter to the the matrix of cepstra
        % to increase the magnitude of the high frequency DCT coeffs
        lift = 1 + (L/2)*sin(pi*(0:size(cc,2)-1)/L);
        Y = bsxfun(@times, lift, cc);
    end

% resample input audio to 16KHz
if(Fs ~= 16000)
    [P, Q] = rat(16000/Fs,0.0001);
    X = resample(double(X), P, Q);
    Fs = 16000;
end

% pre-emphasis
a = 0.95;
X = preemphasize(X, a);

% obtain the power spectrum of input audio
N  = 2^nextpow2(Ws);    % FFT points or bins
Y = zeros(1+ceil((length(X)-Ws)/Wi),m);

% frame the audio in sliding Windows
padding = (size(Y,1)-1)*Wi + Ws;
X = [X; zeros(padding - length(X), 1)];
indices = repmat(1:Ws, size(Y,1), 1) + repmat((0:size(Y,1)-1)' * Wi, 1, Ws);
X = X(indices);

% Hamming windowing
W  = hamming(Ws, 'symmetric')';
X = bsxfun(@times, W, X);
    
% FFT for real input concerns only positive frequency components
y = abs(rfft(X,N,2)); % magnitude specturm including the DC component
y = y.^2 / N;         % power spectrum aka periodogram

% Mel-Frequency ceptrum from 26 Mel filter banks over the spectrum
B = melbank(m,N,Fs,300,6000);

% MFC conversion in dBs
cc = log(y * B');

% MFCC from DCT to decorrelate those overlapped filter bank energies
cc = dct(cc')';

% HTK sinusoidal lifter to increase high frequency coefficients
% cc = lift(cc, 22);

% only the lower coefficients are effective in terms of human hearing
Y = cc(:,1:m);

% replace the first coefficient with the log spectrum energy
Y(:,1) = log(sum(y, 2));
end

