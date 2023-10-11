#include <stdio.h>
#include <stdlib.h>

void print_int(int n);
int* alloc_obj(int size);

// prints n
void print_int(int n){
	printf("%d\n", n);
}

// used to create y and R variables at the top of each run
// make sure you are freeing with free_ints() at the end of the program
// auto initializes elements to zero
int* alloc_obj(int size){
	return (int*) calloc(size, sizeof(int));
}