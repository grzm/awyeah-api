# awyeah-api

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
