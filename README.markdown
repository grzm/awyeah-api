# awyeah-api - aws-api for babashka

Cognitect's [aws-api][] and [babashka][]. Aw yeah.

**Alpha** Only gently tested.

awyeah-api is _another_ Clojure library which provides programmatic
access to AWS services from your Clojure **or babashka** program. Its
_raison d'être_ is to be a drop-in replacement for Cognitect's
brilliant [aws-api][] that will work from source with [babashka][].

awyeah-api should work with `com.cognitect.aws/endpoints` and
`com.cognitect.aws` service packages.

* [aws-api][]
* [API Docs](https://cognitect-labs.github.io/aws-api/)

[aws-api]: https://github.com/cognitect-labs/aws-api
[babashka]: https://babashka.org

### Acknowledgements (and disclaimer)

This port is based on the great work done by the fine folks at
Cognitect. I am not affiliated with Cognitect and this is not a
Cognitect-distributed library.

## Requirements

Requires babashka 0.7.0 or later (clojure.spec.alpha support).

## deps
``` clojure
com.grzm/awyeah-api {:git/url "https://github.com/grzm/awyeah-api"
                     :git/sha "9257dc0159640e46803d69210cae838d411f1789"
                     :git/tag "v0.8.41"}
```

Up to date with `com.cognitect.aws/api {:mvn/version "0.8.603"}` (tagged 2022-10-11).

## In brief

Add to your `deps.edn` for Clojure or `bb.edn` for babashka:

### `deps.edn`
```clojure
{:deps {com.cognitect.aws/endpoints {:mvn/version "1.1.12.307"}
        com.cognitect.aws/s3 {:mvn/version "822.2.1145.0"}
        com.grzm/awyeah-api {:git/url "https://github.com/grzm/awyeah-api"
                             :git/sha "9257dc0159640e46803d69210cae838d411f1789"
                             :git/tag "v0.8.41"}
        ;; dependencies included in babashka
        cheshire/cheshire {:mvn/version "5.10.2"}
        org.clojure/core.async {:mvn/version "1.5.648"}
        org.clojure/data.xml {:mvn/version "0.2.0-alpha8"}
        org.clojure/tools.logging {:mvn/version "1.2.4"}}}
```

### `bb.edn`
Include [`org.babashka/spec.alpha`](https://github.com/babashka/spec.alpha)
``` clojure
{:deps {com.cognitect.aws/endpoints {:mvn/version "1.1.12.307"}
        com.cognitect.aws/s3 {:mvn/version "822.2.1145.0"}
        com.grzm/awyeah-api {:git/url "https://github.com/grzm/awyeah-api"
                             :git/sha "9257dc0159640e46803d69210cae838d411f1789"
                             :git/tag "v0.8.41"}
        org.babashka/spec.alpha {:git/url "https://github.com/babashka/spec.alpha"
                                 :git/sha "8df0712896f596680da7a32ae44bb000b7e45e68"}}}
```

```clojure
(require '[com.grzm.awyeah.client.api :as aws])

(def s3 (aws/client {:api :s3}))

(def buckets (-> (aws/invoke s3 {:op :ListBuckets})
                 :Buckets))

(prn buckets)
```

## Differences from aws-api

The awyeah-api client does not auto-refresh AWS credentials. The
aws-api behavior relies on features that aren't present in babashka,
and I haven't figured out an alternative implementation of the
behavior. My use case is short-lived scripts, where JVM start-up time
can dwarf script execution time: credentials don't have much time to
get stale. If I had longer-lived processes, start-up time wouldn't be
an issue and I'd just use the Clojure.

The [com.cognitect/http-client][] used by aws-api automatically
uncompresses S3 objects if they have the appropriate content-type
metadata in S3. I've chosen _not_ to replicate this behavior with the
bundled HTTP client based on `java.net.http` as I think it's a bit of
a misfeature to have enabled by default in a programmatic client
library like this. (That said, that makes it less of a drop-in
replacement, doesn't it? I may revisit this.)

[com.cognitect/http-client]: https://search.maven.org/artifact/com.cognitect/http-client

See [Porting Decisions](docs/porting-decisions.markdown) if you're
interested in the nitty gritty or are wondering why I made some
decision or other.

## Development

Get thee to a repl!

```sh
bb --classpath $(clojure -Spath -A:bb:dev:test) nrepl-server 1138
```

## Testing

Run the tests against a [LocalStack](https://localstack.cloud) Docker
container.

```sh
bin/dc up
bin/test
bin/dc down
```

## Thanks

Much thanks to [Michiel Borkent](https://michielborkent.nl) for
babashka and providing support and guidance in getting the pieces in
place (java.net.http and various other Java classes) to make this library possible.

## Copyright and License

Mostly © 2015 Cognitect

Parts © 2022 Michael Glaesemann

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
