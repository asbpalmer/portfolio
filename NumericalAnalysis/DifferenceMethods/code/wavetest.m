% wave coefficient
c = 2;

% bounds
x0 = -2;
xmax = 2;
t0 = 0;
tmax = 1;

% initial, top, bottom equations
init = @(x, t) sin(pi*x/2);
ut = @(x, t) 0;
left = @(x, t) 0;
right = @(x, t) 0;

tic
% choose which method
%[x, y, u] = matrixwavefdm(c, 0.05, 0.02, x0, xmax, t0, tmax, init, ut, left, right); % part a matrix
%[x, y, u] = matrixwavefdm(c, 0.05, 0.03, x0, xmax, t0, tmax, init, ut, left, right); % part b matrix
%[x, y, u] = iterwavefdm(c, 0.05, 0.02, x0, xmax, t0, tmax, init, ut, left, right); % part a iter
%[x, y, u] = iterwavefdm(c, 0.05, 0.03, x0, xmax, t0, tmax, init, ut, left, right); % part b iter
toc

% calculate the exact solution
exact = @(x, t)sin(pi*x/c).*cos(pi*t);
ux = @(x, t)cos(pi*x/c).*cos(pi*t);
ut = @(x, t) -sin(pi*x/c).*sin(pi*t);

% form the mesh and evaluate the exact solution at the mesh
[X, Y]= meshgrid(x, y);
exacts = exact(X, Y);
uxs = ux(X, Y);
uts = ut(X, Y);
error = u - exacts;
disp("Mean absolute error: " + mean(abs(error), "all"))