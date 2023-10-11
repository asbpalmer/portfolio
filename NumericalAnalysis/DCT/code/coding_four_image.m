% Coding 4 Image
function X = coding_four_image(I, N)
Y = round(dct2(I));
[m, n] = size(Y);
for i=1:m
    for j=1:n
        if i+j > N
            Y(i, j) = 0;
        end
    end
end

X = uint8(idct2(Y));