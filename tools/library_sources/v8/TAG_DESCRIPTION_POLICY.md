# GameReel v5 — tag description policy

- `label` is the atom name.
- `description` is optional. Missing description is better than generic filler.
- NEVER use reel help/prompt text as an item description.
- Text like “Decide what the player repeats...” belongs to `reel.help`, not to an atom.
- If an atom has no real description, UI should show only its label and source.
- Curated atoms use `frequency: 0`: they expand idea-space even if the source 1000-game library did not contain that exact atom.
- Source-derived atoms keep their real `frequency`, but random `weight` remains 1 by default.
