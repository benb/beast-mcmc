gcc -O4 -march=pentiumpro -mcpu=pentiumpro -funroll-loops -ffast-math -fstrict-aliasing -c -I/usr/java/j2sdk1.4.1_01/include/ -I/usr/java/j2sdk1.4.1_01/include/linux NucleotideLikelihoodCore.c -o libNucleotideLikelihoodCore.o
ld -shared -o libNucleotideLikelihoodCore.so libNucleotideLikelihoodCore.o 

