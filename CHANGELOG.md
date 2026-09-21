# Changelog

## 0.2.0

### Added

- The Kotlin port. `initials(name)` gives the letters drawn in a square for a
  thing with no picture, and `Palette(slots).slot(id)` gives which of your
  colours it is drawn on.

  ```kotlin
  val letters = initials(name)

  val marks = Palette(slots = 12)  // however many colours your theme paints
  val slot = marks.slot(id)        // 0 … marks.slots - 1
  ```

  The first release is 0.2.0 rather than 0.1.0 because the ports share their
  first two numbers: this one answers the same rules as
  [lettermark](https://github.com/botforge-pro/lettermark) 0.2.0, the Go
  repository the ports follow, and a matching `0.2` says the behaviour is the
  same wherever you read it.

- Its own extended grapheme cluster segmentation, from a table generated out
  of the Unicode 16.0 character database. The platform's segmenters answer
  differently in a unit test and on a phone, and bundling ICU4J costs around
  11 MB; this table is 4.5 KB and answers all 1093 cases of the Unicode 16.0
  `GraphemeBreakTest`, which runs as part of the test suite.

- A copy of `cases.yaml`, the contract, and a test that compares it with
  [lettermark](https://github.com/botforge-pro/lettermark) — the Go repository
  this is a port of, and the one every port follows — byte for byte. Without
  the network that test fails rather than passing quietly, because a corpus
  nobody could read proves nothing. It runs in this repository's own suite, so
  it is not something a consumer of the published artifact ever runs.
