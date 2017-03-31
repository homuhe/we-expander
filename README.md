# we-expander
Query expansion without query logs - a semantic approach

Expands any given query by TO EDIT [TO EDIT](http://www.tyr.unlu.edu.ar/tallerIR/2013/papers/querysuggestion.pdf)

TO EDIT:
```
TO EDIT
```


## Input
TO EDIT


## Usage
```
$ ./we-expander TO EDIT
      arg1: CORPUS DIRECTORY - directory with text files, either raw or conll
      opt1: FORMAT           - 'conll', 'raw', default = 'conll'
      opt2: STOPWORDS        - list of stopwords, each line one stopword
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
