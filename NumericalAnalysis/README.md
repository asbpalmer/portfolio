# Numerical Analysis Projects

For Numerical Analysis, I wrote programs in both Python and MATLAB that implemented different algorithms.

The Python codes are included in:
- VariousNewtonsMethods (which explore the Multivariable Newton's Method and the Gauss-Newton method to answer questions about satellite placement)
- FiniteDifferenceVSFiniteElement (which compares the speed and accuracy of the Finite Element Method and the Finite Difference Method for solving the heat equation)

The MATLAB codes are included in:
- DifferenceMethods (which compared the Forwards Difference Method, the Backwards Difference Method, and the Crank-Nicholson Method for the Heat equation, and the Finite Difference Method for wave equations).
- DCT (which explored the Discrete Cosine Tranform and it's uses for compression and solving a heat equation)

Each project includes a report which walks through the initial problem, the code, and the results.

DifferenceMethods also includes two separate approaches for solving the wave equation, which may be notable for some of the more mathematically-inclined people. It proved to be much slower but also much more accurate and stable.

Different programs also highlight different elements I have learned about and subsequently implemented in my code, such as:
- VariousNewtonsMethods demonstrates currying and string formatting to make the results all nice, pretty, and easy to pour over
- FiniteDifferenceVSFiniteElement demonstrates how I collect data using Pandas dataframes to allow nice and easy exporting of results.
- DifferenceMethods show my first major experiences with MATLAB, and also show some of my ability to take unique approaches to problems to end up with unconventional, insightful solutions.
	- It has been some time since I programmed it, but I believe that my "Matrix Solution" was not in the class' instruction and came out as a byproduct of my approaches to get around certain bugs I was encountering.
- DCT's code may not be terribly insightful, as barebones as it is, but some of the remarks in my report, I believe, indicate more of my thoroughness when it comes to data and my ability to take some extra insight out of problems.