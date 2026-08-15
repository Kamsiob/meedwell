# Superseded reference files

**Historical only. Never build from anything in this folder.**

These are earlier visual directions for Meedwell. They are kept because the
reasoning in them is sometimes worth reading, and deleted history is a bad
habit. They are not the design.

The one and only authority is:

```
reference/meedwell-screen-grid-CURRENT.html
```

Its header reads **"Screen grid v2 · 25 screens · supersedes v1"**, it shows a
Surroundings tab, and it has **no ruled-paper background**. If a grid has faint
horizontal lines behind every screen, it is an old one and belongs in here.

---

## What is in here, and how it was identified

**`meedwell-screen-grid-final.html`** — superseded, despite the filename. This
is v1. Identified positively rather than by its name: it carries a
`repeating-linear-gradient` ruled-paper background behind every screen, which
the current grid does not have anywhere. The word "final" in the name is what
made it dangerous, and it is the reason this folder exists.

Nothing named `quiet-direction`, `dual-player` or `floating-control` was found
in this repository. If one turns up later, it goes in here.

---

## The rule

The HTML grid is the **measurement authority**, not `DESIGN.md`. Prose cannot
carry a measurement: "generous spacing" is not 22px, and "a hairline" is not
`1px rgba(28,36,32,.12)`. Where `DESIGN.md` and the grid disagree on any value,
the grid wins and `DESIGN.md` gets corrected.

Building from a translated prose description of a design is how the first build
drifted into a generic media player. Read the CSS.
