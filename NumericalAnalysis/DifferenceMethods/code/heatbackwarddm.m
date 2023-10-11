% Forward Differences for 2D heat equation script
function [xs, ys, vals] = heatbackwarddm(d, hx, hy, xl, xr, yb, yt, init, be, te)
% calculate the size of our mesh
m = (xr - xl)/hx + 1;
n = (yt - yb)/hy + 1;
mn = m*n;

% get hx^2 in a variable for efficiency
hx2 = hx^2;

% set up our A and b vectors for Au = b
A = zeros(mn, mn);
b = zeros(mn, 1);

% find our xs and ys
xs = xl + (0:m-1)*hx; ys = yb + (0:n-1)*hy;

% calculate our sigma
sigma = d*hy/hx2;

% define our indexer function
index = @(xi, yj) xi+m*(yj-1);

% do the interior points
for i=2:m-1
    for j=2:n
        A(index(i,j), index(i,j)) = 1+2*sigma;
        A(index(i,j), index(i-1, j)) = -sigma;
        A(index(i,j), index(i+1, j)) = -sigma;
        A(index(i,j), index(i, j-1)) = -1;
        b(index(i,j)) = 0;
    end
end

% do the left temporal bound
for i=1:m
    A(index(i,1), index(i,1)) = 1;
    b(index(i,1)) = init(xs(i), ys(1));
end

for j=2:n
    A(index(1,j), index(1,j)) = 1;
    b(index(1,j)) = be(xs(1), ys(j));
    A(index(m,j), index(m,j)) = 1;
    b(index(m,j)) = te(xs(m), ys(j));
end

v = A\b;
vals = reshape(v(1:mn), m, n)';


