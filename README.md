# we-expander
Query expansion without query logs - a semantic approach

Expands any given query by applying kNN and cosine similarity calculations on word embeddings, which follows the approach of [Roy, Dwaipayan, et al. "Using word embeddings for automatic query expansion." (2016).](https://arxiv.org/pdf/1606.07608.pdf)



## Pre-Retrieval:

## Input
Input is a directory with one ore more word embedding files. Each line of a file has to start with the word than seperated with withspace followed by the word embedding aka a floating point vector of which each dim is seperated by a whitespace, e.g.:
```
the 0.418 0.24968 -0.41242 0.1217 0.34527 -0.044457 -0.49688 -0.17862 -0.00066023 -0.6566 0.27843 -0.14767 -0.55677 0.14658 
of 0.70853 0.57088 -0.4716 0.18048 0.54449 0.72603 0.18157 -0.52393 0.10381 -0.17566 0.078852 -0.36216 -0.11829 -0.83336 0.11917 to 0.68047 -0.039263 0.30186 -0.17792 0.42962 0.032246 -0.41376 0.13228 -0.29847 -0.085253 0.17118 0.22419 -0.10046 -0.43653
and 0.26818 0.14346 -0.27877 0.016257 0.11384 0.69923 -0.51332 -0.47368 -0.33075 -0.13834 0.2702 0.30938 -0.45012 -0.4127
in 0.33042 0.24995 -0.60874 0.10923 0.036372 0.151 -0.55083 -0.074239 -0.092307 -0.32821 0.09598 -0.82269 -0.36717 -0.67009
```


## Usage
```
$ ./pre-retrieval arg1
      arg1: WORD EMBEDDINGS DIRECTORY\t - directory with word embeddings, separated by whitespace
```
Example run:
```
$ ./pre-retrievals embeddings.txt 
> pre-retrieval expander: nearest
connects, 0.7169279
railhead, 0.70533824
situated, 0.7036282
distance, 0.6845015
connecting, 0.67515314
located, 0.67337537
main, 0.66029966
connect, 0.659874
closest, 0.65352273
bus, 0.6500851
```


## Post-Retrieval:
In the addition to the processes of the Pre-Retrieval, the Post-Retrieval pre-selects relevant documents in which vector space kNN will be applied.

## Input
Input is again a directory with one ore more word embedding files. The second obligatory argument to call post-retrieval is a corpus directory whih contains at least one file conll format.


## Usage
```
$ ./post-retrieval arg1 arg2
      arg1: WORD EMBEDDINGS DIRECTORY\t - directory with word embeddings, separated by whitespace
      arg2: CORPUS DIRECTORY\t           - directory with corpus file, file must have conll format
```
Example run:
```
$ ./post-retrievals embeddings.txt /conll-corpus
> post-retrieval expander: nearest
near, 0.6419043
station, 0.6232734
train, 0.6178746
town, 0.6120607
rail, 0.5961648
line, 0.5947579
small, 0.5820411
where, 0.5563269
east, 0.55490595
city, 0.55174905
```




## Output
Output is for both flavours the query expansion suggestion ranking together with the ranking score.


_

Authors: *Holger Muth-Hellebrandt, Neele Witte*
