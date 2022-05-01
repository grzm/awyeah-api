# Porting Decisions

The primary motivation for the port is compatibilty with
babashka. Beyond compatibility, I also consider maintainability (ease
of keeping the library up-to-date with changes to aws-api),
performnance, ease-of-use, and personal aesthetics (am I happy with
the resulting library?).

## Babashka compatibility

The aws-api library isn't compatible with babashka, as it uses a
number of classes not included in babahska and depends on Cognitect's
[com.cognitect/http-client][], a wrapper around the [Jetty HTTP
Client][jetty-http-client].

### Missing classes

[com.cognitect/http-client]: https://search.maven.org/artifact/com.cognitect/http-client/1.0.111/jar
[jetty-http-client]: https://www.eclipse.org/jetty/documentation/jetty-9/index.html#http-client

The following classes used in aws-api are not included in
babashka.

* `java.lang.Runnable`
* `java.lang.ThreadLocal`
* `java.util.concurrent.ThreadFactory`
* `java.util.concurrent.ScheduledFuture`
* `java.util.concurrent.ScheduledExecutorService`

These classes are referenced in two namespaces: `cognitect.aws.util`
and `cognitect.aws.credentials`. `ThreadLocal` is used in
`cognitect.aws.util` to make [`java.text.SimpleDateFormat`
thread-safe][simple-date-format-bug]. As I'm not concerned with
supporting pre-Java 8 versions, I've decided to use the thread-safe
`java.time.format.DateTimeFormatter` rather than drop thread-safety
work-arounds for `SimpleDateFormat` or implement them in some other
way.

[simple-date-format-bug]: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4228335

The cognitect.aws.util namespace is used throughout the aws-api
library, either directly or transitively.

The balance of the unincluded classes are used in
`cognitect.aws.credentials` to provide auto-refreshing of AWS
credentials. As babashka is commonly used for short-lived scripts as
opposed to long-running server applicatoins, rather than provide an
alternate implementation for credential refresh, I've chosen to omit
this functionality. If credential auto-refresh is something I find
_is_ useful in a babashka context some time in the future, a solution
can be explored at that time.

There are a few other compatiblity issues, such as the use of
`java.lang.ClassLoader::getResources` in
`cognitect.aws.http/configured-client`, and replacing `^int x` hinting
with explicit `(int x)` casts.

### http-client

The aws-api library defines a protocol
(`cognitect.aws.http/HttpClient`) to provide an interface between the
aws-api data transformation logic and the specific HTTP client
implementation. The aws-api includes an implementation for the
`com.cognitect/http-client` library. Interfaces are great: we can
provide our own implementations of the `cognitect.aws.http/HttpClient`
interface based on the various HTTP clients included in babashka.

#### java.net.http.HttpClient (Java 11, Java 12, Java 17)

The java.net.http package, introduced in Java 11, includes
java.net.http.HttpClient, a very nice http-client implementation. It's
also included in babashka. The aws-api sets the `host` header and the
host header in included in the signature for signed AWS requests.

The java.net.http.HttpClient considers `host` a restricted header and
does not allow it to be set. Java 12 added the
`jdk.httpclient.allowRestrictedHeaders` System property to allow
`host` to be set.

[aws-api-host-header]: https://github.com/cognitect-labs/aws-api/blob/79719242587b696acbd160c258d7935595bdc3d1/src/cognitect/aws/client.clj#L59

Currently, babashka is built using Java 11 GraalVM. Building a custom
babashaka binary using a Java 17 GraalVM 22.2.0. Currently this is
only available in a [dev build][graalvm-ce-dev-builds].

The `jdk.httpclient.allowRestrictedHeaders` property must be set at
JVM startup. It cannot be set at run-time after the JVM has
started. You can pass properties to babashka at the command line just
like you can to to `java`.

```bash
bb -Djdk.httpclient.allowRestrictedHeaders=host ...
```

[graalvm-ce-dev-builds]: https://github.com/graalvm/graalvm-ce-dev-builds/releases

That's not a very pretty solution, however, having to set a system
property at the command line.

The solution I've arrived at is to drop "host" from the headers in the
http-client itself and not set it. The java.net.http.HttpRequest
builder sets the host from the value in the URI, which is the value we
want anyway. The com.grzm.awyeah.client/Client instance builds a
request map including "host" in the headers, creates the appropriate
signture taking the "host" header into account, submits the request
map to the com.grzm.awyeah.http-client, and the http-client just
ignores the "host" header. The signature header included in the
request map is set, and the "host" header is set by the HttpRequest
builder.

## Other considerations

From a maintainability perspective, it would be easiest stop after
making the minimum changes for babashka compatibility. I could then
use text tools like `diff` to easily identify differences between the
source code in aws-api and awyeah-api.

### `cognitect.dynaload/load-var` and `requiring-resolve`

The aws-api library defines the `cognitect.dynaload/load-var` function
to dynamically require and resolve the var referenced by a given
symbol. Clojure 1.10 provides the same functionality with the
`requiring-resolve` function. Given that `requiring-resolve` is
compiled into the babashka image, I've chosen to replace `load-var`
with `requiring-resolve` rather than relying on sci to interpret
load-var at run-time.

### `clojure.data.json` and `cheshire`

The aws-api library depends on
[`clojure.data.json`][clojure.data.json] for JSON serializeation and
deserialization, a pure Clojure library. Babashka includes
[Cheshire][] for JSON support and not `clojure.data.json`.

[clojure.data.json]: https://github.com/clojure/data.json
[Cheshire]: https://github.com/dakrone/cheshire

The Clojure source of `clojure.data.json` can be interpreted by sci,
so I could include clojure.data.json as a dependency and use it
as-is. The clojure.data.json usage in aws-api is easily replaced by
Cheshire. Replacing clojure.data.json with Cheshire means one less
dependency to include, and we can leverage compiled code rather than
interpreted. To isolate the library choice, I've extracted the
library-specific calls in the `com.grzm.awyeah.json` namespace.

#### JSON-support injection

One thought would be to make the choice of JSON library a choice
available to the awyeah-api library user, defining a protocol for the
necessary functions. While interesting, that seems overkill for the
desired use case at this time.


### Linting and Formatting

With respect to making awyeah-api easy to update with respect to
upstream aws-api changes, it would make sense to limit the number of
differences between awyeah-api and aws-api to the minimum required to
make awyeah-api work with babashka. Changes such as whitespace
formatting or removing unused bindings, or reordering requires don't
affect the functionality of the code. However, these are important to
me as a developer from a fit and finish perspective.

Frankly, I do struggle a bit with whether I _should_ value these as
highly as I do, putting them above making maintenance as easy as
possible. That said, the impact of these changes should be minimal
with respect to maintainability, and one I'm willing to live with, at
least for now. I can always revisit this decision and with a little
work, reverse it.

It would be an interesting challenge to determine if I could—instead
of maintaining these changes by hand—write transformation rules that
not only include the changes necessary for babashka compatibility, but
also the formatting and linting changes I'd like. Perhaps someday.
