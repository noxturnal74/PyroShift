PyroShift 🔥🛰️
One Fire History. Many Satellites. A Clearer Signal.
Team NUSA FIRE · NASA Space Apps Challenge 2026 · Surabaya, Indonesia
Challenge: Harmonization of MODIS and VIIRS Hot Spots

Explore the live PyroShift website · Project repository · NASA FIRMS
The problem
Wildfires and agricultural burning can damage ecosystems, disrupt communities, and worsen air quality. Satellites have recorded active-fire detections for more than two decades, but a long record does not automatically mean a directly comparable record.

NASA's MODIS and VIIRS instruments differ in spatial resolution, detection sensitivity, and observation characteristics. Combining their hotspot counts as if they represented identical measurements can create misleading impressions of changes in fire activity.

PyroShift addresses this challenge by making historical fire observations easier to inspect without hiding differences between sensors.
What is PyroShift?
PyroShift is a fire-observation exploration project developed by NUSA FIRE for NASA Space Apps Challenge 2026. Its web experience focuses on historical observations, interactive maps, sensor-specific comparisons, and fire-activity timelines. The longer-term scientific objective is a tested, reproducible cross-sensor harmonization method that supports a more consistent view of burning activity over time.

Indonesia, especially South Sumatra, is our starting case study—not the geographic limit of the idea.
Explore the web prototype
https://pyroshift.vercel.app/

The demonstrated web interface includes:
- An interactive map of historical fire detections and active grid cells.
- Geographic and time-period selection.
- Separate views for MODIS and VIIRS observations.
- Grid-cell inspection, a historical timeline, and CSV export.
- Data-coverage and scientific-limitation notices.
Important: A satellite hotspot detection is an observation, not necessarily a unique fire or a measure of burned area. This is a historical exploration prototype, not an operational fire forecast or emergency warning service.

A case study: South Sumatra, September 2019
The project's demonstrated September 2019 view displays:
Observation shown in the prototype	Value
MODIS detections	9,346
VIIRS detections	43,404
Active grid cells	1,133


VIIRS shows about 4.6 times as many detections as MODIS in this example. That does not mean 4.6 times as many fires occurred. The instruments observe burning activity differently, which is precisely why harmonization and careful interpretation matter.

These figures describe a specific captured pilot view; they are not global totals, validated harmonization results, or operational alerts.
Scientific approach
PyroShift's intended data-processing and validation workflow is:
1. Retrieve and document NASA FIRMS MODIS and VIIRS active-fire observations, including sensor, acquisition time, location, and available quality fields.
2. Audit data coverage by time and region. Missing or unprocessed data must never be silently treated as zero fires.
3. Normalize spatial and temporal units using a clearly documented grid and observation period, while retaining sensor identity.
4. Analyze overlap periods to study systematic differences between instruments.
5. Evaluate candidate calibration methods against transparent baseline methods on held-out periods or locations.
6. Calculate historical patterns and trends, reporting statistical significance and uncertainty only when supported by sufficient data.
7. Publish interpretable outputs such as sensor-specific views, fire-activity calendars, and downloadable results with provenance.
Scientific decision rule: If a candidate harmonization approach does not perform better than an appropriate baseline, PyroShift must keep the sensors separate rather than present an unsupported “unified” measurement.
Current scientific boundaries
The available project demonstrations support sensor-separated historical exploration. They do not, on their own, establish that cross-sensor calibration has been scientifically validated. Long-term trends, anomalies, and monitoring priorities should only be described as validated when their methods, coverage, and evaluation results are available.

The pilot interface has also shown partial coverage (a captured status of 50 out of 168 sensor-months processed for a selected study window). Unprocessed ≠ zero fire activity. Coverage should be checked again in the live application because processing may change.
Android project in this repository
This GitHub repository currently contains an Android/Kotlin project. The public PyroShift web experience is linked above; its full web/backend source code is not established by the contents visible at the repository root. Do not assume that cloning this repository also provides the complete Vercel website or data-processing pipeline.

The repository root includes these files and directories:
PyroShift/
├── .github/workflows/
├── app/
├── gradle/
├── .env.example
├── .gitignore
├── build.gradle.kts
├── gradle.properties
├── metadata.json
└── settings.gradle.kts
Getting started with the Android project
1. Clone the repository:
   git clone https://github.com/noxturnal74/PyroShift.git
   cd PyroShift
2. Open the repository root in Android Studio.
3. Allow Gradle to synchronize the project and install any Android SDK components required by the project configuration.
4. Inspect .env.example and the app configuration for any required local settings. Do not commit credentials or API keys. The repository's environment-loading behavior should be verified in code before copying values into any specific file.
5. Select an available emulator or physical Android device and run the app configuration.
Exact SDK versions, build variants, supported API levels, and environment variables should be taken from the checked-in Gradle and app files. They are intentionally not guessed here.

NASA data and resources
- NASA FIRMS — Fire Information for Resource Management System
- NASA FIRMS — Download and historical archive
- NASA FIRMS — Active-fire products and documentation
- NASA FIRMS — Area API
Data credit: NASA FIRMS, including MODIS and VIIRS active-fire observations. Each satellite product has its own detection characteristics and usage documentation.
Why this matters: SDGs
PyroShift is intended to support evidence-informed environmental monitoring and preparedness, contributing to:
Sustainable Development Goal	Intended contribution
SDG 13 — Climate Action	Improve access to historical information useful for resilience and preparedness research.
SDG 15 — Life on Land	Support analysis of burning activity relevant to forests and land ecosystems.
SDG 3 — Good Health and Well-Being	Inform research on fire-related exposure and smoke concerns; PyroShift does not measure health outcomes directly.
SDG 11 — Sustainable Cities and Communities	Make environmental information easier to examine for local and regional planning.


These are intended contributions, not claims that the prototype has already reduced fire damage, emissions, or illness.
Credits, transparency, and responsible use
- Project: PyroShift
- Team: NUSA FIRE
- Event: NASA Space Apps Challenge 2026 — Surabaya, Indonesia
- NASA observations: NASA FIRMS (MODIS/VIIRS)
- Web mapping imagery: Refer to attribution displayed in the live map (including Esri and its imagery contributors where applicable).
- Project development: Super Intelligence (SI) tools, including ChatGPT and Codex, assisted with planning, writing, and development workflows. Scientific claims and code require human review and testing. SI-generated images, video, code, or data must be disclosed and labeled as applicable under the challenge submission rules.
PyroShift is an independent participant project. NASA does not endorse this project, and NASA names, logos, and mission identifiers remain the property of their respective owners. Do not reuse NASA branding in SI-generated graphics.
Limitations and roadmap
Current limitations
- Sensor counts must not be added together or interpreted as equivalent fire counts.
- Satellite observations may miss burning because of cloud, smoke, overpass timing, and detection thresholds.
- Partial processing coverage limits comparisons across dates and regions.
- A promising calibration algorithm is not a validated algorithm until tested against baselines and independent or appropriately held-out evidence.
Next steps
- Expand and document historical data coverage.
- Implement, test, and report cross-sensor harmonization methods.
- Add reproducible trend analysis with statistical uncertainty.
- Improve geographic coverage and downloadable scientific documentation.
Links
- Live experience: https://pyroshift.vercel.app/
- Repository: https://github.com/noxturnal74/PyroShift
- NASA data: https://firms.modaps.eosdis.nasa.gov/
NUSA FIRE · From Indonesia, for clearer Earth observation worldwide.
