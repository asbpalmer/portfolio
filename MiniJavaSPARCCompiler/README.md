# MiniJava to SPARC compiler

Additional Dependencies: javacc, architecture that supports sparc

Created for a Compiler Theory Class that was teaching from Modern Compiler Implementation in Java, 2nd ed. by Andrew W. Appel.

This project was to make a compiler for a toned-down Java implementation that includes basic functions such as multiplication, arrays, and objects, but does not include non-integer numbers, strings, and plenty of other features.

To run the program:
- Make sure to replace any troublesome CRLFs with simple LFs (some Linux programs may not like them)
- Then run "make" in the root directory
- Then run ./compile \[PATH_TO_PROGRAM\]
- Then run ./assemble \[PATH_TO_PROGRAM\]

This complicated assignment was also my first experience with Java, and I am rather happy with the results

This program demonstrates:
- Advanced data structures and algorithm design, object-oriented programming, and scripting languages.
- Low-level programming that is necessary for a compiler to work
- Shell-scripting languages and make
- Ability to design a complicated algorithm in advanced and to adapt when unforeseen changes cause the algorithm to struggle.

The project went through multiple phases even between consecutive due dates, and was, by far, the most complicated coding project I have ever undertaken.

That said, I also have plenty of takeaways:
- I learned a lot about good Java programming philosophies that fundamentally improved how I program. Many of these ideas (object inheritance, especially) were immediately used in my RayTracing project.
- At some crucial parts of the development process, I created a lot of unnecessary bloat functions that I later streamlined or cut completely. Being able to tell the useful functions from the useless ones, and subsequently designing more robust functions improved my abilities what feels like tenfold
- I learned how to create effective debugging tools for complicated algorithms to allow me to quickly and accurately diagnose core (often convoluted) problems in my design.

And some things that, if I return to the project, I would look to improve:
- Proper ability for the compiler to handle arrays, which proved to be the biggest thorn in my side for the test cases
- Ability to check sections that don't have certain errors for certain, other errors
- Further weeding out extra algorithms and streamlining everything to improve performance and usability.

Java script runs to completion on all of Appel's test cases. I have included some additional test cases with "Test" at the beginning of their name.

Known full successes:
- Factorial.java from Appel, runs to completion, final script executes and outputs the correct response.
- TestOverwriteInherit.java, from me, runs to completion, final script executes and outputs the correct response
    - Demonstrates inheriting an already named function (uses the one defined for class, not superclass)
    - Also demonstrates order of operations evaluation
- TestOverload.java, from me, runs to completion, final script executes and outputs the correct response
    - Demonstrates overloading within the class
    - Also demonstrates parentheses grouping expressions
- TestBool.java, from me, runs to completion, final script executes and outputs the correct response
    - Demonstrates boolean branching using various boolean operators (not, less than, and && in the same expression)
- TestBoolField.java, from me, runs to completion, final script executes and outputs the correct response
    - Demonstrates using a field from the class to evaluate a bool and an initialization function to set a parameter

Known failures:
- BinarySearch.java, from Appel
    - compiler runs to completion
    - syntactically correct .s file
    - executable segmentation faults
- BinaryTree.java, from Appel
    - compiler runs to completion
    - syntactically correct .s file
    - but the executable segmentation faults
- BubbleSort.java, from Appel
    - compiler runs to completion
    - syntactically correct .s file
    - the executable segmentation faults
- LinearSearch.java, from Appel
    - compiler runs to completion
    - syntactically correct .s file
    - the executable segmentation faults
- LinkedList.java, from Appel
    - compiler runs to completion
    - syntactically correct .s file
    - executable segmentation faults
- QuickSort.java, from Appel
    - compiler runs to completion
    - produces a syntactically incorrect .s file

These failed test cases demonstrates an issue with arrays (both assignment and lookup) in my compiler's translation

Except for in quick sort, which demonstrates a trouble with handling temps and register allocation
