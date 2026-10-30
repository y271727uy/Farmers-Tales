# HarvestHeritage Crop Stand Notice

The crop stand texture and block model originate from HarvestHeritage by
theabab2333, using the local reference snapshot at
`refer/HarvestHeritage-main/HarvestHeritage-main`:

- `src/main/resources/assets/harvestheritage/textures/block/crop_stand_block.png`
- `src/main/resources/assets/harvestheritage/models/block/crop_stand_block.json`

Imported resources in Farmer's Tales:

- `assets/farmerstales/textures/block/crop_stand.png` (unchanged texture)
- `assets/farmerstales/models/block/crop_stand.json` (adapted model)

The model's texture references were changed to the Farmer's Tales namespace,
the newer editor format marker was removed, and degenerate faces referencing
the undefined `#missing` texture were omitted. Geometry, UVs, and cutout rendering
were retained. The basic block uses the reference stand's selection bounds and
has no collision. Cultivation behavior is not imported.

Copyright (c) 2026 theabab2333. The reference snapshot includes the MIT License;
its full text is included in the built jar at
`META-INF/licenses/HarvestHeritage-MIT.txt`.
