RGB = imread("peppers.png");
I = rgb2gray(RGB);
N = 64;

X = coding_four_image(I, N);

% show the compressed image
%imshow(X);

% show the original image
%imshow(I);
imshow(RGB);

% show the difference between the two images
% diff = abs(X - I);
% mean(diff)
% imshow(diff);
