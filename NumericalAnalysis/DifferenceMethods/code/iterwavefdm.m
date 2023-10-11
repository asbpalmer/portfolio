function [xs, ys, us] = iterwavefdm(c, hx, hy, xl, xr, yb, yt, init, ut, le, re)
% calculate the size of our mesh
m = round((xr - xl)/hx) + 1;
n = round((yt - yb)/hy) + 1;

if m <= 3
    disp("m is too small! Try smaller distance step size")
    return
end
if n < 3
    disp("n is too small! Try small time step size")
    return
end

% set up our current A and s matrices
A = zeros(m-2, m-2);
s = zeros(m-2,1);

% set up our us matrix
% Us(i, j) holds ui at time tj for i=1,...,m-1
% each column is a us vector (will be transposed)
us = zeros(m, n+1);

% find our xs and ys
xs = xl + (0:m-1)*hx; ys = yb + (0:n-1)*hy;

% u is our us at the current timestep, starting with initial condition
us(:,1) = init(xs)';
u = us(2:m-1, 1);

% calculate our sigma and sigma squared
sigma = c*hy/hx;
sigma2 = sigma^2;

% set up our A matrix since it doesn't change
% handle our left values
A(1, 1) = 2-2*sigma2;
A(1, 2) = sigma2;
for i=2:m-3
    % handle inner values
    A(i, i) = 2-2*sigma2;
    A(i, i-1) = sigma2;
    A(i, i+1) = sigma2;
end
% handle the right values
A(m-2,m-2) = 2 - 2*sigma2;
A(m-2,m-3) = sigma2;

% handle our initial timestep (j0)
s(1) = sigma2 * init(xs(1), ys(1));
s(m-2) = sigma2 * init(xs(m-2), ys(1));

% create Uj1
u = (A*u - 2*hy*ut(xs, ys(1)) + sigma2 * s)/2;
us(2:m-1, 2) = u;
us(1, 2) = le(xs(1), ys(2));
us(m, 2) = re(xs(m), ys(2));

for j=3:n
    s(1) = us(1, j-1);
    s(m-2) = us(m, j-1);
    u(1:m-2) = A*us(2:m-1, j-1) - us(2:m-1, j-2) + sigma2 * s;
    us(2:m-1, j) = u(1:m-2);
    us(1, j) = le(xs(1), ys(j));
    us(m, j) = re(xs(m), ys(j));
end

us = us(:, 1:n)';

% vals = reshape(v(1:mn), m, n)';