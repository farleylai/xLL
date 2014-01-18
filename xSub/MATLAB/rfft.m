function Y = rfft(X,N,D)
%RFFT     Calculate the DFT of real data Y=(X,N,D)

sz = size(X);
if prod(sz) == 1
    Y = x;
else
    if(nargin < 3 || isempty(D))
        D = find(sz > 1, 1);
        if(nargin < 2)
            N = sz(D);
        end
    end
    if isempty(N) 
        N = sz(D);
    end
    Y = fft(X,N,D);
    Y = reshape(Y, prod(sz(1:D-1)), N, prod(sz(D+1:end))); 
    sz(D) = 1 + fix(N/2);
    Y(:,sz(D)+1:end,:) = [];
    Y = reshape(Y, sz);
end

