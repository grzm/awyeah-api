# awyeah-api - aws-api for babashka

Cognitect's [aws-api][] and [babashka][]. Aw yeah.

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

Requires babashka v1.0.170 or later (java.util.concurrent.ThreadFactory support).

## deps
``` clojure
com.grzm/awyeah-api {:git/url "https://github.com/grzm/awyeah-api"
                     :git/sha "e5513349a2fd8a980a62bbe0d45a0d55bfcea141"
                     :git/tag "v0.8.84"}
```

Up to date with `com.cognitect.aws/api {:mvn/version "0.8.686"}` (tagged 2023-07-11).

## In brief

Add to your `deps.edn` for Clojure or `bb.edn` for babashka:

### `deps.edn`
```clojure
{:deps {com.cognitect.aws/endpoints {:mvn/version "1.1.12.504"}
        com.cognitect.aws/s3 {:mvn/version "848.2.1413.0"}
        com.grzm/awyeah-api {:git/url "https://github.com/grzm/awyeah-api"
                             :git/sha "e5513349a2fd8a980a62bbe0d45a0d55bfcea141"
                             :git/tag "v0.8.84"}
        ;; dependencies included in babashka
        cheshire/cheshire {:mvn/version "5.12.0"}
        org.clojure/core.async {:mvn/version "1.6.681"}
        org.clojure/data.xml {:mvn/version "0.2.0-alpha8"}
        org.clojure/tools.logging {:mvn/version "1.2.4"}}}
```

### `bb.edn`
``` clojure
{:deps {com.cognitect.aws/endpoints {:mvn/version "1.1.12.504"}
        com.cognitect.aws/s3 {:mvn/version "848.2.1413.0"}
        com.grzm/awyeah-api {:git/url "https://github.com/grzm/awyeah-api"
                             :git/sha "e5513349a2fd8a980a62bbe0d45a0d55bfcea141"
                             :git/tag "v0.8.84"}}}
```

```clojure
(require '[com.grzm.awyeah.client.api :as aws])

(def s3 (aws/client {:api :s3}))

(def buckets (-> (aws/invoke s3 {:op :ListBuckets})
                 :Buckets))

(prn buckets)
```

## Differences from aws-api

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

### Exclude Localstack tests

Sometimes you want to run tests but you don't want to stand up
Localstack. This is currently convenient only with JVM Clojure.

```sh
clj -X:clj:dev:test :excludes '[:integration]'
```

Some additional testing notes so I don't have to look up each time I
do awyeah-api maintenance.

Select namespaces to test:

```sh
clj -X:clj:dev:test :nses '[com.grzm.awyeah.util-test]'
```

#### Testing aws-api

It's useful to run aws-api tests to compare behavior. These are run in
an aws-api repo checkout.

Selecting namespaces to test:

```sh
clj -M:dev:test -n cognitect.aws.util-test
```

## Thanks

Much thanks to [Michiel Borkent](https://michielborkent.nl) for
babashka and providing support and guidance in getting the pieces in
place (java.net.http and various other Java classes) to make this library possible.

## Copyright and License

Mostly © 2015 Cognitect

Parts © 2022–2023 Michael Glaesemann

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
