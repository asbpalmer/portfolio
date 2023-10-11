% Finite Differences for 2D heat equation script
function [xs, ys, vals] = matrixwavefdm(c, hx, hy, xl, xr, yb, yt, init, ut, le, re)
% calculate the size of our mesh
m = round((xr - xl)/hx) + 1;
n = round((yt - yb)/hy) + 1;
mn = m*n;

% set up our A and b vectors for Au = b
A = zeros(mn, mn);
b = zeros(mn, 1);

% find our xs and ys
xs = xl + (0:m-1)*hx; ys = yb + (0:n-1)*hy;

% calculate our sigma and sigma squared
sigma = c*hy/hx;
sigma2 = sigma^2;

% define our indexer function
index = @(xi, yj) xi+m*(yj-1);

% do the interior points all non-initial timesteps
for i=2:m-1
    % handle the first step
    A(index(i,2), index(i, 1)) = 2-2*sigma2;
    A(index(i,2), index(i-1, 1)) = sigma2;
    A(index(i,2), index(i+1, 1)) = sigma2;
    A(index(i,2), index(i, 2)) = -2;
    b(index(i,2)) = 2*hy*ut(xs(i),ys(2));
    for j=3:n
        A(index(i,j), index(i,j-1)) = 2-2*sigma2;
        A(index(i,j), index(i-1, j-1)) = sigma2;
        A(index(i,j), index(i+1, j-1)) = sigma2;
        A(index(i,j), index(i, j-2)) = -1;
        A(index(i,j), index(i, j)) = -1;
        b(index(i,j)) = 0;
    end
end

% fill in the initial condition
for i=1:m
    A(index(i,1), index(i,1)) = 1;
    b(index(i,1)) = init(xs(i), ys(1));
end

% do the left and right for all values of j
for j=2:n
    A(index(1,j), index(1,j)) = 1;
    b(index(1,j)) = le(xs(m), ys(j));
    A(index(m,j), index(m,j)) = 1;
    b(index(m,j)) = re(xs(m), ys(j));
end
v = A\b;
vals = reshape(v(1:mn), m, n)';


