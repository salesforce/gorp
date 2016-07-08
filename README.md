# Gorp

Library for building efficient log-line extractor from a multi-regexp input definition,
starting with an ordered set of regular expressions (a subset of the usual Java regexp definition;
more on this below),
building a big [DFA](https://en.wikipedia.org/wiki/Deterministic_finite_automaton)
using excellent [Automaton](http://www.brics.dk/automaton/), as well as complementary extractors
for actual value extraction.

## Basic operation

To use Gorp, you need three things:

1. This library: comes in a single jar which includes shaded dependencies (so-called "uber-jar")
2. Extraction definition: often a `File`, either stand-alone or a resource from within bigger jar; or possibly read from external storage like Amazon S3
3. Input, in form of `java.lang.String`s, often coming from a line-oriented input source like (a set of) log file(s).

### Extractor input definition

Extractor input definition is a line-oriented text document, consisting of 3 kinds of declarations:

1. Pattern declarations, which define low-level building blocks that consists of snippets of Regular Expressions and/or references to other patterns
2. Template declarations, which define intermediate building blocks that consist of references to patterns, literal text segments, references to named templates, inlined patterns and extractors
3. Extraction declarations, named matching rules that associate a named template with output, possible augmented by additional properties

In addition to these declarations, individual "extractors" are declared as part of templates of extractions.

Simple example declarations would be:

```
# Patterns
pattern %num \d+
pattern %hostname [a-zA-Z0-9_\-\.]+
pattern %status \w+

# Templates
@endpoint %hostname:%num
# or, if we want to create a parametric template:
@extractEndpoint() $1(%hostname):$2(%num)

# Extraction
extract HostDefinition {
  template @extractEndpoint($srcHost,$srcPort) $status(%status)
```

which shows both a simple template (no parameters), `@endpoint`, and parametric variant `@extractEndpoint`.

## Basic usage

Assuming you have file `extractions.xtr` which contains extraction definition (2), and wanted to extract values out of it, you could use:

```java
DefinitionReader r = DefinitionReader.reader(new File("extractions.xtr"));
Gorp gorp = r.read();
final String TEST_INPUT = "prefix: time=12546778 verb=PUT";
ExtractionResult result = gorp.extract(TEST_INPUT);
if (result == null) { // no match, handle
   throw new IllegalArgumentException("no match!");
}
Map<String,Object> properties = asMap();
// and then use extracted property values
```

and a sample extraction definition could be something like (note that this is not the only, or even the simplest, way to define it):

```
pattern %num \d+
pattern %word \w+
# define both simple and parametric just for fun; either one would work
template @extractTime time=$time(%num)
template @extractVerb() verb=$1(%word)
extract SimpleEntry {
   template prefix: @extractTime @extractVerb($verb)
}
```

and as a result you would get Map like:

```json
{
  "time" : "12546778",
  "verb" : "PUT"
}

```

### Sample Extractor Input Definition

Let's try another example, this time for input of form:

```
102456879: GET 123ms 200 /rest-service/v1/endpoint?foo=bar
```

and we could use definition like:

```
pattern %num \d+
pattern %word \w+
pattern %phrase \S+

extract PutRequest {
   # It's ok to: (a) extract constant value; (b) concatenate physical lines with backslash
   template [$timestamp(%num)]: $verb(PUT) $timeTakenInMsec(%num)ms\
 $path(%phrase)
   append { "marker" : "EXTRACTED" }
}
extract GetRequest {
   template [$timestamp(%num)]: $verb(GET) $timeTakenInMsec(%num)ms\
 $path(%phrase)
   append { "marker" : "EXTRACTED" }
}
extract OtherRequest {
   template [$timestamp(%num)]: $verb(%word) $timeTakenInMsec(%num)ms\
 $path(%phrase)
   append { "marker" : "EXTRACTED" }
}
```

which would be one way to define a multi-matcher. Things to note include:

* Multiple physical lines may be concatenated into a single logical line by ending the physical line with backslash ("\")
* Comments are allowed; may have optional leading white-space but the first character has to be '#'
* Order of declarations matters: first match will be taken; in this case this means that "OtherRequest" match needs to come after both "GetRequest" and "PutRequest"
* You can append arbitrary key/value pairs by using "append" property for extraction

It is also worth noting that we did not use templates here; but we could simplify things a bit by doing something like:

```
template @timeAndPath $timeTakenInMsec(%num)ms $path(%phrase)
# ...
extract OtherRequest {
   template [$timestamp(%num)]: $verb(%word) @timeAndPath
   append { "marker" : "EXTRACTED" }
}
```

that is, by creating reusable templates to reduce amount of duplication.

Further simplication may be possible by using parametric templates, where you can use parameters
to refer to either other templates (by name passed as parameter) and different extractor names
(similarly pass name of property to extract to), for example:

```
template @extractBracketed() [$1(@2)]
```

where you would invoke it by something like:

```
template @endpointDef %hostname:%port
extract OtherRequest {
   template Connection: @extractBracketed($src,@endpointDef) ->\
 @extractBracketed($dst,@endpointDef)
}
```

to match lines like:

```
Connection: [foobar.com:8080] -> [barfoo.internal.org:80]
```

and extract values `foobar.com:8080` (as `src`) and `barfoo.internal.org:80` (as `dst`)

## Regular Expressions supported

Expressions supported for named and inline patterns can be thought of either as a subset of
the full `java.util.regexp.Pattern`, or as a superset of what [Automaton](http://www.brics.dk/automaton/)
`RegExp` implementation supports
(see [Automaton RegExp Javadocs](http://www.brics.dk/automaton/doc/index.html?dk/brics/automaton/RegExp.html)).

### Additions to Automaton base

Additions above and beyond `Automaton` `RegExp` are:

* Quoted control characters like `\\t` (stock Automaton does NOT allow those, only literal tabs!)
* Addition of pre-defined character classes `\d`/`\D`, `s`/`S`, `\w`/`\W`

Basic `Automaton` supports

* Simple character classes (one level of brackets, optionally starting with `^` for negation`
* Basic repetition markers `*` (Kleene star), `+`, `?`, `{n}`
* Grouping (`(....)`)
* Literal escaping with `\` (that is, character immediately following is used as-is)
    * NOTE: due to extension here, literal quoting is ONLY used for-alphanumeric characters!
* Concatenation, union (`|`)

but none of the extension features are enabled, to make it more likely that the same input
patterns can be used with both `Automaton` and the regexp-based extractors.

### Features missing from `java.util.regex`

Of all the features, explicitly not supported features include:

* "Advanced" character class features like:
    * Advanced combinations of character classes (subtraction, intersection)
    * POSIX, java.lang.Character etc classes (anything of form `\p{...}`)
* Boundary matchers (except for implicit `^` and `$`)
* Reluctant or Possessive quantifiers (all matching is greedy by default)
* Back-references
* Named matching groups (instead, extractors are used to same effect)
* Special constructors (matching that starts with `(?`
    * NOTE: internally non-matching group markers are used to only capture groups define via extractors, as optimization)

Some of these features may be potentially supportable, if [Automaton](http://www.brics.dk/automaton/) package adds such support; or, in case of named character classes, by adding conversion within Gorp itself.
But some features (like back-references) are unlikely to be supportable.
