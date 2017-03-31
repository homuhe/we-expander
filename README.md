# we-expander
Query expansion without query logs - a semantic approach

Expands any given query by applying kNN and cosine similarity calculations on word embeddings, which follows the approach of [Roy, Dwaipayan, et al. "Using word embeddings for automatic query expansion." (2016).](https://arxiv.org/pdf/1606.07608.pdf)



## Pre-Retrieval:

## Input
Input is a directory with one ore more word embedding files. Each line of a file has to start with the word than seperated with withspace followed by the word embedding aka a floating point vector of which each dim is seperated by a whitespace, e.g.:
```
$ ./we-expander TO EDIT
      arg1: CORPUS DIRECTORY - directory with text files, either raw or conll
      opt1: FORMAT           - 'conll', 'raw', default = 'conll'
      opt2: STOPWORDS        - list of stopwords, each line one stopword
```


## Usage
```
$ ./pre-retrieval arg1")
      arg1: WORD EMBEDDINGS DIRECTORY\t - directory with word embeddings, separated by whitespace
```
Example run:
```
$ ./we-expander TO EDIT
> prob-query-expander: president of the uni
united states 0.091636546
united states government 0.07686396
united 0.0367568
union of police associations 0.02680128
...
```

## Output
Output are the query expansion suggestions together with the ranking score.


_

Authors: *Holger Muth-Hellebrandt, Neele Witte*
