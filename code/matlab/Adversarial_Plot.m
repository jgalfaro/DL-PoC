clear;
m = 100; % max. resources
lambda1 = [1 1 2];
lambda2 = [5 10 8];
n = length(lambda1); % number of simulations
step = 2; % step size
R = [];
for i=1:n
    R = [R ; successrate(step,100,7,1000,lambda1(i),lambda2(i))];
    size(R)
end

figure();
ax = subplot(1,1,1);

plot(1:46,smooth(R(1,:)),'r-p',1:46,smooth(R(2,:)),'k-+',1:46,smooth(R(3,:)),'b-*')
Mylegend=legend(...
    'Quantum',...
    'Balanced',...
    'Classical',...
    'FontSize',14, 'Location', 'northwest')

title(Mylegend,'Adversarial\newline Scenarios');
grid on;

xlim([0 46])
groupCenters = @(nGroups,nMembers,interGroupSpace) ...
    nGroups/2+.5 : nGroups+interGroupSpace : (nGroups+interGroupSpace)*nMembers-1;
%x1CenterTicks = groupCenters(numel(1:5), size(R,2), 1);
%set(ax,'XTick',x1CenterTicks,'XTickLabels',{'10^1','10^2','10^3','10^4','10^5','10^6','...','\infty'},'FontSize',12)
x1CenterTicks = groupCenters(numel(1:9), size(R,2), 1);
set(ax,'XTick',x1CenterTicks,'XTickLabels',{'Very Low','Low','Medium','High','Very High'},'FontSize',12)
xlabel('Adversary Resource Investment','FontSize',24);
ylabel('Attack Success Probability','FontSize',24);


function R = successrate(step,m,T,trials,lambda1,lambda2)
% step size (in terms of resources)
% m = maximum investment
% T = number of waypoints
% trials = number of trials
% lambda1 = defense learning disabled
% lambda2 = defense learning enabled
R =[]; % init success rate
for i=10:step:m % loop indexed by increase of resources
    S = zeros(1,trials); % init success count
    for j=1:trials % loop on the number of trials
        % for each waypoint, step success...
        Y = poissrnd(lambda1,1,T);
        % total success
        Z = poissrnd(lambda2,1,T).*Y;
        % success count
        S(j) = S(j) + i-sum(Z)>0;
    end
    R = [ R sum(S)/trials ];
end
end
