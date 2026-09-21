# Module lettermark-kotlin

The mark a thing gets when it has no picture of its own: the letters that go in
the square, and which of the palette's colour slots it is drawn on.

# Package pro.botforge.lettermark

`initials` reads the name a reader sees and answers the letters to draw. What
that means depends on the writing system, and the rules are kept in one place
for every port: a name written in two words gives two letters, one written in
Arabic or Han gives one, a Devanagari name gives its first written syllable
rather than a consonant with a halant hanging on it, and a letter whose
uppercase is longer than itself is left alone, so ß stays ß.

```kotlin
initials("Wiki Layer")   // "WL"
initials("中文维基")       // "中"
initials("क्षितिज")        // "क्षि"
```

A name with nothing drawable in it answers the empty string. The square is then
painted in the thing's colour with no letters in it, rather than with a "?"
this library invented.

`Palette` says how many colours you paint and which of them a thing is drawn
on. The colours themselves stay where the rest of your design lives — a colour
resource file with light and dark variants, a theme, a stylesheet — and the
palette only hands out the index to look up. The same id always answers the
same slot, so a thing keeps its colour between screens and between runs.
