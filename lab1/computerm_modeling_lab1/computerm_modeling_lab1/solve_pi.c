#include <stdio.h>
#include <stdlib.h>
#include <time.h>

#define N 2000000
int main()
{
	int i;
	int n = 0;
	double x, y;
	srand(time(00));

	for (i = 1; i <= N; i++)
	{
		x = rand() * 2.0 / RAND_MAX - 1;
		y = rand() * 2.0 / RAND_MAX - 1;

		if (x * x + y * y < 1.0)
			n++;
		//printf(" Π=  %f", 4.0 * n / N);
	}
	printf(" Π=  %f", 4.0 * n / N);
	return 0;
}
