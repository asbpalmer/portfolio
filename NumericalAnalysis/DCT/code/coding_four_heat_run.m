N = 10;
n = 2^N;
xl = -1;
xr = 1;

[us, xs] = coding_four_heat(n, xl, xr);

plot(xs, us);