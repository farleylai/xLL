function [timestamp1, run1] = segment(I, s, threshold)
flip = false;
if(size(I,1)~=1)
    I = I';
    flip = true;
end

% compensate the last segment
II = I;
II(I~=s) = 0;
II(I==s) = 1;
if(II(end) == 1)
    I = [II 0];
else
    I = [II 1];
end

% leading index
a = 0;
if(I(1) == 0) 
    a = 1;
end

% segment run length by difference
timestamp = [find(diff([a I]) ~= 0)];
timestamp1 = [find(diff([a I]) == 1)];
run = diff(timestamp);
run1 = run(1+(I(1)==0):2:end);
% runlength0 = runlength(1+(I(1)==1):2:end)
timestamp1 = timestamp1(1:length(run1));

% [timestamp1' run1']
if(nargin >= 3)
    timestamp1 = timestamp1(run1 >= threshold);
    run1 = run1(run1 >= threshold);
end
if(flip)
    timestamp1 = timestamp1';
    run1 = run1';
end
end