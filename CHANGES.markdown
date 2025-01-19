# awyeah-api

## 0.8.84 / e551334 / 2023-12-02

* Now requires babashka v1.1.170 or later.
* No longer requires separate installation of babashka/spec.alpha (https://github.com/grzm/awyeah-api/issues/14).
  Thanks, Dan Meyers, for the report!
* Force HTTP client to use HTTP Version 1.1
  Fixes invalid request signing for AWS services that support HTTP 2 (https://github.com/grzm/awyeah-api/issues/8)
  Thanks, Josh Glover, for the detailed report!

## 0.8.82 / 5ecad02 / 2023-11-14

* Implement aws-api cached-credentials-with-auto-refresh

## 0.8.79 / 0399fec / 2023-11-12

* Catch up to aws-api 0.8.686 (2023-07-11)
* Also include aws-api refactoring commits up to 5900e35 (2023-07-12)

## 0.8.48 / 084f671 / 2023-09-25

* Fix spelling of :patch in http client implementation
  Thanks, Dan Meyers!

## 0.8.41 / 9257dc0 / 2022-10-13

* Catch up to aws-api 0.8.603 (2022-10-11)
* Port remainder of aws-api tests.

## 0.8.35 / 1810bf6 / 2022-10-08

* Catch up to aws-api 0.8.596.
* Fix handling of HEAD HTTP requests (such as S3 HeadObject).
* Fix broken client datafy compatibility.
* Use GMT instead of UTC for timestamp formatting.
