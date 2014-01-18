function [GMM, r] = gmm(D,K,L,official)

if(nargin < 4)
    official = true;
end
% official = false;
if(official)
    % The official MATLAB implementation of GMM is so much better and
    % boost the identifications a lot in terms of vIERs.
    options = statset('MaxIter',300,'TolFun',1e-6);
    obj = gmdistribution.fit(D, K, 'Regularize', 1, 'CovType', 'full', 'Options', options);
    GMM.MU = obj.mu;
    GMM.SIGMA = obj.Sigma;
    GMM.ALPHA = obj.PComponents';
    GMM.K = K;
    r = posterior(obj,D);
    return;
end

% initialization
E = 1e-6;
N = size(D, 1);
P = size(D, 2);
MU = zeros(K,P);
SIGMA = zeros(P,P,K);
% [N K]
ALPHA = randsample([1:N]', K);
ALPHA = ALPHA / sum(ALPHA);
for k = 1:K
    samples = randsample([1:N]', round(N/10));
    samples = D(samples, :);
    MU(k,:) = mean(samples);
    SIGMA(:,:,k) = cov(samples);
    % regularization by adding a small positive value to the diagonal
    SIGMA(:,:,k) = SIGMA(:,:,k) + diag(ones(P,1) * 1e-6);
end
ML = -Inf(L, 1);
% MU
% SIGMA
% ALPHA

% figure;
% g = cell(size(D,1),1);
for i = 1:L
    % Expectation
    r = zeros(N, K);
    for k = 1:K
        r(:,k) = ALPHA(k) .* mvnpdf(D, MU(k,:), SIGMA(:,:,k));
    end
    ML(i) = sum(log(sum(r)));
    r = bsxfun(@rdivide, r,  sum(r,2));
    
%     clf;
%     [~, I] = max(r,[],2);
%     for k = 1:K
%         g(I==k) = {sprintf('s%d', k)};
%     end
%     gscatter(D(:,1), D(:,2), g);
%     hold on;
%     plot(MU(:,1), MU(:,2), 'kx', 'LineWidth', 2);
%     title(sprintf('GMM/EM, i=%d',i));
%     F(i) = getframe();
    if(i > 1 && abs(ML(i) - ML(i-1)) < E)
        ML = ML(1:i);
        break;
    end
    
    % Maximization 
    for k = 1:K
        Nk = sum(r(:,k));
        ALPHA(k) = Nk / N;
        MU(k,:) = (1/Nk) * sum(bsxfun(@times, r(:,k), D));
        D_MUk = bsxfun(@minus, D, MU(k));
        SIGMA(:,:,k) = (1/Nk) * (bsxfun(@times, r(:,k)', D_MUk') * D_MUk);
        SIGMA(:,:,k) = SIGMA(:,:,k) + diag(ones(P,1) * 1e-5);
    end
%     MU
%     SIGMA
%     ALPHA
end
% close(gcf);
GMM.MU = MU;
GMM.SIGMA = SIGMA;
GMM.ALPHA = ALPHA;
GMM.K = K;
end
