% Coding 4 heat equation 
% Uses the approximations and substitutions provided.
function [us, xs] = coding_four_heat(n, xl, xr)
xs = linspace(xl, xr, n);
ys = dct(2*cosh(xs))/sqrt(n/2);
us = zeros(n, 1);

for i=1:n
    x = xs(i);
    u = 0;
    for k=0:n-1
        term = exp(-k^2 * pi^2/4) * cos(k*pi*(x+1)/2);
        u = u + ys(k+1) * term;
    end
    us(i) = u;
end