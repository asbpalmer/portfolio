% diffusion coefficient
d = 2;

% bounds
t0 = 0;
tmax = 1;
x0 = 0;
xmax = 1;

% initial, top, bottom equations
init = @(x, t) 2*cosh(x);
top = @(x, t) exp(2*t+1) + exp(2*t-1);
bottom = @(x, t) 2*exp(2*t);

tic
% choose which method
%[x, y, v] = heatforwarddm(d, .1, .002, x0, xmax, t0, tmax, init, bottom, top); % part a
%[x, y, v] = heatforwarddm(d, .1, .004, x0, xmax, t0, tmax, init, bottom, top); % part b
%[x, y, v] = heatbackwarddm(d, .1, .004, x0, xmax, t0, tmax, init, bottom, top); % part c
%[x, y, v] = heatcranknicholson(d, .1, .1, x0, xmax, t0, tmax, init, bottom, top); % part d
toc

% calculate the exact solution
exact = @(x, t)exp(2*t+x) + exp(2*t-x);

% form the mesh and evaluate the exact solution at the mesh
[X, Y]= meshgrid(x, y);
exacts = exact(X, Y);

error = v - exacts;
disp("Mean absolute error: " + mean(abs(error), "all"))